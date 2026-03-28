package com.militopia.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.militopia.config.GameConfig;
import com.militopia.config.StructureType;
import com.militopia.factories.UnitFactory;
import com.militopia.map.MapGenerator;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import java.util.ArrayList;
import java.util.List;

public class MapRenderSystem extends EntitySystem {

    private final SpriteBatch batch;
    private final UnitFactory unitFactory;
    private final MapGenerator.GameMap gameMap;
    private ShapeRenderer shapeRenderer;

    private int selectedX, selectedY;
    private int bouncingX, bouncingY;
    private float bounceTimer;

    private boolean fogEnabled = true;

    private static class BaseInfo {
        int x, y, owner, radius;
    }

    private final List<BaseInfo> activeBases = new ArrayList<>();

    public MapRenderSystem(SpriteBatch batch, UnitFactory factory, MapGenerator.GameMap map) {
        this.batch = batch;
        this.unitFactory = factory;
        this.gameMap = map;
        this.shapeRenderer = new ShapeRenderer();
        this.priority = 0;
    }

    public void updateState(int selX, int selY, int bX, int bY, float bTimer) {
        this.selectedX = selX;
        this.selectedY = selY;
        this.bouncingX = bX;
        this.bouncingY = bY;
        this.bounceTimer = bTimer;
    }

    public void setFogEnabled(boolean enabled) {
        this.fogEnabled = enabled;
    }

    @Override
    public void update(float deltaTime) {
        // --- Cache active bases for territory calculation ---
        activeBases.clear();
        ImmutableArray<Entity> baseEntities = getEngine().getEntitiesFor(
                Family.all(GridPositionComponent.class, StatsComponent.class, TypeComponent.class).get());
        for (Entity e : baseEntities) {
            TypeComponent t = e.getComponent(TypeComponent.class);
            if (t.type != TypeComponent.Type.OBJECT)
                continue;

            StatsComponent s = e.getComponent(StatsComponent.class);
            if (StructureType.fromDisplayName(s.name) == StructureType.BASE && s.income >= 2) {
                GridPositionComponent p = e.getComponent(GridPositionComponent.class);
                BaseInfo bi = new BaseInfo();
                bi.x = p.x;
                bi.y = p.y;
                bi.owner = s.owner;
                bi.radius = s.vision;
                activeBases.add(bi);
            }
        }

        float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
        float yOffset = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;

        batch.begin();
        for (int x = gameMap.width - 1; x >= 0; x--) {
            for (int y = gameMap.height - 1; y >= 0; y--) {
                drawTerrainTile(x, y, xOffset, yOffset);
            }
        }
        batch.end();
        renderBordersPass();
    }

    private void drawTerrainTile(int x, int y, float xOffset, float yOffset) {
        float[] coords = getIsoCoords(x, y);
        float isoX = coords[0];
        float isoY = coords[1];
        float animY = coords[2];

        TextureRegion regionToDraw = null;

        boolean isVisible = gameMap.visibleTiles[x][y];

        if (fogEnabled && !isVisible) {
            regionToDraw = unitFactory.fogRegion;
        } else {
            regionToDraw = unitFactory.getTextureForTerrain(gameMap.terrain[x][y].ordinal());
        }

        if (regionToDraw != null) {
            batch.draw(regionToDraw, isoX - xOffset, isoY - yOffset + animY, GameConfig.DRAW_WIDTH,
                    GameConfig.DRAW_HEIGHT);
        }

        if (x == selectedX && y == selectedY && regionToDraw != null) {
            drawHighlight(regionToDraw, isoX - xOffset, isoY - yOffset + animY);
        }
    }

