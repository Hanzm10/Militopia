package com.militopia.systems;

import com.militopia.config.UnitType;
import com.militopia.factories.UnitFactory;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CombatRosterTest {

    @Test
    public void testCostConsistency() {
        // Expected costs — canonical source of truth
        Map<UnitType, Integer> expected = new LinkedHashMap<UnitType, Integer>();
        expected.put(UnitType.RECRUIT, 2);
        expected.put(UnitType.RANGER, 5);
        expected.put(UnitType.SNIPER, 8);
        expected.put(UnitType.TANK, 15);
        expected.put(UnitType.JUGGERNAUT, 0);
        expected.put(UnitType.RECON_DRONE, 4);
        expected.put(UnitType.SUICIDE_DRONE, 7);
        expected.put(UnitType.APACHE, 18);
        expected.put(UnitType.B2, 0);
        expected.put(UnitType.GUNBOAT, 6);
        expected.put(UnitType.DESTROYER, 13);
        expected.put(UnitType.CARRIER, 25);
        expected.put(UnitType.SUBMARINE, 0);

        for (Map.Entry<UnitType, Integer> entry : expected.entrySet()) {
            assertEquals(entry.getValue().intValue(), UnitFactory.getUnitCost(entry.getKey()),
                "Cost mismatch for " + entry.getKey());
        }
    }

    @Test
    public void testSummonableUnitCount() {
        UnitType[] allUnits = {UnitType.RECRUIT, UnitType.RANGER, UnitType.SNIPER, UnitType.TANK, UnitType.JUGGERNAUT,
            UnitType.RECON_DRONE, UnitType.SUICIDE_DRONE, UnitType.APACHE, UnitType.B2,
            UnitType.GUNBOAT, UnitType.DESTROYER, UnitType.CARRIER, UnitType.SUBMARINE};
        int summonable = 0;
        for (UnitType unit : allUnits) {
            if (UnitFactory.getUnitCost(unit) > 0) {
                summonable++;
            }
        }
        assertEquals(10, summonable, "Expected exactly 10 summonable units (cost > 0)");
    }
}
