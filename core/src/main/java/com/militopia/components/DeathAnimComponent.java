package com.militopia.components;

import com.badlogic.ashley.core.Component;

/**
 * Added to a unit entity when it reaches 0 HP.
 * The UnitRenderSystem drives the flash-red → fade animation and removes the
 * entity once {@code done} is true.
 */
public class DeathAnimComponent implements Component {

    /** Total duration of the death animation in seconds. */
    public static final float DURATION = 0.5f;

    /** Elapsed time since death started (counts up). */
    public float timer = 0f;

    /** Set to true by UnitRenderSystem when animation is complete. */
    public boolean done = false;
}
