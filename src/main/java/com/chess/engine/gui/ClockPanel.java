package com.chess.engine.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Two-player countdown clock displayed at the top of the board.
 * Calls back into Table when a side runs out of time.
 */
class ClockPanel extends JPanel {

    private final JLabel whiteLabel = new JLabel("10:00", SwingConstants.CENTER);
    private final JLabel blackLabel = new JLabel("10:00", SwingConstants.CENTER);
    private int whiteSeconds = 600;
    private int blackSeconds = 600;
    private boolean whiteActive = false;
    private boolean enabled = true;
    private final javax.swing.Timer ticker;
    private Runnable onWhiteTimeout;
    private Runnable onBlackTimeout;

    ClockPanel(final Runnable onWhiteTimeout, final Runnable onBlackTimeout) {
        super(new GridLayout(1, 4));
        this.onWhiteTimeout = onWhiteTimeout;
        this.onBlackTimeout = onBlackTimeout;

        final Font f = new Font("Monospaced", Font.BOLD, 16);
        whiteLabel.setFont(f);
        blackLabel.setFont(f);
        whiteLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        blackLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        add(new JLabel("White", SwingConstants.RIGHT));
        add(whiteLabel);
        add(blackLabel);
        add(new JLabel("Black", SwingConstants.LEFT));

        ticker = new javax.swing.Timer(1000, e -> tick());
        ticker.setInitialDelay(1000);
    }

    void configure(final boolean on, final int minutes) {
        ticker.stop();
        enabled = on;
        final int secs = minutes * 60;
        whiteSeconds = secs;
        blackSeconds = secs;
        whiteActive = false;
        updateLabels();
        setVisible(on);
    }

    void reset(final boolean on, final int minutes) {
        configure(on, minutes);
    }

    void onMoveMade(final com.chess.engine.Alliance nowToMove) {
        if (!enabled) return;
        whiteActive = nowToMove.isWhite();
        if (!ticker.isRunning()) ticker.start();
    }

    void stop() {
        ticker.stop();
    }

    private void tick() {
        if (!enabled) return;
        if (whiteActive) {
            whiteSeconds--;
            if (whiteSeconds <= 0) {
                whiteSeconds = 0;
                ticker.stop();
                updateLabels();
                if (onWhiteTimeout != null) onWhiteTimeout.run();
                return;
            }
        } else {
            blackSeconds--;
            if (blackSeconds <= 0) {
                blackSeconds = 0;
                ticker.stop();
                updateLabels();
                if (onBlackTimeout != null) onBlackTimeout.run();
                return;
            }
        }
        updateLabels();
    }

    private void updateLabels() {
        whiteLabel.setText(formatTime(whiteSeconds));
        blackLabel.setText(formatTime(blackSeconds));
        final Color activeCol = new Color(200, 240, 200);
        final Color warnCol   = new Color(255, 100, 100);
        final Color idleCol   = getBackground();
        whiteLabel.setBackground(whiteActive
                ? (whiteSeconds <= 10 ? warnCol : activeCol) : idleCol);
        blackLabel.setBackground(!whiteActive
                ? (blackSeconds <= 10 ? warnCol : activeCol) : idleCol);
        whiteLabel.setOpaque(whiteActive || whiteSeconds <= 10);
        blackLabel.setOpaque(!whiteActive || blackSeconds <= 10);
    }

    private String formatTime(final int totalSeconds) {
        final int m = totalSeconds / 60;
        final int s = totalSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
