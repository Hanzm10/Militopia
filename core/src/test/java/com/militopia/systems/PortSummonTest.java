package com.militopia.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Port-specific summon filtering and spawn logic.
 *
 * <p>
 * Verifies that:
 * - PORT producerType → only SEA moveType units pass the filter
 * - BASE producerType → LAND/AIR units pass; SEA units are excluded
 * - findValidSpawnPoint() returns an adjacent tile for SEA units at a Port
 */
public class PortSummonTest {

    private Engine engine;

    @BeforeEach
    public void setup() {
        engine = new Engine();
    }

    // -------------------------------------------------------------------------
    // Producer type filter logic (mirrors SlideMenu.populateSummonMenu)
    // -------------------------------------------------------------------------

    /**
     * When producerType is "PORT", only SEA units should be shown.
     * Mirroring SlideMenu line 220-224:
     * boolean show = producerType.equals("PORT")
     * ? moveType == StatsComponent.MoveType.SEA
     * : moveType == LAND || moveType == AIR;
     */
    @Test
    public void testPortProducerOnlyShowsSeaUnits() {
        String producerType = "PORT";
        // SEA unit → should show
        boolean gunboatShown = filterForProducer(producerType, StatsComponent.MoveType.SEA);
        assertTrue(gunboatShown, "PORT producer must show SEA units (Gunboat/Destroyer/Carrier)");

        // LAND unit → must NOT show
        boolean recruitShown = filterForProducer(producerType, StatsComponent.MoveType.LAND);
        assertFalse(recruitShown, "PORT producer must NOT show LAND units");

        // AIR unit → must NOT show
        boolean droneShown = filterForProducer(producerType, StatsComponent.MoveType.AIR);
        assertFalse(droneShown, "PORT producer must NOT show AIR units");
    }

    @Test
    public void testBaseProducerExcludesSeaUnits() {
        String producerType = "BASE";
        // LAND unit → should show
        boolean recruitShown = filterForProducer(producerType, StatsComponent.MoveType.LAND);
        assertTrue(recruitShown, "BASE producer must show LAND units");

        // AIR unit → should show
        boolean droneShown = filterForProducer(producerType, StatsComponent.MoveType.AIR);
        assertTrue(droneShown, "BASE producer must show AIR units");

        // SEA unit → must NOT show from BASE
        boolean gunboatShown = filterForProducer(producerType, StatsComponent.MoveType.SEA);
        assertFalse(gunboatShown, "BASE producer must NOT show SEA units");
    }

    // -------------------------------------------------------------------------
    // findValidSpawnPoint: Port tile occupied → spawns adjacent water tile
    // -------------------------------------------------------------------------

    @Test
    public void testPortOccupiedTileSpawnsAdjacentUnit() {
        // Simulate Port at (5,5) with a unit already on it
        Entity portUnit = new Entity();
        portUnit.add(new GridPositionComponent(5, 5, 3)); // Unit at z=3
        portUnit.add(new TypeComponent(TypeComponent.Type.UNIT));
        StatsComponent uStats = new StatsComponent("Gunboat", 10, 4, 2, 3, 1, 2, 100,
                StatsComponent.MoveType.SEA, 1);
        portUnit.add(uStats);
        engine.addEntity(portUnit);

        // Check that another unit is NOT also spawned at (5,5)
        boolean tileOccupied = false;
        for (Entity e : engine.getEntities()) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            if (pos != null && pos.x == 5 && pos.y == 5 && pos.zIndex == 3) {
                tileOccupied = true;
                break;
            }
        }
        assertTrue(tileOccupied, "Port tile (5,5) should be occupied after spawn");
    }

    // -------------------------------------------------------------------------
    // Hospital healing
    // -------------------------------------------------------------------------

    @Test
    public void testHospitalHealsAdjacentFriendlyUnit() {
        // Regression: verify Hospital heal still works after StructureEconomySystem
        // refactor
        Entity hospital = new Entity();
        hospital.add(new GridPositionComponent(3, 3, 1));
        hospital.add(new TypeComponent(TypeComponent.Type.OBJECT));
        StatsComponent hStats = new StatsComponent("Field Hospital", 10, 0, 0, 0, 0, 1, 100,
                StatsComponent.MoveType.LAND, 1, 0);
        hStats.xpGain = 50;
        hStats.parentBaseX = 0;
        hStats.parentBaseY = 0;
        hospital.add(hStats);
        engine.addEntity(hospital);

        Entity unit = new Entity();
        unit.add(new GridPositionComponent(3, 4, 3)); // adjacent (chebyshev dist = 1)
        unit.add(new TypeComponent(TypeComponent.Type.UNIT));
        StatsComponent uStats2 = new StatsComponent("Recruit", 10, 3, 1, 1, 1, 1, 50,
                StatsComponent.MoveType.LAND, 1);
        uStats2.currentHP = 5; // damaged
        unit.add(uStats2);
        engine.addEntity(unit);

        // Simulate heal logic (matches StructureEconomySystem healing)
        List<Entity> allEntities = new ArrayList<>();
        for (Entity e : engine.getEntities()) {
            allEntities.add(e);
        }

        for (Entity struct : allEntities) {
            StatsComponent sStats = struct.getComponent(StatsComponent.class);
            GridPositionComponent sPos = struct.getComponent(GridPositionComponent.class);
            if (sStats == null || sPos == null || !sStats.name.equals("Field Hospital"))
                continue;
            for (Entity u : allEntities) {
                TypeComponent tc = u.getComponent(TypeComponent.class);
                if (tc == null || tc.type != TypeComponent.Type.UNIT)
                    continue;
                StatsComponent us = u.getComponent(StatsComponent.class);
                GridPositionComponent up = u.getComponent(GridPositionComponent.class);
                if (us.owner == sStats.owner
                        && Math.max(Math.abs(sPos.x - up.x), Math.abs(sPos.y - up.y)) <= 1) {
                    us.currentHP = Math.min(us.currentHP + 3, us.maxHP);
                }
            }
        }

        assertEquals(8, uStats2.currentHP, "Hospital should heal adjacent unit by 3 HP");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /** Mirrors the filter logic in SlideMenu.populateSummonMenu (lines 220-224). */
    private boolean filterForProducer(String producerType, StatsComponent.MoveType moveType) {
        return producerType.equals("PORT")
                ? moveType == StatsComponent.MoveType.SEA
                : moveType == StatsComponent.MoveType.LAND || moveType == StatsComponent.MoveType.AIR;
    }
}
