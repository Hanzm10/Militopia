package com.militopia.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.data.GameState;
import com.militopia.map.MapGenerator;
import com.militopia.managers.AssetManager;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Texture;
import com.militopia.factories.UnitFactory;
import com.militopia.systems.StructurePlacementSystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StructurePlacementTest {
    private Engine engine;

    @BeforeEach
    public void setup() {
        engine = new Engine();
    }

    @Test
    public void testStructurePlacementConstraints() {
        PooledEngine pooledEngine = new PooledEngine();
        GameState state = new GameState(12345, "test", 10, 10);
        state.p1Funding = 100;
        MapGenerator.GameMap map = new MapGenerator.GameMap(10, 10);

        AssetManager mockAssets = mock(AssetManager.class);
        Texture mockTexture = mock(Texture.class);
        when(mockAssets.get(anyString())).thenReturn(mockTexture);
        UnitFactory factory = new UnitFactory(pooledEngine, mockAssets);

        StructurePlacementSystem system = new StructurePlacementSystem(pooledEngine, factory, state, map);

        // Initial update to ensure engine is clean and family matches are ready
        pooledEngine.update(0);

        // 1. Test Oil Derrick on Oil
        map.objects[5][5] = MapGenerator.ObjectType.OIL;
        assertTrue(system.canBuild("OIL_DERRICK", 5, 5, 1, 50, false, false, true), "Should build Oil Derrick on OIL");

        // 2. Test Non-Oil on Oil
        assertFalse(system.canBuild("TOWN", 5, 5, 1, 50, false, false, true), "Should NOT build Town on OIL");

        // 3. Test Oil Derrick on Non-Oil
        map.objects[5][6] = MapGenerator.ObjectType.NONE;
        assertFalse(system.canBuild("OIL_DERRICK", 5, 6, 1, 50, false, false, true),
                "Should NOT build Oil Derrick on empty");

        // 4. Test Port on Water (using 1,1 to avoid potential 0,0 issues)
        assertTrue(system.canBuild("PORT", 1, 1, 1, 50, true, true, false), "Should build Port on coastal water");

        // 5. Test Port on Land
        assertFalse(system.canBuild("PORT", 2, 2, 1, 50, false, false, true), "Should NOT build Port on land");
    }

    @Test
    public void testInsufficientFunds() {
        PooledEngine pooledEngine = new PooledEngine();
        GameState state = new GameState(12345, "test", 10, 10);
        state.p1Funding = 10;
        MapGenerator.GameMap map = new MapGenerator.GameMap(10, 10);
        UnitFactory mockFactory = mock(UnitFactory.class);

        StructurePlacementSystem system = new StructurePlacementSystem(pooledEngine, mockFactory, state, map);
        assertFalse(system.canBuild("TOWN", 3, 3, 1, 50, false, false, true), "Should block build due to cost");
    }
}
