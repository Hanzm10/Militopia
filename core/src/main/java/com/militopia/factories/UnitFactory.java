package com.militopia.factories;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.militopia.components.*;
import com.militopia.map.MapGenerator; // For TerrainType

public class UnitFactory {

    private final PooledEngine engine;

    // --- 1. STORE TEXTURE REGIONS ---
    private final TextureRegion recruitLeftRegion;
    private final TextureRegion recruitRightRegion;

    // Terrain Textures
    private final TextureRegion grassRegion;
    private final TextureRegion waterRegion;
    private final TextureRegion deepWaterRegion;

    public UnitFactory(PooledEngine engine) {
        this.engine = engine;

        // --- 2. LOAD TEXTURES ---
        // Unit Assets
        Texture texRight = new Texture("recruit_right.png");
        Texture texLeft = new Texture("recruit_left.png");
        this.recruitRightRegion = new TextureRegion(texRight);
        this.recruitLeftRegion = new TextureRegion(texLeft);

        // Terrain Assets (Assuming these file names exist in your assets folder!)
        // Make sure you actually have these .png files
        this.grassRegion = new TextureRegion(new Texture("tile_grass.png"));
        this.waterRegion = new TextureRegion(new Texture("tile_water.png"));
        this.deepWaterRegion = new TextureRegion(new Texture("tile_deepwater.png"));
    }

    public void createRecruit(int x, int y, int owner) {
        Entity entity = engine.createEntity();

        GridPositionComponent pos = new GridPositionComponent(x, y, 2);
        entity.add(pos);

        TextureComponent tex = new TextureComponent(recruitRightRegion);
        entity.add(tex);

        FacingComponent facing = new FacingComponent(recruitLeftRegion, recruitRightRegion);
        entity.add(facing);

        TypeComponent type = new TypeComponent(TypeComponent.Type.UNIT);
        entity.add(type);

        StatsComponent stats = new StatsComponent("Recruit", 1, 10, 5, StatsComponent.MoveType.LAND, owner);
        entity.add(stats);

        engine.addEntity(entity);
    }

    // --- 3. FIXED HELPER METHOD ---
    public TextureRegion getTextureForTerrain(int terrainId) {
        // 1. Safety Check (Prevent crashing if ID is weird)
        MapGenerator.TerrainType[] allTypes = MapGenerator.TerrainType.values();
        if (terrainId < 0 || terrainId >= allTypes.length) {
            return grassRegion; // Default fallback
        }

        // 2. CONVERT INT -> ENUM
        // This restores the "Meaning" of the number
        MapGenerator.TerrainType type = allTypes[terrainId];

        // 3. SEMANTIC COMPARISON
        // Now you can compare Enum to Enum safely!
        if (type == MapGenerator.TerrainType.WATER) {
            return waterRegion;
        } else if (type == MapGenerator.TerrainType.DEEP_WATER) {
            return deepWaterRegion;
        } else {
            return grassRegion;
        }
    }
}
