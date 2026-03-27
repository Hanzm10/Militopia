package com.militopia.factories;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.militopia.components.*;
import com.militopia.config.BaseLevelConfig;
import com.militopia.config.GameConfig;
import com.militopia.data.GameState;
import com.militopia.data.StructureSnapshot;
import com.militopia.data.TurnSnapshot;
import com.militopia.data.UnitSnapshot;
import com.militopia.managers.AssetManager;
import com.militopia.map.MapGenerator;
import com.militopia.ui.GameHUD;
import com.militopia.utils.GameLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UnitFactory {

    private final PooledEngine engine;
    private final AssetManager assets;
    private EntityFactory entityFactory;

    // Storage for the manually loaded regions
    private final Map<String, TextureRegion[]> unitRegions = new java.util.HashMap<>();
    private final Map<String, TextureRegion> baseRegions = new java.util.HashMap<>();
    private final Map<String, TextureRegion> structRegions = new java.util.HashMap<>();

    // Regions
    private final TextureRegion grassRegion;
    private final TextureRegion waterRegion;
    private final TextureRegion deepWaterRegion;
    private final TextureRegion sandRegion;
    private final TextureRegion mountainRegion;
    private final TextureRegion treeRegion;
    private final TextureRegion ruinsRegion;
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

        // --- MANUAL LOADING: Base Levels ---
        baseRegions.put("lvl1_blue", new TextureRegion(assets.get(AssetManager.BASE_LVL1_BLUE)));
        baseRegions.put("lvl1_red", new TextureRegion(assets.get(AssetManager.BASE_LVL1_RED)));

        baseRegions.put("lvl2_blue", new TextureRegion(assets.get(AssetManager.BASE_LVL2_BLUE)));
        baseRegions.put("lvl2_red", new TextureRegion(assets.get(AssetManager.BASE_LVL2_RED)));

        baseRegions.put("lvl3_blue", new TextureRegion(assets.get(AssetManager.BASE_LVL3_BLUE)));
        baseRegions.put("lvl3_red", new TextureRegion(assets.get(AssetManager.BASE_LVL3_RED)));

        baseRegions.put("lvl4_blue", new TextureRegion(assets.get(AssetManager.BASE_LVL4_BLUE)));
        baseRegions.put("lvl4_red", new TextureRegion(assets.get(AssetManager.BASE_LVL4_RED)));

        baseRegions.put("lvl5_blue", new TextureRegion(assets.get(AssetManager.BASE_LVL5_BLUE)));
        baseRegions.put("lvl5_red", new TextureRegion(assets.get(AssetManager.BASE_LVL5_RED)));

        baseRegions.put("lvl6_blue", new TextureRegion(assets.get(AssetManager.BASE_LVL6_BLUE)));
        baseRegions.put("lvl6_red", new TextureRegion(assets.get(AssetManager.BASE_LVL6_RED)));

        baseRegions.put("lvl7_blue", new TextureRegion(assets.get(AssetManager.BASE_LVL7_BLUE)));
        baseRegions.put("lvl7_red", new TextureRegion(assets.get(AssetManager.BASE_LVL7_RED)));

        baseRegions.put("lvl8_blue", new TextureRegion(assets.get(AssetManager.BASE_LVL8_BLUE)));
        baseRegions.put("lvl8_red", new TextureRegion(assets.get(AssetManager.BASE_LVL8_RED)));

        baseRegions.put("lvl9_blue", new TextureRegion(assets.get(AssetManager.BASE_LVL9_BLUE)));
        baseRegions.put("lvl9_red", new TextureRegion(assets.get(AssetManager.BASE_LVL9_RED)));

        baseRegions.put("lvl10_blue", new TextureRegion(assets.get(AssetManager.BASE_LVL10_BLUE)));
        baseRegions.put("lvl10_red", new TextureRegion(assets.get(AssetManager.BASE_LVL10_RED)));

        // UNITS
        // --- MANUAL LOADING: Units ---
        // Recruit
        unitRegions.put("RECRUIT", new TextureRegion[] {
                new TextureRegion(assets.get(AssetManager.RECRUIT_RIGHT)),
                new TextureRegion(assets.get(AssetManager.RECRUIT_LEFT)),
                new TextureRegion(assets.get(AssetManager.RECRUIT_DISPLAY))
        });

        // Ranger
        unitRegions.put("RANGER", new TextureRegion[] {
                new TextureRegion(assets.get(AssetManager.RANGER_RIGHT)),
                new TextureRegion(assets.get(AssetManager.RANGER_LEFT)),
                new TextureRegion(assets.get(AssetManager.RANGER_DISPLAY))
        });

        // Tank
        unitRegions.put("TANK", new TextureRegion[] {
                new TextureRegion(assets.get(AssetManager.TANK_RIGHT)),
                new TextureRegion(assets.get(AssetManager.TANK_LEFT)),
                new TextureRegion(assets.get(AssetManager.TANK_DISPLAY))
        });

        // Sniper
        unitRegions.put("SNIPER", new TextureRegion[] {
                new TextureRegion(assets.get(AssetManager.SNIPER_RIGHT)),
                new TextureRegion(assets.get(AssetManager.SNIPER_LEFT)),
                new TextureRegion(assets.get(AssetManager.SNIPER_DISPLAY))
        });

        // Recon Drone
        unitRegions.put("RECON_DRONE", new TextureRegion[] {
                new TextureRegion(assets.get(AssetManager.RECON_DRONE_RIGHT)),
                new TextureRegion(assets.get(AssetManager.RECON_DRONE_LEFT)),
                new TextureRegion(assets.get(AssetManager.RECON_DRONE_DISPLAY))
        });

        // Suicide Drone
        unitRegions.put("SUICIDE_DRONE", new TextureRegion[] {
                new TextureRegion(assets.get(AssetManager.SUICIDE_DRONE_RIGHT)),
                new TextureRegion(assets.get(AssetManager.SUICIDE_DRONE_LEFT)),
                new TextureRegion(assets.get(AssetManager.SUICIDE_DRONE_DISPLAY))
        });

        // Apache
        unitRegions.put("APACHE", new TextureRegion[] {
                new TextureRegion(assets.get(AssetManager.APACHE_RIGHT)),
                new TextureRegion(assets.get(AssetManager.APACHE_LEFT)),
                new TextureRegion(assets.get(AssetManager.APACHE_DISPLAY))
        });

        // Gunboat
        unitRegions.put("GUNBOAT", new TextureRegion[] {
                new TextureRegion(assets.get(AssetManager.GUNBOAT_RIGHT)),
                new TextureRegion(assets.get(AssetManager.GUNBOAT_LEFT)),
                new TextureRegion(assets.get(AssetManager.GUNBOAT_DISPLAY))
        });

        // Destroyer
        unitRegions.put("DESTROYER", new TextureRegion[] {
                new TextureRegion(assets.get(AssetManager.DESTROYER_RIGHT)),
                new TextureRegion(assets.get(AssetManager.DESTROYER_LEFT)),
                new TextureRegion(assets.get(AssetManager.DESTROYER_DISPLAY))
        });

        // Carrier
        unitRegions.put("CARRIER", new TextureRegion[] {
                new TextureRegion(assets.get(AssetManager.CARRIER_RIGHT)),
                new TextureRegion(assets.get(AssetManager.CARRIER_LEFT)),
                new TextureRegion(assets.get(AssetManager.CARRIER_DISPLAY))
        });

        // Juggernaut
        unitRegions.put("JUGGERNAUT", new TextureRegion[] {
                new TextureRegion(assets.get(AssetManager.JUGGERNAUT_RIGHT)),
                new TextureRegion(assets.get(AssetManager.JUGGERNAUT_LEFT)),
                new TextureRegion(assets.get(AssetManager.JUGGERNAUT_DISPLAY))
        });

        // B2 Bomber
        unitRegions.put("B2", new TextureRegion[] {
                new TextureRegion(assets.get(AssetManager.B2_RIGHT)),
                new TextureRegion(assets.get(AssetManager.B2_LEFT)),
                new TextureRegion(assets.get(AssetManager.B2_DISPLAY))
        });

        // Submarine
        unitRegions.put("SUBMARINE", new TextureRegion[] {
                new TextureRegion(assets.get(AssetManager.SUBMARINE_RIGHT)),
                new TextureRegion(assets.get(AssetManager.SUBMARINE_LEFT)),
                new TextureRegion(assets.get(AssetManager.SUBMARINE_DISPLAY))
        });

        // Structures
        structRegions.put("MUNITION_FACTORY", new TextureRegion(assets.get(AssetManager.MUNITION_FACTORY)));
        structRegions.put("PORT", new TextureRegion(assets.get(AssetManager.PORT)));
        structRegions.put("SOLAR", new TextureRegion(assets.get(AssetManager.SOLAR_ARRAY)));
        structRegions.put("OIL_DERRICK", new TextureRegion(assets.get(AssetManager.OIL_DERRICK)));
        structRegions.put("NUCLEAR", new TextureRegion(assets.get(AssetManager.NUCLEAR_PLANT)));
        structRegions.put("HOSPITAL", new TextureRegion(assets.get(AssetManager.FIELD_HOSPITAL)));
        structRegions.put("RADAR", new TextureRegion(assets.get(AssetManager.RADAR_STATION)));
        structRegions.put("JAMMER", new TextureRegion(assets.get(AssetManager.SIGNAL_JAMMER)));

        // Tiles
        this.grassRegion = new TextureRegion(assets.get(AssetManager.TILE_GRASS));
        this.waterRegion = new TextureRegion(assets.get(AssetManager.TILE_WATER));
        this.deepWaterRegion = new TextureRegion(assets.get(AssetManager.TILE_DEEPWATER));
        this.sandRegion = new TextureRegion(assets.get(AssetManager.TILE_SAND));
        this.mountainRegion = new TextureRegion(assets.get(AssetManager.TILE_MOUNTAIN));

        // Objects
        this.treeRegion = new TextureRegion(assets.get(AssetManager.OBJ_TREE));
        this.ruinsRegion = new TextureRegion(assets.get(AssetManager.OBJ_RUINS));
        this.townRegion = new TextureRegion(assets.get(AssetManager.STRUCT_TOWN));
        this.oilRegion = new TextureRegion(assets.get(AssetManager.OBJ_OIL));
        this.cactusRegion = new TextureRegion(assets.get(AssetManager.OBJ_CACTUS));
        this.mountainObjRegion = new TextureRegion(assets.get(AssetManager.OBJ_MOUNTAIN));
        this.fogRegion = new TextureRegion(assets.get(AssetManager.FOG_OF_WAR));

        // Add to map for consistent lookup via getTextureForPopup
        structRegions.put("TOWN", townRegion);
        structRegions.put("RUINS", ruinsRegion);
        structRegions.put("OIL", oilRegion);

        // Animals
        this.horseRegion = new TextureRegion(assets.get(AssetManager.HORSE));
        this.fishRegion = new TextureRegion(assets.get(AssetManager.FISH));
        this.deerRegion = new TextureRegion(assets.get(AssetManager.DEER));
        this.zebraRegion = new TextureRegion(assets.get(AssetManager.ZEBRA));

        this.horseDisplayRegion = new TextureRegion(assets.get(AssetManager.HORSE_DISPLAY));
        this.fishDisplayRegion = new TextureRegion(assets.get(AssetManager.FISH_DISPLAY));
        this.deerDisplayRegion = new TextureRegion(assets.get(AssetManager.DEER_DISPLAY));
        this.zebraDisplayRegion = new TextureRegion(assets.get(AssetManager.ZEBRA_DISPLAY));
    }

    public void createUnit(String unitType, int x, int y, int owner, boolean isSummoned) {
        TextureRegion[] regions = unitRegions.get(unitType);
        if (regions == null) {
            regions = unitRegions.get("RECRUIT");
        }

        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 3));
        entity.add(new TextureComponent(regions[0]));
        entity.add(new FacingComponent(regions[1], regions[0]));
        entity.add(new TypeComponent(TypeComponent.Type.UNIT));
        entity.add(new AbilitiesComponent());
        entity.add(new AnimationComponent());

        // --- MANUAL STATS CONFIGURATION ---
        int hp = 10, atk = 5, def = 0, move = 3, rng = 1, vis = 3, cost = 3;
        StatsComponent.MoveType moveType = StatsComponent.MoveType.LAND;

        switch (unitType) {
            case "RECRUIT":
                hp = 10;
                atk = 3;
                def = 1;
                move = 1;
                rng = 1;
                vis = 1;
                cost = 2;
                moveType = StatsComponent.MoveType.LAND;
                break;
            case "RANGER":
                hp = 12;
                atk = 5;
                def = 1;
                move = 1;
                rng = 2;
                vis = 2;
                cost = 5;
                moveType = StatsComponent.MoveType.LAND;
                break;
            case "SNIPER":
                hp = 8;
                atk = 15;
                def = 0;
                move = 1;
                rng = 3;
                vis = 3;
                cost = 8;
                moveType = StatsComponent.MoveType.LAND;
                break;
            case "TANK":
                hp = 30;
                atk = 12;
                def = 5;
                move = 2;
                rng = 3;
                vis = 3;
                cost = 15;
                moveType = StatsComponent.MoveType.LAND;
                break;
            case "JUGGERNAUT":
                hp = 50;
                atk = 12;
                def = 6;
                move = 3;
                rng = 1;
                vis = 3;
                cost = 0;
                moveType = StatsComponent.MoveType.LAND;
                break;
            case "RECON_DRONE":
                hp = 5;
                atk = 0;
                def = 0;
                move = 3;
                rng = 0;
                vis = 3;
                cost = 4;
                moveType = StatsComponent.MoveType.AIR;
                break;
            case "SUICIDE_DRONE":
                hp = 5;
                atk = 20;
                def = 0;
                move = 2;
                rng = 1;
                vis = 2;
                cost = 7;
                moveType = StatsComponent.MoveType.AIR;
                break;
            case "APACHE":
                hp = 20;
                atk = 15;
                def = 2;
                move = 3;
                rng = 2;
                vis = 3;
                cost = 18;
                moveType = StatsComponent.MoveType.AIR;
                break;
            case "B2":
                hp = 45;
                atk = 18;
                def = 3;
                move = 3;
                rng = 3;
                vis = 3;
                cost = 0;
                moveType = StatsComponent.MoveType.AIR;
                break;
            case "GUNBOAT":
                hp = 10;
                atk = 5;
                def = 2;
                move = 2;
                rng = 2;
                vis = 2;
                cost = 6;
                moveType = StatsComponent.MoveType.SEA;
                break;
            case "DESTROYER":
                hp = 30;
                atk = 15;
                def = 3;
                move = 3;
                rng = 3;
                vis = 3;
                cost = 13;
                moveType = StatsComponent.MoveType.SEA;
                break;
            case "CARRIER":
                hp = 45;
                atk = 5;
                def = 4;
                move = 3;
                rng = 3;
                vis = 3;
                cost = 25;
                moveType = StatsComponent.MoveType.SEA;
                break;
            case "SUBMARINE":
                hp = 40;
                atk = 25;
                def = 3;
                move = 4;
                rng = 4;
                vis = 3;
                cost = 0;
                moveType = StatsComponent.MoveType.SEA;
                break;
        }

        // Use Unit Constructor (No Income Parameter)
        StatsComponent stats = new StatsComponent(toNiceName(unitType), hp, atk, def, move, rng, vis, cost, moveType,
                owner);
        stats.unitTypeKey = unitType; // store raw key for snapshot restoration
        stats.hasActed = isSummoned;

        // --- Post-processing for specific abilities ---
        AbilitiesComponent abilities = entity.getComponent(AbilitiesComponent.class);
        if (unitType.equals("APACHE")) {
            abilities.fuel = 5;
            abilities.fuelMax = 5;
        }
        if (unitType.equals("SUBMARINE")) {
            abilities.isCloaked = true;
        }

        entity.add(stats);
        engine.addEntity(entity);
    }

    public static int getUnitCost(String unitType) {
        // Must match cost in createUnit
        switch (unitType) {
            case "RECRUIT":
                return 2;
            case "RANGER":
                return 5;
            case "SNIPER":
                return 8;
            case "TANK":
                return 15;
            case "JUGGERNAUT":
                return 0;

            case "RECON_DRONE":
                return 4;
            case "SUICIDE_DRONE":
                return 7;
            case "APACHE":
                return 18;
            case "B2":
                return 0;

            case "GUNBOAT":
                return 6;
            case "DESTROYER":
                return 13;
            case "CARRIER":
                return 25;
            case "SUBMARINE":
                return 0;
            default:
                return 0;
        }
    }

    public StatsComponent.MoveType getUnitMoveType(String unitType) {
        switch (unitType) {
            case "RECRUIT":
            case "RANGER":
            case "SNIPER":
            case "TANK":
            case "JUGGERNAUT":
                return StatsComponent.MoveType.LAND;

            case "RECON_DRONE":
            case "SUICIDE_DRONE":
            case "APACHE":
            case "B2":
                return StatsComponent.MoveType.AIR;

            case "GUNBOAT":
            case "DESTROYER":
            case "CARRIER":
            case "SUBMARINE":
                return StatsComponent.MoveType.SEA;

            default:
                return StatsComponent.MoveType.LAND;
        }
    }

    // -------------------------------------------------------------------------
    // Snapshot / Undo support
    // -------------------------------------------------------------------------

    /**
     * Captures a full snapshot of current game state for undo.
     * Call this at the START of every turn before the player acts.
     */
    public TurnSnapshot captureSnapshot(Engine engine, GameState state, MapGenerator.GameMap map) {
        List<UnitSnapshot> unitSnaps = new ArrayList<>();
        List<StructureSnapshot> structSnaps = new ArrayList<>();

        ImmutableArray<Entity> all = engine.getEntitiesFor(
                Family.all(GridPositionComponent.class, StatsComponent.class, TypeComponent.class).get());

        for (Entity e : all) {
            TypeComponent type = e.getComponent(TypeComponent.class);
            StatsComponent s = e.getComponent(StatsComponent.class);
            GridPositionComponent p = e.getComponent(GridPositionComponent.class);

            if (type.type == TypeComponent.Type.UNIT) {
                unitSnaps.add(new UnitSnapshot(
                        s.unitTypeKey, p.x, p.y, s.owner,
                        s.currentHP, s.hasActed, s.hasMoved, s.moveType));

            } else if (type.type == TypeComponent.Type.OBJECT && s.owner >= 0
                    && (s.income > 0 || s.maxBaseXP > 0 || !s.name.startsWith("ANIMAL_"))) {
                // Only snapshot income-generating structures: bases (income >= 2) and towns
                // (income = 1)
                // Exclude decorative objects (trees, ruins, oil, etc.) — they don't need
                // restoration
                if (s.income > 0) {
                    structSnaps.add(new StructureSnapshot(
                            p.x, p.y, s.owner, s.level,
                            s.currentBaseXP, s.income, s.name, s.baseOrdinal));
                }
            }
        }

        // Clone the map objects array
        MapGenerator.ObjectType[][] objClone = new MapGenerator.ObjectType[map.width][map.height];
        for (int x = 0; x < map.width; x++) {
            System.arraycopy(map.objects[x], 0, objClone[x], 0, map.height);
        }

        return new TurnSnapshot(
                state.p1Funding, state.p2Funding,
                state.p1XP, state.p2XP,
                state.turnCount, state.currentPlayer,
                state.p1BaseCount, state.p2BaseCount,
                unitSnaps, structSnaps, objClone);
    }

    public int getStructureCost(String type) {
        switch (type) {
            case "MUNITION_FACTORY":
                return 5;
            case "PORT":
                return 7;
            case "HOSPITAL":
                return 15;
            case "SOLAR":
                return 8;
            case "RADAR":
                return 20;
            case "OIL_DERRICK":
                return 10;
            case "JAMMER":
                return 25;
            case "NUCLEAR":
                return 40;
            default:
                return 0;
        }
    }

    public void createStructure(String type, int x, int y, int owner, int parentX, int parentY) {
        String regionKey = type;

        TextureRegion region = structRegions.get(regionKey);
        if (region == null) {
            region = horseDisplayRegion; // Fallback
        }
        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 1)); // Layer 1
        entity.add(new TextureComponent(region));
        entity.add(new TypeComponent(TypeComponent.Type.OBJECT));
        entity.add(new AbilitiesComponent()); // NEW: Add abilities state

        String niceName = type.replace("_", " ");
        if (type.equals("SOLAR"))
            niceName = "Solar Array";
        if (type.equals("RADAR"))
            niceName = "Radar Station";
        if (type.equals("JAMMER"))
            niceName = "Signal Jammer";
        if (type.equals("NUCLEAR"))
            niceName = "Nuclear Plant";
        if (type.equals("OIL_DERRICK"))
            niceName = "Oil Derrick";
        if (type.equals("MUNITION_FACTORY"))
            niceName = "Munition Factory";

        // Create Stats (Default Income = 0, we add it to Base XP instead)
        StatsComponent stats = new StatsComponent(niceName, 10, 0, 0, 0, 0, 1, getStructureCost(type),
                StatsComponent.MoveType.LAND, owner, 0);
        stats.unitTypeKey = type; // Store the key for HUD icon lookup

        // --- NEW: Link to Parent Base ---
        stats.parentBaseX = parentX;
        stats.parentBaseY = parentY;

        // --- Set XP Gain & Income per structure type ---
        if (type.equals("MUNITION_FACTORY")) {
            stats.xpGain = 50;
            stats.income = 2;
        }

        if (type.equals("PORT")) {
            stats.xpGain = 50;
            stats.income = 0;
        }

        if (type.equals("HOSPITAL")) {
            stats.xpGain = 50;
            stats.income = 0;
        }

        if (type.equals("SOLAR")) {
            stats.xpGain = 75;
            stats.income = 3;
        }

        if (type.equals("OIL_DERRICK")) {
            stats.xpGain = 100;
            stats.income = 6;
        }

        if (type.equals("NUCLEAR")) {
            stats.xpGain = 150;
            stats.income = 15;
        }

        if (type.equals("RADAR")) {
            stats.xpGain = 75;
            stats.income = 0;
            stats.vision = 4;
        }

        if (type.equals("JAMMER")) {
            stats.xpGain = 75;
            stats.income = 0;
            stats.vision = 2;
        }

        entity.add(stats);
        engine.addEntity(entity);
    }

    public void createObjectEntity(int x, int y, MapGenerator.ObjectType type, GameState state) {
        UiInfo info = getObjectUi(type);
        if (info.region == null) {
            return;
        }

        boolean isAnimal = (type.name().contains("HORSE") || type.name().contains("FISH")
                || type.name().contains("DEER") || type.name().contains("ZEBRA"));
        int zIndex = isAnimal ? 2 : 1;

        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, zIndex));
        entity.add(new TextureComponent(info.region));
        entity.add(new TypeComponent(TypeComponent.Type.OBJECT));
        entity.add(new AnimationComponent());

        if (isAnimal) {
            // Animal: Use Unit Constructor (0 Income)
            StatsComponent animalStats = new StatsComponent(info.name, 1, 0, 0, 0, 0, 0, 0,
                    StatsComponent.MoveType.LAND, 0);
            animalStats.name = "ANIMAL_" + type.name();
            animalStats.unitTypeKey = type.name();
            entity.add(animalStats);
        } else if (type == MapGenerator.ObjectType.BASE_P1 || type == MapGenerator.ObjectType.BASE_P2) {
            int owner = (type == MapGenerator.ObjectType.BASE_P1) ? 1 : 2;
            state.p1BaseCount += (owner == 1 ? 1 : 0);
            state.p2BaseCount += (owner == 2 ? 1 : 0);
            String ordinal = getOrdinal((owner == 1) ? state.p1BaseCount : state.p2BaseCount);

            // Base: Use Structure Constructor (With Income = 2)
            StatsComponent stats = new StatsComponent(
                    (owner == 1 ? state.p1Name : state.p2Name) + "'s " + ordinal + " Base",
                    100, 0, 0, 0, 0, GameConfig.BORDER_RADIUS, 0, StatsComponent.MoveType.LAND, owner, 2);

            stats.unitTypeKey = type.name(); // Store key (e.g. "BASE_P1")
            stats.baseOrdinal = ordinal;
            updateBaseTexture(entity, stats);
            entity.add(stats);
        } else {
            // Town: Use Structure Constructor (With Income = 1)
            int income = (type == MapGenerator.ObjectType.TOWN) ? 1 : 0;
            StatsComponent stats = new StatsComponent(info.name, 10, 0, 0, 0, 0, 1, 0, StatsComponent.MoveType.LAND, 0,
                    income);
            stats.unitTypeKey = type.name(); // Store key (e.g. "TOWN", "RUINS")
            entity.add(stats);
        }
        engine.addEntity(entity);
    }

    public void checkAndApplyLevelUp(Entity baseEntity, GameState state, GameHUD hud) {
        StatsComponent stats = baseEntity.getComponent(StatsComponent.class);
        if (stats == null) {
            return;
        }
        while (stats.currentBaseXP >= stats.maxBaseXP) {
            stats.currentBaseXP -= stats.maxBaseXP;
            stats.level++;
            BaseLevelConfig.LevelData data = BaseLevelConfig.getLevel(stats.level);
            stats.maxBaseXP = data.maxXP;
            stats.income = data.income;
            stats.vision = data.borderRadius;
            if (data.fundingBonus > 0) {
                if (stats.owner == 1) {
                    state.p1Funding += data.fundingBonus;
                } else {
                    state.p2Funding += data.fundingBonus;
                }
            }
            updateBaseTexture(baseEntity, stats);
            if (hud != null) {
                hud.showLevelUpPopup(stats.owner, stats.name, stats.level, data.fundingBonus, data.unlockedUnits,
                        data.unlockedStructures, this);
            }
            GameLogger.log(GameLogger.ECONOMY, stats.owner,
                    stats.name + " leveled up to Level " + stats.level + "!");
        }
    }

    public void updateBaseTexture(Entity entity, StatsComponent stats) {
        TextureComponent tex = entity.getComponent(TextureComponent.class);
        String color = (stats.owner == 1) ? "blue" : "red";
        int lvl = Math.min(stats.level, 10);
        TextureRegion reg = baseRegions.get("lvl" + lvl + "_" + color);
        if (reg != null) {
            tex.region = reg;
        } else {
            tex.region = baseRegions.get("lvl1_" + color);
        }
    }

    public void updateStructureFromSave(Entity entity, com.militopia.data.StructureData data,
            MapGenerator.GameMap map) {
        StatsComponent stats = entity.getComponent(StatsComponent.class);
        if (stats == null)
            return;

        GridPositionComponent pos = entity.getComponent(GridPositionComponent.class);

        stats.owner = data.owner;
        stats.level = data.level;
        stats.currentBaseXP = data.currentBaseXP;
        stats.name = data.baseName;
        stats.baseOrdinal = data.baseOrdinal;

        boolean isTown = (pos != null && map.objects[pos.x][pos.y] == MapGenerator.ObjectType.TOWN);

        // --- NEW: Sync Base Stats after load ---
        if (!isTown) {
            com.militopia.config.BaseLevelConfig.LevelData levelData = com.militopia.config.BaseLevelConfig
                    .getLevel(stats.level);
            stats.maxBaseXP = levelData.maxXP;
            stats.income = levelData.income;
            stats.vision = levelData.borderRadius;
            updateBaseTexture(entity, stats);
        } else {
            // Towns have fixed stats
            stats.maxBaseXP = 2000;
            stats.income = 1;
            stats.vision = 1;
        }
    }

    public void captureStructure(Entity objectEntity, int newOwner, MapGenerator.GameMap map, GameState state) {
        StatsComponent stats = objectEntity.getComponent(StatsComponent.class);
        TextureComponent tex = objectEntity.getComponent(TextureComponent.class);
        GridPositionComponent pos = objectEntity.getComponent(GridPositionComponent.class);

        MapGenerator.ObjectType oldType = map.objects[pos.x][pos.y];
        boolean wasBase = (oldType == MapGenerator.ObjectType.BASE_P1 || oldType == MapGenerator.ObjectType.BASE_P2);
        boolean isTown = (oldType == MapGenerator.ObjectType.TOWN);
        int oldOwner = stats.owner;
        int preservedLevel = stats.level;

        // Transform into a Base for the new owner, whether it was a Town or an Enemy
        // Base.
        if (newOwner == 1) {
            map.objects[pos.x][pos.y] = MapGenerator.ObjectType.BASE_P1;
            state.p1BaseCount++;
            stats.owner = 1;

            // Update Old Owner
            if (wasBase && oldOwner == 2) {
                state.p2BaseCount = Math.max(0, state.p2BaseCount - 1);
            }

            stats.baseOrdinal = getOrdinal(state.p1BaseCount);
            stats.name = state.p1Name + "'s " + stats.baseOrdinal + " Base";
            int xpGain = isTown ? 50 : 250;
            state.p1XP += xpGain;

            // --- NEW: Floating Text ---
            if (entityFactory != null) {
                float worldX = EntityFactory.gridToIsoX(pos.x, pos.y);
                float worldY = EntityFactory.gridToIsoY(pos.x, pos.y);
                entityFactory.createFloatingText("+" + xpGain + " XP", worldX, worldY, FloatingTextComponent.Type.XP);
            }
        } else if (newOwner == 2) {
            map.objects[pos.x][pos.y] = MapGenerator.ObjectType.BASE_P2;
            state.p2BaseCount++;
            stats.owner = 2;

            // Update Old Owner
            if (wasBase && oldOwner == 1) {
                state.p1BaseCount = Math.max(0, state.p1BaseCount - 1);
            }

            stats.baseOrdinal = getOrdinal(state.p2BaseCount);
            stats.name = state.p2Name + "'s " + stats.baseOrdinal + " Base";
            int xpGain = isTown ? 50 : 250;
            state.p2XP += xpGain;

            // --- NEW: Floating Text ---
            if (entityFactory != null) {
                float worldX = EntityFactory.gridToIsoX(pos.x, pos.y);
                float worldY = EntityFactory.gridToIsoY(pos.x, pos.y);
                entityFactory.createFloatingText("+" + xpGain + " XP", worldX, worldY, FloatingTextComponent.Type.XP);
            }
        }

        // Apply preserved level or reset to Level 1 if it was a Town
        if (wasBase) {
            BaseLevelConfig.LevelData levelData = BaseLevelConfig.getLevel(preservedLevel);
            stats.level = preservedLevel;
            stats.income = levelData.income;
            stats.maxBaseXP = levelData.maxXP;
            stats.vision = levelData.borderRadius;
        } else {
            stats.level = 1;
            stats.income = 2;
            stats.maxBaseXP = 2000;
            stats.vision = GameConfig.BORDER_RADIUS;
        }
        stats.currentBaseXP = 0; // Reset progress but keep the level (if wasBase)

        // Ensure texture matches the new owner & level
        updateBaseTexture(objectEntity, stats);

        // Spawn animals upon capture only if it was an enemy base
        if (!isTown) {
            spawnAnimalsAroundBase(pos.x, pos.y, map, state);
        }
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
    public boolean hasEntityAt(int x, int y, int zLayer) {
        return getEntityAt(x, y, zLayer) != null;
    }

    public Entity getEntityAt(int x, int y, int zLayer) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            if (pos.x == x && pos.y == y && pos.zIndex == zLayer) {
                return e;
            }
        }
        return null;
    }

    /**
     * Finds a valid spawn point adjacent to the producer (x, y) that matches the
     * unit's moveType.
     * Scans the 8 adjacent tiles for an unoccupied tile (no unit at zIndex 3).
     */
    public int[] findValidSpawnPoint(int startX, int startY, StatsComponent.MoveType moveType,
            MapGenerator.GameMap map) {

        // Priority 1: Check the producer tile itself
        if (isValidSpawn(startX, startY, moveType, map)) {
            return new int[] { startX, startY };
        }

        int[][] dirs = {
                { -1, -1 }, { 0, -1 }, { 1, -1 },
                { -1, 0 }, { 1, 0 },
                { -1, 1 }, { 0, 1 }, { 1, 1 }
        };

        for (int[] d : dirs) {
            int tx = startX + d[0];
            int ty = startY + d[1];

            if (tx >= 0 && tx < map.width && ty >= 0 && ty < map.height) {
                if (isValidSpawn(tx, ty, moveType, map)) {
                    return new int[] { tx, ty };
                }
            }
        }
        return null; // No valid spot found
    }

    private boolean isValidSpawn(int x, int y, StatsComponent.MoveType moveType, MapGenerator.GameMap map) {
        if (hasEntityAt(x, y, 3))
            return false;

        MapGenerator.TerrainType terrain = map.terrain[x][y];
        boolean isWater = (terrain == MapGenerator.TerrainType.WATER || terrain == MapGenerator.TerrainType.DEEP_WATER);

        if (moveType == StatsComponent.MoveType.SEA)
            return isWater;
        if (moveType == StatsComponent.MoveType.LAND)
            return !isWater;
        return true; // Air or other
    }

    private static class GridPoint {

        int x, y;

        GridPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

    }

    private String getOrdinal(int i) {
        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];
        }
    }

    public String toNiceName(String type) {
        return type.charAt(0) + type.substring(1).toLowerCase().replace("_", " ");
    }

    public TextureRegion getTextureForPopup(String key) {
        if (unitRegions.containsKey(key)) {
            return unitRegions.get(key)[2];
        }
        if (structRegions.containsKey(key)) {
            return structRegions.get(key);
        }
        if (key.startsWith("BASE_P")) {
            return getHudIcon(MapGenerator.ObjectType.valueOf(key));
        }
        return horseDisplayRegion;
    }

    public UiInfo getUnitUi(String unitType) {
        TextureRegion[] regs = unitRegions.get(unitType);
        return (regs != null) ? new UiInfo(toNiceName(unitType), regs[2])
                : new UiInfo("Unknown", unitRegions.get("RECRUIT")[2]);
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
                return baseRegions.get("lvl1_blue");
            case BASE_P2:
                return baseRegions.get("lvl1_red");

            case BASE_P1_LVL2:
                return baseRegions.get("lvl2_blue");
            case BASE_P2_LVL2:
                return baseRegions.get("lvl2_red");

            case BASE_P1_LVL3:
                return baseRegions.get("lvl3_blue");
            case BASE_P2_LVL3:
                return baseRegions.get("lvl3_red");

            case BASE_P1_LVL4:
                return baseRegions.get("lvl4_blue");
            case BASE_P2_LVL4:
                return baseRegions.get("lvl4_red");

            case BASE_P1_LVL5:
                return baseRegions.get("lvl5_blue");
            case BASE_P2_LVL5:
                return baseRegions.get("lvl5_red");

            case BASE_P1_LVL6:
                return baseRegions.get("lvl6_blue");
            case BASE_P2_LVL6:
                return baseRegions.get("lvl6_red");

            case BASE_P1_LVL7:
                return baseRegions.get("lvl7_blue");
            case BASE_P2_LVL7:
                return baseRegions.get("lvl7_red");

            case BASE_P1_LVL8:
                return baseRegions.get("lvl8_blue");
            case BASE_P2_LVL8:
                return baseRegions.get("lvl8_red");

            case BASE_P1_LVL9:
                return baseRegions.get("lvl9_blue");
            case BASE_P2_LVL9:
                return baseRegions.get("lvl9_red");

            case BASE_P1_LVL10:
                return baseRegions.get("lvl10_blue");
            case BASE_P2_LVL10:
                return baseRegions.get("lvl10_red");

            case TOWN:
                return townRegion;
            default:
                return getObjectUi(type).region;
        }
    }

    public UiInfo getObjectUi(MapGenerator.ObjectType type) {
        // Base Object
        if (type == MapGenerator.ObjectType.BASE_P1) {
            return new UiInfo("Blue Base", baseRegions.get("lvl1_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2) {
            return new UiInfo("Red Base", baseRegions.get("lvl1_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL2) {
            return new UiInfo("Blue Base", baseRegions.get("lvl2_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL2) {
            return new UiInfo("Red Base", baseRegions.get("lvl2_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL3) {
            return new UiInfo("Blue Base", baseRegions.get("lvl3_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL3) {
            return new UiInfo("Red Base", baseRegions.get("lvl3_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL4) {
            return new UiInfo("Blue Base", baseRegions.get("lvl4_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL4) {
            return new UiInfo("Red Base", baseRegions.get("lvl4_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL5) {
            return new UiInfo("Blue Base", baseRegions.get("lvl5_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL5) {
            return new UiInfo("Red Base", baseRegions.get("lvl5_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL6) {
            return new UiInfo("Blue Base", baseRegions.get("lvl6_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL6) {
            return new UiInfo("Red Base", baseRegions.get("lvl6_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL7) {
            return new UiInfo("Blue Base", baseRegions.get("lvl7_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL7) {
            return new UiInfo("Red Base", baseRegions.get("lvl7_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL8) {
            return new UiInfo("Blue Base", baseRegions.get("lvl8_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL8) {
            return new UiInfo("Red Base", baseRegions.get("lvl8_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL9) {
            return new UiInfo("Blue Base", baseRegions.get("lvl9_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL9) {
            return new UiInfo("Red Base", baseRegions.get("lvl9_red"));
        }

        if (type == MapGenerator.ObjectType.BASE_P1_LVL10) {
            return new UiInfo("Blue Base", baseRegions.get("lvl10_blue"));
        }
        if (type == MapGenerator.ObjectType.BASE_P2_LVL10) {
            return new UiInfo("Red Base", baseRegions.get("lvl10_red"));
        }

        // Other Object
        if (type == MapGenerator.ObjectType.TOWN) {
            return new UiInfo("Town", townRegion);
        }
        if (type == MapGenerator.ObjectType.TREE) {
            return new UiInfo("Oak Tree", treeRegion);
        }
        if (type == MapGenerator.ObjectType.RUINS) {
            return new UiInfo("Ancient Ruins", ruinsRegion);
        }
        if (type == MapGenerator.ObjectType.OIL) {
            return new UiInfo("Oil Reservoir", oilRegion);
        }
        if (type == MapGenerator.ObjectType.CACTUS) {
            return new UiInfo("Cactus", cactusRegion);
        }
        if (type == MapGenerator.ObjectType.MOUNTAIN_OBJ) {
            return new UiInfo("Mountain", mountainObjRegion);
        }
        if (type == MapGenerator.ObjectType.HORSE) {
            return new UiInfo("Wild Horse", horseRegion);
        }
        if (type == MapGenerator.ObjectType.FISH) {
            return new UiInfo("Fish School", fishRegion);
        }
        if (type == MapGenerator.ObjectType.DEER) {
            return new UiInfo("Forest Deer", deerRegion);
        }
        if (type == MapGenerator.ObjectType.ZEBRA) {
            return new UiInfo("Zebra", zebraRegion);
        }

        return new UiInfo("Unknown Object", grassRegion);
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

    public void setEntityFactory(EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
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
