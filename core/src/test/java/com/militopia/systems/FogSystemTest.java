package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.config.CombatConstants;
import com.militopia.config.StructureType;
import com.militopia.map.MapGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FogSystemTest {

    private PooledEngine engine;
    private FogSystem fogSystem;
    private MapGenerator.GameMap gameMap;

    @BeforeEach
    public void setup() {
        engine = new PooledEngine();
        gameMap = new MapGenerator.GameMap(20, 20);
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                gameMap.terrain[x][y] = MapGenerator.TerrainType.GRASS;
                gameMap.objects[x][y] = MapGenerator.ObjectType.NONE;
            }
        }
        fogSystem = new FogSystem(gameMap, 1);
        engine.addSystem(fogSystem);
    }

    private Entity createUnit(String name, int x, int y, int owner, int vision) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 3));
        StatsComponent stats = new StatsComponent(name, 10, 3, 1, 2, 1, vision, 100,
                StatsComponent.MoveType.LAND, owner);
        e.add(stats);
        engine.addEntity(e);
        return e;
    }

    // -------------------------------------------------------------------------
    // Basic fog clearing
    // -------------------------------------------------------------------------

    @Test
    public void testUnitClearsFogWithinVisionRadius() {
        createUnit("RECRUIT", 10, 10, 1, 2);
        fogSystem.update(0f);

        assertTrue(gameMap.visibleTiles[10][10], "Unit tile should be visible");
        assertTrue(gameMap.visibleTiles[10][12], "2 tiles north should be visible");
        assertTrue(gameMap.visibleTiles[12][12], "Diagonal 2 tiles should be visible");
    }

    @Test
    public void testFogBeyondVisionRadiusNotCleared() {
        createUnit("RECRUIT", 10, 10, 1, 2);
        fogSystem.update(0f);

        assertFalse(gameMap.visibleTiles[10][13], "3 tiles away should remain foggy (radius=2)");
        assertFalse(gameMap.visibleTiles[13][10], "3 tiles right should remain foggy (radius=2)");
    }

    @Test
    public void testEnemyUnitsDoNotClearFog() {
        createUnit("RECRUIT", 5, 5, 2, 3); // owner=2, active player=1
        fogSystem.update(0f);

        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                assertFalse(gameMap.visibleTiles[x][y], "Enemy unit should not clear any fog");
            }
        }
    }

    @Test
    public void testMultipleUnitsUnionTheirVision() {
        createUnit("RECRUIT", 5, 10, 1, 1);
        createUnit("RECRUIT", 15, 10, 1, 1);
        fogSystem.update(0f);

        assertTrue(gameMap.visibleTiles[5][10]);
        assertTrue(gameMap.visibleTiles[15][10]);
        assertFalse(gameMap.visibleTiles[10][10], "Gap between units should remain foggy");
    }

    // -------------------------------------------------------------------------
    // Radar Station vision bonus
    // -------------------------------------------------------------------------

    @Test
    public void testRadarStationGetsVisionBonus() {
        int baseVision = 2;
        createUnit(StructureType.RADAR.getDisplayName(), 10, 10, 1, baseVision);
        fogSystem.update(0f);

        int expectedRadius = baseVision + CombatConstants.RADAR_VISION_BONUS;
        assertTrue(gameMap.visibleTiles[10][10 + expectedRadius],
                "Radar should see " + expectedRadius + " tiles");
        assertFalse(gameMap.visibleTiles[10][10 + expectedRadius + 1],
                "Radar should not see beyond its extended radius");
    }

    // -------------------------------------------------------------------------
    // Jammer suppression
    // -------------------------------------------------------------------------

    @Test
    public void testEnemyJammerSuppressesVisionInsideZone() {
        // Enemy jammer at (10,10) with JAMMER_RADIUS=4
        createUnit(StructureType.SIGNAL_JAMMER.getDisplayName(), 10, 10, 2, 0);
        // Friendly unit at (10,11) — inside jammer zone (dist=1 <= 4), vision=5
        createUnit("RECRUIT", 10, 11, 1, 5);
        fogSystem.update(0f);

        // Vision suppressed to JAMMER_SUPPRESSED_VISION (1); tile at dist=2 stays foggy
        assertFalse(gameMap.visibleTiles[10][13], "Jammer should suppress vision inside zone to radius 1");
    }

    @Test
    public void testJammerDoesNotSuppressUnitOutsideZone() {
        // Enemy jammer at (10,10) with JAMMER_RADIUS=4
        createUnit(StructureType.SIGNAL_JAMMER.getDisplayName(), 10, 10, 2, 0);
        // Friendly unit at (10,15) — outside jammer zone (dist=5 > 4), vision=3
        createUnit("RECRUIT", 10, 15, 1, 3);
        fogSystem.update(0f);

        // Full vision=3 applies; tile at dist=3 visible
        assertTrue(gameMap.visibleTiles[10][18], "Unit outside jammer zone should have full vision");
    }

    @Test
    public void testFriendlyJammerDoesNotSuppressFriendlyUnits() {
        // Friendly jammer — owner=1 same as active player; should NOT mark jammerMask
        createUnit(StructureType.SIGNAL_JAMMER.getDisplayName(), 10, 10, 1, 0);
        createUnit("RECRUIT", 10, 11, 1, 3);
        fogSystem.update(0f);

        // Full vision=3 applies; tile at dist=3 visible
        assertTrue(gameMap.visibleTiles[10][14], "Friendly jammer should not suppress own units");
    }

    // -------------------------------------------------------------------------
    // Stealth detection tiles
    // -------------------------------------------------------------------------

    @Test
    public void testStealthDetectionMarksTilesInRadius() {
        createUnit("RECRUIT", 10, 10, 1, 2);
        fogSystem.update(0f);

        // STEALTH_DETECTION_RADIUS=1 — adjacent tiles marked
        assertTrue(gameMap.detectedTiles[10][10], "Unit tile should be detected");
        assertTrue(gameMap.detectedTiles[10][11], "Adjacent tile should be detected");
        assertTrue(gameMap.detectedTiles[11][11], "Diagonal adjacent tile should be detected");
    }

    @Test
    public void testStealthDetectionDoesNotExtendBeyondRadius() {
        createUnit("RECRUIT", 10, 10, 1, 2);
        fogSystem.update(0f);

        assertFalse(gameMap.detectedTiles[10][12],
                "Tile at dist=2 should not be detected (STEALTH_DETECTION_RADIUS=1)");
    }

    // -------------------------------------------------------------------------
    // setPlayer
    // -------------------------------------------------------------------------

    @Test
    public void testSetPlayerSwitchesActivePlayer() {
        createUnit("RECRUIT", 5, 5, 1, 2);
        createUnit("RECRUIT", 15, 15, 2, 2);

        fogSystem.setPlayer(2);
        fogSystem.update(0f);

        // Player 2's unit clears fog; player 1's unit does not
        assertTrue(gameMap.visibleTiles[15][15], "P2 unit should clear fog when P2 is active");
        assertFalse(gameMap.visibleTiles[5][5], "P1 unit should not clear fog when P2 is active");
    }

    @Test
    public void testFogIsResetEachUpdate() {
        // First update: unit at (5,5) clears fog there
        createUnit("RECRUIT", 5, 5, 1, 2);
        fogSystem.update(0f);
        assertTrue(gameMap.visibleTiles[5][5]);

        // Remove all entities and update again — fog should reset
        engine.removeAllEntities();
        // Re-add the system since removeAllEntities doesn't remove systems
        fogSystem.update(0f);

        assertFalse(gameMap.visibleTiles[5][5], "Fog should reset each update when no units are present");
    }
}
