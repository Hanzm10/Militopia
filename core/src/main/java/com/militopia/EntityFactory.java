package com.militopia;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Texture;
import com.militopia.components.*;

public class EntityFactory {
    private PooledEngine engine;
    private MilitopiaGame game; // Access to textures

    public EntityFactory(PooledEngine engine, MilitopiaGame game) {
        this.engine = engine;
        this.game = game;
    }

    public void createRecruit(int x, int y) {
        Entity entity = engine.createEntity();
        
        // Z-Index 2 puts it ON TOP of the Base (Index 1)
        entity.add(new GridPositionComponent(x, y, 2)); 
        entity.add(new TextureComponent(new Texture("unit_recruit.png")));
        entity.add(new TypeComponent(TypeComponent.Type.UNIT));
        entity.add(new StatsComponent(1, "Recruit"));

        engine.addEntity(entity);
    }

    public void createMovementMarker(int x, int y) {
        Entity entity = engine.createEntity();
        
        // Z-Index 3 puts it ON TOP of everything
        entity.add(new GridPositionComponent(x, y, 3));
        entity.add(new TextureComponent(new Texture("marker_dot.png"))); // Small white circle
        entity.add(new TypeComponent(TypeComponent.Type.MARKER));

        engine.addEntity(entity);
    }
}