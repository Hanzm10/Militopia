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
    private final int playerID; 

    public FogSystem(MapGenerator.GameMap map, int playerID) {
        this.gameMap = map;
        this.playerID = playerID;
        this.priority = 0; 
    }

    @Override
    public void addedToEngine(com.badlogic.ashley.core.Engine engine) {
        entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class, StatsComponent.class).get());
    }

    @Override
    public void update(float deltaTime) {
        // 1. Reset Visibility (Fog everything)
        for (int x = 0; x < gameMap.width; x++) {
            for (int y = 0; y < gameMap.height; y++) {
                gameMap.visibleTiles[x][y] = false;
            }
        }

        // 2. Clear Fog
        for (Entity e : entities) {
            StatsComponent stats = e.getComponent(StatsComponent.class);
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);

            // Clear fog for CURRENT PLAYER's units
            if (stats.owner == 1 || stats.owner == 2) {
                clearFog(pos.x, pos.y, stats.vision);
            }
            // OPTIONAL: If you want Player 2 to ALSO clear fog (God Mode / Shared Vision), 
            // you can change the condition above to: 
            // if (stats.owner == 1 || stats.owner == 2)
        }
    }

    private void clearFog(int centerX, int centerY, int radius) {
        // FIX: Removed Manhattan check. Now it clears a full SQUARE (8 tiles around).
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