package com.chess.engine.gui;

import javax.sound.sampled.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Synthesises chess.com-style move sounds at runtime — no audio files required.
 * <p>
 * Sound design:
 * MOVE    – soft wooden "clunk" (short low-frequency thump + click transient)
 * CAPTURE – harder, sharper impact (louder thump + brighter click)
 * CHECK   – two-note alert chime (high-pitched double ping)
 * GAME_END– three-note descending chord (gentle resolution)
 * CASTLE  – double-clunk (two move sounds played in quick succession)
 */
public class SoundManager {

    private static final int SAMPLE_RATE = 44100;
    /**
     * Single-thread executor so sounds queue and never overlap destructively
     */
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SoundManager");
        t.setDaemon(true);
        return t;
    });
    /**
     * Globally mutable — set from the EDT via Preferences menu
     */
    private static volatile boolean enabled = true;

    private SoundManager() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(final boolean on) {
        enabled = on;
    }

    public static void play(final SoundType type) {
        if (!enabled) return;
        EXEC.submit(() -> {
            try {
                switch (type) {
                    case MOVE -> playBytes(buildMove(false));
                    case CAPTURE -> playBytes(buildMove(true));
                    case CHECK -> playBytes(buildCheck());
                    case GAME_END -> playBytes(buildGameEnd());
                    case CASTLE -> {
                        playBytes(buildMove(false));
                        Thread.sleep(80);
                        playBytes(buildMove(false));
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * Wooden clunk: low-frequency sine burst (body of the piece) +
     * very short white-noise transient (the click of contact).
     */
    private static byte[] buildMove(final boolean hard) {
        final double duration = hard ? 0.18 : 0.14;   // seconds
        final double bodyFreq = hard ? 160.0 : 120.0;  // Hz  – body thump
        final double bodyAmp = hard ? 0.55 : 0.40;
        final double noiseAmp = hard ? 0.30 : 0.18;
        final double noiseDecay = hard ? 0.008 : 0.005; // seconds of click
        final int samples = (int) (SAMPLE_RATE * duration);
        final byte[] buf = new byte[samples * 2];

        for (int i = 0; i < samples; i++) {
            final double t = (double) i / SAMPLE_RATE;
            final double envBody = Math.exp(-t / (hard ? 0.06 : 0.04));
            final double envNoise = Math.exp(-t / noiseDecay);
            final double body = bodyAmp * Math.sin(2 * Math.PI * bodyFreq * t) * envBody;
            final double noise = noiseAmp * (Math.random() * 2 - 1) * envNoise;
            final short sample = (short) Math.max(Short.MIN_VALUE,
                    Math.min(Short.MAX_VALUE,
                            (body + noise) * Short.MAX_VALUE));
            buf[i * 2] = (byte) (sample & 0xFF);
            buf[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return buf;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Sound builders
    // ─────────────────────────────────────────────────────────────────

    /**
     * Two-ping chime: two high-pitched bell tones, slight delay between them.
     */
    private static byte[] buildCheck() {
        final double duration = 0.55;
        final int samples = (int) (SAMPLE_RATE * duration);
        final byte[] buf = new byte[samples * 2];
        final double[] freqs = {880.0, 1108.0};   // A5, C#6  — alert interval
        final double[] starts = {0.0, 0.07};        // second ping at 70 ms

        for (int i = 0; i < samples; i++) {
            final double t = (double) i / SAMPLE_RATE;
            double value = 0;
            for (int p = 0; p < 2; p++) {
                final double dt = t - starts[p];
                if (dt < 0) continue;
                final double env = Math.exp(-dt / 0.15);
                value += 0.38 * env * Math.sin(2 * Math.PI * freqs[p] * dt);
            }
            final short sample = (short) Math.max(Short.MIN_VALUE,
                    Math.min(Short.MAX_VALUE,
                            value * Short.MAX_VALUE));
            buf[i * 2] = (byte) (sample & 0xFF);
            buf[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return buf;
    }

    /**
     * Three-note descending resolution chord (G4→E4→C4, soft bell).
     */
    private static byte[] buildGameEnd() {
        final double duration = 1.4;
        final int samples = (int) (SAMPLE_RATE * duration);
        final byte[] buf = new byte[samples * 2];
        final double[] freqs = {392.0, 330.0, 261.6};  // G4, E4, C4
        final double[] starts = {0.0, 0.22, 0.44};

        for (int i = 0; i < samples; i++) {
            final double t = (double) i / SAMPLE_RATE;
            double value = 0;
            for (int n = 0; n < 3; n++) {
                final double dt = t - starts[n];
                if (dt < 0) continue;
                final double env = Math.exp(-dt / 0.35);
                value += 0.30 * env * Math.sin(2 * Math.PI * freqs[n] * dt);
                // add subtle harmonic
                value += 0.10 * env * Math.sin(4 * Math.PI * freqs[n] * dt);
            }
            final short sample = (short) Math.max(Short.MIN_VALUE,
                    Math.min(Short.MAX_VALUE,
                            value * Short.MAX_VALUE));
            buf[i * 2] = (byte) (sample & 0xFF);
            buf[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return buf;
    }

    private static void playBytes(final byte[] pcm) throws LineUnavailableException {
        final AudioFormat fmt = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                SAMPLE_RATE, 16, 1, 2, SAMPLE_RATE, false);
        final DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
        if (!AudioSystem.isLineSupported(info)) return;

        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(fmt, pcm.length);
            line.start();
            line.write(pcm, 0, pcm.length);
            line.drain();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Playback
    // ─────────────────────────────────────────────────────────────────

    public enum SoundType {MOVE, CAPTURE, CHECK, GAME_END, CASTLE}
}
