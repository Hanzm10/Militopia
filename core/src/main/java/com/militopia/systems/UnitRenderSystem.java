package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.militopia.components.*;
import com.militopia.config.GameConfig;
import com.militopia.config.UnitType;
import com.militopia.map.MapGenerator;
import com.militopia.utils.ZComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnitRenderSystem extends EntitySystem {

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private ImmutableArray<Entity> entities;
    private ZComparator comparator;

    private final MapGenerator.GameMap gameMap;
    private boolean fogEnabled = true;
    private int activePlayer = 1;

    private int selectedX = -1, selectedY = -1;
    private int bouncingX = -1, bouncingY = -1;
    private float bounceTimer = 0;

    // Reusable removal list (avoids per-frame allocation accumulation)
    private final Array<Entity> toRemove = new Array<>();

    public UnitRenderSystem(SpriteBatch batch, MapGenerator.GameMap map, BitmapFont font) {
        this.batch = batch;
        this.gameMap = map;
        this.font = font;
        this.comparator = new ZComparator();
        this.shapeRenderer = new ShapeRenderer();
        this.priority = 1;
    }

    public void setPlayer(int playerID) {
        this.activePlayer = playerID;
    }

    public void setFogEnabled(boolean enabled) {
        this.fogEnabled = enabled;
    }

    public void updateState(int selX, int selY, int bX, int bY, float bTimer) {
        this.selectedX = selX;
        this.selectedY = selY;
        this.bouncingX = bX;
        this.bouncingY = bY;
        this.bounceTimer = bTimer;
    }

    @Override
    public void addedToEngine(com.badlogic.ashley.core.Engine engine) {
        entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class)
                .one(TextureComponent.class, SpriteAnimationComponent.class).get());
    }

    @Override
    public void update(float deltaTime) {
        // Sorted sprite pass
        List<Entity> sorted = new ArrayList<>();
        for (Entity e : entities)
            sorted.add(e);
        Collections.sort(sorted, comparator);

        batch.begin();
        for (Entity e : sorted) {
            drawEntity(e, deltaTime);
        }
        batch.end();

        drawBaseOverlay();
        drawFloatingTexts(deltaTime);

        // Remove dead + expired floating-text entities (done after all rendering)
        for (Entity e : toRemove) {
            getEngine().removeEntity(e);
        }
        toRemove.clear();
    }

    // -------------------------------------------------------------------------
    // Entity drawing
    // -------------------------------------------------------------------------

    private void drawEntity(Entity e, float deltaTime) {
        GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
        TextureComponent tex = e.getComponent(TextureComponent.class);
        MovementComponent move = e.getComponent(MovementComponent.class);
        TypeComponent typeC = e.getComponent(TypeComponent.class);
        StatsComponent stats = e.getComponent(StatsComponent.class);
        DeathAnimComponent death = e.getComponent(DeathAnimComponent.class);
        SpriteAnimationComponent spriteAnim = e.getComponent(SpriteAnimationComponent.class);

        boolean isMarker = (typeC.type == TypeComponent.Type.MARKER);
        boolean isAttackMarker = (typeC.type == TypeComponent.Type.ATTACK_MARKER);

        // Fog and Stealth culling
        if (!isMarker && !isAttackMarker) {
            boolean isMyUnit = (stats != null && stats.owner == activePlayer);

            if (fogEnabled) {
                if (!gameMap.visibleTiles[pos.x][pos.y] && !isMyUnit) {
                    return;
                }
            }

            // Stealth check (Enemy view only)
            if (!isMyUnit && stats != null) {
                AbilitiesComponent abilities = e.getComponent(AbilitiesComponent.class);
                boolean isStealth = (abilities != null && abilities.isCloaked);

                // Sniper Camouflage: Grass/Forest/Mountain?
                // The prompt says "forest/mountains". We use Tree objects or Mountain terrain.
                if (stats.unitType == UnitType.SNIPER) {
                    MapGenerator.TerrainType terrain = gameMap.terrain[pos.x][pos.y];
                    MapGenerator.ObjectType obj = gameMap.objects[pos.x][pos.y];
                    if (terrain == MapGenerator.TerrainType.MOUNTAIN || obj == MapGenerator.ObjectType.TREE
                            || obj == MapGenerator.ObjectType.RUINS) {
                        isStealth = true;
                    }
                }

                // If unit has already acted/attacked, it is revealed
                if (stats.hasActed) {
                    isStealth = false;
                }

                if (isStealth && !gameMap.detectedTiles[pos.x][pos.y]) {
                    // Hidden!
                    return;
                }
            }
        }

        // --- Death animation ---
        if (death != null) {
            death.timer += deltaTime;
            if (death.timer >= DeathAnimComponent.DURATION) {
                death.done = true;
                toRemove.add(e);
                return; // don't draw after fully faded
            }
            float progress = death.timer / DeathAnimComponent.DURATION;
            float alpha = 1f - progress;
            // Flash red: alternate between red and white every 0.1s
            boolean flashRed = ((int) (death.timer / 0.1f) % 2 == 0);
            if (flashRed) {
                batch.setColor(1f, 0f, 0f, alpha);
            } else {
                batch.setColor(1f, 1f, 1f, alpha);
            }
            float isoX = (pos.x - pos.y) * (GameConfig.TILE_WIDTH / 2.0f);
            float isoY = (pos.x + pos.y) * (GameConfig.TILE_HEIGHT / 2.0f);
            float xOff = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
            float yOff = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;
            batch.draw(tex.region, isoX - xOff, isoY - yOff + 10f, GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);
            batch.setColor(Color.WHITE);
            return;
        }

        // --- Iso position (with movement lerp) ---
        float isoX, isoY;
        if (move != null) {
            float alpha = Math.min(move.time / move.duration, 1.0f);
            float startIsoX = (move.startX - move.startY) * (GameConfig.TILE_WIDTH / 2.0f);
            float startIsoY = (move.startX + move.startY) * (GameConfig.TILE_HEIGHT / 2.0f);
            float endIsoX = (move.targetX - move.targetY) * (GameConfig.TILE_WIDTH / 2.0f);
            float endIsoY = (move.targetX + move.targetY) * (GameConfig.TILE_HEIGHT / 2.0f);
            isoX = MathUtils.lerp(startIsoX, endIsoX, alpha) + pos.visualOffsetX;
            isoY = MathUtils.lerp(startIsoY, endIsoY, alpha) + pos.visualOffsetY;
        } else {
            isoX = (pos.x - pos.y) * (GameConfig.TILE_WIDTH / 2.0f) + pos.visualOffsetX;
            isoY = (pos.x + pos.y) * (GameConfig.TILE_HEIGHT / 2.0f) + pos.visualOffsetY;
        }

        // Bounce animation
        float animY = 0;
        if (move == null && pos.x == bouncingX && pos.y == bouncingY) {
            float progress = bounceTimer / GameConfig.BOUNCE_DURATION;
            animY = (float) Math.sin(progress * Math.PI) * GameConfig.BOUNCE_HEIGHT;
        }

        float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
        float yOffset = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;
        float verticalOff = isMarker || isAttackMarker ? 5f : 10f;

        // --- Colour tinting ---
        if (isAttackMarker) {
            // New enemy marker (draw with original colours)
            batch.setColor(Color.WHITE);
        } else if (isMarker) {
            batch.setColor(Color.WHITE);
        } else if (typeC.type == TypeComponent.Type.UNIT) {
            if (!GameConfig.TESTING_MODE && stats != null && stats.hasActed) {
                if (stats.owner == 1)
                    batch.setColor(0.3f, 0.3f, 0.6f, 1.0f);
                else if (stats.owner == 2)
                    batch.setColor(0.6f, 0.3f, 0.3f, 1.0f);
                else
                    batch.setColor(Color.DARK_GRAY);
            } else if (stats != null && stats.owner == 2) {
                batch.setColor(1.0f, 0.6f, 0.6f, 1.0f);
            } else if (stats != null && stats.owner == 1) {
                batch.setColor(0.6f, 0.6f, 1.0f, 1.0f);
            } else {
                batch.setColor(Color.WHITE);
            }
        } else {
            batch.setColor(Color.WHITE);
        }

        if (tex != null && tex.region != null) {
            batch.draw(tex.region,
                    isoX - xOffset,
                    isoY - yOffset + verticalOff + animY,
                    GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);
        }
        
        // --- Sprite Animation (Overlay) ---
        if (spriteAnim != null && spriteAnim.animation != null) {
            com.badlogic.gdx.graphics.g2d.TextureRegion frame = spriteAnim.animation.getKeyFrame(spriteAnim.stateTime, spriteAnim.loop);
            if (frame != null) {
                float dW = (spriteAnim.drawWidth > 0) ? spriteAnim.drawWidth : GameConfig.DRAW_WIDTH;
                float dH = (spriteAnim.drawHeight > 0) ? spriteAnim.drawHeight : GameConfig.DRAW_HEIGHT;
                
                // Center the sprite relative to the 16x10 tile size
                float currentXOffset = (dW - GameConfig.TILE_WIDTH) / 2f;
                float currentYOffset = (dH - GameConfig.TILE_HEIGHT) / 2f;

                batch.draw(frame,
                        isoX - currentXOffset + spriteAnim.worldOffsetX,
                        isoY - currentYOffset + verticalOff + animY + spriteAnim.worldOffsetY,
                        dW, dH);
            }
        }

        batch.setColor(Color.WHITE);

        // Selection glow
        if (tex != null && !isMarker && !isAttackMarker && pos.x == selectedX && pos.y == selectedY) {
            Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
            batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE);
            batch.setColor(0.4f, 0.4f, 0.4f, 1f);
            batch.draw(tex.region,
                    isoX - xOffset,
                    isoY - yOffset + verticalOff + animY,
                    GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);
            batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
            batch.setColor(Color.WHITE);
        }
    }

    // -------------------------------------------------------------------------
    // Floating damage text
    // -------------------------------------------------------------------------

    private void drawFloatingTexts(float deltaTime) {
        ImmutableArray<Entity> floaters = getEngine().getEntitiesFor(
                Family.all(FloatingTextComponent.class).get());

        if (floaters.size() == 0)
            return;

        batch.begin();

        float originalScaleX = font.getData().scaleX;
        float originalScaleY = font.getData().scaleY;
        font.getData().setScale(0.22f);

        for (Entity e : floaters) {
            FloatingTextComponent ft = e.getComponent(FloatingTextComponent.class);

            // Colour based on text type
            switch (ft.type) {
                case BLOCKED:
                    font.setColor(1f, 0.85f, 0f, ft.alpha); // Gold
                    break;
                case XP:
                    font.setColor(0.2f, 0.8f, 1f, ft.alpha); // Light Blue/Cyan
                    break;
                case FUNDING:
                    font.setColor(0.2f, 1f, 0.2f, ft.alpha); // Green
                    break;
                case DAMAGE:
                default:
                    font.setColor(1f, 0.2f, 0.2f, ft.alpha); // Red
                    break;
            }

            GlyphLayout layout = new GlyphLayout(font, ft.text);
            font.draw(batch, ft.text,
                    ft.worldX - layout.width / 2f,
                    ft.worldY);
        }

        font.getData().setScale(originalScaleX, originalScaleY);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // -------------------------------------------------------------------------
    // Base overlay (unchanged from original)
    // -------------------------------------------------------------------------

    private void drawBaseOverlay() {
        if (selectedX == -1 || selectedY == -1)
            return;
        Entity selectedEntity = null;
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            if (pos.x == selectedX && pos.y == selectedY) {
                if (e.getComponent(TypeComponent.class).type == TypeComponent.Type.UNIT) {
                    selectedEntity = e;
                    break;
                }
                selectedEntity = e;
            }
        }
        if (selectedEntity != null) {
            StatsComponent stats = selectedEntity.getComponent(StatsComponent.class);
            TypeComponent type = selectedEntity.getComponent(TypeComponent.class);
            if (stats != null && type.type == TypeComponent.Type.OBJECT
                    && (stats.owner == 1 || stats.owner == 2) && stats.name.contains("Base")) {
                float isoX = (selectedX - selectedY) * (GameConfig.TILE_WIDTH / 2.0f);
                float isoY = (selectedX + selectedY) * (GameConfig.TILE_HEIGHT / 2.0f);
                float xOff = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
                float yOff = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;
                float dX = isoX - xOff + GameConfig.DRAW_WIDTH / 2f;
                float dY = isoY - yOff + 15;
                drawXPBar(dX, dY - 8, stats);
                // drawBaseName removed per user request
            }
        }
    }

    private void drawXPBar(float x, float y, StatsComponent stats) {
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float width = 32f, height = 4f, radius = 2f;
        float progress = MathUtils.clamp(stats.currentBaseXP / stats.maxBaseXP, 0, 1);
        shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
        drawRoundedRect(x - width / 2, y, width, height, radius);
        if (progress > 0) {
            shapeRenderer.setColor(stats.owner == 1 ? new Color(0.2f, 0.4f, 1f, 1f) : new Color(1f, 0.2f, 0.2f, 1f));
            float barWidth = Math.max(width * progress, 2f);
            drawRoundedRect(x - width / 2, y, barWidth, height, radius);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);
    }

    private void drawRoundedRect(float x, float y, float width, float height, float radius) {
        float r = Math.min(radius, Math.min(width / 2, height / 2));
        shapeRenderer.rect(x + r, y + r, width - 2 * r, height - 2 * r);
        shapeRenderer.rect(x + r, y, width - 2 * r, r);
        shapeRenderer.rect(x + width - r, y + r, r, height - 2 * r);
        shapeRenderer.rect(x + r, y + height - r, width - 2 * r, r);
        shapeRenderer.rect(x, y + r, r, height - 2 * r);
        shapeRenderer.arc(x + r, y + r, r, 180f, 90f, 16);
        shapeRenderer.arc(x + width - r, y + r, r, 270f, 90f, 16);
        shapeRenderer.arc(x + width - r, y + height - r, r, 0f, 90f, 16);
        shapeRenderer.arc(x + r, y + height - r, r, 90f, 90f, 16);
    }

    private void drawBaseName(float x, float y, String name) {
        batch.begin();
        float ox = font.getData().scaleX, oy = font.getData().scaleY;
        font.getData().setScale(0.25f);
        GlyphLayout layout = new GlyphLayout(font, name);
        font.draw(batch, name, x - layout.width / 2, y);
        font.getData().setScale(ox, oy);
        batch.end();
    }
}
