package com.militopia.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.militopia.config.GameConfig;
import com.militopia.map.MapGenerator;
import com.militopia.MilitopiaGame;

public class MapRenderSystem extends EntitySystem {

    private final SpriteBatch batch;
    private final MilitopiaGame game;
    private final MapGenerator.GameMap gameMap;
    private ShapeRenderer shapeRenderer;

    // State Variables (Updated every frame from GameScreen)
    private int selectedX, selectedY;
    private int bouncingX, bouncingY;
    private float bounceTimer;
    private final String p1Name;
    private final String p2Name;

    public MapRenderSystem(SpriteBatch batch, MilitopiaGame game, MapGenerator.GameMap map, String p1, String p2) {
        this.batch = batch;
        this.game = game;
        this.gameMap = map;
        this.p1Name = p1;
        this.p2Name = p2;
        this.shapeRenderer = new ShapeRenderer();

        // IMPORTANT: Set priority to 0 so it runs FIRST (Bottom Layer)
        this.priority = 0;
    }

    // Call this from GameScreen before engine.update()
    public void updateState(int selX, int selY, int bX, int bY, float bTimer) {
        this.selectedX = selX;
        this.selectedY = selY;
        this.bouncingX = bX;
        this.bouncingY = bY;
        this.bounceTimer = bTimer;
    }

    @Override
    public void update(float deltaTime) {
        // Calculate Global Offsets once per frame
        float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
        float yOffset = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;

        // --- PASS 1: TERRAIN (Batch is ALREADY open from GameScreen) ---
        for (int x = gameMap.width - 1; x >= 0; x--) {
            for (int y = gameMap.height - 1; y >= 0; y--) {
                drawTerrainTile(x, y, xOffset, yOffset);
            }
        }

        // --- PASS 2: BORDERS (Switch to ShapeRenderer) ---
        batch.end();
        renderBordersPass();
        batch.begin();

        // --- PASS 3: OBJECTS (Switch back to Batch) ---
        for (int x = gameMap.width - 1; x >= 0; x--) {
            for (int y = gameMap.height - 1; y >= 0; y--) {
                drawObjectTile(x, y, xOffset, yOffset);
            }
        }
    }

    // ========================================================================
    //                          HELPER METHODS
    // ========================================================================
    private void drawTerrainTile(int x, int y, float xOffset, float yOffset) {
        float[] coords = getIsoCoords(x, y);
        float isoX = coords[0];
        float isoY = coords[1];
        float animY = coords[2];

        // 1. Get Texture
        Texture t = null;
        switch (gameMap.terrain[x][y]) {
            case GRASS:
                t = game.texGrass;
                break;
            case WATER:
                t = game.texWater;
                break;
            case DEEP_WATER:
                t = game.texDeepWater;
                break;
            case SAND:
                t = game.texSand;
                break;
            case FOREST:
                t = game.texForest;
                break;
        }

        // 2. Draw Base Terrain
        if (t != null) {
            batch.draw(t, isoX - xOffset, isoY - yOffset + animY, GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);
        }

        // 3. Draw Floor Highlight (If selected)
        if (x == selectedX && y == selectedY && t != null) {
            drawHighlight(t, isoX - xOffset, isoY - yOffset + animY);
        }
    }

