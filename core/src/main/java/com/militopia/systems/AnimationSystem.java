package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Interpolation;
import com.militopia.components.AnimationComponent;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.JumpLandingComponent;

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

        // Early damage trigger (e.g. Juggernaut jump fires damage before animation ends)
        if (anim.damageTime > 0 && !anim.damageFired && anim.stateTime >= anim.damageTime) {
            anim.damageFired = true;
            if (anim.type == AnimationComponent.Type.JUMP) {
                JumpLandingComponent jlc = entity.getComponent(JumpLandingComponent.class);
                if (jlc != null) jlc.landed = true;
            }
        }

        if (anim.stateTime >= anim.duration) {
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
                break;
            case JUMP:
                updateJump(anim, pos, progress);
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

    private void updateJump(AnimationComponent anim, GridPositionComponent pos, float progress) {
        // Linear XY travel from source to destination + parabolic arc
        // jumpStartOff is the negative of the travel delta (so at progress=0 we appear at source)
        pos.visualOffsetX = anim.jumpStartOffX * (1 - progress);
        // sin(progress * PI) traces a parabolic arc: 0 at start, peaks at 1 at midpoint, 0 at end.
        // Multiplied by arcHeight to control the peak altitude of the jump.
        pos.visualOffsetY = anim.jumpStartOffY * (1 - progress)
                + anim.arcHeight * (float) Math.sin(progress * Math.PI);
    }
}
