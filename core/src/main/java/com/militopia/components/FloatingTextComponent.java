package com.militopia.components;

import com.badlogic.ashley.core.Component;

/**
 * Attached to a short-lived entity that displays a floating damage number or
 * "BLOCKED" label above a unit in world-space.
 *
 * The UnitRenderSystem renders and ages these entities, removing them when
 * {@code timer >= MAX_TIME}.
 */
public class FloatingTextComponent implements Component {

    /** How long the text floats before disappearing (seconds). */
    public static final float MAX_TIME = 1.0f;

    /** The text to display, e.g. "5" or "BLOCKED". */
    public String text;

    /**
     * Iso-world X position (calculated once from the target's grid position).
     * This is the raw isometric X before the camera transform.
     */
    public float worldX;

    /**
     * Iso-world Y position (base, before the upward drift).
     */
    public float worldY;

    /** Elapsed time (counts up to MAX_TIME). */
    public float timer = 0f;

    /**
     * True when this text represents a counterattack result.
     * The UnitRenderSystem uses this to select the colour (same red for now,
     * kept as a hook for future styling).
     */
    public boolean isCounter;

    /** Current alpha for fading out */
    public float alpha = 1f;

    public FloatingTextComponent(String text, float worldX, float worldY, boolean isCounter) {
        this.text = text;
        this.worldX = worldX;
        this.worldY = worldY;
        this.isCounter = isCounter;
    }
}
