package com.militopia.managers;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.militopia.data.GameState;
import com.militopia.data.StructureData;
import com.militopia.data.UnitData;
import com.militopia.data.AnimalData; // Import AnimalData
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.map.MapGenerator; // Import MapGenerator

public class SaveManager {

    // --- UPDATED SIGNATURE: Added GameMap map ---
    public void saveGame(GameState state, PooledEngine engine, MapGenerator.GameMap map) {

        // 1. Clear old data lists
        state.units.clear();
        state.structures.clear();
        state.animals.clear(); // Clear animals

        // 2. Extract Data from Engine
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class).get());

        for (Entity e : entities) {
            TypeComponent type = e.getComponent(TypeComponent.class);
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            StatsComponent stats = e.getComponent(StatsComponent.class);

            if (type.type == TypeComponent.Type.UNIT) {
                // Save Unit
                state.units.add(new UnitData(pos.x, pos.y, "RECRUIT", stats.owner));
            } 
            else if (type.type == TypeComponent.Type.OBJECT) {
                // Check the Map to see what this object really is
                MapGenerator.ObjectType objType = map.objects[pos.x][pos.y];

                // A. Save Structures (Bases / Towns)
                if (objType == MapGenerator.ObjectType.BASE_P1 || 
                    objType == MapGenerator.ObjectType.BASE_P2 || 
                    objType == MapGenerator.ObjectType.TOWN) {
                    
                    if (stats != null) {
                        state.structures.add(new StructureData(
                            pos.x, pos.y, stats.owner, 
                            stats.currentBaseXP, stats.name, stats.baseOrdinal
                        ));
                    }
                }
                // B. Save Animals
                else if (objType == MapGenerator.ObjectType.HORSE ||
                         objType == MapGenerator.ObjectType.FISH ||
                         objType == MapGenerator.ObjectType.DEER ||
                         objType == MapGenerator.ObjectType.ZEBRA) {
                    
                    state.animals.add(new AnimalData(pos.x, pos.y, objType.name()));
                }
            }
        }

        // 3. Write to JSON File
        Json json = new Json();
        String jsonText = json.toJson(state);

        FileHandle file = Gdx.files.local("saves/" + state.saveName + ".json");
        file.writeString(jsonText, false);

        System.out.println("Game Saved: " + file.path());
    }
}