    private void renderBordersPass() {
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // --- UPDATED: Use dynamic width/height ---
        for (int x = gameMap.width - 1; x >= 0; x--) {
            for (int y = gameMap.height - 1; y >= 0; y--) {
                if (fogEnabled && !gameMap.visibleTiles[x][y]) {
                    continue;
                }

                int owner = getTileOwner(x, y);
                if (owner != 0) {
                    if (owner == 1) {
                        shapeRenderer.setColor(Color.BLUE);
                    } else if (owner == 2) {
                        shapeRenderer.setColor(Color.RED);
                    }
                    drawSmartBorders(x, y, owner);
                }
            }
        }
        shapeRenderer.end();
    }

    private float[] getIsoCoords(int x, int y) {
        float isoX = (x - y) * (GameConfig.TILE_WIDTH / 2.0f);
        float isoY = (x + y) * (GameConfig.TILE_HEIGHT / 2.0f);
        float animY = 0;
        if (x == bouncingX && y == bouncingY) {
            float progress = bounceTimer / GameConfig.BOUNCE_DURATION;
            animY = (float) Math.sin(progress * Math.PI) * GameConfig.BOUNCE_HEIGHT;
        }
        return new float[] { isoX, isoY, animY };
    }

    private void drawHighlight(TextureRegion t, float x, float y) {
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE);
        batch.setColor(0.4f, 0.4f, 0.4f, 1f);
        batch.draw(t, x, y, GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);
        batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    private void drawSmartBorders(int x, int y, int currentOwner) {
        float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
        float isoX = (x - y) * (GameConfig.TILE_WIDTH / 2.0f);
        float isoY = (x + y) * (GameConfig.TILE_HEIGHT / 2.0f);
        float drawX = isoX - xOffset;
        float centerX = drawX + (GameConfig.DRAW_WIDTH / 2f);
        float surfaceLift = 10f;
        float centerY = isoY + surfaceLift;
        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;
        float topX = centerX;
        float topY = centerY + halfH;
        float rightX = centerX + halfW;
        float rightY = centerY;
        float botX = centerX;
        float botY = centerY - halfH;
        float leftX = centerX - halfW;
        float leftY = centerY;
        float thick = 2.0f;
        float jointSize = thick / 2f;

        if (getTileOwner(x, y + 1) != currentOwner) {
            shapeRenderer.rectLine(leftX, leftY, topX, topY, thick);
            shapeRenderer.circle(leftX, leftY, jointSize);
            shapeRenderer.circle(topX, topY, jointSize);
        }
        if (getTileOwner(x + 1, y) != currentOwner) {
            shapeRenderer.rectLine(rightX, rightY, topX, topY, thick);
            shapeRenderer.circle(rightX, rightY, jointSize);
            shapeRenderer.circle(topX, topY, jointSize);
        }
        if (getTileOwner(x, y - 1) != currentOwner) {
            shapeRenderer.rectLine(rightX, rightY, botX, botY, thick);
            shapeRenderer.circle(rightX, rightY, jointSize);
            shapeRenderer.circle(botX, botY, jointSize);
        }
        if (getTileOwner(x - 1, y) != currentOwner) {
            shapeRenderer.rectLine(leftX, leftY, botX, botY, thick);
            shapeRenderer.circle(leftX, leftY, jointSize);
            shapeRenderer.circle(botX, botY, jointSize);
        }
    }

    private int getTileOwner(int x, int y) {
        if (x < 0 || x >= gameMap.width || y < 0 || y >= gameMap.height) {
            return 0;
        }

        // Check against cached bases
        for (BaseInfo bi : activeBases) {
            int dx = Math.abs(bi.x - x);
            int dy = Math.abs(bi.y - y);
            if (dx <= bi.radius && dy <= bi.radius) {
                return bi.owner;
            }
        }

        return 0;
    }

    private boolean isBase(int x, int y, int player) {
        MapGenerator.ObjectType obj = gameMap.objects[x][y];
        if (player == 1) {
            return obj == MapGenerator.ObjectType.BASE_P1;
        }
        if (player == 2) {
            return obj == MapGenerator.ObjectType.BASE_P2;
        }
        return false;
    }
}
