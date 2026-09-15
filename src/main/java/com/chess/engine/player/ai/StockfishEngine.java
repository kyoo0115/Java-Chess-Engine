package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.pieces.Piece;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Chess engine backed by a local Stockfish process via the UCI protocol.
 *
 * <p>Binary discovery order:
 * <ol>
 *   <li>The path stored in the system property {@code stockfish.path} (set at startup or in tests).</li>
 *   <li>{@code stockfish} on the system PATH (works after {@code winget install} or Linux package).</li>
 *   <li>{@code C:\stockfish\stockfish.exe} (the manually-installed location).</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>
 *   try (StockfishEngine sf = new StockfishEngine(500)) {
 *       Move best = sf.execute(board);
 *   }
 * </pre>
 *
 * <p>Each {@link #execute} call sends:
 * <pre>
 *   position fen &lt;fen&gt;
 *   go movetime &lt;ms&gt;
 * </pre>
 * and reads back the {@code bestmove} line. The engine process stays alive across
 * calls so UCI initialization only happens once per instance.
 */
public final class StockfishEngine implements MoveStrategy, Closeable {

    // ── Difficulty presets (movetime in ms) ───────────────────────────────────
    public static final int MOVETIME_EASY = 100;
    public static final int MOVETIME_MEDIUM = 500;
    public static final int MOVETIME_HARD = 2000;
    public static final int MOVETIME_MASTER = 5000;
    // ── Known fallback binary paths ───────────────────────────────────────────
    private static final String[] FALLBACK_PATHS = {
            "C:\\stockfish\\stockfish.exe",
            "/usr/bin/stockfish",
            "/usr/local/bin/stockfish",
            "/opt/homebrew/bin/stockfish",
    };
    private final int moveTimeMs;
    private final Process process;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final Thread shutdownHook;
    /**
     * Set to true by {@link #close()} so {@link #execute} knows not to log pipe errors.
     */
    private volatile boolean closed = false;

    /**
     * Creates a new engine instance and initializes the UCI handshake.
     *
     * @param moveTimeMs milliseconds Stockfish is allowed to think per move
     * @throws IOException if the Stockfish binary cannot be found or started
     */
    public StockfishEngine(final int moveTimeMs) throws IOException {
        this.moveTimeMs = moveTimeMs;
        final String binaryPath = findBinary();
        this.process = new ProcessBuilder(binaryPath)
                .redirectErrorStream(true)
                .start();
        this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

        // UCI handshake
        send("uci");
        waitFor("uciok");

        // Use half the available logical cores, capped at 4.
        // Searches are always sequential (one side at a time), so there is never
        // a need for more than one engine's worth of parallelism.  Half-cores
        // keeps the laptop responsive while still letting Stockfish use SMP.
        final int cores = Runtime.getRuntime().availableProcessors();
        final int threads = Math.max(1, Math.min(cores / 2, 4));
        send("setoption name Threads value " + threads);

        send("isready");
        waitFor("readyok");

        // Shutdown hook — kills the process if the JVM exits without close() being
        // called (e.g. IDE stop button, uncaught exception in main thread).
        // Stored so close() can deregister it and avoid hook accumulation across restarts.
        final Process proc = this.process;
        this.shutdownHook = new Thread(() -> {
            if (proc.isAlive()) proc.destroyForcibly();
        }, "stockfish-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(this.shutdownHook);
    }

    // ── Move translation ──────────────────────────────────────────────────────

    /**
     * Converts a UCI coordinate string (e.g. "e2e4", "e7e8q") to the matching
     * {@link Move} from the board's current legal moves.
     *
     * @param board the current position
     * @param uci   UCI move string (4 or 5 characters)
     * @return the matching legal {@link Move}, or {@code null} if not found
     */
    private static Move findMove(final Board board, final String uci) {
        if (uci.length() < 4) return null;

        final int fromFile = uci.charAt(0) - 'a';
        final int fromRank = '8' - uci.charAt(1);
        final int toFile = uci.charAt(2) - 'a';
        final int toRank = '8' - uci.charAt(3);
        final int fromId = fromRank * 8 + fromFile;
        final int toId = toRank * 8 + toFile;

        // Promotion piece (5th char): q/r/b/n
        final char promoCh = (uci.length() >= 5) ? Character.toLowerCase(uci.charAt(4)) : 0;

        for (final Move move : board.getCurrentPlayer().getLegalMoves()) {
            if (move.getCurrentCoordinate() != fromId) continue;
            if (move.getDestinationCoordinate() != toId) continue;

            if (promoCh != 0 && move instanceof Move.PawnPromotion pp) {
                final Piece.PieceType want = promoCharToType(promoCh);
                if (pp.getPromotionPiece().getPieceType() != want) continue;
            }

            return move;
        }
        return null;
    }

    private static Piece.PieceType promoCharToType(final char c) {
        return switch (c) {
            case 'r' -> Piece.PieceType.ROOK;
            case 'b' -> Piece.PieceType.BISHOP;
            case 'n' -> Piece.PieceType.KNIGHT;
            default -> Piece.PieceType.QUEEN;
        };
    }

    // ── Binary discovery ──────────────────────────────────────────────────────

    /**
     * Finds the Stockfish binary. Checks (in order):
     * <ol>
     *   <li>System property {@code stockfish.path}</li>
     *   <li>{@code stockfish} on PATH</li>
     *   <li>Known fallback absolute paths</li>
     * </ol>
     *
     * @throws IOException if no binary is found
     */
    public static String findBinary() throws IOException {
        // 1. Explicit override via system property
        final String prop = System.getProperty("stockfish.path");
        if (prop != null && !prop.isBlank() && new File(prop).canExecute()) {
            return prop;
        }

        // 2. Known fixed locations (checked before PATH to avoid spurious probe processes)
        for (final String path : FALLBACK_PATHS) {
            if (new File(path).canExecute()) return path;
        }

        // 3. On PATH — resolve the full path first so ProcessBuilder is unambiguous
        final String resolved = resolveOnPath("stockfish");
        if (resolved != null) return resolved;

        throw new IOException(
                "Stockfish binary not found. Install it (winget install Stockfish.Stockfish) " +
                        "or set the system property -Dstockfish.path=<path> to its location.");
    }

    /**
     * Resolves {@code name} to its absolute path via PATH lookup (using {@code where} on
     * Windows, {@code which} on Unix). Returns {@code null} if not found.
     * Unlike the old probe approach this never starts a Stockfish process.
     */
    private static String resolveOnPath(final String name) {
        final boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        final String locator = isWindows ? "where" : "which";
        try {
            final Process p = new ProcessBuilder(locator, name)
                    .redirectErrorStream(true)
                    .start();
            try (final java.io.BufferedReader br =
                         new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                final String line = br.readLine();
                p.waitFor(3, TimeUnit.SECONDS);
                if (line != null && !line.isBlank() && new File(line.trim()).canExecute()) {
                    return line.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Returns {@code true} if a Stockfish binary can be located without throwing.
     * Use this to decide whether to offer Stockfish as an option in the UI.
     */
    public static boolean isAvailable() {
        try {
            findBinary();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ── MoveStrategy ──────────────────────────────────────────────────────────

    @Override
    public Move execute(final Board board) {
        if (closed) return null;
        final String fen = board.toFEN();
        try {
            send("position fen " + fen);
            send("go movetime " + moveTimeMs);
            final String bestmove = readBestMove();
            if (bestmove == null) return null;
            System.out.println("Stockfish bestmove: " + bestmove + "  (" + moveTimeMs + " ms)");
            return findMove(board, bestmove);
        } catch (IOException e) {
            if (!closed) System.err.println("StockfishEngine error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void close() {
        closed = true;
        try {
            send("quit");
        } catch (IOException ignored) {
        }
        process.destroyForcibly();
        try {
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Deregister the hook now that we have cleanly shut down; prevents accumulation
        // when the engine is restarted multiple times within the same JVM session.
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // JVM is already shutting down — the hook is running or done, nothing to do
        }
    }

    // ── UCI helpers ───────────────────────────────────────────────────────────

    private void send(final String command) throws IOException {
        writer.write(command);
        writer.newLine();
        writer.flush();
    }

    /**
     * Reads lines until one starts with {@code prefix}, discarding everything else.
     */
    private void waitFor(final String prefix) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(prefix)) return;
        }
    }

    /**
     * Reads lines until a {@code bestmove} line arrives and returns the first token
     * after "bestmove" (the move in UCI coordinate notation, e.g. "e2e4" or "e7e8q").
     * Returns {@code null} if the stream ends or Stockfish reports "bestmove (none)".
     */
    private String readBestMove() throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("bestmove")) {
                final String[] parts = line.split("\\s+");
                if (parts.length >= 2 && !parts[1].equals("(none)")) {
                    return parts[1];
                }
                return null;
            }
        }
        return null;
    }
}
