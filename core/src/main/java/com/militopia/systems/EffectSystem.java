package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.militopia.components.SpriteAnimationComponent;

/**
 * Updates the state time of sprite animations and removes them when finished.
 */
public class EffectSystem extends IteratingSystem {
    public EffectSystem() {
        super(Family.all(SpriteAnimationComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        SpriteAnimationComponent anim = entity.getComponent(SpriteAnimationComponent.class);
        anim.stateTime += deltaTime;

        if (anim.autoRemove && !anim.loop && anim.animation.isAnimationFinished(anim.stateTime)) {
            getEngine().removeEntity(entity);
        }
    }
}
