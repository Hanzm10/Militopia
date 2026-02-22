package com.militopia.factories;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.militopia.components.*;
import com.militopia.config.GameConfig;
import com.militopia.managers.AssetManager;

public class EntityFactory {
    private PooledEngine engine;
    private TextureRegion markerRegion;

    public EntityFactory(PooledEngine engine, AssetManager assets) {
        this.engine = engine;
        this.markerRegion = new TextureRegion(assets.get(AssetManager.MARKER_DOT));
    }

    /** Blue movement-range marker. */
    public void createMovementMarker(int x, int y) {
        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 3));
        entity.add(new TextureComponent(markerRegion));
        entity.add(new TypeComponent(TypeComponent.Type.MARKER));
        engine.addEntity(entity);
    }

    /**
     * Red attack-range marker. Rendered by UnitRenderSystem with a red tint.
     * Uses the same dot texture; colour is applied at draw time.
     */
    public void createAttackMarker(int x, int y) {
        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 3));
        entity.add(new TextureComponent(markerRegion));
        entity.add(new TypeComponent(TypeComponent.Type.ATTACK_MARKER));
        engine.addEntity(entity);
    }

    /**
     * A short-lived entity that carries floating combat feedback.
     *
     * @param text      damage amount or "BLOCKED"
     * @param worldX    iso-world X of the target tile
     * @param worldY    iso-world Y of the target tile (base; system drifts up)
     * @param isCounter true when this feedback represents a counterattack
     */
    public Entity createFloatingText(String text, float worldX, float worldY, boolean isCounter) {
        Entity entity = engine.createEntity();
        entity.add(new FloatingTextComponent(text, worldX, worldY, isCounter));
        engine.addEntity(entity);
        return entity;
    }

    // -------------------------------------------------------------------------
    // Internal helpers for world-space coordinate conversion
    // -------------------------------------------------------------------------

    /** Convert grid coords to isometric world X. */
    public static float gridToIsoX(int gx, int gy) {
        return (gx - gy) * (GameConfig.TILE_WIDTH / 2.0f);
    }

    /**
     * Convert grid coords to isometric world Y.
     * The +20 offset lifts the text above the unit sprite.
     */
    public static float gridToIsoY(int gx, int gy) {
        float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
        float yOffset = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;
        return (gx + gy) * (GameConfig.TILE_HEIGHT / 2.0f) - yOffset + 20f;
    }
}