    private void renderBordersPass() {
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int x = gameMap.width - 1; x >= 0; x--) {
            for (int y = gameMap.height - 1; y >= 0; y--) {
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

    private void drawObjectTile(int x, int y, float xOffset, float yOffset) {
        float[] coords = getIsoCoords(x, y);
        float isoX = coords[0];
        float isoY = coords[1];
        float animY = coords[2];

        // 1. Get Object Texture
        Texture o = null;
        switch (gameMap.objects[x][y]) {
            case BASE_P1:
                o = game.texBaseP1;
                break;
            case BASE_P2:
                o = game.texBaseP2;
                break;
            case BASE_NEUTRAL:
                o = game.texBaseNeutral;
                break;
            case TREE:
                o = game.texTree;
                break;
            case RUINS:
                o = game.texRuins;
                break;
            case OIL:
                o = game.texOil;
                break;
            case CACTUS:
                o = game.texCactus;
                break;
        }

        if (o != null) {
            float objOffsetX = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
            float surfaceLift = 15f;
            float drawX = isoX - objOffsetX;
            float drawY = isoY - yOffset + surfaceLift + animY;

            // 2. Draw Object
            batch.draw(o, drawX, drawY, GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);

            // 3. Draw Object Highlight (If selected) - NEW!
            if (x == selectedX && y == selectedY) {
                drawHighlight(o, drawX, drawY);
            }
        }
    }

    // --- SHARED UTILITIES ---
    private float[] getIsoCoords(int x, int y) {
        float isoX = (x - y) * (GameConfig.TILE_WIDTH / 2.0f);
        float isoY = (x + y) * (GameConfig.TILE_HEIGHT / 2.0f);
        float animY = 0;

        if (x == bouncingX && y == bouncingY) {
            float progress = bounceTimer / GameConfig.BOUNCE_DURATION;
            animY = (float) Math.sin(progress * Math.PI) * GameConfig.BOUNCE_HEIGHT;
        }
        return new float[]{isoX, isoY, animY};
    }

    private void drawHighlight(Texture t, float x, float y) {
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE);
        batch.setColor(0.4f, 0.4f, 0.4f, 1f); // Glow Intensity
        batch.draw(t, x, y, GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);
        batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    private void drawSmartBorders(int x, int y, int currentOwner) {
        // 1. CALCULATE CENTER
        float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
        float isoX = (x - y) * (GameConfig.TILE_WIDTH / 2.0f);
        float isoY = (x + y) * (GameConfig.TILE_HEIGHT / 2.0f);

        float drawX = isoX - xOffset;
        float centerX = drawX + (GameConfig.DRAW_WIDTH / 2f);
        float surfaceLift = 15f;
        float centerY = isoY + surfaceLift;

        // 2. DIMENSIONS
        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;

        // 3. VERTICES
        float topX = centerX;
        float topY = centerY + halfH;
        float rightX = centerX + halfW;
        float rightY = centerY;
        float botX = centerX;
        float botY = centerY - halfH;
        float leftX = centerX - halfW;
        float leftY = centerY;

        // 4. SETTINGS
        float thick = 3.0f;
        float jointSize = thick / 2f; // Radius of the corner cap

        // 5. CHECK NEIGHBORS & DRAW LINES + JOINTS
        // --- TOP-LEFT EDGE (North Neighbor) ---
        if (getTileOwner(x, y + 1) != currentOwner) {
            shapeRenderer.rectLine(leftX, leftY, topX, topY, thick);
            // Add caps to both ends of this line
            shapeRenderer.circle(leftX, leftY, jointSize);
            shapeRenderer.circle(topX, topY, jointSize);
        }

        // --- TOP-RIGHT EDGE (East Neighbor) ---
        if (getTileOwner(x + 1, y) != currentOwner) {
            shapeRenderer.rectLine(rightX, rightY, topX, topY, thick);
            shapeRenderer.circle(rightX, rightY, jointSize);
            shapeRenderer.circle(topX, topY, jointSize);
        }

        // --- BOTTOM-RIGHT EDGE (South Neighbor) ---
        if (getTileOwner(x, y - 1) != currentOwner) {
            shapeRenderer.rectLine(rightX, rightY, botX, botY, thick);
            shapeRenderer.circle(rightX, rightY, jointSize);
            shapeRenderer.circle(botX, botY, jointSize);
        }

        // --- BOTTOM-LEFT EDGE (West Neighbor) ---
        if (getTileOwner(x - 1, y) != currentOwner) {
            shapeRenderer.rectLine(leftX, leftY, botX, botY, thick);
            shapeRenderer.circle(leftX, leftY, jointSize);
            shapeRenderer.circle(botX, botY, jointSize);
        }
    }

    // Check if this tile is owned by a player (Radius 1 around bases)
    private int getTileOwner(int x, int y) {
        // Bounds check
        if (x < 0 || x >= GameConfig.MAP_WIDTH || y < 0 || y >= GameConfig.MAP_HEIGHT) {
            return 0;
        }

        // 1. Check the tile itself
        if (isBase(x, y, 1)) {
            return 1;
        }
        if (isBase(x, y, 2)) {
            return 2;
        }

        // 2. Check Neighbors (Radius 1)
        // If a neighbor is a base, then THIS tile is part of its territory
        for (int i = -GameConfig.BORDER_RADIUS; i <= GameConfig.BORDER_RADIUS; i++) {
            for (int j = -GameConfig.BORDER_RADIUS; j <= GameConfig.BORDER_RADIUS; j++) {
                int nx = x + i;
                int ny = y + j;
                if (nx >= 0 && nx < GameConfig.MAP_WIDTH && ny >= 0 && ny < GameConfig.MAP_HEIGHT) {
                    if (isBase(nx, ny, 1)) {
                        return 1;
                    }
                    if (isBase(nx, ny, 2)) {
                        return 2;
                    }
                }
            }
        }
        return 0;
    }

    // Helper to check specific base
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
