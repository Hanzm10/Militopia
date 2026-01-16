package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
    private ImmutableArray<Entity> entities;
    private ZComparator comparator;

    private final MapGenerator.GameMap gameMap;
    private boolean fogEnabled = true;

    // --- NEW: Track Active Player ---
    private int activePlayer = 1;

    private int selectedX = -1, selectedY = -1;
    private int bouncingX = -1, bouncingY = -1;
    private float bounceTimer = 0;

    public UnitRenderSystem(SpriteBatch batch, MapGenerator.GameMap map) {
        this.batch = batch;
        this.gameMap = map;
        this.comparator = new ZComparator();
        this.priority = 1;
    }

    // --- NEW: Setter for Turn Switching ---
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
        List<Entity> sortedEntities = new ArrayList<>();
        for (Entity e : entities) {
            sortedEntities.add(e);
        }
        Collections.sort(sortedEntities, comparator);

        batch.begin();

        for (Entity e : sortedEntities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TextureComponent tex = e.getComponent(TextureComponent.class);
            MovementComponent move = e.getComponent(MovementComponent.class);
            TypeComponent typeC = e.getComponent(TypeComponent.class);
            StatsComponent stats = e.getComponent(StatsComponent.class);

            boolean isMarker = (typeC.type == TypeComponent.Type.MARKER);

            // --- VISIBILITY CHECK ---
            if (fogEnabled) {
                if (!gameMap.visibleTiles[pos.x][pos.y]) {

                    // FIX: Only show units if they belong to the ACTIVE PLAYER
                    // Enemy units and neutral objects in fog will now be hidden.
                    boolean isMyUnit = (stats != null && stats.owner == activePlayer);

                    // Draw if it's MY Unit OR a Movement Marker
                    if (!isMyUnit && !isMarker) {
                        continue;
                    }
                }
            }
            // ------------------------

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
                // NEW: Check if exhausted (and not in testing mode)
                if (!GameConfig.TESTING_MODE && stats != null && stats.hasActed) {
                    batch.setColor(Color.DARK_GRAY); // Darken unit to show it's done
                } // Existing Owner Logic
                else if (stats != null && stats.owner == 2) {
                    batch.setColor(1.0f, 0.6f, 0.6f, 1.0f); // Red Tint
                } else if (stats != null && stats.owner == 1) {
                    batch.setColor(0.6f, 0.6f, 1.0f, 1.0f); // Blue Tint
                } else {
                    batch.setColor(Color.WHITE);
                }
            } else {
                batch.setColor(Color.WHITE);
            }

            batch.draw(tex.region,
                    isoX - xOffset,
                    isoY - yOffset + verticalOffset + animY,
                    GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);

            batch.setColor(Color.WHITE);

            if (!isMarker && pos.x == selectedX && pos.y == selectedY) {
                Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
                batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE);
                batch.setColor(0.4f, 0.4f, 0.4f, 1f);

                batch.draw(tex.region,
                        isoX - xOffset,
                        isoY - yOffset + verticalOffset + animY,
                        GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);

                batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
                batch.setColor(Color.WHITE);
            }
        }
        batch.end();
    }
}
