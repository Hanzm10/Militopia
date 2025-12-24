package com.militopia.managers;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.militopia.data.GameState;
import com.militopia.data.GameState;
import com.militopia.data.UnitData;
import com.militopia.data.UnitData;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.TypeComponent;

public class SaveManager {

    /**
     * Saves the current game state to a JSON file.
     * * @param seed      The map seed
     * @param p1Name    Player 1 Name
     * @param p2Name    Player 2 Name
     * @param saveName  The filename (without .json)
     * @param engine    The ECS engine (to find and save units)
     */
    public void saveGame(long seed, String p1Name, String p2Name, String saveName, PooledEngine engine) {
        
        // 1. Create the Data Container
        GameState state = new GameState(seed, p1Name, p2Name, saveName);

        // 2. Extract Units from the Engine
        // We look for everything that has a GridPosition and is tagged as a UNIT
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class, TypeComponent.class).get());

        for (Entity e : entities) {
            TypeComponent type = e.getComponent(TypeComponent.class);
            
            // Only save actual UNITs (Ignore markers, effects, etc.)
            if (type.type == TypeComponent.Type.UNIT) {
                GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
                
                // Currently hardcoded to "RECRUIT". 
                // Later, you can check StatsComponent to save "TANK", "ARCHER", etc.
                state.units.add(new UnitData(pos.x, pos.y, "RECRUIT"));
            }
        }

        // 3. Convert to JSON
        Json json = new Json();
        String text = json.toJson(state);

        // 4. Write to File
        try {
            FileHandle file = Gdx.files.local("saves/" + saveName + ".json");
            file.writeString(text, false);
            System.out.println("Game saved successfully to: " + file.path());
        } catch (Exception e) {
            System.err.println("Failed to save game: " + e.getMessage());
            e.printStackTrace();
        }
    }
}