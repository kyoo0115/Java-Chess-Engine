package com.chess.engine.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.player.MoveTransition;
import com.chess.engine.player.ai.Minimax;

import java.util.function.Supplier;

/**
 * Engine-vs-Engine match runner.
 *
 * <p>Plays two {@link Minimax} configurations against each other over a
 * configurable number of games, alternating colours, and computes:
 * <ul>
 *   <li>Win / Draw / Loss counts for Engine A</li>
 *   <li>Score percentage</li>
 *   <li>Estimated Elo difference  Δ = 400 × log₁₀(W/L)</li>
 * </ul>
 *
 * <p>Run with: {@code ./gradlew run} (or execute main() directly from your IDE).
 * Adjust {@link #GAMES} and the two engine factories at the top of {@code main()}.
 *
 * <p>A single game is capped at {@link #MAX_PLIES} half-moves to avoid infinite loops
 * in positions where neither side can force a win.
 */
public class EngineMatch {

    /** Total number of games to play (split evenly: A-white then A-black). */
    private static final int GAMES = 20;

    /**
     * Maximum half-moves per game before declaring a draw.
     * 200 plies ≈ 100 full moves — well beyond any realistic game length at low depth.
     */
    private static final int MAX_PLIES = 200;

    public static void main(final String[] args) {
        // ── Configure the two engines here ───────────────────────────────────
        // Engine A = "new" (with all recent improvements)
        final Supplier<Minimax> engineA = () -> new Minimax(4);

        // Engine B = "baseline" (weaker / shallower)
        final Supplier<Minimax> engineB = () -> new Minimax(3);

        final String nameA = "Engine-A (depth 4)";
        final String nameB = "Engine-B (depth 3)";
        // ─────────────────────────────────────────────────────────────────────

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║        JavaChess Engine Match Runner             ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf( "║  %-20s  vs  %-20s ║%n", nameA, nameB);
        System.out.printf( "║  Games: %-5d   Max plies/game: %-5d           ║%n", GAMES, MAX_PLIES);
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();

        int winsA = 0, winsB = 0, draws = 0;

        for (int g = 1; g <= GAMES; g++) {
            // Alternate colours every game
            final boolean aIsWhite = (g % 2 == 1);
            final Minimax white = aIsWhite ? engineA.get() : engineB.get();
            final Minimax black = aIsWhite ? engineB.get() : engineA.get();
            final String whiteLabel = aIsWhite ? nameA : nameB;
            final String blackLabel = aIsWhite ? nameB : nameA;

            final GameResult result = playGame(white, black, g, whiteLabel, blackLabel);

            if (result == GameResult.WHITE_WIN) {
                if (aIsWhite) winsA++; else winsB++;
            } else if (result == GameResult.BLACK_WIN) {
                if (!aIsWhite) winsA++; else winsB++;
            } else {
                draws++;
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
        } else if (winsA > 0 && winsB == 0) {
            System.out.println("  Estimated Elo delta: >> +400 (no losses recorded)");
        } else if (winsA == 0 && winsB > 0) {
            System.out.println("  Estimated Elo delta: << -400 (no wins recorded)");
        } else {
            System.out.println("  Estimated Elo delta: N/A (all draws)");
        }

        // Full Elo estimate using score percentage (more robust with draws)
        if (score > 0.0 && score < 1.0) {
            final double eloDeltaScore = -400.0 * Math.log10(1.0 / score - 1.0);
            System.out.printf("  Estimated Elo delta (score %%):   %+.0f%n", eloDeltaScore);
        }
        System.out.println("══════════════════════════════════════════════════");
    }

    // ── Single game ──────────────────────────────────────────────────────────

    private static GameResult playGame(final Minimax white, final Minimax black,
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

            final Minimax engine = board.getCurrentPlayer().getAlliance().isWhite() ? white : black;
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
