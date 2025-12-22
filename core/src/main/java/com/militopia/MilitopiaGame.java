package com.militopia;

import com.badlogic.gdx.Game; // Note: We extend 'Game', not 'ApplicationAdapter'
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MilitopiaGame extends Game {
    // The SpriteBatch is heavy, so we create it once here and share it with all screens
    public SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();
        // INSTANTLY switch to the GameScreen for testing (skip menu for now)
        this.setScreen(new GameScreen(this));
    }

    @Override
    public void render() {
        super.render(); // This delegates the render method to the active screen
    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}