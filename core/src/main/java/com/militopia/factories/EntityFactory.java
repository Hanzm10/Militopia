package com.militopia.factories;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Texture;
import com.militopia.components.*;

public class EntityFactory {
    private PooledEngine engine;
    
    // We only need textures for markers/effects here
    private Texture texMarker;

    public EntityFactory(PooledEngine engine) {
        this.engine = engine;
        texMarker = new Texture("marker_dot.png");
    }

    // --- DELETED createRecruit() (Moved to UnitFactory) ---

    public void createMovementMarker(int x, int y) {
        Entity entity = engine.createEntity();
        
        // Z-Index 3 puts it ON TOP of everything (Terrain=0, Unit=2)
        entity.add(new GridPositionComponent(x, y, 3));
        entity.add(new TextureComponent(texMarker));
        entity.add(new TypeComponent(TypeComponent.Type.MARKER));

        engine.addEntity(entity);
    }
}