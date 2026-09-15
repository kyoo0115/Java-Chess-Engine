package com.chess.engine.gui;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.MoveLog;
import com.chess.engine.player.ai.AnalysisResult;
import com.chess.engine.player.ai.StockfishEngine;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SwingWorker that iterates a {@link MoveLog}, analyses every position with
 * Stockfish, and publishes results to the EDT one row at a time.
 *
 * <p>Each published chunk is a single {@link MoveResult} record; the panel is
 * updated via {@code process()} so the user sees rows appear progressively.
 *
 * <p>Usage:
 * <pre>
 *   GameAnalyser analyser = new GameAnalyser(moveLog, analysisPanel, onLiveEval);
 *   analyser.execute();
 *   // later:
 *   analyser.cancel(true);   // e.g. when a new game starts
 * </pre>
 */
public class GameAnalyser extends SwingWorker<Void, GameAnalyser.MoveResult> {

    /**
     * Analysis time in ms per position during full game analysis.
     * Kept short so a full game of 40 moves completes in ~20 seconds.
     */
    private static final int ANALYSIS_MOVETIME_MS = 300;

    // ── Fields ────────────────────────────────────────────────────────────────

    /**
     * Snapshot of the move list taken at construction time — safe to read off-EDT.
     */
    private final List<Move> moves;
    private final AnalysisPanel panel;
    /**
     * Called on EDT with the latest eval (White centipawns) for the live eval bar.
     */
    private final java.util.function.IntConsumer onLiveEval;
    private final JLabel progressLabel;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * @param moveLog       the game's move log (a snapshot is taken immediately)
     * @param panel         the {@link AnalysisPanel} to receive results
     * @param onLiveEval    callback (EDT) receiving White-cp eval after each position;
     *                      may be {@code null} if live eval bar is not wired
     * @param progressLabel optional label to show analysis progress; may be {@code null}
     */
    public GameAnalyser(final MoveLog moveLog,
                        final AnalysisPanel panel,
                        final java.util.function.IntConsumer onLiveEval,
                        final JLabel progressLabel) {
        // Snapshot so the worker doesn't race with Table mutations
        this.moves = new ArrayList<>(moveLog.getMoves());
        this.panel = panel;
        this.onLiveEval = onLiveEval;
        this.progressLabel = progressLabel;
    }

    // ── SwingWorker ───────────────────────────────────────────────────────────

    @Override
    protected Void doInBackground() {
        if (moves.isEmpty()) return null;

        try (final StockfishEngine sf = new StockfishEngine(ANALYSIS_MOVETIME_MS)) {
            Board board = Board.createStandardBoard();

            // Analyse the starting position (before any moves)
            final AnalysisResult startResult = sf.analysePosition(board.toFEN(), ANALYSIS_MOVETIME_MS);
            int prevScore = (startResult != null) ? startResult.scoreWhiteCp() : 0;

            final int total = moves.size();
            for (int i = 0; i < total; i++) {
                if (isCancelled()) break;

                final Move move = moves.get(i);
                final com.chess.engine.player.MoveTransition t =
                        board.getCurrentPlayer().makeMove(move);

                if (!t.getMoveStatus().isDone()) continue;
                board = t.getTransitionBoard();

                final int idx = i;
                final int prevScoreFinal = prevScore;
                final String notation = move.toString();
                final boolean isWhite = move.getMovedPiece().getPieceAlliance().isWhite();
                final int fullMove = (i / 2) + 1;

                final AnalysisResult result = sf.analysePosition(board.toFEN(), ANALYSIS_MOVETIME_MS);

                if (result != null && onLiveEval != null) {
                    final int evalForBar = result.scoreWhiteCp();
                    SwingUtilities.invokeLater(() -> onLiveEval.accept(evalForBar));
                }

                final int scoreAfter = (result != null) ? result.scoreWhiteCp() : prevScoreFinal;
                prevScore = scoreAfter;

                publish(new MoveResult(fullMove, notation, isWhite, prevScoreFinal, result, idx, total));
            }
        } catch (IOException e) {
            System.err.println("GameAnalyser: Stockfish error — " + e.getMessage());
        } catch (Exception e) {
            System.err.println("GameAnalyser: unexpected error — " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void process(final List<MoveResult> chunks) {
        for (final MoveResult r : chunks) {
            panel.addRow(
                    r.fullMove(),
                    r.notation(),
                    r.isWhite() ? com.chess.engine.Alliance.WHITE : com.chess.engine.Alliance.BLACK,
                    r.evalBefore(),
                    r.result());

            if (progressLabel != null) {
                progressLabel.setText("Analysing move " + (r.moveIndex() + 1) + " / " + r.total() + "…");
                progressLabel.setVisible(true);
            }
        }
    }

    @Override
    protected void done() {
        if (!isCancelled()) {
            panel.setProgress(null);
            panel.onAnalysisComplete();
        }
        if (progressLabel != null) progressLabel.setVisible(false);
    }

    // ── Result record ─────────────────────────────────────────────────────────

    /**
     * Carries all data for one half-move from the background thread to the EDT.
     */
    public record MoveResult(
            int fullMove,
            String notation,
            boolean isWhite,
            int evalBefore,
            AnalysisResult result,
            int moveIndex,
            int total) {
    }
}
