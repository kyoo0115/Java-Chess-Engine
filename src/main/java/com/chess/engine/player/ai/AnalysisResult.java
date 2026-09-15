package com.chess.engine.player.ai;

/**
 * Result of a single Stockfish analysis call for one half-move position.
 *
 * @param scoreWhiteCp  Score in centipawns from White's perspective.
 *                      Positive = White is better, negative = Black is better.
 *                      {@link Integer#MIN_VALUE} signals a mate score; use
 *                      {@link #isMate()} and {@link #mateIn()} to inspect it.
 * @param bestUci       Stockfish's best-move suggestion in UCI notation (e.g. "e2e4"),
 *                      or {@code null} if the position has no legal moves.
 */
public record AnalysisResult(int scoreWhiteCp, String bestUci) {

    /** Sentinel value used when Stockfish reports a forced mate (e.g. "mate 3"). */
    public static final int MATE_SCORE = Integer.MAX_VALUE / 2;

    /**
     * Creates an AnalysisResult from a "mate N" token.
     *
     * @param mateIn positive = White mates in N, negative = Black mates in N
     */
    public static AnalysisResult fromMate(final int mateIn, final String bestUci) {
        // White mating → very large positive; Black mating → very large negative
        final int score = mateIn > 0 ? MATE_SCORE : -MATE_SCORE;
        return new AnalysisResult(score, bestUci);
    }

    /** Returns {@code true} if the score represents a forced mate (not a centipawn value). */
    public boolean isMate() {
        return Math.abs(scoreWhiteCp) >= MATE_SCORE;
    }

    /**
     * If {@link #isMate()} is true, returns the raw mate-in value passed to
     * {@link #fromMate} (positive = White mates, negative = Black mates).
     * Returns 0 when not a mate score.
     */
    public int mateIn() {
        if (!isMate()) return 0;
        return scoreWhiteCp > 0 ? 1 : -1; // sign only; exact ply not stored
    }

    /**
     * Human-readable evaluation string, e.g. "+1.23", "-0.50", "M3", "-M2".
     */
    public String evalString() {
        if (isMate()) {
            return (scoreWhiteCp > 0 ? "M" : "-M") + "…";
        }
        final float pawns = scoreWhiteCp / 100f;
        return (pawns >= 0 ? "+" : "") + String.format("%.2f", pawns);
    }

    /**
     * Classifies the quality of the move that led to this position given the
     * evaluation before ({@code prevScore}) and after ({@code thisScore}) the move.
     *
     * @param prevScore eval BEFORE the move (White cp, from the position the move was played from)
     * @param thisScore eval AFTER  the move (White cp, after the move was executed)
     * @param whiteJustMoved true if the move was played by White
     */
    public static MoveClassification classify(
            final int prevScore, final int thisScore, final boolean whiteJustMoved) {
        // Convert to "eval from the moving side's perspective"
        final int before = whiteJustMoved ? prevScore : -prevScore;
        final int after  = whiteJustMoved ? thisScore : -thisScore;
        final int drop   = before - after;   // positive = player lost eval

        if (drop >= 200) return MoveClassification.BLUNDER;
        if (drop >= 100) return MoveClassification.MISTAKE;
        if (drop >= 50)  return MoveClassification.INACCURACY;
        if (drop >= -20) return MoveClassification.GOOD;
        return MoveClassification.BEST; // player found the best move or better
    }
}
