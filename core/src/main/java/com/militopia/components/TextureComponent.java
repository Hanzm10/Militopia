package com.militopia.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Texture;

public class TextureComponent implements Component {
    public Texture region;

    public TextureComponent(Texture region) {
        this.region = region;
    }
}