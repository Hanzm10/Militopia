package com.militopia.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

/**
 * Attached to a unit performing a jump attack.
 * AnimationSystem sets {@code landed = true} when the JUMP animation completes,
 * and CombatSystem processes the landing effect that same frame.
 */
public class JumpLandingComponent implements Component {
    public Entity primaryTarget; // null if jumped to an empty tile
    public boolean landed = false;
}
