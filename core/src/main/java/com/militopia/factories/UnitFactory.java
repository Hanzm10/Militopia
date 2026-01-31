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

    // Animals
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

        this.horseRegion = new TextureRegion(assets.get(AssetManager.HORSE));
        this.fishRegion = new TextureRegion(assets.get(AssetManager.FISH));
        this.deerRegion = new TextureRegion(assets.get(AssetManager.DEER));
        this.zebraRegion = new TextureRegion(assets.get(AssetManager.ZEBRA));

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
        if ("RECRUIT".equals(unitType)) {
            Entity entity = engine.createEntity();
            entity.add(new GridPositionComponent(x, y, 3)); // Layer 3 (Units)
            entity.add(new TextureComponent(recruitRightRegion));
            entity.add(new FacingComponent(recruitLeftRegion, recruitRightRegion));
            entity.add(new TypeComponent(TypeComponent.Type.UNIT));

            StatsComponent stats = new StatsComponent("Recruit", 1, 10, 5, 1, 0, StatsComponent.MoveType.LAND, owner);
            stats.hasActed = isSummoned;
            entity.add(stats);
            engine.addEntity(entity);
        }
    }

    public void createObjectEntity(int x, int y, MapGenerator.ObjectType type, GameState state) {
        UiInfo info = getObjectUi(type);
        if (info.region == null) {
            return;
        }

        boolean isAnimal = (type == MapGenerator.ObjectType.HORSE || type == MapGenerator.ObjectType.FISH
                || type == MapGenerator.ObjectType.DEER || type == MapGenerator.ObjectType.ZEBRA);
        int zIndex = isAnimal ? 2 : 1; // Layer 2 for Animals, 1 for Objects

        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, zIndex));
        entity.add(new TextureComponent(info.region));
        entity.add(new TypeComponent(TypeComponent.Type.OBJECT));

        if (isAnimal) {
            StatsComponent animalStats = new StatsComponent(info.name, 0, 0, 0, 0, 0, StatsComponent.MoveType.LAND, 0);
            animalStats.name = "ANIMAL_" + type.name();
            entity.add(animalStats);
        }

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

        if (!isAnimal) {
            StatsComponent stats = new StatsComponent(finalName, 0, 0, 0, vision, income, StatsComponent.MoveType.LAND, owner);
            stats.baseOrdinal = ordinal;
            stats.currentBaseXP = startXP;
            stats.maxBaseXP = maxXP;
            entity.add(stats);
        }

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
        } else if (stats.owner == 2) {
            tex.region = baseP2Region;
            map.objects[data.x][data.y] = MapGenerator.ObjectType.BASE_P2;
        }
    }

    public void captureStructure(Entity objectEntity, int newOwner, MapGenerator.GameMap map, GameState state) {
        TextureComponent tex = objectEntity.getComponent(TextureComponent.class);
        StatsComponent stats = objectEntity.getComponent(StatsComponent.class);
        GridPositionComponent pos = objectEntity.getComponent(GridPositionComponent.class);

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
        spawnAnimalsAroundBase(pos.x, pos.y, map, state);
    }

    public void spawnAnimalsAroundBase(int baseX, int baseY, MapGenerator.GameMap map, GameState state) {
        int radius = GameConfig.BORDER_RADIUS;
        List<GridPoint> validSpots = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int nx = baseX + dx;
                int ny = baseY + dy;
                if (nx < 0 || nx >= map.width || ny < 0 || ny >= map.height) {
                    continue;
                }
                if (nx == baseX && ny == baseY) {
                    continue;
                }
                if (map.terrain[nx][ny] == MapGenerator.TerrainType.MOUNTAIN) {
                    continue;
                }
                validSpots.add(new GridPoint(nx, ny));
            }
        }
        Collections.shuffle(validSpots);

        int spawnCount = MathUtils.random(GameConfig.WILD_ANIMAL_COUNT_MIN, GameConfig.WILD_ANIMAL_COUNT_MAX);
        int spawned = 0;

        for (GridPoint spot : validSpots) {
            if (spawned >= spawnCount) {
                break;
            }

            MapGenerator.ObjectType animalType = null;
            MapGenerator.TerrainType terrain = map.terrain[spot.x][spot.y];
            MapGenerator.ObjectType existingObj = map.objects[spot.x][spot.y];

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
                // --- FIX: CHECK IF ANIMAL ALREADY EXISTS (Prevents stacking) ---
                if (hasEntityAt(spot.x, spot.y, 2)) {
                    continue; // Skip this spot, an animal (Z=2) is already here
                }

                // Create the visual entity (Layer 2)
                createObjectEntity(spot.x, spot.y, animalType, state);
                spawned++;
            }
        }
    }

    // --- HELPER: Checks for entity at coordinates and layer ---
    private boolean hasEntityAt(int x, int y, int zLayer) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            if (pos.x == x && pos.y == y && pos.zIndex == zLayer) {
                return true;
            }
        }
        return false;
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
        return "RECRUIT".equals(unitType) ? new UiInfo("Infantry Recruit", recruitDisplayRegion) : new UiInfo("Unknown Unit", recruitDisplayRegion);
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
            case BASE_P1:
                return baseP1Region;
            case BASE_P2:
                return baseP2Region;
            case TOWN:
                return townRegion;
            default:
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
            case HORSE:
                return new UiInfo("Wild Horse", horseRegion);
            case FISH:
                return new UiInfo("Fish School", fishRegion);
            case DEER:
                return new UiInfo("Forest Deer", deerRegion);
            case ZEBRA:
                return new UiInfo("Zebra", zebraRegion);
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

    public static class UiInfo {

        public String name;
        public TextureRegion region;

        public UiInfo(String name, TextureRegion region) {
            this.name = name;
            this.region = region;
        }
    }
}
