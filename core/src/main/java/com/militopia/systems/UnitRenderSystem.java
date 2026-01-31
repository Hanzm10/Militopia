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
import com.militopia.components.*;
import com.militopia.config.GameConfig;
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
    private ZComparator comparator; // Ensure this is initialized

    private final MapGenerator.GameMap gameMap;
    private boolean fogEnabled = true;
    private int activePlayer = 1;

    private int selectedX = -1, selectedY = -1;
    private int bouncingX = -1, bouncingY = -1;
    private float bounceTimer = 0;

    public UnitRenderSystem(SpriteBatch batch, MapGenerator.GameMap map, BitmapFont font) {
        this.batch = batch;
        this.gameMap = map;
        this.font = font;
        this.comparator = new ZComparator(); // Initialized
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
        entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class, TextureComponent.class).get());
    }

    @Override
    public void update(float deltaTime) {
        // Sort entities by Z and Depth
        List<Entity> sortedEntities = new ArrayList<>();
        for (Entity e : entities) {
            sortedEntities.add(e);
        }
        Collections.sort(sortedEntities, comparator); // Use ZComparator

        batch.begin();
        for (Entity e : sortedEntities) {
            drawEntity(e);
        }
        batch.end();

        drawBaseOverlay();
    }

    // (Rest of the class methods drawBaseOverlay, drawEntity, etc. remain exactly as they were in your uploaded file)
    private void drawBaseOverlay() {
        if (selectedX == -1 || selectedY == -1) {
            return;
        }
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
            if (stats != null && type.type == TypeComponent.Type.OBJECT && (stats.owner == 1 || stats.owner == 2) && stats.income > 0) {
                float isoX = (selectedX - selectedY) * (GameConfig.TILE_WIDTH / 2.0f);
                float isoY = (selectedX + selectedY) * (GameConfig.TILE_HEIGHT / 2.0f);
                float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
                float yOffset = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;
                float drawX = isoX - xOffset + GameConfig.DRAW_WIDTH / 2f;
                float drawY = isoY - yOffset + 15;
                drawXPBar(drawX, drawY - 8, stats);
                drawBaseName(drawX, drawY + 20, stats.name);
            }
        }
    }

    private void drawXPBar(float x, float y, StatsComponent stats) {
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float width = 32f;
        float height = 4f;
        float radius = 2f;
        float progress = MathUtils.clamp(stats.currentBaseXP / stats.maxBaseXP, 0, 1);
        shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
        drawRoundedRect(x - width / 2, y, width, height, radius);
        if (progress > 0) {
            if (stats.owner == 1) {
                shapeRenderer.setColor(0.2f, 0.4f, 1.0f, 1f);
            } else {
                shapeRenderer.setColor(1.0f, 0.2f, 0.2f, 1f);
            }
            float barWidth = Math.max(width * progress, radius * 2);
            if (width * progress < radius * 2) {
                barWidth = width * progress;
            }
            drawRoundedRect(x - width / 2, y, Math.max(barWidth, 2f), height, radius);
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
        float originalScaleX = font.getData().scaleX;
        float originalScaleY = font.getData().scaleY;
        font.getData().setScale(0.35f);
        GlyphLayout layout = new GlyphLayout(font, name);
        font.draw(batch, name, x - layout.width / 2, y);
        font.getData().setScale(originalScaleX, originalScaleY);
        batch.end();
    }

    private void drawEntity(Entity e) {
        GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
        TextureComponent tex = e.getComponent(TextureComponent.class);
        MovementComponent move = e.getComponent(MovementComponent.class);
        TypeComponent typeC = e.getComponent(TypeComponent.class);
        StatsComponent stats = e.getComponent(StatsComponent.class);
        boolean isMarker = (typeC.type == TypeComponent.Type.MARKER);
        if (fogEnabled) {
            if (!gameMap.visibleTiles[pos.x][pos.y]) {
                boolean isMyUnit = (stats != null && stats.owner == activePlayer);
                if (!isMyUnit && !isMarker) {
                    return;
                }
            }
        }
        float isoX, isoY;
        if (move != null) {
            float alpha = Math.min(move.time / move.duration, 1.0f);
            float startIsoX = (move.startX - move.startY) * (GameConfig.TILE_WIDTH / 2.0f);
            float startIsoY = (move.startX + move.startY) * (GameConfig.TILE_HEIGHT / 2.0f);
            float endIsoX = (move.targetX - move.targetY) * (GameConfig.TILE_WIDTH / 2.0f);
            float endIsoY = (move.targetX + move.targetY) * (GameConfig.TILE_HEIGHT / 2.0f);
            isoX = MathUtils.lerp(startIsoX, endIsoX, alpha);
            isoY = MathUtils.lerp(startIsoY, endIsoY, alpha);
        } else {
            isoX = (pos.x - pos.y) * (GameConfig.TILE_WIDTH / 2.0f);
            isoY = (pos.x + pos.y) * (GameConfig.TILE_HEIGHT / 2.0f);
        }
        float animY = 0;
        if (move == null && pos.x == bouncingX && pos.y == bouncingY) {
            float progress = bounceTimer / GameConfig.BOUNCE_DURATION;
            animY = (float) Math.sin(progress * Math.PI) * GameConfig.BOUNCE_HEIGHT;
        }
        float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
        float yOffset = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;
        float verticalOffset = isMarker ? 5f : 10f;
        if (isMarker) {
            batch.setColor(Color.WHITE);
        } else if (typeC.type == TypeComponent.Type.UNIT) {
            if (!GameConfig.TESTING_MODE && stats != null && stats.hasActed) {
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
        batch.draw(tex.region, isoX - xOffset, isoY - yOffset + verticalOffset + animY, GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);
        batch.setColor(Color.WHITE);
        if (!isMarker && pos.x == selectedX && pos.y == selectedY) {
            Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
            batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE);
            batch.setColor(0.4f, 0.4f, 0.4f, 1f);
            batch.draw(tex.region, isoX - xOffset, isoY - yOffset + verticalOffset + animY, GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);
            batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
            batch.setColor(Color.WHITE);
        }
    }
}
