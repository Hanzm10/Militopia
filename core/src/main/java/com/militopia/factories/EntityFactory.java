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
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> recruitAttackAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> tankAttackAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> nuclearAttackAnim;
    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> gunNozzleFlashAnim;

    public EntityFactory(PooledEngine engine, AssetManager assets) {
        this.engine = engine;
        this.markerRegion = new TextureRegion(assets.get(AssetManager.MARKER_DOT));
        this.enemyMarkerRegion = new TextureRegion(assets.get(AssetManager.ENEMY_MARKER));

        // --- Placeholders for animations ---
        TextureRegion placeholder = new TextureRegion(assets.get(AssetManager.MARKER_DOT));
        explosionAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.1f, placeholder);
        hitAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(0.05f, enemyMarkerRegion);

        // --- New Attack Animations ---
        recruitAttackAnim = createAnim(assets, AssetManager.ATTACK_RECRUIT_FRAMES, 0.08f);
        tankAttackAnim = createAnim(assets, AssetManager.ATTACK_TANK_FRAMES, 0.06f);
        nuclearAttackAnim = createAnim(assets, AssetManager.ATTACK_NUCLEAR_FRAMES, 0.07f);
        gunNozzleFlashAnim = createAnim(assets, AssetManager.GUN_NOZZLE_FLASH_FRAMES, 0.05f);
    }

    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> createAnim(AssetManager assets, String[] paths,
            float frameDuration) {
        com.badlogic.gdx.utils.Array<TextureRegion> frames = new com.badlogic.gdx.utils.Array<>();
        for (String path : paths) {
            frames.add(new TextureRegion(assets.get(path)));
        }
        return new com.badlogic.gdx.graphics.g2d.Animation<>(frameDuration, frames);
    }

    /** Blue movement-range marker. */
    public void createMovementMarker(int x, int y) {
        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 3));
        entity.add(new TextureComponent(markerRegion));
        entity.add(new TypeComponent(TypeComponent.Type.MARKER));
        engine.addEntity(entity);
    }

    public void createTransformMarker(int x, int y) {
        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 3));
        entity.add(new TextureComponent(markerRegion));
        entity.add(new TypeComponent(TypeComponent.Type.TRANSFORM_MARKER));
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
        anim.drawWidth = 9f;
        anim.drawHeight = 10f;
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

    public void createRecruitAttack(int x, int y) {
        // Recruit is a standard 18x20 tile size
        createEffect(x, y, recruitAttackAnim, 0.6f, 12f, 14f);
    }

    public void createTankAttack(int x, int y) {
        // Tank is wider: 24x18
        createEffect(x, y, tankAttackAnim, 0.8f, 24f, 18f, 0f, 5f);
    }

    public void createNuclearAttack(int x, int y) {
        // Nuclear (Super Units) is much larger: 40x40
        createEffect(x, y, nuclearAttackAnim, 0.9f, 30f, 30f, 0f, 13f);
    }

    public void createDramaticExplosion(int x, int y) {
        // 1. Main large explosion on target tile
        createNuclearAttack(x, y);

        // 2. Adjacent little explosions on THE SAME TILE (approx 0.4s delay)
        // Lowered y-offsets as requested (from 10f base to 6f base)
        float[][] offsets = {
                { -4f, -2f }, { 4f, -2f }, { -4f, 2f }, { 4f, 2f }
        };
        for (float[] off : offsets) {
            createDelayedSmallExplosion(x, y, off[0], off[1] + 6f);
        }

        // 3. Surrounding tile explosions (all 8 neighbors in the 3x3 damage scope)
        int[][] neighbors = {
                { x + 1, y }, { x - 1, y }, { x, y + 1 }, { x, y - 1 },
                { x + 1, y + 1 }, { x + 1, y - 1 }, { x - 1, y + 1 }, { x - 1, y - 1 }
        };
        for (int[] n : neighbors) {
            // Smaller explosions on surrounding tiles with same delay
            createDelayedSmallExplosion(n[0], n[1], 0, 6f);
        }
    }

    private void createDelayedSmallExplosion(int x, int y, float offsetX, float offsetY) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 4));
        e.add(new TypeComponent(TypeComponent.Type.OBJECT));

        SpriteAnimationComponent anim = new SpriteAnimationComponent();
        anim.animation = nuclearAttackAnim;
        anim.duration = 0.9f;
        anim.drawWidth = 12f; // Smaller scale (40%)
        anim.drawHeight = 12f;
        anim.worldOffsetX = offsetX;
        anim.worldOffsetY = offsetY;
        anim.stateTime = -0.4f; // 0.4s delay
        anim.autoRemove = true;
        e.add(anim);

        engine.addEntity(e);
    }

    public void createMuzzleFlash(int x, int y, float offsetX, float offsetY) {
        // Muzzle flash is very small: 3x3
        createEffect(x, y, gunNozzleFlashAnim, 0.45f, 3f, 3f, offsetX, offsetY);
    }

    private void createEffect(int x, int y, com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> animation,
            float duration, float width, float height) {
        createEffect(x, y, animation, duration, width, height, 0, 0);
    }

    private void createEffect(int x, int y, com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> animation,
            float duration, float width, float height, float offsetX, float offsetY) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 4));
        e.add(new TypeComponent(TypeComponent.Type.OBJECT));

        SpriteAnimationComponent anim = new SpriteAnimationComponent();
        anim.animation = animation;
        anim.duration = duration;
        anim.drawWidth = width;
        anim.drawHeight = height;
        anim.worldOffsetX = offsetX;
        anim.worldOffsetY = offsetY;
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