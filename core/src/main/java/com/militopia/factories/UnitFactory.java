package com.militopia.factories;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.militopia.components.*;
import com.militopia.config.GameConfig; // Import GameConfig
import com.militopia.managers.AssetManager;
import com.militopia.map.MapGenerator;

public class UnitFactory {

    private final PooledEngine engine;
    private final AssetManager assets;

    // Regions
    private final TextureRegion recruitRightRegion;
    private final TextureRegion recruitLeftRegion;
    private final TextureRegion recruitDisplayRegion;
    private final TextureRegion grassRegion;
    private final TextureRegion waterRegion;
    private final TextureRegion deepWaterRegion;
    private final TextureRegion sandRegion;
    private final TextureRegion mountainRegion;
    private final TextureRegion treeRegion;
    private final TextureRegion ruinsRegion;
    private final TextureRegion baseP1Region;
    private final TextureRegion baseP2Region;
    private final TextureRegion townRegion;
    private final TextureRegion oilRegion;
    private final TextureRegion cactusRegion;
    private final TextureRegion mountainObjRegion;
    public final TextureRegion fogRegion;

    public UnitFactory(PooledEngine engine, AssetManager assets) {
        this.engine = engine;
        this.assets = assets;

        this.recruitRightRegion = new TextureRegion(assets.get(AssetManager.RECRUIT_RIGHT));
        this.recruitLeftRegion = new TextureRegion(assets.get(AssetManager.RECRUIT_LEFT));
        this.recruitDisplayRegion = new TextureRegion(assets.get(AssetManager.RECRUIT_DISPLAY));

        this.grassRegion = new TextureRegion(assets.get(AssetManager.TILE_GRASS));
        this.waterRegion = new TextureRegion(assets.get(AssetManager.TILE_WATER));
        this.deepWaterRegion = new TextureRegion(assets.get(AssetManager.TILE_DEEPWATER));
        this.sandRegion = new TextureRegion(assets.get(AssetManager.TILE_SAND));
        this.mountainRegion = new TextureRegion(assets.get(AssetManager.TILE_MOUNTAIN));

        this.treeRegion = new TextureRegion(assets.get(AssetManager.OBJ_TREE));
        this.ruinsRegion = new TextureRegion(assets.get(AssetManager.OBJ_RUINS));
        this.baseP1Region = new TextureRegion(assets.get(AssetManager.STRUCT_BASE_BLUE));
        this.baseP2Region = new TextureRegion(assets.get(AssetManager.STRUCT_BASE_RED));
        this.townRegion = new TextureRegion(assets.get(AssetManager.STRUCT_TOWN));
        this.oilRegion = new TextureRegion(assets.get(AssetManager.OBJ_OIL));
        this.cactusRegion = new TextureRegion(assets.get(AssetManager.OBJ_CACTUS));
        this.mountainObjRegion = new TextureRegion(assets.get(AssetManager.OBJ_MOUNTAIN));

        this.fogRegion = new TextureRegion(assets.get(AssetManager.FOG_OF_WAR));
    }

    public void createUnit(String unitType, int x, int y, int owner) {
        switch (unitType) {
            case "RECRUIT":
                createRecruit(x, y, owner);
                break;
            default:
                Gdx.app.error("UnitFactory", "Unknown unit type: " + unitType);
        }
    }

    private void createRecruit(int x, int y, int owner) {
        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 2));
        entity.add(new TextureComponent(recruitRightRegion));
        entity.add(new FacingComponent(recruitLeftRegion, recruitRightRegion));
        entity.add(new TypeComponent(TypeComponent.Type.UNIT));
        entity.add(new StatsComponent("Recruit", 1, 10, 5, 1, StatsComponent.MoveType.LAND, owner));
        engine.addEntity(entity);
    }

    public void createObjectEntity(int x, int y, MapGenerator.ObjectType type) {
        UiInfo info = getObjectUi(type);
        if (info.region == null) {
            return;
        }

        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 1));
        entity.add(new TextureComponent(info.region));
        entity.add(new TypeComponent(TypeComponent.Type.OBJECT));

        int owner = 0;
        int vision = 1; // Default vision for objects (Trees, Towns, etc)

        if (type == MapGenerator.ObjectType.BASE_P1) {
            owner = 1;
            vision = GameConfig.BORDER_RADIUS; // FIX: Base gets border radius vision
        } else if (type == MapGenerator.ObjectType.BASE_P2) {
            owner = 2;
            vision = GameConfig.BORDER_RADIUS; // FIX: Base gets border radius vision
        }

        // Use the 7-argument constructor (No income)
        entity.add(new StatsComponent(info.name, 0, 0, 0, vision, StatsComponent.MoveType.LAND, owner));
        engine.addEntity(entity);
    }

    // --- Convert Town to Base ---
    public void captureStructure(Entity objectEntity, int newOwner, MapGenerator.GameMap map) {
        // 1. Update Texture to the new owner's Base
        TextureComponent tex = objectEntity.getComponent(TextureComponent.class);
        if (newOwner == 1) {
            tex.region = baseP1Region;
        } else if (newOwner == 2) {
            tex.region = baseP2Region;
        }

        // 2. Update Stats (Owner and Income)
        StatsComponent stats = objectEntity.getComponent(StatsComponent.class);
        if (stats != null) {
            stats.owner = newOwner;
            stats.name = (newOwner == 1) ? "Blue Base" : "Red Base";
//            stats.income = 5;
            stats.vision = GameConfig.BORDER_RADIUS;
        }

        // 3. Update GameMap Data (Crucial for borders and future captures)
        GridPositionComponent pos = objectEntity.getComponent(GridPositionComponent.class);
        if (pos != null) {
            if (newOwner == 1) {
                map.objects[pos.x][pos.y] = MapGenerator.ObjectType.BASE_P1;
            } else {
                map.objects[pos.x][pos.y] = MapGenerator.ObjectType.BASE_P2;
            }
        }
    }

    // ... (Keep existing methods: getUnitUi, getTerrainUi, getObjectUi, getTextureForTerrain, UiInfo) ...
    public UiInfo getUnitUi(String unitType) {
        if ("RECRUIT".equals(unitType)) {
            return new UiInfo("Infantry Recruit", recruitDisplayRegion);
        }
        return new UiInfo("Unknown Unit", recruitDisplayRegion);
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
                return new UiInfo("Mountain", mountainObjRegion);
            default:
                return new UiInfo("Unknown Object", grassRegion);
        }
    }

    public TextureRegion getTextureForTerrain(int terrainId) {
        MapGenerator.TerrainType[] allTypes = MapGenerator.TerrainType.values();
        if (terrainId < 0 || terrainId >= allTypes.length) {
            return grassRegion;
        }
        MapGenerator.TerrainType type = allTypes[terrainId];
        if (type == MapGenerator.TerrainType.WATER) {
            return waterRegion;
        } else if (type == MapGenerator.TerrainType.DEEP_WATER) {
            return deepWaterRegion;
        } else if (type == MapGenerator.TerrainType.SAND) {
            return sandRegion;
        } else if (type == MapGenerator.TerrainType.MOUNTAIN) {
            return mountainRegion;
        } else {
            return grassRegion;
        }
    }

    public static class UiInfo {

        public String name;
        public TextureRegion region;

        public UiInfo(String name, TextureRegion region) {
            this.name = name;
            this.region = region;
        }
    }
}
