package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.militopia.components.*;
import com.militopia.data.GameState;
import com.militopia.map.MapGenerator;
import com.militopia.systems.CombatSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AbilityTest {
    private PooledEngine engine;
    private CombatSystem combatSystem;
    private MapGenerator.GameMap gameMap;
    private GameState gameState;

    @BeforeEach
    public void setup() {
        engine = new PooledEngine();
        gameMap = new MapGenerator.GameMap(10, 10);
        // Initialize map with GRASS
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                gameMap.terrain[x][y] = MapGenerator.TerrainType.GRASS;
                gameMap.objects[x][y] = MapGenerator.ObjectType.NONE;
            }
        }
        gameState = new GameState(12345L, "TestSave", 10, 10);
        combatSystem = new CombatSystem(gameMap, null, gameState);
        engine.addSystem(combatSystem);
    }

    @Test
    public void testTankBlitz() {
        Entity tank = createUnit("TANK", 0, 0, 1);
        Entity defender = createUnit("RECRUIT", 1, 1, 2);

        StatsComponent tStats = tank.getComponent(StatsComponent.class);
        StatsComponent dStats = defender.getComponent(StatsComponent.class);

        dStats.currentHP = 1;
        tStats.hasMoved = true;

        combatSystem.resolveAttack(tank, defender);

        assertFalse(tStats.hasMoved, "Tank should have moved reset after kill (Blitz)");
        assertTrue(tStats.hasActed, "Tank should be marked as having acted");
    }

    @Test
    public void testRangerOverwatch() {
        Entity ranger = createUnit("RANGER", 0, 0, 1);
        Entity movingUnit = createUnit("RECRUIT", 2, 2, 2);

        AbilitiesComponent rAbil = ranger.getComponent(AbilitiesComponent.class);
        rAbil.isOverwatchActive = true;

        StatsComponent mStats = movingUnit.getComponent(StatsComponent.class);
        int initialHP = mStats.currentHP;

        // Ranger range is 2. (2,2) is distance 2 from (0,0) in Chebyshev.
        combatSystem.checkOverwatch(movingUnit, 1, 1); // Unit moves to (1,1)

        assertTrue(mStats.currentHP < initialHP, "Moving unit should have taken damage from Overwatch at (1,1)");
        assertFalse(rAbil.isOverwatchActive, "Overwatch should consume after trigger");
    }

    @Test
    public void testRecruitDigIn() {
        Entity attacker = createUnit("RECRUIT", 0, 0, 1);
        Entity defender = createUnit("RECRUIT", 1, 1, 2);

        AbilitiesComponent dAbil = defender.getComponent(AbilitiesComponent.class);
        dAbil.isDiggingIn = true;

        StatsComponent dStats = defender.getComponent(StatsComponent.class);

        int initialHP = dStats.currentHP;
        combatSystem.resolveAttack(attacker, defender);

        assertEquals(initialHP, dStats.currentHP,
                "Defender should take 0 damage due to Dig In bonus (Atk 3 vs Def 1+3)");
    }

    @Test
    public void testRecruitDigInExpiration() {
        Entity defender = createUnit("RECRUIT", 1, 1, 2);
        AbilitiesComponent dAbil = defender.getComponent(AbilitiesComponent.class);
        dAbil.isDiggingIn = true;

        AbilityStatusSystem statusSystem = new AbilityStatusSystem(gameMap);
        engine.addSystem(statusSystem);

        // Turn start for Player 2 (the defender)
        statusSystem.onTurnStart(2);

        assertFalse(dAbil.isDiggingIn, "Dig In should expire at the start of the owner's turn");
    }

    @Test
    public void testReconDroneHighAltitude() {
        Entity attacker = createUnit("RECRUIT", 0, 0, 1);
        Entity drone = createUnit("RECON_DRONE", 0, 0, 2);

        StatsComponent dStats = drone.getComponent(StatsComponent.class);
        int initialHP = dStats.currentHP;

        combatSystem.resolveAttack(attacker, drone);

        assertEquals(initialHP, dStats.currentHP, "Drone should be immune to Range-1 land attacks (High Altitude)");
    }

    private Entity createUnit(String type, int x, int y, int owner) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 3));
        e.add(new TypeComponent(TypeComponent.Type.UNIT));
        e.add(new AbilitiesComponent());

        int hp = 10, atk = 5, def = 0, move = 3, rng = 1, vis = 3, cost = 3;
        StatsComponent.MoveType moveType = StatsComponent.MoveType.LAND;

        if ("TANK".equals(type)) {
            hp = 30;
            atk = 12;
            def = 5;
            move = 2;
            rng = 3;
        }
        if ("RECRUIT".equals(type)) {
            hp = 10;
            atk = 3;
            def = 1;
            move = 1;
            rng = 1;
        }
        if ("RANGER".equals(type)) {
            hp = 12;
            atk = 5;
            def = 1;
            move = 1;
            rng = 2;
        }
        if ("RECON_DRONE".equals(type)) {
            hp = 5;
            atk = 0;
            def = 0;
            move = 6;
            rng = 0;
            moveType = StatsComponent.MoveType.AIR;
        }

        StatsComponent stats = new StatsComponent(type, hp, atk, def, move, rng, vis, cost, moveType, owner);
        stats.unitTypeKey = type;
        e.add(stats);

        engine.addEntity(e);
        return e;
    }
}
