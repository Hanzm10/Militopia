package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.militopia.components.*;
import com.militopia.config.CombatConstants;
import com.militopia.config.StructureType;
import com.militopia.config.UnitStatConfig;
import com.militopia.config.UnitType;
import com.militopia.data.GameState;
import com.militopia.map.MapGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CombatSystemTest {

    private PooledEngine engine;
    private CombatSystem combatSystem;
    private MapGenerator.GameMap gameMap;

    @BeforeEach
    public void setup() {
        engine = new PooledEngine();
        gameMap = new MapGenerator.GameMap(10, 10);
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                gameMap.terrain[x][y] = MapGenerator.TerrainType.GRASS;
                gameMap.objects[x][y] = MapGenerator.ObjectType.NONE;
            }
        }
        GameState gameState = new GameState(1L, "test", 10, 10);
        combatSystem = new CombatSystem(gameMap, null, gameState);
        engine.addSystem(combatSystem);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Entity createUnit(UnitType type, int x, int y, int owner) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 3));
        e.add(new TypeComponent(TypeComponent.Type.UNIT));
        e.add(new AbilitiesComponent());

        UnitStatConfig.UnitStatData data = UnitStatConfig.get(type);
        StatsComponent stats = new StatsComponent(
                type.name(), data.hp, data.atk, data.def,
                data.move, data.rng, data.vis, data.cost, data.moveType, owner);
        stats.unitType = type;
        stats.unitTypeKey = type.name();
        e.add(stats);
        engine.addEntity(e);
        return e;
    }

    private Entity createStructure(String displayName, int x, int y, int owner) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 2));
        e.add(new TypeComponent(TypeComponent.Type.OBJECT));

        // Structures have stats with no attack range — minimal setup
        StatsComponent stats = new StatsComponent(displayName, 30, 0, 5, 0, 0, 0, 0,
                StatsComponent.MoveType.LAND, owner);
        stats.unitTypeKey = displayName;
        e.add(stats);
        engine.addEntity(e);
        return e;
    }

    // -------------------------------------------------------------------------
    // Damage formula
    // -------------------------------------------------------------------------

    @Test
    public void testBasicDamageFormula() {
        // TANK (atk=12) vs JUGGERNAUT (def=10, hp=60) on GRASS = 12-10 = 2 dmg,
        // JUGGERNAUT survives
        Entity tank = createUnit(UnitType.TANK, 0, 0, 1);
        Entity JUGGERNAUT = createUnit(UnitType.JUGGERNAUT, 1, 0, 2); // adjacent, within range

        StatsComponent dStats = JUGGERNAUT.getComponent(StatsComponent.class);
        int initialHP = dStats.currentHP;

        combatSystem.resolveAttack(tank, JUGGERNAUT);

        int expectedDmg = Math.max(0,
                UnitStatConfig.get(UnitType.TANK).atk - UnitStatConfig.get(UnitType.JUGGERNAUT).def);
        assertEquals(initialHP - expectedDmg, dStats.currentHP);
    }

    @Test
    public void testDamageNeverNegative() {
        // RECRUIT (atk=3) vs JUGGERNAUT (def=10) — would be negative without the floor
        Entity recruit = createUnit(UnitType.RECRUIT, 0, 0, 1);
        Entity JUGGERNAUT = createUnit(UnitType.JUGGERNAUT, 1, 1, 2);

        StatsComponent dStats = JUGGERNAUT.getComponent(StatsComponent.class);
        int initialHP = dStats.currentHP;

        combatSystem.resolveAttack(recruit, JUGGERNAUT);

        assertEquals(initialHP, dStats.currentHP, "HP should be unchanged when attack < defense");
    }

    @Test
    public void testTerrainBonusMountain() {
        // RECRUIT (atk=3) vs RECRUIT (def=1) on MOUNTAIN — bonus=3, so 3-1-3 = -1 → 0
        gameMap.terrain[5][5] = MapGenerator.TerrainType.MOUNTAIN;

        Entity attacker = createUnit(UnitType.RECRUIT, 4, 4, 1);
        Entity defender = createUnit(UnitType.RECRUIT, 5, 5, 2);

        StatsComponent dStats = defender.getComponent(StatsComponent.class);
        int initialHP = dStats.currentHP;

        combatSystem.resolveAttack(attacker, defender);

        assertEquals(initialHP, dStats.currentHP, "Mountain terrain should negate recruit damage");
    }

    @Test
    public void testMaxRangePenalty() {
        // SNIPER (atk=15, rng=3) vs JUGGERNAUT (def=10, hp=60) — penalty distinguishes
        // the result
        // At max range (dist=3): 15-10-1 = 4 dmg
        // Without penalty: 15-10 = 5 dmg
        Entity sniper = createUnit(UnitType.SNIPER, 0, 0, 1);
        Entity JUGGERNAUT = createUnit(UnitType.JUGGERNAUT, 3, 0, 2);

        StatsComponent dStats = JUGGERNAUT.getComponent(StatsComponent.class);
        int initialHP = dStats.currentHP;

        combatSystem.resolveAttack(sniper, JUGGERNAUT);

        int expectedDmg = Math.max(0,
                UnitStatConfig.get(UnitType.SNIPER).atk
                        - UnitStatConfig.get(UnitType.JUGGERNAUT).def
                        - CombatConstants.MAX_RANGE_ATTACK_PENALTY);
        assertEquals(initialHP - expectedDmg, dStats.currentHP, "Max range penalty should apply at distance = range");
    }

    // -------------------------------------------------------------------------
    // Attacker state
    // -------------------------------------------------------------------------

    @Test
    public void testAttackerExhaustedAfterAttack() {
        Entity attacker = createUnit(UnitType.RANGER, 0, 0, 1);
        Entity defender = createUnit(UnitType.RECRUIT, 1, 1, 2);

        StatsComponent aStats = attacker.getComponent(StatsComponent.class);
        combatSystem.resolveAttack(attacker, defender);

        assertTrue(aStats.hasActed, "Attacker should be marked hasActed after attacking");
    }

    // -------------------------------------------------------------------------
    // Counterattack
    // -------------------------------------------------------------------------

    @Test
    public void testDefenderCounterattacksWhenInRange() {
        // RECRUIT (atk=3, rng=1) attacks adjacent RANGER (atk=5, def=0, rng=2)
        // Defender counter: 5-0 = 5 dmg on attacker
        Entity recruit = createUnit(UnitType.RECRUIT, 0, 0, 1);
        Entity ranger = createUnit(UnitType.RANGER, 1, 0, 2);

        StatsComponent aStats = recruit.getComponent(StatsComponent.class);
        int initialAtkHP = aStats.currentHP;

        combatSystem.resolveAttack(recruit, ranger);

        int ctrDmg = Math.max(0,
                UnitStatConfig.get(UnitType.RANGER).atk - UnitStatConfig.get(UnitType.RECRUIT).def);
        assertEquals(initialAtkHP - ctrDmg, aStats.currentHP, "Defender should counter when in range");
    }

    @Test
    public void testNoCounterWhenDefenderOutOfRange() {
        // SNIPER (rng=3) attacks RECRUIT (rng=1) from distance 3 — recruit can't
        // counter
        Entity sniper = createUnit(UnitType.SNIPER, 0, 0, 1);
        Entity recruit = createUnit(UnitType.RECRUIT, 3, 0, 2);

        StatsComponent aStats = sniper.getComponent(StatsComponent.class);
        int initialAtkHP = aStats.currentHP;

        combatSystem.resolveAttack(sniper, recruit);

        assertEquals(initialAtkHP, aStats.currentHP, "Recruit should not counter at distance 3 (range=1)");
    }

    @Test
    public void testAttackerDeathFromCounter() {
        // RECRUIT (1 HP) attacks adjacent RANGER
        Entity recruit = createUnit(UnitType.RECRUIT, 0, 0, 1);
        Entity ranger = createUnit(UnitType.RANGER, 1, 0, 2);

        StatsComponent aStats = recruit.getComponent(StatsComponent.class);
        aStats.currentHP = 1;

        combatSystem.resolveAttack(recruit, ranger);

        // Counter from ranger (atk=5 - def=1 = 4) exceeds 1 HP
        assertTrue(aStats.currentHP <= 0, "Attacker with 1HP should die from ranger counter");
    }

    // -------------------------------------------------------------------------
    // Death
    // -------------------------------------------------------------------------

    @Test
    public void testDefenderDeathSetsHpToZero() {
        Entity attacker = createUnit(UnitType.TANK, 0, 0, 1);
        Entity defender = createUnit(UnitType.RECRUIT, 1, 0, 2);

        StatsComponent dStats = defender.getComponent(StatsComponent.class);
        dStats.currentHP = 1; // one-shot

        combatSystem.resolveAttack(attacker, defender);

        assertEquals(0, dStats.currentHP, "Dead defender HP should be clamped to 0");
    }

    @Test
    public void testDefenderDeathFlagsDeathComponent() {
        Entity attacker = createUnit(UnitType.TANK, 0, 0, 1);
        Entity defender = createUnit(UnitType.RECRUIT, 1, 0, 2);

        StatsComponent dStats = defender.getComponent(StatsComponent.class);
        dStats.currentHP = 1;

        combatSystem.resolveAttack(attacker, defender);

        assertNotNull(defender.getComponent(DeathAnimComponent.class),
                "Dead entity should have DeathAnimComponent added");
    }

    // -------------------------------------------------------------------------
    // Indestructible structures
    // -------------------------------------------------------------------------

    @Test
    public void testOilDerrickIsIndestructible() {
        Entity attacker = createUnit(UnitType.JUGGERNAUT, 0, 0, 1);
        Entity oilDerrick = createStructure(StructureType.OIL_DERRICK.getDisplayName(), 1, 0, 2);

        StatsComponent dStats = oilDerrick.getComponent(StatsComponent.class);
        int initialHP = dStats.currentHP;

        combatSystem.resolveAttack(attacker, oilDerrick);

        assertEquals(initialHP, dStats.currentHP, "Oil Derrick should be indestructible");
    }

    @Test
    public void testNuclearPlantIsIndestructible() {
        Entity attacker = createUnit(UnitType.JUGGERNAUT, 0, 0, 1);
        Entity nuclearPlant = createStructure(StructureType.NUCLEAR_PLANT.getDisplayName(), 1, 0, 2);

        StatsComponent dStats = nuclearPlant.getComponent(StatsComponent.class);
        int initialHP = dStats.currentHP;

        combatSystem.resolveAttack(attacker, nuclearPlant);

        assertEquals(initialHP, dStats.currentHP, "Nuclear Plant should be indestructible");
    }

    // -------------------------------------------------------------------------
    // ReconDrone high-altitude immunity
    // -------------------------------------------------------------------------

    @Test
    public void testReconDroneImmuneToLandRangeOneAttack() {
        Entity recruit = createUnit(UnitType.RECRUIT, 0, 0, 1);
        Entity drone = createUnit(UnitType.RECON_DRONE, 0, 0, 2);

        StatsComponent dStats = drone.getComponent(StatsComponent.class);
        int initialHP = dStats.currentHP;

        combatSystem.resolveAttack(recruit, drone);

        assertEquals(initialHP, dStats.currentHP, "ReconDrone should be immune to range-1 land attacks");
    }

    // -------------------------------------------------------------------------
    // Stealth & Cloaking Tests
    // -------------------------------------------------------------------------

    @Test
    public void testSniperStealthImmunityInCover() {
        // Sniper in forest (TREE) should not be counterattacked
        gameMap.objects[1][1] = MapGenerator.ObjectType.TREE;
        Entity sniper = createUnit(UnitType.SNIPER, 1, 1, 1);
        Entity defender = createUnit(UnitType.RECRUIT, 1, 0, 2);

        StatsComponent sStats = sniper.getComponent(StatsComponent.class);
        int initialHP = sStats.currentHP;

        combatSystem.resolveAttack(sniper, defender);

        assertEquals(initialHP, sStats.currentHP, "Sniper in cover should skip counterattack");
    }

    @Test
    public void testSniperRevealedInOpen() {
        // Sniper in open should be counterattacked
        gameMap.objects[1][1] = MapGenerator.ObjectType.NONE;
        Entity sniper = createUnit(UnitType.SNIPER, 1, 1, 1);
        Entity defender = createUnit(UnitType.JUGGERNAUT, 1, 0, 2); // Tougher defender

        StatsComponent sStats = sniper.getComponent(StatsComponent.class);
        int initialHP = sStats.currentHP;

        combatSystem.resolveAttack(sniper, defender);

        assertTrue(sStats.currentHP < initialHP, "Sniper in open should receive counterattack");
    }

    @Test
    public void testSubmarineStealthImmunity() {
        // Submarine should always skip counterattack (unless cloak broken)
        Entity submarine = createUnit(UnitType.SUBMARINE, 1, 1, 1);
        AbilitiesComponent ab = submarine.getComponent(AbilitiesComponent.class);
        ab.isCloaked = true; // Submarines are always cloaked unless broken

        Entity defender = createUnit(UnitType.JUGGERNAUT, 1, 0, 2); // Tougher defender
        StatsComponent sStats = submarine.getComponent(StatsComponent.class);
        int initialHP = sStats.currentHP;

        combatSystem.resolveAttack(submarine, defender);

        assertEquals(initialHP, sStats.currentHP, "Submarine should skip counterattack");
        assertTrue(ab.isCloakBroken, "Cloak should be broken after attack");
    }

    @Test
    public void testBrokenStealthReceivesCounter() {
        // A unit with a broken cloak should receive a counterattack
        Entity submarine = createUnit(UnitType.SUBMARINE, 1, 1, 1);
        AbilitiesComponent ab = submarine.getComponent(AbilitiesComponent.class);
        ab.isCloaked = true;
        ab.isCloakBroken = true; // Already revealed

        Entity defender = createUnit(UnitType.JUGGERNAUT, 1, 0, 2); // Tougher defender
        StatsComponent sStats = submarine.getComponent(StatsComponent.class);
        int initialHP = sStats.currentHP;

        combatSystem.resolveAttack(submarine, defender);

        assertTrue(sStats.currentHP < initialHP, "Unit with broken cloak should receive counterattack");
    }

    @Test
    public void testSuicideDroneRangeAndSelfDestruction() {
        // Suicide Drone (rng=2) attacks RECRUIT from distance 2
        Entity drone = createUnit(UnitType.SUICIDE_DRONE, 0, 0, 1);
        Entity recruit = createUnit(UnitType.RECRUIT, 2, 0, 2);

        StatsComponent aStats = drone.getComponent(StatsComponent.class);
        GridPositionComponent aPos = drone.getComponent(GridPositionComponent.class);
        GridPositionComponent dPos = recruit.getComponent(GridPositionComponent.class);

        // Before attack
        assertEquals(0, aPos.x);
        assertEquals(0, aPos.y);

        combatSystem.resolveAttack(drone, recruit);

        // Position should move to target tile (dive-attack)
        assertEquals(dPos.x, aPos.x);
        assertEquals(dPos.y, aPos.y);

        // Drone should be marked for death
        assertNotNull(drone.getComponent(DeathAnimComponent.class), "Suicide Drone should have DeathAnimComponent after attack");
    }
}
