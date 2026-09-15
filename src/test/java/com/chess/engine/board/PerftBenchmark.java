package com.chess.engine.board;

import com.chess.engine.player.MoveTransition;
import org.junit.jupiter.api.Test;

/**
 * Timing-only perft benchmark. Runs each position/depth 3 times (warm-up + 2
 * measured runs) and prints wall-clock milliseconds and nodes-per-second.
 * Not a correctness test — just for measuring raw engine throughput.
 */
class PerftBenchmark {

    private static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -";
    private static final String KIWIPETE = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
    private static final String POS5 = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ -";

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

    private void bench(final String label, final String fen, final int depth) {
        final Board board = fen.equals(START)
                ? Board.createStandardBoard()
                : Board.fromFEN(fen);

        // warm-up
        perft(board, depth);

        // 2 measured runs
        long totalMs = 0;
        long totalNodes = 0;
        final int RUNS = 2;
        for (int i = 0; i < RUNS; i++) {
            final long t0 = System.currentTimeMillis();
            final long nodes = perft(board, depth);
            final long ms = System.currentTimeMillis() - t0;
            totalMs += ms;
            totalNodes = nodes;
        }
        final long avgMs = totalMs / RUNS;
        final long nps = avgMs == 0 ? Long.MAX_VALUE : (totalNodes * 1000L) / avgMs;
        System.out.printf("%-35s  depth=%d  nodes=%,12d  avg=%,6d ms  nps=%,12d%n",
                label, depth, totalNodes, avgMs, nps);
    }

    @Test
    void runBenchmarks() {
        System.out.println("\n=== PERFT BENCHMARK (baseline) ===");
        System.out.println("JVM: " + System.getProperty("java.version")
                + "  OS: " + System.getProperty("os.name"));
        System.out.println();

        bench("StartPos",          START,    4);
        bench("StartPos",          START,    5);
        bench("Kiwipete",          KIWIPETE, 3);
        bench("Kiwipete",          KIWIPETE, 4);
        bench("Pos5 (promotions)", POS5,     4);

        System.out.println();
    }
}
