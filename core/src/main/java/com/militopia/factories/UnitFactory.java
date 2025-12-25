package com.militopia.factories;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.militopia.components.*;

public class UnitFactory {

    private final PooledEngine engine;

    // Store the textures here so we load them only once
    private final TextureRegion recruitLeftRegion;
    private final TextureRegion recruitRightRegion;

    public UnitFactory(PooledEngine engine) {
        this.engine = engine;

        // 1. Load the Textures (Make sure these files are in assets!)
        // Note: In a big game, you would use an AssetManager here.
        Texture texRight = new Texture("recruit_right.png");
        Texture texLeft = new Texture("recruit_left.png");

        this.recruitRightRegion = new TextureRegion(texRight);
        this.recruitLeftRegion = new TextureRegion(texLeft);
    }

    public void createRecruit(int x, int y, int owner) {
        Entity entity = engine.createEntity();

        // 1. GridPositionComponent (Matches your code: x, y, z)
        // We pass '2' because Units sit at zIndex 2 (above terrain and borders)
        GridPositionComponent pos = new GridPositionComponent(x, y, 2);
        entity.add(pos);

        // 2. TextureComponent
        // (Assuming you have a constructor that takes the region)
        TextureComponent tex = new TextureComponent(recruitRightRegion);
        entity.add(tex);

        // 3. FacingComponent
        // (Matches your FacingComponent constructor)
        FacingComponent facing = new FacingComponent(recruitLeftRegion, recruitRightRegion);
        entity.add(facing);

        // 4. TypeComponent
        // (Assuming you have a constructor that takes the Type enum)
        TypeComponent type = new TypeComponent(TypeComponent.Type.UNIT);
        entity.add(type);

        // 5. StatsComponent
        // (Matches your StatsComponent constructor)
        StatsComponent stats = new StatsComponent("Recruit", 1, 10, 5, StatsComponent.MoveType.LAND, owner);
        entity.add(stats);

        engine.addEntity(entity);
    }
}
