package com.chess.engine.board;

import com.chess.engine.player.MoveTransition;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Perft (performance test) — counts leaf nodes at a given depth to verify
 * move generation correctness against known-good values from:
 * https://www.chessprogramming.org/Perft_Results
 */
class PerftTest {

    // ── Core perft function ───────────────────────────────────────────────────

    private long perft(final Board board, final int depth) {
        if (depth == 0) return 1L;
        long nodes = 0;
        for (final Move move : board.getCurrentPlayer().getLegalMoves()) {
            final MoveTransition t = board.getCurrentPlayer().makeMove(move);
            if (t.getMoveStatus().isDone())
                nodes += perft(t.getTransitionBoard(), depth - 1);
        }
        return nodes;
    }

    /** Divide — prints per-move node counts at depth to help locate discrepancies. */
    private void divide(final Board board, final int depth) {
        final Map<String, Long> counts = new TreeMap<>();
        long total = 0;
        for (final Move move : board.getCurrentPlayer().getLegalMoves()) {
            final MoveTransition t = board.getCurrentPlayer().makeMove(move);
            if (!t.getMoveStatus().isDone()) continue;
            final long n = perft(t.getTransitionBoard(), depth - 1);
            counts.put(move.toString(), n);
            total += n;
        }
        counts.forEach((m, n) -> System.out.println(m + ": " + n));
        System.out.println("Total: " + total);
    }

    @Test
    void diagnose_pos4_depth1() {
        System.out.println("=== POS4 depth 1 divide ===");
        divide(Board.fromFEN(POS4), 1);
    }

    @Test
    void diagnose_kiwipete_depth3() {
        System.out.println("=== KIWIPETE depth 3 divide ===");
        divide(Board.fromFEN(KIWIPETE), 3);
    }

    @Test
    void diagnose_pos5_depth2() {
        System.out.println("=== POS5 depth 2 divide ===");
        divide(Board.fromFEN(POS5), 2);
    }

    // ── Position 1: Standard starting position ────────────────────────────────
    // https://www.chessprogramming.org/Perft_Results#Initial_Position

    @Test
    void startPos_depth1() {
        assertEquals(20, perft(Board.createStandardBoard(), 1));
    }

    @Test
    void startPos_depth2() {
        assertEquals(400, perft(Board.createStandardBoard(), 2));
    }

    @Test
    void startPos_depth3() {
        assertEquals(8_902, perft(Board.createStandardBoard(), 3));
    }

    @Test
    void startPos_depth4() {
        assertEquals(197_281, perft(Board.createStandardBoard(), 4));
    }

    @Test
    void startPos_depth5() {
        assertEquals(4_865_609, perft(Board.createStandardBoard(), 5));
    }

    // ── Position 2: Kiwipete ─────────────────────────────────────────────────
    // Stresses castling, en passant, promotions, and pins.
    // FEN: r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -
    // https://www.chessprogramming.org/Perft_Results#Position_2

    private static final String KIWIPETE =
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";

    @Test
    void kiwipete_depth1() {
        assertEquals(48, perft(Board.fromFEN(KIWIPETE), 1));
    }

    @Test
    void kiwipete_depth2() {
        assertEquals(2_039, perft(Board.fromFEN(KIWIPETE), 2));
    }

    @Test
    void kiwipete_depth3() {
        assertEquals(97_862, perft(Board.fromFEN(KIWIPETE), 3));
    }

    @Test
    void kiwipete_depth4() {
        assertEquals(4_085_603, perft(Board.fromFEN(KIWIPETE), 4));
    }

    // ── Position 3: Endgame with promotions ──────────────────────────────────
    // FEN: 8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -
    // https://www.chessprogramming.org/Perft_Results#Position_3

    private static final String POS3 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -";

    @Test
    void pos3_depth1() {
        assertEquals(14, perft(Board.fromFEN(POS3), 1));
    }

    @Test
    void pos3_depth2() {
        assertEquals(191, perft(Board.fromFEN(POS3), 2));
    }

    @Test
    void pos3_depth3() {
        assertEquals(2_812, perft(Board.fromFEN(POS3), 3));
    }

    @Test
    void pos3_depth4() {
        assertEquals(43_238, perft(Board.fromFEN(POS3), 4));
    }

    // ── Position 4: Mirror — stresses pins and discovered checks ─────────────
    // FEN: r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2QK2R b KQkq -
    // https://www.chessprogramming.org/Perft_Results#Position_4

    private static final String POS4 =
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2QK2R b KQkq -";

    @Test
    void pos4_depth1() {
        assertEquals(6, perft(Board.fromFEN(POS4), 1));
    }

    @Test
    void pos4_depth2() {
        assertEquals(264, perft(Board.fromFEN(POS4), 2));
    }

    @Test
    void pos4_depth3() {
        assertEquals(9_467, perft(Board.fromFEN(POS4), 3));
    }

    // ── Position 5: Complex middlegame ────────────────────────────────────────
    // FEN: rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ -
    // https://www.chessprogramming.org/Perft_Results#Position_5

    private static final String POS5 =
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ -";

    @Test
    void pos5_depth1() {
        assertEquals(44, perft(Board.fromFEN(POS5), 1));
    }

    @Test
    void pos5_depth2() {
        assertEquals(1_486, perft(Board.fromFEN(POS5), 2));
    }

    @Test
    void pos5_depth3() {
        assertEquals(62_379, perft(Board.fromFEN(POS5), 3));
    }
}
