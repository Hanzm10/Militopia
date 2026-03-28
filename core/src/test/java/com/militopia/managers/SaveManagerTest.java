package com.militopia.managers;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.config.UnitType;
import com.militopia.components.AnimalComponent;
import com.militopia.data.AnimalData;
import com.militopia.data.GameState;
import com.militopia.data.StructureData;
import com.militopia.data.UnitData;
import com.militopia.map.MapGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SaveManagerTest {

    private PooledEngine engine;
    private MapGenerator.GameMap map;
    private GameState state;
    private SaveManager saveManager;

    @BeforeEach
    public void setup() {
        engine = new PooledEngine();
        map = new MapGenerator.GameMap(10, 10);
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                map.terrain[x][y] = MapGenerator.TerrainType.GRASS;
                map.objects[x][y] = MapGenerator.ObjectType.NONE;
            }
        }
        state = new GameState(12345L, "TestSave", 10, 10);
        saveManager = new SaveManager();
    }

    // --- helpers ---

    private Entity addUnit(UnitType type, int x, int y, int owner) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 3));
        e.add(new TypeComponent(TypeComponent.Type.UNIT));
        StatsComponent stats = new StatsComponent(type.name(), 20, 5, 1, 2, 1, 3, 5,
                StatsComponent.MoveType.LAND, owner);
        stats.unitType = type;
        stats.unitTypeKey = type.name();
        stats.currentHP = 15;
        stats.hasMoved = true;
        stats.hasActed = false;
        e.add(stats);
        engine.addEntity(e);
        return e;
    }

    private Entity addStructure(int x, int y, int owner, int level, float xp,
                                String baseName, String baseOrdinal,
                                int xpGain, String chosenSuperUnit) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 3));
        e.add(new TypeComponent(TypeComponent.Type.OBJECT));
        StatsComponent stats = new StatsComponent(baseName, 100, 0, 10, 0, 0, 0, 0,
                StatsComponent.MoveType.LAND, owner);
        stats.level = level;
        stats.currentBaseXP = xp;
        stats.baseOrdinal = baseOrdinal;
        stats.xpGain = xpGain;
        stats.chosenSuperUnit = chosenSuperUnit;
        e.add(stats);
        engine.addEntity(e);
        return e;
    }

    private Entity addAnimal(int x, int y, String animalType) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 3));
        e.add(new TypeComponent(TypeComponent.Type.OBJECT));
        StatsComponent stats = new StatsComponent("ANIMAL_" + animalType, 5, 0, 0, 0, 0, 0, 0,
                StatsComponent.MoveType.LAND, 0);
        e.add(stats);
        e.add(new AnimalComponent(animalType));
        engine.addEntity(e);
        return e;
    }

    // --- tests ---

    @Test
    public void testUnitRoundTrip() {
        addUnit(UnitType.TANK, 3, 4, 1);
        saveManager.collectState(state, engine, map);

        assertEquals(1, state.units.size());
        UnitData u = state.units.get(0);
        assertEquals(3, u.x);
        assertEquals(4, u.y);
        assertEquals(1, u.owner);
        assertEquals(15, u.hp);
        assertEquals(20, u.maxHp);
        assertTrue(u.hasMoved);
        assertFalse(u.hasActed);
    }

    @Test
    public void testUnitTypeSavedAsEnumName() {
        addUnit(UnitType.TANK, 0, 0, 1);
        saveManager.collectState(state, engine, map);

        UnitData u = state.units.get(0);
        assertEquals("TANK", u.unitTypeKey);
    }

    @Test
    public void testUnitTypeKeyFallsBackToStringWhenEnumNull() {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(1, 1, 3));
        e.add(new TypeComponent(TypeComponent.Type.UNIT));
        StatsComponent stats = new StatsComponent("RECRUIT", 10, 3, 1, 1, 1, 3, 2,
                StatsComponent.MoveType.LAND, 2);
        stats.unitType = null;       // no enum
        stats.unitTypeKey = "RECRUIT";
        e.add(stats);
        engine.addEntity(e);

        saveManager.collectState(state, engine, map);

        assertEquals("RECRUIT", state.units.get(0).unitTypeKey);
    }

    @Test
    public void testStructureXpGainPersisted() {
        addStructure(2, 3, 1, 2, 500f, "BASE", "Alpha", 50, null);
        saveManager.collectState(state, engine, map);

        assertEquals(1, state.structures.size());
        assertEquals(50, state.structures.get(0).xpGain);
    }

    @Test
    public void testStructureChosenSuperUnitPersisted() {
        addStructure(2, 3, 1, 5, 2000f, "BASE", "Alpha", 0, "TITAN");
        saveManager.collectState(state, engine, map);

        assertEquals("TITAN", state.structures.get(0).chosenSuperUnit);
    }

    @Test
    public void testStructureFieldsRoundTrip() {
        addStructure(5, 6, 2, 3, 750f, "BASE", "Bravo", 30, "WRAITH");
        saveManager.collectState(state, engine, map);

        StructureData s = state.structures.get(0);
        assertEquals(5, s.x);
        assertEquals(6, s.y);
        assertEquals(2, s.owner);
        assertEquals(3, s.level);
        assertEquals(750f, s.currentBaseXP, 0.01f);
        assertEquals("BASE", s.baseName);
        assertEquals("Bravo", s.baseOrdinal);
        assertEquals(30, s.xpGain);
        assertEquals("WRAITH", s.chosenSuperUnit);
    }

    @Test
    public void testAnimalDetectedByComponent() {
        addAnimal(4, 5, "WOLF");
        saveManager.collectState(state, engine, map);

        assertEquals(0, state.units.size());
        assertEquals(0, state.structures.size());
        assertEquals(1, state.animals.size());
        AnimalData a = state.animals.get(0);
        assertEquals(4, a.x);
        assertEquals(5, a.y);
        assertEquals("WOLF", a.type);
    }

    @Test
    public void testNeutralTownSavedToStructures() {
        map.objects[7][7] = MapGenerator.ObjectType.TOWN;
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(7, 7, 3));
        e.add(new TypeComponent(TypeComponent.Type.OBJECT));
        StatsComponent stats = new StatsComponent("TOWN", 50, 0, 5, 0, 0, 0, 0,
                StatsComponent.MoveType.LAND, 0); // owner=0 neutral
        stats.level = 1;
        e.add(stats);
        engine.addEntity(e);

        saveManager.collectState(state, engine, map);

        assertEquals(1, state.structures.size());
        assertEquals(0, state.structures.get(0).owner);
    }

    @Test
    public void testMapObjectsReferenceSet() {
        saveManager.collectState(state, engine, map);
        assertSame(map.objects, state.mapObjects);
    }

    @Test
    public void testMultipleEntitiesAllCategorized() {
        addUnit(UnitType.RECRUIT, 0, 0, 1);
        addUnit(UnitType.RANGER, 1, 0, 2);
        addAnimal(2, 2, "DEER");
        addStructure(3, 3, 1, 1, 100f, "BASE", "Charlie", 10, null);

        saveManager.collectState(state, engine, map);

        assertEquals(2, state.units.size());
        assertEquals(1, state.structures.size());
        assertEquals(1, state.animals.size());
    }

    @Test
    public void testCollectStateClearsExistingData() {
        state.units.add(new UnitData(0, 0, "STALE", 1));
        state.structures.add(new StructureData(0, 0, 1, 1, 0f, "OLD", "", 0, null));

        saveManager.collectState(state, engine, map);

        assertEquals(0, state.units.size());
        assertEquals(0, state.structures.size());
    }
}
