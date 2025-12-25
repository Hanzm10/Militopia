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
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;

public class SaveManager {

    /**
     * Saves the current game state to a JSON file.
     *
     * * @param seed The map seed
     * @param p1Name Player 1 Name
     * @param p2Name Player 2 Name
     * @param saveName The filename (without .json)
     * @param engine The ECS engine (to find and save units)
     */
    public void saveGame(GameState state, PooledEngine engine) {

        // 1. You don't need to create a new GameState anymore.
        //    Just clear the old lists inside the existing one to avoid duplicates.
        state.units.clear();

        // 2. Extract Data from Engine (Entities)
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class).get());

        for (Entity e : entities) {
            TypeComponent type = e.getComponent(TypeComponent.class);
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);

            if (type.type == TypeComponent.Type.UNIT) {
                StatsComponent stats = e.getComponent(StatsComponent.class);
                // Save unit with OWNER
                state.units.add(new UnitData(pos.x, pos.y, "RECRUIT", stats.owner));
            }
        }

        // 3. Write to JSON File
        Json json = new Json();
        String jsonText = json.toJson(state);

        // Use state.saveName for the filename
        FileHandle file = Gdx.files.local("saves/" + state.saveName + ".json");
        file.writeString(jsonText, false);

        System.out.println("Game Saved: " + file.path());
    }
}
