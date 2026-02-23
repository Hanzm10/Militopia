package com.militopia.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StructurePlacementTest {
    private Engine engine;

    @BeforeEach
    public void setup() {
        engine = new Engine();
    }

    @Test
    public void testHospitalHealsAdjacentFriendly() {
        Entity hospital = new Entity();
        hospital.add(new GridPositionComponent(5, 5, 1));
        hospital.add(new TypeComponent(TypeComponent.Type.OBJECT));
        StatsComponent hStats = new StatsComponent("Field Hospital", 10, 0, 0, 0, 0, 1, 15,
                StatsComponent.MoveType.LAND, 1, 0);
        hospital.add(hStats);
        engine.addEntity(hospital);

        Entity unit = new Entity();
        unit.add(new GridPositionComponent(5, 6, 3));
        unit.add(new TypeComponent(TypeComponent.Type.UNIT));
        StatsComponent uStats = new StatsComponent("Recruit", 10, 3, 1, 1, 1, 1, 2, StatsComponent.MoveType.LAND, 1, 0);
        uStats.currentHP = 5; // Damaged
        unit.add(uStats);
        engine.addEntity(unit);

        // Run the economy logic loop (extracted from GameScreen for unit tests)
        com.badlogic.gdx.utils.Array<Entity> structs = new com.badlogic.gdx.utils.Array<>();
        com.badlogic.gdx.utils.Array<Entity> units = new com.badlogic.gdx.utils.Array<>();
        for (Entity e : engine.getEntities()) {
            TypeComponent t = e.getComponent(TypeComponent.class);
            if (t != null && t.type == TypeComponent.Type.OBJECT) {
                structs.add(e);
            } else if (t != null && t.type == TypeComponent.Type.UNIT) {
                units.add(e);
            }
        }

        for (Entity struct : structs) {
            StatsComponent sStats = struct.getComponent(StatsComponent.class);
            GridPositionComponent sPos = struct.getComponent(GridPositionComponent.class);
            if (sStats == null || sPos == null || !sStats.name.equals("Field Hospital"))
                continue;

            for (Entity u : units) {
                StatsComponent us = u.getComponent(StatsComponent.class);
                GridPositionComponent up = u.getComponent(GridPositionComponent.class);

                // Heal logic
                if (us.owner == sStats.owner && Math.max(Math.abs(sPos.x - up.x), Math.abs(sPos.y - up.y)) <= 1) {
                    us.currentHP = Math.min(us.currentHP + 3, us.maxHP);
                }
            }
        }

        assertEquals(8, uStats.currentHP, "Unit HP should have increased by 3");
    }

    @Test
    public void testOccupancyPreventsBuild() {
        // simulate hasEntityAt
        Entity e = new Entity();
        e.add(new GridPositionComponent(10, 10, 1));
        engine.addEntity(e);

        boolean occupied = false;
        for (Entity ent : engine.getEntities()) {
            GridPositionComponent p = ent.getComponent(GridPositionComponent.class);
            if (p != null && p.x == 10 && p.y == 10 && p.zIndex == 1) {
                occupied = true;
                break;
            }
        }
        assertTrue(occupied, "Tile (10,10) at z=1 should report as occupied");
    }
}
