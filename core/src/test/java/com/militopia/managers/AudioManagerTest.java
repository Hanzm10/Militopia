package com.militopia.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class AudioManagerTest {

    private Sound mockSound;

    @BeforeEach
    public void setup() throws Exception {
        // Reset singleton
        Field instanceField = AudioManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        // Mock Gdx.audio and Gdx.files
        Audio mockAudio = mock(Audio.class);
        Files mockFiles = mock(Files.class);
        FileHandle mockFileHandle = mock(FileHandle.class);
        mockSound = mock(Sound.class);

        Gdx.audio = mockAudio;
        Gdx.files = mockFiles;

        when(mockFiles.internal(any(String.class))).thenReturn(mockFileHandle);
        when(mockAudio.newSound(any(FileHandle.class))).thenReturn(mockSound);
    }

    @AfterEach
    public void tearDown() throws Exception {
        Gdx.audio = null;
        Gdx.files = null;

        // Reset singleton again for isolation
        Field instanceField = AudioManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    public void rateLimiting_capsAt5PerFrame() {
        // Arrange
        AudioManager audio = AudioManager.getInstance();

        // Act: play same SFX 6 times in one frame (no update() call between)
        for (int i = 0; i < 6; i++) {
            audio.playSFX("hit.wav");
        }

        // Assert: Sound.play() called at most 5 times (MAX_IDENTICAL_SFX_PER_FRAME)
        verify(mockSound, times(5)).play(anyFloat());
    }

    @Test
    public void update_resetsRateLimiter() {
        // Arrange
        AudioManager audio = AudioManager.getInstance();

        // Play 5 times (hits the cap)
        for (int i = 0; i < 5; i++) {
            audio.playSFX("hit.wav");
        }

        // Act: reset frame counter
        audio.update();

        // Play one more
        audio.playSFX("hit.wav");

        // Assert: 6 total plays (5 before reset + 1 after)
        verify(mockSound, times(6)).play(anyFloat());
    }
}
