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
import com.militopia.utils.ZComparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnitRenderSystem extends EntitySystem {

    private SpriteBatch batch;
    private ImmutableArray<Entity> entities;
    private ZComparator comparator;

    // State for Highlighting
    private int selectedX = -1, selectedY = -1;
    private int bouncingX = -1, bouncingY = -1;
    private float bounceTimer = 0;

    public UnitRenderSystem(SpriteBatch batch) {
        this.batch = batch;
        this.comparator = new ZComparator();
        this.priority = 1; // Runs after MapRenderSystem
    }

    // Call this from GameScreen to sync mouse position
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
        // 1. Sort Entities (Back to Front)
        List<Entity> sortedEntities = new ArrayList<>();
        for (Entity e : entities) {
            sortedEntities.add(e);
        }
        Collections.sort(sortedEntities, comparator);

        batch.begin(); // <--- Start
        
        // 2. Draw Loop
        for (Entity e : sortedEntities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TextureComponent tex = e.getComponent(TextureComponent.class);
            MovementComponent move = e.getComponent(MovementComponent.class);
            TypeComponent typeC = e.getComponent(TypeComponent.class);
            StatsComponent stats = e.getComponent(StatsComponent.class);

            float isoX, isoY;

            // --- ANIMATION CALCULATION ---
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

            // --- DRAWING ---
            float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
            float yOffset = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;

            boolean isMarker = (typeC.type == TypeComponent.Type.MARKER);
            float verticalOffset = isMarker ? 5f : 10f;

            // --- COLOR TINTING LOGIC ---            
            if (typeC.type == TypeComponent.Type.MARKER) {
                // FORCE MARKERS TO ALWAYS BE WHITE
                batch.setColor(Color.WHITE);
            } else if (stats != null && stats.owner == 2) {
                // Tint Red ONLY if it is a Player 2 Unit
                batch.setColor(1.0f, 0.6f, 0.6f, 1.0f);
            } else if (stats != null && stats.owner == 1) {
                // Tint Blue for Player 1
                batch.setColor(0.6f, 0.6f, 1.0f, 1.0f);
            } else {
                // Default White for Player 1 Units
                batch.setColor(Color.WHITE);
            }

            // Draw Normal Unit
            batch.draw(tex.region,
                    isoX - xOffset,
                    isoY - yOffset + verticalOffset + animY,
                    GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);

            batch.setColor(Color.WHITE);

            // --- HIGHLIGHT LOGIC (NEW) ---
            // We only highlight UNITs (not markers) and only if mouse is over them
            if (!isMarker && pos.x == selectedX && pos.y == selectedY) {
                Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
                batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE);
                batch.setColor(0.4f, 0.4f, 0.4f, 1f); // Grey Glow

                batch.draw(tex.region,
                        isoX - xOffset,
                        isoY - yOffset + verticalOffset + animY,
                        GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);

                batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
                batch.setColor(Color.WHITE);
            }
        }
        
        batch.end(); // <--- End
    }
}
