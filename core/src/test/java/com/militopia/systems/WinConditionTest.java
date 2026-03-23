package com.militopia.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.militopia.components.StatsComponent;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.TextureComponent;
import com.militopia.data.GameState;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.AssetManager;
import com.militopia.map.MapGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.badlogic.gdx.graphics.Texture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class WinConditionTest {
    private GameState gameState;
    private UnitFactory factory;
    private Engine engine;
    private CombatSystem combatSystem;
    private MapGenerator.GameMap map;

    @BeforeEach
    public void setup() {
        gameState = new GameState();
        engine = new Engine();

        // Mock Assets to return dummy textures
        AssetManager assets = Mockito.mock(AssetManager.class);
        Texture mockTex = Mockito.mock(Texture.class);
        when(assets.get(anyString())).thenReturn(mockTex);

        // UnitFactory(PooledEngine engine, AssetManager assets)
        factory = new UnitFactory(new PooledEngine(), assets);

        // CombatSystem(MapGenerator.GameMap map, UnitFactory factory, GameState state)
        combatSystem = new CombatSystem(new MapGenerator.GameMap(10, 10), null, gameState);
        engine.addSystem(combatSystem);

        map = new MapGenerator.GameMap(16, 16);
    }

    @Test
    public void testBaseCreationIncrementsCount() {
        assertEquals(0, gameState.p1BaseCount);
        assertEquals(0, gameState.p2BaseCount);

        // createObjectEntity(int x, int y, MapGenerator.ObjectType type, GameState
        // state)
        factory.createObjectEntity(5, 5, MapGenerator.ObjectType.BASE_P1, gameState);
        assertEquals(1, gameState.p1BaseCount);
        assertEquals(0, gameState.p2BaseCount);

        factory.createObjectEntity(10, 10, MapGenerator.ObjectType.BASE_P2, gameState);
        assertEquals(1, gameState.p1BaseCount);
        assertEquals(1, gameState.p2BaseCount);
    }

    @Test
    public void testBaseDestructionDecrementsCount() {
        Entity base = engine.createEntity();
        StatsComponent stats = new StatsComponent("Test Base", 100, 0, 0, 0, 0, 1, 0, StatsComponent.MoveType.LAND, 1);
        base.add(stats);

        gameState.p1BaseCount = 1;
        combatSystem.flagDeath(base);
        assertEquals(0, gameState.p1BaseCount);
    }

    @Test
    public void testBaseCaptureUpdatesCounts() {
        Entity p1Base = engine.createEntity();
        StatsComponent stats = new StatsComponent("P1 Base", 100, 0, 0, 0, 0, 1, 0, StatsComponent.MoveType.LAND, 1);
        p1Base.add(stats);
        p1Base.add(new GridPositionComponent(5, 5, 1));
        p1Base.add(new TextureComponent(null));

        map.objects[5][5] = MapGenerator.ObjectType.BASE_P1;
        gameState.p1BaseCount = 1;
        gameState.p2BaseCount = 0;

        // captureStructure(Entity objectEntity, int newOwner, MapGenerator.GameMap map,
        // GameState state)
        factory.captureStructure(p1Base, 2, map, gameState);

        assertEquals(0, gameState.p1BaseCount);
        assertEquals(1, gameState.p2BaseCount);
        assertEquals(2, stats.owner);
    }

    @Test
    public void testTownCaptureIncrementsCount() {
        Entity town = engine.createEntity();
        StatsComponent stats = new StatsComponent("Town", 10, 0, 0, 0, 0, 1, 0, StatsComponent.MoveType.LAND, 0, 1);
        town.add(stats);
        town.add(new GridPositionComponent(5, 5, 1));
        town.add(new TextureComponent(null));

        map.objects[5][5] = MapGenerator.ObjectType.TOWN;
        gameState.p1BaseCount = 0;

        // Capture town for P1
        factory.captureStructure(town, 1, map, gameState);

        assertEquals(1, gameState.p1BaseCount);
        assertEquals(1, stats.owner);
    }
}
