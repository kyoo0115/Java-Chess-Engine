package com.chess.engine.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.player.MoveTransition;
import com.chess.engine.player.ai.StockfishEngine;

import java.io.IOException;

/**
 * Engine-vs-Engine match runner using Stockfish at two different move-time settings.
 *
 * <p>Plays two Stockfish configurations against each other over a configurable number
 * of games, alternating colors, and computes:
 * <ul>
 *   <li>Win / Draw / Loss counts for Engine A</li>
 *   <li>Score percentage</li>
 *   <li>Estimated Elo difference  Δ = 400 × log₁₀(W/L)</li>
 * </ul>
 *
 * <p>Run directly from the IDE or with:
 * <pre>
 *   ./gradlew test --tests "com.chess.engine.ai.EngineMatch"
 * </pre>
 * Adjust {@link #GAMES} and the two move-time constants at the top of {@code main()}.
 */
public class EngineMatch {

    /**
     * Total number of games to play (split evenly: A-white then A-black).
     */
    private static final int GAMES = 10;

    /**
     * Maximum half-moves per game before declaring a draw.
     * 200 plies ≈ 100 full moves — well beyond any realistic game length.
     */
    private static final int MAX_PLIES = 200;

    public static void main(final String[] args) throws IOException {
        if (!StockfishEngine.isAvailable()) {
            System.err.println("Stockfish binary not found — cannot run engine match.");
            System.err.println("Install Stockfish and ensure it is on PATH or at C:\\stockfish\\stockfish.exe");
            return;
        }

        // ── Configure the two engines here ───────────────────────────────────
        final int movetimeA = 500;   // Engine A — 500 ms/move
        final int movetimeB = 100;   // Engine B — 100 ms/move (weaker)

        final String nameA = "Stockfish-" + movetimeA + "ms";
        final String nameB = "Stockfish-" + movetimeB + "ms";
        // ─────────────────────────────────────────────────────────────────────

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║        JavaChess Engine Match Runner             ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║  %-20s  vs  %-20s ║%n", nameA, nameB);
        System.out.printf("║  Games: %-5d   Max plies/game: %-5d           ║%n", GAMES, MAX_PLIES);
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();

        int winsA = 0, winsB = 0, draws = 0;

        for (int g = 1; g <= GAMES; g++) {
            final boolean aIsWhite = (g % 2 == 1);
            final String whiteLabel = aIsWhite ? nameA : nameB;
            final String blackLabel = aIsWhite ? nameB : nameA;

            // Create fresh engine instances per game so TT and state don't bleed across
            try (final StockfishEngine white = new StockfishEngine(aIsWhite ? movetimeA : movetimeB);
                 final StockfishEngine black = new StockfishEngine(aIsWhite ? movetimeB : movetimeA)) {

                final GameResult result = playGame(white, black, g, whiteLabel, blackLabel);

                if (result == GameResult.WHITE_WIN) {
                    if (aIsWhite) winsA++;
                    else winsB++;
                } else if (result == GameResult.BLACK_WIN) {
                    if (!aIsWhite) winsA++;
                    else winsB++;
                } else {
                    draws++;
                }
            }

            final int played = winsA + winsB + draws;
            System.out.printf("  After %2d game(s): %s  %d W / %d D / %d L%n",
                    played, nameA, winsA, draws, winsB);
        }

        // ── Summary ──────────────────────────────────────────────────────────
        System.out.println();
        System.out.println("══════════════════════════════════════════════════");
        System.out.println("  FINAL RESULTS  —  " + nameA);
        System.out.println("══════════════════════════════════════════════════");
        System.out.printf("  Wins:   %d%n", winsA);
        System.out.printf("  Draws:  %d%n", draws);
        System.out.printf("  Losses: %d%n", winsB);

        final double score = (winsA + 0.5 * draws) / GAMES;
        System.out.printf("  Score:  %.1f / %d  (%.1f%%)%n",
                winsA + 0.5 * draws, GAMES, score * 100);

        System.out.println();
        if (winsA > 0 && winsB > 0) {
            final double eloDelta = 400.0 * Math.log10((double) winsA / winsB);
            System.out.printf("  Estimated Elo delta (wins only): %+.0f%n", eloDelta);
        } else if (winsA > 0) {
            System.out.println("  Estimated Elo delta: >> +400 (no losses recorded)");
        } else if (winsB > 0) {
            System.out.println("  Estimated Elo delta: << -400 (no wins recorded)");
        } else {
            System.out.println("  Estimated Elo delta: N/A (all draws)");
        }

        if (score > 0.0 && score < 1.0) {
            final double eloDeltaScore = -400.0 * Math.log10(1.0 / score - 1.0);
            System.out.printf("  Estimated Elo delta (score %%):   %+.0f%n", eloDeltaScore);
        }
        System.out.println("══════════════════════════════════════════════════");
    }

    // ── Single game ──────────────────────────────────────────────────────────

    private static GameResult playGame(final StockfishEngine white, final StockfishEngine black,
                                       final int gameNo,
                                       final String whiteLabel, final String blackLabel) {
        Board board = Board.createStandardBoard();
        int ply = 0;

        System.out.printf("%nGame %2d  White=%-22s  Black=%s%n",
                gameNo, whiteLabel, blackLabel);

        while (ply < MAX_PLIES) {
            if (board.getCurrentPlayer().isCheckMate()) {
                final String winner = board.getCurrentPlayer().getOpponent().getAlliance().isWhite()
                        ? "White" : "Black";
                System.out.printf("  Checkmate on ply %d — %s wins%n", ply, winner);
                return board.getCurrentPlayer().getAlliance().isWhite()
                        ? GameResult.BLACK_WIN : GameResult.WHITE_WIN;
            }
            if (board.getCurrentPlayer().isStaleMate()) {
                System.out.printf("  Stalemate on ply %d — Draw%n", ply);
                return GameResult.DRAW;
            }

            final StockfishEngine engine = board.getCurrentPlayer().getAlliance().isWhite() ? white : black;
            final Move move = engine.execute(board);

            if (move == null) {
                System.out.printf("  Engine returned null on ply %d — Draw%n", ply);
                return GameResult.DRAW;
            }

            final MoveTransition t = board.getCurrentPlayer().makeMove(move);
            if (!t.getMoveStatus().isDone()) {
                System.out.printf("  Illegal move on ply %d — Draw%n", ply);
                return GameResult.DRAW;
            }

            board = t.getTransitionBoard();
            ply++;
        }

        System.out.printf("  Max plies (%d) reached — Draw%n", MAX_PLIES);
        return GameResult.DRAW;
    }

    // ── Result enum ──────────────────────────────────────────────────────────

    private enum GameResult {
        WHITE_WIN, BLACK_WIN, DRAW
    }
}
