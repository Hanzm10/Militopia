package com.militopia.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Component for entities that display a frame-based sprite animation (e.g., explosions).
 */
public class SpriteAnimationComponent implements Component {
    public Animation<TextureRegion> animation;
    public float stateTime = 0;
    public boolean loop = false;
    public boolean autoRemove = true;
    public float duration = 0;
    
    // Optional offset from the grid tile center
    public float worldOffsetX = 0;
    public float worldOffsetY = 0;

    // Optional size override (if 0, use GameConfig.DRAW_WIDTH/HEIGHT)
    public float drawWidth = 0;
    public float drawHeight = 0;
}
