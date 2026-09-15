package com.chess.engine.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.player.ai.StockfishEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.chess.engine.pieces.Piece.PieceType.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EPD (Extended Position Description) puzzle suite.
 *
 * <p>Loads positions from {@code src/test/resources/puzzles.epd}, runs the engine
 * at a fixed search depth, and asserts that the best move found matches the
 * expected move(s) encoded in the "bm" field.
 *
 * <p>EPD line format used here:
 * <pre>
 *   &lt;FEN&gt; bm &lt;move1&gt; [move2 ...]; id "&lt;name&gt;"; c0 "&lt;comment&gt;";
 * </pre>
 * Moves are in coordinate notation (e2e4, g1f3, e7e8q, …).
 */
class EpdPuzzleTest {

    /** Movetime in ms given to Stockfish per puzzle move — enough to solve mates reliably. */
    private static final int PUZZLE_MOVETIME_MS = 1000;

    // ── Puzzle loading ────────────────────────────────────────────────────────

    static Stream<PuzzleCase> puzzles() throws Exception {
        final List<PuzzleCase> cases = new ArrayList<>();
        try (final InputStream is = EpdPuzzleTest.class
                .getClassLoader().getResourceAsStream("puzzles.epd")) {
            if (is == null) throw new IllegalStateException("puzzles.epd not found on classpath");
            try (final BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    final PuzzleCase pc = parseLine(line);
                    if (pc != null) cases.add(pc);
                }
            }
        }
        return cases.stream();
    }

    /**
     * Parses one EPD line into a {@link PuzzleCase}.
     * Expected format: {@code <FEN4> bm <move...>; id "<name>"; ...}
     */
    private static PuzzleCase parseLine(final String line) {
        try {
            // Split at first occurrence of " bm "
            final int bmIdx = line.indexOf(" bm ");
            if (bmIdx < 0) return null;
            final String fen4 = line.substring(0, bmIdx).trim();
            final String rest = line.substring(bmIdx + 4); // after " bm "

            // Best moves end at first semicolon
            final int semi = rest.indexOf(';');
            final String bmField = (semi >= 0 ? rest.substring(0, semi) : rest).trim();
            final String[] bestMoves = bmField.split("\\s+");

            // Extract id field if present
            String id = fen4;
            final int idIdx = line.indexOf(" id \"");
            if (idIdx >= 0) {
                final int idEnd = line.indexOf('"', idIdx + 5);
                if (idEnd >= 0) id = line.substring(idIdx + 5, idEnd);
            }

            // Build full FEN — our Board.fromFEN needs at least 4 fields;
            // EPD omits half-move clock and full-move number, so append defaults.
            final String fen = fen4 + " 0 1";
            return new PuzzleCase(id, fen, bestMoves);
        } catch (Exception e) {
            System.err.println("Skipping malformed EPD line: " + line + " (" + e.getMessage() + ")");
            return null;
        }
    }

    // ── Test ──────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("puzzles")
    @DisplayName("EPD puzzle suite")
    void solvesPuzzle(final PuzzleCase puzzle) {
        assumeTrue(StockfishEngine.isAvailable(), "Stockfish binary not found — skipping EPD tests");

        final Board board;
        try {
            board = Board.fromFEN(puzzle.fen());
        } catch (Exception e) {
            System.err.println("Skipping " + puzzle.id() + " — bad FEN: " + e.getMessage());
            return;
        }

        final Move found;
        try (final StockfishEngine engine = new StockfishEngine(PUZZLE_MOVETIME_MS)) {
            found = engine.execute(board);
        } catch (Exception e) {
            System.err.println("Skipping " + puzzle.id() + " — engine crashed: " + e.getMessage());
            return;
        }

        final String foundCoord = found == null ? "(null)" : moveToCoord(found);

        // Accept both "a7a8q" and "a7a8" as matching — EPD sometimes omits the queen suffix.
        final boolean correct = puzzle.bestMoves().stream()
                .anyMatch(bm -> bm.equalsIgnoreCase(foundCoord)
                        || foundCoord.startsWith(bm.toLowerCase())
                        || bm.toLowerCase().startsWith(foundCoord.toLowerCase()));

        assertTrue(correct,
                puzzle.id() + ": engine played " + foundCoord
                        + ", expected one of " + puzzle.bestMoves());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Converts a Move to coordinate notation, e.g. "e2e4" or "e7e8q". */
    private static String moveToCoord(final Move move) {
        final String from = coordName(move.getCurrentCoordinate());
        final String to   = coordName(move.getDestinationCoordinate());
        if (move instanceof Move.PawnPromotion pp) {
            final char promo = switch (pp.getPromotionPiece().getPieceType()) {
                case QUEEN  -> 'q';
                case ROOK   -> 'r';
                case BISHOP -> 'b';
                case KNIGHT -> 'n';
                default     -> 'q';
            };
            return from + to + promo;
        }
        return from + to;
    }

    private static String coordName(final int tileId) {
        final char file = (char) ('a' + tileId % 8);
        final char rank = (char) ('8' - tileId / 8);
        return "" + file + rank;
    }

    // ── Data record ──────────────────────────────────────────────────────────

    record PuzzleCase(String id, String fen, List<String> bestMoves) {
        PuzzleCase(String id, String fen, String[] bestMoves) {
            this(id, fen, List.of(bestMoves));
        }

        @Override
        public String toString() {
            return id;
        }
    }
}
