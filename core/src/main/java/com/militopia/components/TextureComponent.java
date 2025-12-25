package com.militopia.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.TextureRegion; // Import this!

public class TextureComponent implements Component {
    public TextureRegion region; // Change from Texture to TextureRegion

    // Update constructor to accept TextureRegion
    public TextureComponent(TextureRegion region) {
        this.region = region;
    }
}