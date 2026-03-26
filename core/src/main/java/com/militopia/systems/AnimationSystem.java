package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Interpolation;
import com.militopia.components.AnimationComponent;
import com.militopia.components.GridPositionComponent;

public class AnimationSystem extends IteratingSystem {

    public AnimationSystem() {
        super(Family.all(AnimationComponent.class, GridPositionComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        AnimationComponent anim = entity.getComponent(AnimationComponent.class);
        GridPositionComponent pos = entity.getComponent(GridPositionComponent.class);

        if (anim.type == AnimationComponent.Type.NONE) {
            return;
        }

        anim.stateTime += deltaTime;

        if (anim.stateTime >= anim.duration) {
            // End of animation
            pos.visualOffsetX = 0;
            pos.visualOffsetY = 0;
            anim.type = AnimationComponent.Type.NONE;
            return;
        }

        float progress = anim.stateTime / anim.duration;

        switch (anim.type) {
            case NONE:
                break;
            case LUNGE:
                updateLunge(anim, pos, progress);
                break;
            case PROJECTILE:
                updateProjectile(anim, pos, progress);
                break;
            case HIT_FLASH:
                // Hit flash is handled by RenderSystem (batch color tinting)
                // but we could handle logic here if needed.
                break;
        }
    }

    private void updateLunge(AnimationComponent anim, GridPositionComponent pos, float progress) {
        // Lunge forward and back using a sine wave or similar curve
        // Sine(progress * PI) goes 0 -> 1 -> 0
        float intensity = (float) Math.sin(progress * Math.PI);
        pos.visualOffsetX = (anim.targetX - anim.startX) * intensity;
        pos.visualOffsetY = (anim.targetY - anim.startY) * intensity;
    }

    private void updateProjectile(AnimationComponent anim, GridPositionComponent pos, float progress) {
        // Linear travel from start to target
        float t = anim.interpolation.apply(progress);
        pos.visualOffsetX = (anim.targetX - anim.startX) * t;
        pos.visualOffsetY = (anim.targetY - anim.startY) * t;
    }
}
