package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.militopia.components.FloatingTextComponent;

public class FloatingTextSystem extends IteratingSystem {

    public FloatingTextSystem() {
        super(Family.all(FloatingTextComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        FloatingTextComponent ft = entity.getComponent(FloatingTextComponent.class);

        ft.timer += deltaTime;

        if (ft.timer >= FloatingTextComponent.MAX_TIME) {
            getEngine().removeEntity(entity);
            return;
        }

        float progress = ft.timer / FloatingTextComponent.MAX_TIME;
        ft.alpha = 1f - progress;
        ft.worldY += deltaTime * 18f; // drift 18 world units upward over 1s
    }
}
