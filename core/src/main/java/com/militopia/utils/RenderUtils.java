package com.militopia.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;

public class RenderUtils {

    /**
     * Draws a texture that fills the entire screen while maintaining its aspect ratio.
     * Acts like CSS "background-size: cover".
     * Automatically corrects the projection matrix to prevent stretching on resize.
     */
    public static void drawProportionalBackground(Batch batch, Texture texture) {
        if (texture == null) return;

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // --- FIX: Reset the projection matrix to match the current screen size ---
        // This ensures 1 drawing unit = 1 pixel, preventing aspect ratio distortion.
        batch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH);
        // -----------------------------------------------------------------------

        float texW = texture.getWidth();
        float texH = texture.getHeight();

        // Calculate "Cover" Scale (fit to the larger dimension)
        float scale = Math.max(screenW / texW, screenH / texH);
        
        float drawnWidth = texW * scale;
        float drawnHeight = texH * scale;

        // Center the image
        float x = (screenW - drawnWidth) / 2;
        float y = (screenH - drawnHeight) / 2;

        batch.begin();
        batch.draw(texture, x, y, drawnWidth, drawnHeight);
        batch.end();
    }
}