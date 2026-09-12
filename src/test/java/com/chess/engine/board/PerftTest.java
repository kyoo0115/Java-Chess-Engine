package com.chess.engine.board;

import com.chess.engine.player.MoveTransition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Perft (performance test) — counts leaf nodes at a given depth to verify
 * move generation correctness against known-good values from:
 * https://www.chessprogramming.org/Perft_Results
 * <p>
 * Also uses detailed perft (captures, en passants, castles, promotions, checks)
 * to pinpoint exactly which move category has a bug.
 */
class PerftTest {

    // ── Detailed perft result ─────────────────────────────────────────────────

    private static final String KIWIPETE =
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";

    // ── Core perft ────────────────────────────────────────────────────────────
    private static final String POS3 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -";
    private static final String POS4 =
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1";

    // ── Position 6: Mirror / both castled ────────────────────────────────────
    // FEN: r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - -
    // https://www.chessprogramming.org/Perft_Results#Position_6
    // D1: nodes=46  D2: nodes=2079  D3: nodes=89890  D4: nodes=3894594
    private static final String POS6 =
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - -";

    // ── Position 1: Standard starting position ────────────────────────────────
    // https://www.chessprogramming.org/Perft_Results#Initial_Position
    // Detailed reference values from the wiki:
    // D1:  nodes=20,      captures=0,    ep=0, castles=0, promos=0, checks=0
    // D2:  nodes=400,     captures=0,    ep=0, castles=0, promos=0, checks=0
    // D3:  nodes=8902,    captures=34,   ep=0, castles=0, promos=0, checks=12
    // D4:  nodes=197281,  captures=1576, ep=0, castles=0, promos=0, checks=469
    // D5:  nodes=4865609, captures=82719,ep=258,castles=0,promos=0, checks=27351
    private static final String POS5 =
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ -";

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

    /**
     * Detailed perft — counts captures, en passants, castles, promotions, checks
     * at the leaf nodes so we know exactly which move category is wrong.
     */
    private PerftResult detail(final Board board, final int depth) {
        if (depth == 0) return new PerftResult(1, 0, 0, 0, 0, 0);

        long nodes = 0, captures = 0, ep = 0, castles = 0, promos = 0, checks = 0;
        for (final Move move : board.getCurrentPlayer().getLegalMoves()) {
            final MoveTransition t = board.getCurrentPlayer().makeMove(move);
            if (!t.getMoveStatus().isDone()) continue;

            if (depth == 1) {
                nodes++;
                if (move.isAttack()) captures++;
                if (move instanceof Move.PawnEnPassantAttackMove) ep++;
                if (move.isCastlingMove()) castles++;
                if (move instanceof Move.PawnPromotion) promos++;
                if (t.getTransitionBoard().getCurrentPlayer().isInCheck()) checks++;
            } else {
                final PerftResult r = detail(t.getTransitionBoard(), depth - 1);
                nodes += r.nodes();
                captures += r.captures();
                ep += r.enPassants();
                castles += r.castles();
                promos += r.promotions();
                checks += r.checks();
            }
        }
        return new PerftResult(nodes, captures, ep, castles, promos, checks);
    }

    @Test
    void startPos_depth1() {
        assertEquals(20, perft(Board.createStandardBoard(), 1));
    }

    @Test
    void startPos_depth2() {
        assertEquals(400, perft(Board.createStandardBoard(), 2));
    }

    // ── Position 2: Kiwipete ─────────────────────────────────────────────────
    // FEN: r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -
    // https://www.chessprogramming.org/Perft_Results#Position_2
    // D1:  nodes=48,       captures=8,     ep=0,  castles=2, promos=0,  checks=0
    // D2:  nodes=2039,     captures=351,   ep=1,  castles=91,promos=0,  checks=3
    // D3:  nodes=97862,    captures=17102, ep=45, castles=3162,promos=0,checks=993
    // D4:  nodes=4085603,  captures=757163,ep=1929,castles=128013,promos=15172,checks=25523

    @Test
    void startPos_depth3() {
        final PerftResult r = detail(Board.createStandardBoard(), 3);
        assertAll("startPos depth 3",
                () -> assertEquals(8_902, r.nodes(), "nodes"),
                () -> assertEquals(34, r.captures(), "captures"),
                () -> assertEquals(0, r.enPassants(), "en passants"),
                () -> assertEquals(0, r.castles(), "castles"),
                () -> assertEquals(0, r.promotions(), "promotions"),
                () -> assertEquals(12, r.checks(), "checks")
        );
    }

    @Test
    void startPos_depth4() {
        final PerftResult r = detail(Board.createStandardBoard(), 4);
        assertAll("startPos depth 4",
                () -> assertEquals(197_281, r.nodes(), "nodes"),
                () -> assertEquals(1_576, r.captures(), "captures"),
                () -> assertEquals(0, r.enPassants(), "en passants"),
                () -> assertEquals(0, r.castles(), "castles"),
                () -> assertEquals(0, r.promotions(), "promotions"),
                () -> assertEquals(469, r.checks(), "checks")
        );
    }

    @Test
    void startPos_depth5() {
        final PerftResult r = detail(Board.createStandardBoard(), 5);
        assertAll("startPos depth 5",
                () -> assertEquals(4_865_609, r.nodes(), "nodes"),
                () -> assertEquals(82_719, r.captures(), "captures"),
                () -> assertEquals(258, r.enPassants(), "en passants"),
                () -> assertEquals(0, r.castles(), "castles"),
                () -> assertEquals(0, r.promotions(), "promotions"),
                () -> assertEquals(27_351, r.checks(), "checks")
        );
    }

