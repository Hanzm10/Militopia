package com.militopia.factories;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion; // Import this
import com.militopia.components.*;

public class EntityFactory {
    private PooledEngine engine;
    
    // Store it as a Region, not just a Texture
    private TextureRegion markerRegion; 

    public EntityFactory(PooledEngine engine) {
        this.engine = engine;
        
        // Load Texture and immediately wrap it in a Region
        Texture tex = new Texture("marker_dot.png");
        this.markerRegion = new TextureRegion(tex);
    }

    public void createMovementMarker(int x, int y) {
        Entity entity = engine.createEntity();
        
        // Z-Index 3 puts it ON TOP of everything
        entity.add(new GridPositionComponent(x, y, 3));
        
        // Fix: Pass the TextureRegion (markerRegion) instead of a Texture
        entity.add(new TextureComponent(markerRegion)); 
        
        entity.add(new TypeComponent(TypeComponent.Type.MARKER));

        engine.addEntity(entity);
    }
}