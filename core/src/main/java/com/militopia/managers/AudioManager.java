package com.militopia.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.ObjectMap;
import com.militopia.utils.GameLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages audio playback for SFX and BGM.
 * Features rate-limiting for identical SFX to prevent "audio blowing".
 */
public class AudioManager {

    private static final int MAX_IDENTICAL_SFX_PER_FRAME = 5;
    private static AudioManager instance;

    private final ObjectMap<String, Sound> sounds = new ObjectMap<>();
    private final Map<String, Integer> sfxCountThisFrame = new HashMap<>();
    private Music currentBGM;
    private String currentBGMPath;
    
    private float masterVolume = 0.8f;
    private float bgmVolume = 0.5f;

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    private AudioManager() {}

    /**
     * Resets the frame-based rate limiting counters.
     * Should be called once per frame (e.g., in the game's render loop).
     */
    public void update() {
        sfxCountThisFrame.clear();
    }

    /**
     * Plays a sound effect from the assets/audio/sfx/ directory.
     * @param path The relative path within assets/audio/sfx/ (e.g., "move.wav")
     */
    public void playSFX(String path) {
        // Rate limiting check
        int count = sfxCountThisFrame.getOrDefault(path, 0);
        if (count >= MAX_IDENTICAL_SFX_PER_FRAME) {
            return;
        }

        Sound sound = getSound(path);
        if (sound != null) {
            sound.play(masterVolume);
            sfxCountThisFrame.put(path, count + 1);
        }
    }

    /**
     * Plays background music from the assets/audio/bgm/ directory.
     * @param path The relative path within assets/audio/bgm/ (e.g., "battle_theme.mp3")
     * @param loop Whether the music should loop.
     */
    public void playBGM(String path, boolean loop) {
        if (currentBGMPath != null && currentBGMPath.equals(path)) {
            return; // Already playing
        }

        stopBGM();

        try {
            currentBGM = Gdx.audio.newMusic(Gdx.files.internal("audio/bgm/" + path));
            currentBGM.setVolume(bgmVolume * masterVolume);
            currentBGM.setLooping(loop);
            currentBGM.play();
            currentBGMPath = path;
        } catch (Exception e) {
            GameLogger.log(GameLogger.UI, "ERROR: Could not load BGM: " + path);
        }
    }

    public void stopBGM() {
        if (currentBGM != null) {
            currentBGM.stop();
            currentBGM.dispose();
            currentBGM = null;
            currentBGMPath = null;
        }
    }

    private Sound getSound(String path) {
        if (sounds.containsKey(path)) {
            return sounds.get(path);
        }

        try {
            Sound sound = Gdx.audio.newSound(Gdx.files.internal("audio/sfx/" + path));
            sounds.put(path, sound);
            return sound;
        } catch (Exception e) {
            GameLogger.log(GameLogger.UI, "ERROR: Could not load SFX: " + path);
            return null;
        }
    }

    public void dispose() {
        for (Sound s : sounds.values()) {
            s.dispose();
        }
        sounds.clear();
        stopBGM();
    }

    public void setMasterVolume(float volume) {
        this.masterVolume = volume;
        if (currentBGM != null) {
            currentBGM.setVolume(bgmVolume * masterVolume);
        }
    }
}
