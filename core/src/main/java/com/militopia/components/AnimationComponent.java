package com.militopia.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Interpolation;

public class AnimationComponent implements Component {

    public enum Type {
        NONE,
        LUNGE,      // Melee attack movement
        PROJECTILE, // Ranged attack movement
        HIT_FLASH   // Visual feedback on hit
    }

    public Type type = Type.NONE;
    public float stateTime = 0;
    public float duration = 0.3f; // Default duration
    
    // For Lunge/Projectile
    public float startX, startY;
    public float targetX, targetY;
    
    public Interpolation interpolation = Interpolation.linear;
    
    // For frame-based animations (placeholder support)
    public boolean isFrameBased = false;
    public String animationKey;
    public boolean loop = false;

    public void reset() {
        type = Type.NONE;
        stateTime = 0;
        isFrameBased = false;
        animationKey = null;
    }
}
