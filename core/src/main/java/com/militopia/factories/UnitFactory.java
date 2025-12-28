package com.militopia.factories;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.militopia.components.*;
import com.militopia.map.MapGenerator; // For TerrainType

public class UnitFactory {

    private final PooledEngine engine;

    // --- TEXTURE REGIONS ---
    private final TextureRegion recruitRightRegion;
    private final TextureRegion recruitLeftRegion;
    
    private final TextureRegion recruitDisplayRegion;

    // Terrain
    private final TextureRegion grassRegion;
    private final TextureRegion waterRegion;
    private final TextureRegion deepWaterRegion;
    private final TextureRegion sandRegion;
    private final TextureRegion mountainRegion;

    // Objects (NEW!)
    private final TextureRegion treeRegion;
    private final TextureRegion ruinsRegion;
    private final TextureRegion baseP1Region;
    private final TextureRegion baseP2Region;
    private final TextureRegion townRegion;
    private final TextureRegion oilRegion;
    private final TextureRegion cactusRegion;
    private final TextureRegion mountainObjRegion;

    public UnitFactory(PooledEngine engine) {
        this.engine = engine;

        // 1. Load Unit
        this.recruitRightRegion = new TextureRegion(new Texture("recruit_right.png"));
        this.recruitLeftRegion = new TextureRegion(new Texture("recruit_left.png"));
        
        // Display Unit
        this.recruitDisplayRegion = new TextureRegion(new Texture("display_recruit.png"));

        // 2. Load Terrain (Ensure these files exist!)
        this.grassRegion = new TextureRegion(new Texture("tile_grass.png"));
        this.waterRegion = new TextureRegion(new Texture("tile_water.png"));
        this.deepWaterRegion = new TextureRegion(new Texture("tile_deepwater.png"));
        this.sandRegion = new TextureRegion(new Texture("tile_sand.png"));
        this.mountainRegion = new TextureRegion(new Texture("tile_mountain.png"));

        // 3. Load Objects (Ensure these files exist!)
        // Use your actual file names here. If you don't have one, reuse a placeholder.
        this.treeRegion = new TextureRegion(new Texture("obj_tree.png"));
        this.ruinsRegion = new TextureRegion(new Texture("obj_ruins.png"));
        this.baseP1Region = new TextureRegion(new Texture("struct_base_blue.png"));
        this.baseP2Region = new TextureRegion(new Texture("struct_base_red.png"));
        this.townRegion = new TextureRegion(new Texture("struct_town.png"));
        this.oilRegion = new TextureRegion(new Texture("obj_oil.png"));
        this.cactusRegion = new TextureRegion(new Texture("obj_cactus.png"));
        this.mountainObjRegion = new TextureRegion(new Texture("obj_mountain.png"));

    }

    public UiInfo getTerrainUi(MapGenerator.TerrainType type) {
        switch (type) {
            case WATER:
                return new UiInfo("Shallow Water", waterRegion);
            case DEEP_WATER:
                return new UiInfo("Deep Ocean", deepWaterRegion);
            case SAND:
                return new UiInfo("Desert", sandRegion);
            case MOUNTAIN:
                return new UiInfo("Mountain Range", mountainRegion);
            default:
                return new UiInfo("Grassland", grassRegion);
        }
    }

    public UiInfo getObjectUi(MapGenerator.ObjectType type) {
        switch (type) {
            case BASE_P1:
                return new UiInfo("Blue Base", baseP1Region);
            case BASE_P2:
                return new UiInfo("Red Base", baseP2Region);
            case TOWN:
                return new UiInfo("Town", townRegion);
            case TREE:
                return new UiInfo("Oak Tree", treeRegion);
            case RUINS:
                return new UiInfo("Ancient Ruins", ruinsRegion);
            case OIL:
                return new UiInfo("Oil Reservoir", oilRegion);
            case CACTUS:
                return new UiInfo("Cactus", cactusRegion);
            case MOUNTAIN_OBJ:
                return new UiInfo("Mountain", mountainObjRegion); // New Case
            default:
                return new UiInfo("Unknown Object", grassRegion);
        }
    }

    public UiInfo getUnitUi(String unitType) {
        // You can expand this switch when you add Tanks/Planes
        if ("RECRUIT".equals(unitType)) {
            return new UiInfo("Infantry Recruit", recruitDisplayRegion);
        }
        return new UiInfo("Unknown Unit", recruitDisplayRegion);
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

    // Simple container for HUD data
    public static class UiInfo {

        public String name;
        public TextureRegion region;

        public UiInfo(String name, TextureRegion region) {
            this.name = name;
            this.region = region;
        }
    }

    public void createObjectEntity(int x, int y, MapGenerator.ObjectType type) {
        UiInfo info = getObjectUi(type);
        if (info.region == null) {
            return;
        }

        Entity entity = engine.createEntity();

        // 1. Position (Z-Index 1: Between floor and units)
        // Note: The ZComparator will prioritize Y-sorting, so zIndex is just a tie-breaker.
        entity.add(new GridPositionComponent(x, y, 1));

        // 2. Texture
        entity.add(new TextureComponent(info.region));

        // 3. Type (Important for the RenderSystem to not crash)
        // We reuse 'MARKER' or 'UNIT' for now if you don't have an 'OBJECT' enum in TypeComponent.
        // It's safer to add a dummy type so the renderer knows it's not a Unit (no HP bar, etc).
        // Assuming you have Type.MARKER, we use that to treat it as a "prop".
        entity.add(new TypeComponent(TypeComponent.Type.OBJECT));

        // 4. Stats (Optional) - Add this if you want to click it and see a name!
        // We use owner=0 (Neutral)
        entity.add(new StatsComponent(info.name, 0, 0, 0, StatsComponent.MoveType.LAND, 0));

        engine.addEntity(entity);
    }
}
