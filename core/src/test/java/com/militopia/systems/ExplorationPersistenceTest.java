package com.militopia.systems;

import com.militopia.components.TextureComponent;
import com.militopia.data.StructureData;
import com.badlogic.gdx.graphics.Texture;
import com.militopia.data.UnitData;
import com.militopia.data.GameState;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.AssetManager;
import com.militopia.managers.SaveManager;
import com.militopia.factories.EntityFactory;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.core.Entity;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.map.MapGenerator;
import com.militopia.config.BaseLevelConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ExplorationPersistenceTest {

    @Test
    public void testScavengeSystemRewards() {
        PooledEngine engine = new PooledEngine();
        GameState state = new GameState(12345, "test", 10, 10);
        state.p1Funding = 10;
        state.p1XP = 0;
        MapGenerator.GameMap map = new MapGenerator.GameMap(10, 10);
        map.objects[5][5] = MapGenerator.ObjectType.RUINS;

        AssetManager mockAssets = mock(AssetManager.class);
        Texture mockTexture = mock(Texture.class);
        when(mockAssets.get(anyString())).thenReturn(mockTexture);
        UnitFactory factory = new UnitFactory(engine, mockAssets);

        Entity ruins = engine.createEntity();
        ruins.add(new GridPositionComponent(5, 5, 1));
        ruins.add(new TypeComponent(TypeComponent.Type.OBJECT));
        engine.addEntity(ruins);

        Entity unit = engine.createEntity();
        unit.add(new GridPositionComponent(5, 5, 3));
        unit.add(new TypeComponent(TypeComponent.Type.UNIT));
        unit.add(new StatsComponent("Recruit", 10, 3, 1, 1, 1, 1, 1, StatsComponent.MoveType.LAND, 1));
        engine.addEntity(unit);

        // Process additions
        engine.update(0);

        EntityFactory mockEntityFactory = mock(EntityFactory.class);
        ScavengeSystem system = new ScavengeSystem(engine, factory, mockEntityFactory, state, map);
        ScavengeSystem.ScavengeReward reward = system.performScavenge(ruins, unit);

        // Process removals with small delta
        engine.update(0.1f);
        engine.update(0.1f);

        assertNotNull(reward);
        assertTrue(unit.getComponent(StatsComponent.class).hasActed);
        assertEquals(MapGenerator.ObjectType.NONE, map.objects[5][5]);

        // Entity should be gone from Layer 1
        Entity atLayer1 = factory.getEntityAt(5, 5, 1);
        assertTrue(atLayer1 == null || atLayer1 != ruins, "Ruins entity must be removed or replaced");

        // Final state check (funding or XP or unit should have changed)
        assertTrue(state.p1Funding > 10 || state.p1XP > 0 || engine.getEntities().size() > 1);
    }

    @Test
    public void testUnitDataCapturesFlags() {
        PooledEngine engine = new PooledEngine();
        Entity unit = engine.createEntity();
        unit.add(new GridPositionComponent(1, 1, 3));
        unit.add(new TypeComponent(TypeComponent.Type.UNIT));
        StatsComponent stats = new StatsComponent("Tank", 10, 2, 2, 1, 1, 1, 5, StatsComponent.MoveType.LAND, 1);
        stats.hasMoved = true;
        stats.hasActed = true;
        unit.add(stats);

        GridPositionComponent pos = unit.getComponent(GridPositionComponent.class);
        UnitData data = new UnitData(pos.x, pos.y, stats.name, stats.owner, stats.unitTypeKey, stats.currentHP,
                stats.maxHP, stats.hasMoved, stats.hasActed, false, false);
        assertTrue(data.hasMoved);
        assertTrue(data.hasActed);
        assertEquals(1, data.x);
        assertEquals(1, data.y);
    }
}