    @Test
    void kiwipete_depth1() {
        final PerftResult r = detail(Board.fromFEN(KIWIPETE), 1);
        assertAll("kiwipete depth 1",
                () -> assertEquals(48, r.nodes(), "nodes"),
                () -> assertEquals(8, r.captures(), "captures"),
                () -> assertEquals(0, r.enPassants(), "en passants"),
                () -> assertEquals(2, r.castles(), "castles"),
                () -> assertEquals(0, r.promotions(), "promotions"),
                () -> assertEquals(0, r.checks(), "checks")
        );
    }

    @Test
    void kiwipete_depth2() {
        final PerftResult r = detail(Board.fromFEN(KIWIPETE), 2);
        assertAll("kiwipete depth 2",
                () -> assertEquals(2_039, r.nodes(), "nodes"),
                () -> assertEquals(351, r.captures(), "captures"),
                () -> assertEquals(1, r.enPassants(), "en passants"),
                () -> assertEquals(91, r.castles(), "castles"),
                () -> assertEquals(0, r.promotions(), "promotions"),
                () -> assertEquals(3, r.checks(), "checks")
        );
    }

    // ── Position 3: Endgame with promotions ──────────────────────────────────
    // FEN: 8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -
    // https://www.chessprogramming.org/Perft_Results#Position_3
    // D3:  nodes=2812,  captures=209,  ep=2,  castles=0, promos=0, checks=267
    // D4:  nodes=43238, captures=3348, ep=123,castles=0, promos=0,  checks=1680

    @Test
    void kiwipete_depth3() {
        final PerftResult r = detail(Board.fromFEN(KIWIPETE), 3);
        assertAll("kiwipete depth 3",
                () -> assertEquals(97_862, r.nodes(), "nodes"),
                () -> assertEquals(17_102, r.captures(), "captures"),
                () -> assertEquals(45, r.enPassants(), "en passants"),
                () -> assertEquals(3_162, r.castles(), "castles"),
                () -> assertEquals(0, r.promotions(), "promotions"),
                () -> assertEquals(993, r.checks(), "checks")
        );
    }

    @Test
    void kiwipete_depth4() {
        final PerftResult r = detail(Board.fromFEN(KIWIPETE), 4);
        assertAll("kiwipete depth 4",
                () -> assertEquals(4_085_603, r.nodes(), "nodes"),
                () -> assertEquals(757_163, r.captures(), "captures"),
                () -> assertEquals(1_929, r.enPassants(), "en passants"),
                () -> assertEquals(128_013, r.castles(), "castles"),
                () -> assertEquals(15_172, r.promotions(), "promotions"),
                () -> assertEquals(25_523, r.checks(), "checks")
        );
    }

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
        final PerftResult r = detail(Board.fromFEN(POS3), 3);
        assertAll("pos3 depth 3",
                () -> assertEquals(2_812, r.nodes(), "nodes"),
                () -> assertEquals(209, r.captures(), "captures"),
                () -> assertEquals(2, r.enPassants(), "en passants"),
                () -> assertEquals(0, r.castles(), "castles"),
                () -> assertEquals(0, r.promotions(), "promotions"),
                () -> assertEquals(267, r.checks(), "checks")
        );
    }

    // ── Position 4: (White already castled, only Black can castle) ───────────
    // https://www.chessprogramming.org/Perft_Results#Position_4

    @Test
    void pos3_depth4() {
        final PerftResult r = detail(Board.fromFEN(POS3), 4);
        assertAll("pos3 depth 4",
                () -> assertEquals(43_238, r.nodes(), "nodes"),
                () -> assertEquals(3_348, r.captures(), "captures"),
                () -> assertEquals(123, r.enPassants(), "en passants"),
                () -> assertEquals(0, r.castles(), "castles"),
                () -> assertEquals(0, r.promotions(), "promotions"),
                () -> assertEquals(1680, r.checks(), "checks")
        );
    }

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

    @Test
    void pos4_depth4() {
        // D4: nodes=422333, castles=7795
        // https://www.chessprogramming.org/Perft_Results#Position_4
        final PerftResult r = detail(Board.fromFEN(POS4), 4);
        assertAll("pos4 depth 4",
                () -> assertEquals(422_333, r.nodes(), "nodes"),
                () -> assertEquals(7_795, r.castles(), "castles")
        );
    }

    // ── Position 5: Complex middlegame with promotions ────────────────────────
    // FEN: rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ -
    // https://www.chessprogramming.org/Perft_Results#Position_5
    // D1:  nodes=44
    // D2:  nodes=1486
    // D3:  nodes=62379
    // D4:  nodes=2103487
    // D5:  nodes=89941194  (skipped — too slow for CI)

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

    @Test
    void pos5_depth4() {
        assertEquals(2_103_487, perft(Board.fromFEN(POS5), 4));
    }

    // ── Position 6: Mirror / both sides already castled ──────────────────────
    // FEN: r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - -
    // https://www.chessprogramming.org/Perft_Results#Position_6
    // D1:  nodes=46
    // D2:  nodes=2079
    // D3:  nodes=89890
    // D4:  nodes=3894594

    @Test
    void pos6_depth1() {
        assertEquals(46, perft(Board.fromFEN(POS6), 1));
    }

    @Test
    void pos6_depth2() {
        assertEquals(2_079, perft(Board.fromFEN(POS6), 2));
    }

    @Test
    void pos6_depth3() {
        assertEquals(89_890, perft(Board.fromFEN(POS6), 3));
    }

    @Test
    void pos6_depth4() {
        assertEquals(3_894_594, perft(Board.fromFEN(POS6), 4));
    }

    record PerftResult(long nodes, long captures, long enPassants,
                       long castles, long promotions, long checks) {
    }
}
