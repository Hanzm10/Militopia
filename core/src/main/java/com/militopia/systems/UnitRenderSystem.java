package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.militopia.config.GameConfig;
import com.militopia.MilitopiaGame;
import com.militopia.components.*;
import com.militopia.utils.ZComparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnitRenderSystem extends EntitySystem {
    private SpriteBatch batch;
    private ImmutableArray<Entity> entities;
    private ZComparator comparator;

    public UnitRenderSystem(SpriteBatch batch) {
        this.batch = batch;
        this.comparator = new ZComparator();
        this.priority = 1;
    }

    @Override
    public void addedToEngine(com.badlogic.ashley.core.Engine engine) {
        entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class, TextureComponent.class).get());
    }

    @Override
    public void update(float deltaTime) {
        // 1. Copy entities to a list so we can sort them
        List<Entity> sortedEntities = new ArrayList<>();
        for (Entity e : entities) sortedEntities.add(e);
        Collections.sort(sortedEntities, comparator);

        // 2. Draw them
        for (Entity e : sortedEntities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TextureComponent tex = e.getComponent(TextureComponent.class);
            MovementComponent move = e.getComponent(MovementComponent.class);
            TypeComponent typeC = e.getComponent(TypeComponent.class);

            float isoX, isoY;

            // --- ANIMATION LERP ---
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

            // --- OFFSETS ---
            float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
            float yOffset = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;
            boolean isMarker = (typeC.type == TypeComponent.Type.MARKER);
            float verticalOffset = isMarker ? 7.5f : 15f;

            // Draw
            batch.draw(tex.region, 
                       isoX - xOffset, 
                       isoY - yOffset + verticalOffset, 
                       GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);
        }
    }
}