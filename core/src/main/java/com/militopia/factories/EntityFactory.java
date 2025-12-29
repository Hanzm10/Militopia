package com.militopia.factories;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.militopia.components.*;
import com.militopia.managers.AssetManager;

public class EntityFactory {
    private PooledEngine engine;
    private TextureRegion markerRegion; 

    // UPDATE CONSTRUCTOR
    public EntityFactory(PooledEngine engine, AssetManager assets) {
        this.engine = engine;
        this.markerRegion = new TextureRegion(assets.get(AssetManager.MARKER_DOT));
    }

    public void createMovementMarker(int x, int y) {
        Entity entity = engine.createEntity();
        entity.add(new GridPositionComponent(x, y, 3));
        entity.add(new TextureComponent(markerRegion)); 
        entity.add(new TypeComponent(TypeComponent.Type.MARKER));
        engine.addEntity(entity);
    }
}