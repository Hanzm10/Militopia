package com.militopia.config;

import com.militopia.components.StatsComponent;
import com.militopia.factories.UnitFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UnitStatConfigTest {

    @Test
    public void testAllUnitTypesHaveEntry() {
        for (UnitType type : UnitType.values()) {
            UnitStatConfig.UnitStatData data = UnitStatConfig.get(type);
            assertNotNull(data, "Missing stat entry for: " + type);
        }
    }

    @Test
    public void testAllUnitsHavePositiveHp() {
        for (UnitType type : UnitType.values()) {
            assertTrue(UnitStatConfig.get(type).hp > 0, type + " hp must be > 0");
        }
    }

    @Test
    public void testAllUnitsHaveNonNegativeCost() {
        for (UnitType type : UnitType.values()) {
            assertTrue(UnitStatConfig.get(type).cost >= 0, type + " cost must be >= 0");
        }
    }

    @Test
    public void testAllUnitsHaveMoveType() {
        for (UnitType type : UnitType.values()) {
            assertNotNull(UnitStatConfig.get(type).moveType, type + " must have a moveType");
        }
    }

    @Test
    public void testAirUnitsHaveAirMoveType() {
        assertEquals(StatsComponent.MoveType.AIR, UnitStatConfig.get(UnitType.RECON_DRONE).moveType);
        assertEquals(StatsComponent.MoveType.AIR, UnitStatConfig.get(UnitType.SUICIDE_DRONE).moveType);
        assertEquals(StatsComponent.MoveType.AIR, UnitStatConfig.get(UnitType.APACHE).moveType);
        assertEquals(StatsComponent.MoveType.AIR, UnitStatConfig.get(UnitType.B2).moveType);
        assertEquals(StatsComponent.MoveType.AIR, UnitStatConfig.get(UnitType.WRAITH).moveType);
    }

    @Test
    public void testSeaUnitsHaveSeaMoveType() {
        assertEquals(StatsComponent.MoveType.SEA, UnitStatConfig.get(UnitType.GUNBOAT).moveType);
        assertEquals(StatsComponent.MoveType.SEA, UnitStatConfig.get(UnitType.DESTROYER).moveType);
        assertEquals(StatsComponent.MoveType.SEA, UnitStatConfig.get(UnitType.CARRIER).moveType);
        assertEquals(StatsComponent.MoveType.SEA, UnitStatConfig.get(UnitType.SUBMARINE).moveType);
        assertEquals(StatsComponent.MoveType.SEA, UnitStatConfig.get(UnitType.DREADNOUGHT).moveType);
    }

    @Test
    public void testLandUnitsHaveLandMoveType() {
        assertEquals(StatsComponent.MoveType.LAND, UnitStatConfig.get(UnitType.RECRUIT).moveType);
        assertEquals(StatsComponent.MoveType.LAND, UnitStatConfig.get(UnitType.TANK).moveType);
        assertEquals(StatsComponent.MoveType.LAND, UnitStatConfig.get(UnitType.TITAN).moveType);
    }

    @Test
    public void testGetUnitCostMatchesConfig() {
        // UnitFactory.getUnitCost() is a static delegate — verify it matches the source
        for (UnitType type : UnitType.values()) {
            assertEquals(UnitStatConfig.get(type).cost, UnitFactory.getUnitCost(type),
                    "UnitFactory.getUnitCost mismatch for: " + type);
        }
    }

    @Test
    public void testSuperUnitsHaveZeroCost() {
        // Super units are unlocked via base level-up, not purchased — cost = 0
        assertEquals(0, UnitStatConfig.get(UnitType.JUGGERNAUT).cost);
        assertEquals(0, UnitStatConfig.get(UnitType.B2).cost);
        assertEquals(0, UnitStatConfig.get(UnitType.SUBMARINE).cost);
    }

    @Test
    public void testKnownStatValues() {
        // Regression guard — if someone accidentally edits the config, these catch it
        UnitStatConfig.UnitStatData recruit = UnitStatConfig.get(UnitType.RECRUIT);
        assertEquals(10, recruit.hp);
        assertEquals(3,  recruit.atk);
        assertEquals(1,  recruit.def);
        assertEquals(1,  recruit.rng);

        UnitStatConfig.UnitStatData tank = UnitStatConfig.get(UnitType.TANK);
        assertEquals(30, tank.hp);
        assertEquals(12, tank.atk);
        assertEquals(5,  tank.def);
        assertEquals(3,  tank.rng);
    }
}
