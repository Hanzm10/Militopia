package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
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

    @Override
    public void update(float deltaTime) {
        // 1. Reset Visibility
        for (int x = 0; x < gameMap.width; x++) {
            for (int y = 0; y < gameMap.height; y++) {
                gameMap.visibleTiles[x][y] = false;
            }
        }

        // 2. Clear Fog
        for (Entity e : entities) {
            StatsComponent stats = e.getComponent(StatsComponent.class);
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);

            // FIX: Only clear fog for the CURRENT ACTIVE PLAYER
            if (stats.owner == playerID) {
                clearFog(pos.x, pos.y, stats.vision);
            }
        }
    }

    private void clearFog(int centerX, int centerY, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                if (isValid(x, y)) {
                    gameMap.visibleTiles[x][y] = true;
                }
            }
        }
    }

    private boolean isValid(int x, int y) {
        return x >= 0 && x < gameMap.width && y >= 0 && y < gameMap.height;
    }
}