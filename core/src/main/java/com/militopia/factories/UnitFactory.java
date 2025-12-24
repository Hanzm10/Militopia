package com.militopia.factories;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Texture;
import com.militopia.components.*;

public class UnitFactory {
    private PooledEngine engine;
    
    // Load textures once (or pass Game object if you prefer)
    private Texture texRecruit;

    public UnitFactory(PooledEngine engine) {
        this.engine = engine;
        // Ensure you have this image!
        texRecruit = new Texture("unit_recruit.png"); 
    }

    public void createRecruit(int x, int y) {
        Entity entity = engine.createEntity();

        // 1. Position & Visuals
        entity.add(new GridPositionComponent(x, y, 2)); // Z-Index 2 (Above base)
        entity.add(new TextureComponent(texRecruit));
        entity.add(new TypeComponent(TypeComponent.Type.UNIT));

        // 2. Stats & Constraints (The new part)
        // Name: Recruit, Range: 3, HP: 10, Atk: 5, Type: LAND
        entity.add(new StatsComponent("Recruit", 1, 10, 5, StatsComponent.MoveType.LAND));

        engine.addEntity(entity);
    }

    // Later you can add:
    // public void createTank(int x, int y) { ... }
    // public void createBoat(int x, int y) { ... }
}