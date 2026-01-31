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
import com.militopia.data.AnimalData;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.map.MapGenerator;

public class SaveManager {

    public void saveGame(GameState state, PooledEngine engine, MapGenerator.GameMap map) {

        // 1. Clear old data lists
        state.units.clear();
        state.structures.clear();
        state.animals.clear();

        // 2. Extract Data from Engine
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class).get());

        for (Entity e : entities) {
            TypeComponent type = e.getComponent(TypeComponent.class);
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            StatsComponent stats = e.getComponent(StatsComponent.class);

            if (type.type == TypeComponent.Type.UNIT) {
                // Save Unit
                state.units.add(new UnitData(pos.x, pos.y, "RECRUIT", stats.owner));
            } else if (type.type == TypeComponent.Type.OBJECT) {

                // --- FIX: DETECT ANIMALS VIA NAME TAG (Layer 2) ---
                // We check the stats name because animals are no longer in map.objects
                if (stats != null && stats.name != null && stats.name.startsWith("ANIMAL_")) {
                    String animalType = stats.name.replace("ANIMAL_", "");
                    state.animals.add(new AnimalData(pos.x, pos.y, animalType));
                } // --- DETECT STRUCTURES VIA MAP DATA (Layer 1) ---
                else {
                    // Check the Map to see if this is a Base or Town
                    MapGenerator.ObjectType objType = map.objects[pos.x][pos.y];

                    if (objType == MapGenerator.ObjectType.BASE_P1
                            || objType == MapGenerator.ObjectType.BASE_P2
                            || objType == MapGenerator.ObjectType.TOWN) {

                        if (stats != null) {
                            state.structures.add(new StructureData(
                                    pos.x, pos.y, stats.owner,
                                    stats.currentBaseXP, stats.name, stats.baseOrdinal
                            ));
                        }
                    }
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
