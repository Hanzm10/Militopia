package com.militopia.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class FacingComponent implements Component {
    public TextureRegion leftRegion;
    public TextureRegion rightRegion;

    // Default constructor (needed for PooledEngine sometimes)
    public FacingComponent() {}

    // --- ADD THIS CONSTRUCTOR ---
    public FacingComponent(TextureRegion left, TextureRegion right) {
        this.leftRegion = left;
        this.rightRegion = right;
    }
}