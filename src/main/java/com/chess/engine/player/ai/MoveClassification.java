package com.chess.engine.player.ai;

import java.awt.*;

/**
 * Quality classification for a chess move, based on centipawn loss.
 */
public enum MoveClassification {
    BEST("Best", new Color(106, 200, 116)),       // ≤ −20 cp loss (played best or better)
    GOOD("Good", new Color(149, 185, 230)),       // −20..49 cp loss
    INACCURACY("Inaccuracy", new Color(246, 196, 78)),  // 50..99 cp loss
    MISTAKE("Mistake", new Color(239, 152, 68)),   // 100..199 cp loss
    BLUNDER("Blunder", new Color(230, 90, 90));    // ≥ 200 cp loss

    public final String label;
    public final Color color;

    MoveClassification(final String label, final Color color) {
        this.label = label;
        this.color = color;
    }
}
