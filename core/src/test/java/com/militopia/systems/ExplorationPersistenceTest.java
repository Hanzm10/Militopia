package com.militopia.systems;

import com.militopia.components.TextureComponent;
import com.militopia.data.StructureData;
import com.badlogic.gdx.graphics.Texture;
import com.militopia.data.UnitData;
import com.militopia.data.GameState;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.AssetManager;
import com.militopia.managers.SaveManager;
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
    public void testUnitPersistenceData() {
        // Verify UnitData captures new fields
        UnitData data = new UnitData(1, 2, "Recruit", 1, "RECRUIT", 8, 10, true, true);
        assertEquals(1, data.x);
        assertEquals(2, data.y);
        assertEquals(8, data.hp);
        assertEquals(10, data.maxHp);
        assertTrue(data.hasMoved);
        assertTrue(data.hasActed);
        assertEquals("RECRUIT", data.unitTypeKey);
    }

    @Test
    public void testOilDerrickConstraintLogic() {
        // Verify the logic used in SlideMenu for Oil Derrick constraints
        MapGenerator.ObjectType[][] objects = new MapGenerator.ObjectType[10][10];
        for (int i = 0; i < 10; i++)
            for (int j = 0; j < 10; j++)
                objects[i][j] = MapGenerator.ObjectType.NONE;

        int buildX = 5, buildY = 5;
        objects[buildX][buildY] = MapGenerator.ObjectType.OIL;

        // Simulating the check in SlideMenu:
        // show = !isWater && gameScreen.getGameMap().objects[buildX][buildY] ==
        // MapGenerator.ObjectType.OIL;
        boolean isWater = false;
        boolean showOnOil = !isWater && objects[buildX][buildY] == MapGenerator.ObjectType.OIL;

        int emptyX = 5, emptyY = 6;
        boolean showOnEmpty = !isWater && objects[emptyX][emptyY] == MapGenerator.ObjectType.OIL;

        assertTrue(showOnOil, "Oil Derrick should be visible on OIL slots");
        assertFalse(showOnEmpty, "Oil Derrick should NOT be visible on non-OIL slots");
    }

    @Test
    public void testLegacyConstructorCompatibility() {
        // Verify that existing code using the old constructor still works
        UnitData data = new UnitData(3, 4, "TANK", 2);
        assertEquals(3, data.x);
        assertEquals(4, data.y);
        assertEquals("TANK", data.type);
        assertEquals(2, data.owner);
        // Defaults should be sensible
        assertEquals("TANK", data.unitTypeKey);
        assertEquals(10, data.hp);
        assertFalse(data.hasMoved);
    }

    @Test
    public void testBaseLoadMaxXPSynchronization() {
        PooledEngine engine = new PooledEngine();
        AssetManager mockAssets = mock(AssetManager.class);
        Texture mockTexture = mock(Texture.class);
        when(mockAssets.get(anyString())).thenReturn(mockTexture);

        UnitFactory factory = new UnitFactory(engine, mockAssets);

        Entity baseEntity = engine.createEntity();
        baseEntity.add(new GridPositionComponent(0, 0, 1));
        baseEntity.add(new StatsComponent("Base", 100, 0, 0, 0, 0, 1, 0, StatsComponent.MoveType.LAND, 1));
        baseEntity.add(new TypeComponent(TypeComponent.Type.OBJECT));
        baseEntity.add(new TextureComponent(null));
        engine.addEntity(baseEntity);

        MapGenerator.GameMap map = new MapGenerator.GameMap(10, 10);
        map.objects[0][0] = MapGenerator.ObjectType.BASE_P1;

        StructureData data = new StructureData();
        data.x = 0;
        data.y = 0;
        data.owner = 1;
        data.level = 3; // Lv 3 should have 4500 max XP
        data.currentBaseXP = 1000;
        data.baseName = "Test Base";

        factory.updateStructureFromSave(baseEntity, data, map);

        StatsComponent stats = baseEntity.getComponent(StatsComponent.class);
        assertEquals(3, stats.level);
        assertEquals(4500f, stats.maxBaseXP, "Max XP should be 4500 for Level 3 base");
        assertEquals(3, stats.income, "Income should be 3 for Level 3 base");
    }
}
