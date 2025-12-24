package com.militopia.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.militopia.config.GameConfig;
import com.militopia.map.MapGenerator;
import com.militopia.MilitopiaGame;

public class MapRenderSystem extends EntitySystem {
    private final SpriteBatch batch;
    private final MilitopiaGame game;
    private final MapGenerator.GameMap gameMap;

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
        // Common offsets
        float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
        float yOffset = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;

        // Loop purely for Terrain and Static Objects
        for (int x = gameMap.width - 1; x >= 0; x--) {
            for (int y = gameMap.height - 1; y >= 0; y--) {
                float isoX = (x - y) * (GameConfig.TILE_WIDTH / 2.0f);
                float isoY = (x + y) * (GameConfig.TILE_HEIGHT / 2.0f);

                float animY = 0;
                if (x == bouncingX && y == bouncingY) {
                    float progress = bounceTimer / GameConfig.BOUNCE_DURATION;
                    animY = (float) Math.sin(progress * Math.PI) * GameConfig.BOUNCE_HEIGHT;
                }

                // A. Draw Terrain
                Texture t = null;
                switch (gameMap.terrain[x][y]) {
                    case GRASS: t = game.texGrass; break;
                    case WATER: t = game.texWater; break;
                    case DEEP_WATER: t = game.texDeepWater; break;
                    case SAND: t = game.texSand; break;
                    case FOREST: t = game.texForest; break;
                }
                if (t != null) batch.draw(t, isoX - xOffset, isoY - yOffset + animY, GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);

                // B. Draw Objects
                Texture o = null;
                switch (gameMap.objects[x][y]) {
                    case BASE_P1: o = game.texBaseP1; break;
                    case BASE_P2: o = game.texBaseP2; break;
                    case BASE_NEUTRAL: o = game.texBaseNeutral; break;
                    case TREE: o = game.texTree; break;
                    case RUINS: o = game.texRuins; break;
                    case OIL: o = game.texOil; break;
                    case CACTUS: o = game.texCactus; break;
                }
                
                if (o != null) {
                     float objOffsetX = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f; 
                     float surfaceLift = 15f; 
                     batch.draw(o, isoX - objOffsetX, isoY - yOffset + surfaceLift + animY, GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);
                }

                // C. Selection Highlight
                if (x == selectedX && y == selectedY) {
                    Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
                    batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE);
                    batch.setColor(0.4f, 0.4f, 0.4f, 1f);
                    if (t != null) batch.draw(t, isoX - xOffset, isoY - yOffset + animY, GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);
                    batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
                    batch.setColor(Color.WHITE);
                }
            }
        }
    }
}