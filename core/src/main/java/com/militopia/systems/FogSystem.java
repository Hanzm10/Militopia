package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.config.StructureType;
import com.militopia.map.MapGenerator;

public class FogSystem extends EntitySystem {

    private final MapGenerator.GameMap gameMap;
    private ImmutableArray<Entity> entities;

    // Changed from final to allow switching
    private int playerID;

    public FogSystem(MapGenerator.GameMap map, int initialPlayerID) {
        this.gameMap = map;
        this.playerID = initialPlayerID;
        this.priority = 0;
    }

    // --- NEW: Switch Active Player ---
    public void setPlayer(int id) {
        this.playerID = id;
    }

    @Override
    public void addedToEngine(com.badlogic.ashley.core.Engine engine) {
        entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class, StatsComponent.class).get());
    }

    private boolean[][] jammerMask;

    @Override
    public void update(float deltaTime) {
        // 1. Reset Visibility and Jammer Mask
        if (jammerMask == null || jammerMask.length != gameMap.width || jammerMask[0].length != gameMap.height) {
            jammerMask = new boolean[gameMap.width][gameMap.height];
        }

        for (int x = 0; x < gameMap.width; x++) {
            for (int y = 0; y < gameMap.height; y++) {
                gameMap.visibleTiles[x][y] = false;
                gameMap.detectedTiles[x][y] = false; // Reset Detection
                jammerMask[x][y] = false;
            }
        }

        // 2. Identify Enemy Jammers (Blocking our vision)
        for (Entity e : entities) {
            StatsComponent stats = e.getComponent(StatsComponent.class);
            if (stats.owner != playerID && StructureType.fromDisplayName(stats.name) == StructureType.SIGNAL_JAMMER) {
                GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
                markJammingZone(pos.x, pos.y, 4); // Radius 4 for Static
            }
        }

        // 3. Clear Fog and Detect Stealth for Current Active Player
        for (Entity e : entities) {
            StatsComponent stats = e.getComponent(StatsComponent.class);
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);

            if (stats.owner == playerID) {
                int radius = stats.vision;
                // RADAR STATION: Scanner (+4 Vision)
                if (StructureType.fromDisplayName(stats.name) == StructureType.RADAR) {
                    radius += 4;
                }

                // --- NEW: Jammer Override ---
                // If the unit is inside a jammed zone, its vision radius is forced to 1.
                if (jammerMask[pos.x][pos.y]) {
                    radius = 1;
                }

                clearFog(pos.x, pos.y, radius);

                // --- NEW: Stealth Detection ---
                // All units can see cloaked enemies in adjacent tiles (radius 1)
                markDetectionZone(pos.x, pos.y, 1);
            }
        }
    }

    private void markDetectionZone(int centerX, int centerY, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                if (isValid(x, y)) {
                    gameMap.detectedTiles[x][y] = true;
                }
            }
        }
    }

    private void markJammingZone(int centerX, int centerY, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                if (isValid(x, y)) {
                    jammerMask[x][y] = true;
                }
            }
        }
    }

    private void clearFog(int centerX, int centerY, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                if (isValid(x, y)) {
                    // Normal tiles revealed up to radius.
                    // Jammed tiles ONLY revealed if within radius 1 of the unit.
                    int dist = Math.max(Math.abs(x - centerX), Math.abs(y - centerY));
                    if (!jammerMask[x][y] || dist <= 1) {
                        gameMap.visibleTiles[x][y] = true;
                    }
                }
            }
        }
    }

    private boolean isValid(int x, int y) {
        return x >= 0 && x < gameMap.width && y >= 0 && y < gameMap.height;
    }
}