package com.militopia.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;
import com.militopia.utils.GameLogger;

/**
 * Singleton that manages the looping video background for all non-game screens.
 *
 * Usage per screen:
 *   show()    → VideoBackgroundManager.getInstance().play()
 *   render()  → VideoBackgroundManager.getInstance().render(batch)
 *   hide()    → VideoBackgroundManager.getInstance().pause()
 *
 * MilitopiaGame.dispose() must call VideoBackgroundManager.getInstance().dispose().
 */
public class VideoBackgroundManager {

    private static final String VIDEO_PATH = "game-system/militopia-video-bg.webm";

    private static VideoBackgroundManager instance;

    private VideoPlayer player;
    private boolean loaded = false;
    private boolean loopPending = false; // set to true by completion listener

    private VideoBackgroundManager() {
        init();
    }

    public static VideoBackgroundManager getInstance() {
        if (instance == null) {
            instance = new VideoBackgroundManager();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Initialise / load
    // -------------------------------------------------------------------------

    private void init() {
        try {
            player = VideoPlayerCreator.createVideoPlayer();
            FileHandle file = Gdx.files.internal(VIDEO_PATH);
            if (!file.exists()) {
                GameLogger.logScreen("[VideoBackground] Video file not found: " + VIDEO_PATH);
                return;
            }
            player.load(file);
            player.setVolume(0f); // background — silent

            // Loop: when video ends, flag a restart for the next render() call
            player.setOnCompletionListener(new VideoPlayer.CompletionListener() {
                @Override
                public void onCompletionListener(FileHandle file) {
                    loopPending = true;
                }
            });

            player.play();
            loaded = true;
            GameLogger.logScreen("[VideoBackground] Loaded and playing: " + VIDEO_PATH);
        } catch (Exception e) {
            GameLogger.logScreen("[VideoBackground] Failed to initialise: " + e.getMessage());
            loaded = false;
        }
    }

    // -------------------------------------------------------------------------
    // Public lifecycle API
    // -------------------------------------------------------------------------

    /** Call from screen's show() to start / resume playback. */
    public void play() {
        if (!loaded || player == null) return;
        player.play();
    }

    /** Call from screen's hide() to pause while in the game. */
    public void pause() {
        if (!loaded || player == null) return;
        player.pause();
    }

    /** Restart playback from the beginning (called on loop). */
    private void restart() {
        if (player != null) {
            player.dispose();
        }
        loaded = false;
        loopPending = false;
        init();
    }

    /** Call once from MilitopiaGame.dispose(). */
    public void dispose() {
        if (player != null) {
            player.dispose();
            player = null;
        }
        loaded = false;
        loopPending = false;
        instance = null;
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    /**
     * Call inside a screen's render() method in place of RenderUtils.drawProportionalBackground().
     * Draws the current video frame scaled proportionally to fill the screen.
     * Handles looping automatically via the completion listener.
     */
    public void render(SpriteBatch batch) {
        if (!loaded || player == null) return;

        // Handle loop: restart after completion (must happen on GL thread, inside render)
        if (loopPending) {
            restart();
            return;
        }

        // Advance the video decoder one frame
        player.update();

        Texture frame = player.getTexture();
        if (frame == null) return;

        // Only the region [0, 0, videoWidth, videoHeight] inside the texture is valid.
        int videoW = player.getVideoWidth();
        int videoH = player.getVideoHeight();
        if (videoW <= 0 || videoH <= 0) return;

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        // Proportional "cover" scale — same logic as RenderUtils.drawProportionalBackground
        float scaleX = (float) screenW / videoW;
        float scaleY = (float) screenH / videoH;
        float scale  = Math.max(scaleX, scaleY);

        float drawW = videoW * scale;
        float drawH = videoH * scale;
        float drawX = (screenW - drawW) / 2f;
        float drawY = (screenH - drawH) / 2f;

        // The texture atlas may be larger than the video; use UV math to clip correctly.
        float u2 = (float) videoW / frame.getWidth();
        float v2 = (float) videoH / frame.getHeight();

        // Always sync projection to the current screen size so resize/maximize works
        batch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH);

        batch.begin();
        batch.draw(frame,
            drawX, drawY, drawW, drawH,  // destination rect
            0, v2, u2, 0);               // UV flipped vertically (fixes upside-down)
        batch.end();
    }
}
