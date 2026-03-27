package com.militopia.systems;

import com.militopia.factories.UnitFactory;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CombatRosterTest {

    @Test
    public void testCostConsistency() {
        // Expected costs — canonical source of truth
        Map<String, Integer> expected = new LinkedHashMap<String, Integer>();
        expected.put("RECRUIT", 2);
        expected.put("RANGER", 5);
        expected.put("SNIPER", 8);
        expected.put("TANK", 15);
        expected.put("JUGGERNAUT", 0);
        expected.put("RECON_DRONE", 4);
        expected.put("SUICIDE_DRONE", 7);
        expected.put("APACHE", 18);
        expected.put("B2", 0);
        expected.put("GUNBOAT", 6);
        expected.put("DESTROYER", 13);
        expected.put("CARRIER", 25);
        expected.put("SUBMARINE", 0);

        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            assertEquals(entry.getValue().intValue(), UnitFactory.getUnitCost(entry.getKey()),
                "Cost mismatch for " + entry.getKey());
        }
    }

    @Test
    public void testSummonableUnitCount() {
        String[] allUnits = {"RECRUIT", "RANGER", "SNIPER", "TANK", "JUGGERNAUT",
            "RECON_DRONE", "SUICIDE_DRONE", "APACHE", "B2",
            "GUNBOAT", "DESTROYER", "CARRIER", "SUBMARINE"};
        int summonable = 0;
        for (String unit : allUnits) {
            if (UnitFactory.getUnitCost(unit) > 0) {
                summonable++;
            }
        }
        assertEquals(10, summonable, "Expected exactly 10 summonable units (cost > 0)");
    }
}
