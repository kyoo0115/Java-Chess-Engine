package com.chess.engine.gui;

import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Plays chess sound effects from MP3 files in the sounds/ directory.
 * Uses JLayer (javazoom) for MP3 decoding — no native audio files bundled.
 *
 * Expected files (relative to working directory):
 *   sounds/Move.mp3
 *   sounds/Capture.mp3
 *   sounds/Check.mp3
 *   sounds/Checkmate.mp3
 *   sounds/Victory.mp3
 *   sounds/Defeat.mp3
 *   sounds/NewChallenge.mp3
 */
public class SoundManager {

    private static final String SOUNDS_DIR = "sounds/";

    /** Single-thread executor so sounds queue and never overlap destructively */
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SoundManager");
        t.setDaemon(true);
        return t;
    });

    /** Globally mutable — set from the EDT via Preferences menu */
    private static volatile boolean enabled = true;

    private SoundManager() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(final boolean on) {
        enabled = on;
    }

    public static void play(final SoundType type) {
        if (!enabled) return;
        final String file = fileFor(type);
        if (file == null) return;
        EXEC.submit(() -> {
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(SOUNDS_DIR + file))) {
                new Player(bis).play();
            } catch (Exception ignored) {}
        });
    }

    private static String fileFor(final SoundType type) {
        return switch (type) {
            case MOVE     -> "Move.mp3";
            case CAPTURE  -> "Capture.mp3";
            case CASTLE   -> "Move.mp3";
            case CHECK    -> "Check.mp3";
            case GAME_END -> "Victory.mp3";
        };
    }

    public enum SoundType {MOVE, CAPTURE, CHECK, GAME_END, CASTLE}
}
