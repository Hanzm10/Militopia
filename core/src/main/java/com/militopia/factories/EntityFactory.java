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
    private TextureRegion enemyMarkerRegion;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> explosionAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> hitAnim;

    public EntityFactory(PooledEngine engine, AssetManager assets) {
        this.engine = engine;
        this.markerRegion = new TextureRegion(assets.get(AssetManager.MARKER_DOT));
        this.enemyMarkerRegion = new TextureRegion(assets.get(AssetManager.ENEMY_MARKER));
        
        // --- Placeholders for animations ---
        TextureRegion placeholder = new TextureRegion(assets.get(AssetManager.MARKER_DOT));
        explosionAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.1f, placeholder);
        hitAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.05f, enemyMarkerRegion);
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
     * Red attack-range marker. Rendered by UnitRenderSystem.
     * Uses the new enemy_marker texture.
     */
    public void createAttackMarker(int x, int y) {
        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 3));
        entity.add(new TextureComponent(enemyMarkerRegion));
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
    public Entity createFloatingText(String text, float worldX, float worldY, FloatingTextComponent.Type type) {
        Entity entity = engine.createEntity();
        entity.add(new FloatingTextComponent(text, worldX, worldY, type));
        engine.addEntity(entity);
        return entity;
    }

    public void createExplosion(int x, int y) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 4)); // Above units
        e.add(new TypeComponent(TypeComponent.Type.OBJECT));
        e.add(new TextureComponent(markerRegion)); // Placeholder base
        
        SpriteAnimationComponent anim = new SpriteAnimationComponent();
        anim.animation = explosionAnim;
        anim.duration = 0.5f;
        anim.autoRemove = true;
        e.add(anim);
        
        engine.addEntity(e);
    }

    public void createHit(int x, int y) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 4));
        e.add(new TypeComponent(TypeComponent.Type.OBJECT));
        e.add(new TextureComponent(markerRegion));
        
        SpriteAnimationComponent anim = new SpriteAnimationComponent();
        anim.animation = hitAnim;
        anim.duration = 0.2f;
        anim.autoRemove = true;
        e.add(anim);
        
        engine.addEntity(e);
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