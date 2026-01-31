package com.militopia.factories;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.militopia.components.*;
import com.militopia.config.GameConfig;
import com.militopia.data.GameState;
import com.militopia.data.UnitData;
import com.militopia.managers.AssetManager;
import com.militopia.map.MapGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    // --- ANIMAL PLACEHOLDERS (Assign your own assets here) ---
    private final TextureRegion horseRegion;
    private final TextureRegion fishRegion;
    private final TextureRegion deerRegion;
    private final TextureRegion zebraRegion;

    private final TextureRegion horseDisplayRegion;
    private final TextureRegion fishDisplayRegion;
    private final TextureRegion deerDisplayRegion;
    private final TextureRegion zebraDisplayRegion;

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

        // --- ANIMAL ASSETS ---
        this.horseRegion = new TextureRegion(assets.get(AssetManager.HORSE));
        this.fishRegion = new TextureRegion(assets.get(AssetManager.FISH));
        this.deerRegion = new TextureRegion(assets.get(AssetManager.DEER));
        this.zebraRegion = new TextureRegion(assets.get(AssetManager.ZEBRA));

        // ANIMAL DISPLAY ASSETS
        this.horseDisplayRegion = new TextureRegion(assets.get(AssetManager.HORSE_DISPLAY));
        this.fishDisplayRegion = new TextureRegion(assets.get(AssetManager.FISH_DISPLAY));
        this.deerDisplayRegion = new TextureRegion(assets.get(AssetManager.DEER_DISPLAY));
        this.zebraDisplayRegion = new TextureRegion(assets.get(AssetManager.ZEBRA_DISPLAY));
    }

    public int getUnitCost(String unitType) {
        if ("RECRUIT".equals(unitType)) {
            return 3;
        }
        return 0;
    }

    public void createUnit(String unitType, int x, int y, int owner, boolean isSummoned) {
        switch (unitType) {
            case "RECRUIT":
                createRecruit(x, y, owner, isSummoned);
                break;
            default:
                Gdx.app.error("UnitFactory", "Unknown unit type: " + unitType);
        }
    }

    private void createRecruit(int x, int y, int owner, boolean isSummoned) {
        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 2));
        entity.add(new TextureComponent(recruitRightRegion));
        entity.add(new FacingComponent(recruitLeftRegion, recruitRightRegion));
        entity.add(new TypeComponent(TypeComponent.Type.UNIT));

        StatsComponent stats = new StatsComponent("Recruit", 1, 10, 5, 1, 0, StatsComponent.MoveType.LAND, owner);
        stats.hasActed = isSummoned;
        entity.add(stats);

        engine.addEntity(entity);
    }

    public void createObjectEntity(int x, int y, MapGenerator.ObjectType type, GameState state) {
        UiInfo info = getObjectUi(type);
        if (info.region == null) {
            return;
        }

        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 1));
        entity.add(new TextureComponent(info.region));
        entity.add(new TypeComponent(TypeComponent.Type.OBJECT));

        int owner = 0;
        int vision = 1;
        int income = 0;
        String finalName = info.name;
        String ordinal = "";

        float startXP = 0;
        float maxXP = 2000;

        if (type == MapGenerator.ObjectType.BASE_P1) {
            owner = 1;
            vision = GameConfig.BORDER_RADIUS;
            income = 2;
            state.p1BaseCount++;
            ordinal = getOrdinal(state.p1BaseCount);
            finalName = state.p1Name + "'s " + ordinal + " Base";
            startXP = 500;
        } else if (type == MapGenerator.ObjectType.BASE_P2) {
            owner = 2;
            vision = GameConfig.BORDER_RADIUS;
            income = 2;
            state.p2BaseCount++;
            ordinal = getOrdinal(state.p2BaseCount);
            finalName = state.p2Name + "'s " + ordinal + " Base";
            startXP = 500;
        } else if (type == MapGenerator.ObjectType.TOWN) {
            income = 1;
        }

        StatsComponent stats = new StatsComponent(finalName, 0, 0, 0, vision, income, StatsComponent.MoveType.LAND, owner);
        stats.baseOrdinal = ordinal;
        stats.currentBaseXP = startXP;
        stats.maxBaseXP = maxXP;

        entity.add(stats);
        engine.addEntity(entity);
    }

    public void updateStructureFromSave(Entity entity, com.militopia.data.StructureData data, MapGenerator.GameMap map) {
        StatsComponent stats = entity.getComponent(StatsComponent.class);
        TextureComponent tex = entity.getComponent(TextureComponent.class);

        if (stats == null) {
            return;
        }

        stats.owner = data.owner;
        stats.currentBaseXP = data.currentBaseXP;
        stats.name = data.baseName;
        stats.baseOrdinal = data.baseOrdinal;

        if (stats.owner == 1) {
            tex.region = baseP1Region;
            map.objects[data.x][data.y] = MapGenerator.ObjectType.BASE_P1;
            stats.vision = GameConfig.BORDER_RADIUS;
            stats.income = 2;
        } else if (stats.owner == 2) {
            tex.region = baseP2Region;
            map.objects[data.x][data.y] = MapGenerator.ObjectType.BASE_P2;
            stats.vision = GameConfig.BORDER_RADIUS;
            stats.income = 2;
        } else {
            if (map.objects[data.x][data.y] == MapGenerator.ObjectType.TOWN) {
                stats.income = 1;
            }
        }
    }

    public void captureStructure(Entity objectEntity, int newOwner, MapGenerator.GameMap map, GameState state) {
        TextureComponent tex = objectEntity.getComponent(TextureComponent.class);
        StatsComponent stats = objectEntity.getComponent(StatsComponent.class);
        GridPositionComponent pos = objectEntity.getComponent(GridPositionComponent.class);

        // 1. UPDATE BASE VISUALS & STATS
        if (newOwner == 1) {
            tex.region = baseP1Region;
            map.objects[pos.x][pos.y] = MapGenerator.ObjectType.BASE_P1;

            state.p1BaseCount++;
            stats.owner = 1;
            stats.baseOrdinal = getOrdinal(state.p1BaseCount);
            stats.name = state.p1Name + "'s " + stats.baseOrdinal + " Base";
            state.p1XP += 250;

        } else if (newOwner == 2) {
            tex.region = baseP2Region;
            map.objects[pos.x][pos.y] = MapGenerator.ObjectType.BASE_P2;

            state.p2BaseCount++;
            stats.owner = 2;
            stats.baseOrdinal = getOrdinal(state.p2BaseCount);
            stats.name = state.p2Name + "'s " + stats.baseOrdinal + " Base";
            state.p2XP += 250;
        }

        stats.vision = GameConfig.BORDER_RADIUS;
        stats.income = 2;
        stats.maxBaseXP = 2000;
        stats.currentBaseXP = 0;

        // 2. SPAWN ANIMALS LOGIC
        spawnAnimalsAroundBase(pos.x, pos.y, map, state);
    }

    public void spawnAnimalsAroundBase(int baseX, int baseY, MapGenerator.GameMap map, GameState state) {
        int radius = GameConfig.BORDER_RADIUS; // e.g. 2 or 3
        List<GridPoint> validSpots = new ArrayList<>();

        // Find valid spots
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int nx = baseX + dx;
                int ny = baseY + dy;

                // 1. Check Bounds
                if (nx < 0 || nx >= map.width || ny < 0 || ny >= map.height) {
                    continue;
                }
                // 2. No Animals on Base itself
                if (nx == baseX && ny == baseY) {
                    continue;
                }
                // 3. No Animals in Mountains
                if (map.terrain[nx][ny] == MapGenerator.TerrainType.MOUNTAIN) {
                    continue;
                }

                validSpots.add(new GridPoint(nx, ny));
            }
        }

        // Randomize
        Collections.shuffle(validSpots);

        // Spawn 4-5 Animals
        int spawnCount = MathUtils.random(GameConfig.WILD_ANIMAL_COUNT_MIN, GameConfig.WILD_ANIMAL_COUNT_MAX);
        int spawned = 0;

        for (GridPoint spot : validSpots) {
            if (spawned >= spawnCount) {
                break;
            }

            MapGenerator.ObjectType animalType = null;
            MapGenerator.TerrainType terrain = map.terrain[spot.x][spot.y];
            MapGenerator.ObjectType existingObj = map.objects[spot.x][spot.y];

            // Determine Animal Type
            if (existingObj == MapGenerator.ObjectType.TREE) {
                animalType = MapGenerator.ObjectType.DEER;
            } else if (terrain == MapGenerator.TerrainType.WATER || terrain == MapGenerator.TerrainType.DEEP_WATER) {
                animalType = MapGenerator.ObjectType.FISH;
            } else if (terrain == MapGenerator.TerrainType.SAND) {
                animalType = MapGenerator.ObjectType.ZEBRA;
            } else if (terrain == MapGenerator.TerrainType.GRASS) {
                animalType = MapGenerator.ObjectType.HORSE;
            }

            if (animalType != null) {
                // REMOVE EXISTING ENTITY (e.g. Tree) IF ANY
                removeEntityAt(spot.x, spot.y);

                // UPDATE MAP DATA
                map.objects[spot.x][spot.y] = animalType;

                // CREATE VISUAL ENTITY
                createObjectEntity(spot.x, spot.y, animalType, state);
                spawned++;
            }
        }
    }

    // Helper to remove entity at coords (needed to clear Trees before placing Deer)
    private void removeEntityAt(int x, int y) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class, TypeComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TypeComponent type = e.getComponent(TypeComponent.class);

            if (pos.x == x && pos.y == y && type.type == TypeComponent.Type.OBJECT) {
                engine.removeEntity(e);
                return; // Found and removed
            }
        }
    }

    private static class GridPoint {

        int x, y;

        GridPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private String getOrdinal(int i) {
        String[] suffixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];
        }
    }

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

    public TextureRegion getHudIcon(MapGenerator.ObjectType type) {
        switch (type) {
            case HORSE:
                return horseDisplayRegion;
            case FISH:
                return fishDisplayRegion;
            case DEER:
                return deerDisplayRegion;
            case ZEBRA:
                return zebraDisplayRegion;

            // For structures, we likely still want the standard building icon
            case BASE_P1:
                return baseP1Region;
            case BASE_P2:
                return baseP2Region;
            case TOWN:
                return townRegion;

            default:
                // Fallback: If no special HUD icon exists, use the map texture
                return getObjectUi(type).region;
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
            // --- NEW ANIMALS ---
            case HORSE:
                return new UiInfo("Horse", horseRegion);
            case FISH:
                return new UiInfo("Fish", fishRegion);
            case DEER:
                return new UiInfo("Forest Deer", deerRegion);
            case ZEBRA:
                return new UiInfo("Zebra", zebraRegion);
            // -------------------
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
