package com.chess.engine.gui;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Plays chess sound effects from MP3 files in the sounds/ directory.
 * All files are decoded to PCM once at class-load time so playback is instant.
 */
public class SoundManager {

    private static final String SOUNDS_DIR = "sounds/";

    /**
     * Single-thread executor so sounds queue and never overlap destructively
     */
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SoundManager");
        t.setDaemon(true);
        return t;
    });
    /**
     * Pre-decoded PCM cache — loaded once at class init
     */
    private static final Map<SoundType, byte[]> PCM_CACHE = new EnumMap<>(SoundType.class);
    /**
     * Globally mutable — set from the EDT via Preferences menu
     */
    private static volatile boolean enabled = true;
    private static AudioFormat pcmFormat;

    static {
        // Decode all MP3s up front so play() has zero decode latency
        for (final SoundType type : SoundType.values()) {
            final String path = SOUNDS_DIR + fileFor(type);
            try (FileInputStream fis = new FileInputStream(path)) {
                final DecodedAudio da = decode(fis);
                if (da != null) {
                    PCM_CACHE.put(type, da.pcm);
                    if (pcmFormat == null) pcmFormat = da.format;
                }
            } catch (Exception e) {
                System.err.println("SoundManager: could not load " + path);
            }
        }
    }

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
        final byte[] pcm = PCM_CACHE.get(type);
        if (pcm == null || pcmFormat == null) return;
        EXEC.submit(() -> {
            try {
                final DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcmFormat);
                if (!AudioSystem.isLineSupported(info)) return;
                try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                    line.open(pcmFormat, pcm.length);
                    line.start();
                    line.write(pcm, 0, pcm.length);
                    line.drain();
                }
            } catch (Exception ignored) {
            }
        });
    }

    // ── MP3 → PCM decoder using JLayer internals ─────────────────────

    private static DecodedAudio decode(final InputStream in) {
        try {
            final Bitstream bitstream = new Bitstream(in);
            final Decoder decoder = new Decoder();
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            AudioFormat fmt = null;
            Header header;
            while ((header = bitstream.readFrame()) != null) {
                final SampleBuffer buf = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                if (fmt == null) {
                    fmt = new AudioFormat(
                            header.frequency(),
                            16,
                            buf.getChannelCount(),
                            true,
                            false);
                }
                // SampleBuffer gives short[] samples — write as little-endian 16-bit PCM
                final short[] samples = buf.getBuffer();
                final int count = buf.getBufferLength();
                for (int i = 0; i < count; i++) {
                    out.write(samples[i] & 0xFF);
                    out.write((samples[i] >> 8) & 0xFF);
                }
                bitstream.closeFrame();
            }
            return (fmt != null) ? new DecodedAudio(out.toByteArray(), fmt) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String fileFor(final SoundType type) {
        return switch (type) {
            case MOVE -> "Move.mp3";
            case CAPTURE -> "Capture.mp3";
            case CASTLE -> "Move.mp3";
            case CHECK -> "Check.mp3";
            case GAME_END -> "Victory.mp3";
        };
    }

    public enum SoundType {MOVE, CAPTURE, CHECK, GAME_END, CASTLE}

    private static final class DecodedAudio {
        final byte[] pcm;
        final AudioFormat format;

        DecodedAudio(byte[] pcm, AudioFormat format) {
            this.pcm = pcm;
            this.format = format;
        }
    }
}
