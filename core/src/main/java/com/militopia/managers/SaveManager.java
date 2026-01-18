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
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;

public class SaveManager {

    public void saveGame(GameState state, PooledEngine engine) {
        // 1. Clear OLD data lists
        state.units.clear();
        state.structures.clear(); // Clear new list

        // 2. Extract Data
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
                // Save Structure (Base/Town)
                if (stats != null && (stats.owner == 1 || stats.owner == 2 || stats.income > 0)) {
                     // We only save relevant structures (Bases/Towns) that have stats
                    state.structures.add(new StructureData(
                        pos.x, pos.y, stats.owner, 
                        stats.currentBaseXP, stats.name, stats.baseOrdinal
                    ));
                }
            }
        }

        // 3. Write to JSON
        Json json = new Json();
        String jsonText = json.toJson(state);

        FileHandle file = Gdx.files.local("saves/" + state.saveName + ".json");
        file.writeString(jsonText, false);

        Gdx.app.log("SaveManager", "Game Saved to " + file.path());
    }
}