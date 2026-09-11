package com.chess.engine.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Two-player countdown clock displayed as stacked cards in the right sidebar.
 * Black's clock sits at the top; White's clock sits at the bottom.
 * Calls back into Table when a side runs out of time.
 */
class ClockPanel extends JPanel {

    // ── Palette (light theme) ─────────────────────────────────────────────
    private static final Color BG_CARD_IDLE    = new Color(240, 240, 242);
    private static final Color BG_CARD_ACTIVE  = new Color(255, 255, 255);
    private static final Color BG_CARD_WARN    = new Color(255, 220, 220);
    private static final Color TEXT_IDLE       = new Color(140, 140, 148);
    private static final Color TEXT_ACTIVE     = new Color(30,  30,  30);
    private static final Color TEXT_WARN       = new Color(180, 30,  30);
    private static final Color NAME_COLOR      = new Color(140, 140, 148);
    private static final Color BORDER_ACTIVE   = new Color(60,  140, 60);

    // ── Widgets ───────────────────────────────────────────────────────────
    private final JPanel whiteCard;
    private final JPanel blackCard;
    private final JLabel whiteTimeLabel;
    private final JLabel blackTimeLabel;
    private final JLabel whiteNameLabel;
    private final JLabel blackNameLabel;

    // ── State ─────────────────────────────────────────────────────────────
    private int whiteSeconds = 600;
    private int blackSeconds = 600;
    private boolean whiteActive = false;
    private boolean enabled = true;

    private final Runnable onWhiteTimeout;
    private final Runnable onBlackTimeout;
    private final javax.swing.Timer ticker;

    ClockPanel(final Runnable onWhiteTimeout, final Runnable onBlackTimeout) {
        super(new GridLayout(2, 1, 0, 6));
        this.onWhiteTimeout = onWhiteTimeout;
        this.onBlackTimeout = onBlackTimeout;

        setOpaque(false);
        setBorder(new EmptyBorder(4, 4, 4, 4));

        blackNameLabel = makeNameLabel("BLACK");
        blackTimeLabel = makeTimeLabel();
        blackCard = makeCard(blackNameLabel, blackTimeLabel);

        whiteNameLabel = makeNameLabel("WHITE");
        whiteTimeLabel = makeTimeLabel();
        whiteCard = makeCard(whiteNameLabel, whiteTimeLabel);

        add(blackCard);
        add(whiteCard);

        ticker = new javax.swing.Timer(1000, e -> tick());
        ticker.setInitialDelay(1000);

        updateLabels();
    }

    // ── Factory helpers ───────────────────────────────────────────────────

    private static JLabel makeNameLabel(final String name) {
        final JLabel l = new JLabel(name, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(NAME_COLOR);
        l.setBorder(new EmptyBorder(6, 0, 2, 0));
        return l;
    }

    private static JLabel makeTimeLabel() {
        final JLabel l = new JLabel("10:00", SwingConstants.CENTER);
        l.setFont(new Font("Monospaced", Font.BOLD, 28));
        l.setForeground(TEXT_IDLE);
        l.setBorder(new EmptyBorder(2, 0, 8, 0));
        return l;
    }

    private static JPanel makeCard(final JLabel nameLabel, final JLabel timeLabel) {
        final JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(final Graphics g) {
                final Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        card.setBackground(BG_CARD_IDLE);
        card.setOpaque(false);
        card.add(nameLabel, BorderLayout.NORTH);
        card.add(timeLabel, BorderLayout.CENTER);
        return card;
    }

    // ── Public API ────────────────────────────────────────────────────────

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
        updateLabels();
    }

    void stop() {
        ticker.stop();
    }

    // ── Tick ──────────────────────────────────────────────────────────────

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

    // ── Rendering ─────────────────────────────────────────────────────────

    private void updateLabels() {
        whiteTimeLabel.setText(formatTime(whiteSeconds));
        blackTimeLabel.setText(formatTime(blackSeconds));

        styleCard(whiteCard, whiteTimeLabel, whiteNameLabel, whiteActive, whiteSeconds);
        styleCard(blackCard, blackTimeLabel, blackNameLabel, !whiteActive, blackSeconds);
    }

    private static void styleCard(final JPanel card, final JLabel timeLabel,
                                   final JLabel nameLabel, final boolean active,
                                   final int seconds) {
        final boolean warn = seconds <= 10;
        if (warn) {
            card.setBackground(BG_CARD_WARN);
            timeLabel.setForeground(TEXT_WARN);
            nameLabel.setForeground(TEXT_WARN);
            card.setBorder(BorderFactory.createEmptyBorder());
        } else if (active) {
            card.setBackground(BG_CARD_ACTIVE);
            timeLabel.setForeground(TEXT_ACTIVE);
            nameLabel.setForeground(BORDER_ACTIVE);
            card.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, BORDER_ACTIVE));
        } else {
            card.setBackground(BG_CARD_IDLE);
            timeLabel.setForeground(TEXT_IDLE);
            nameLabel.setForeground(NAME_COLOR);
            card.setBorder(BorderFactory.createEmptyBorder());
        }
        card.repaint();
    }

    private static String formatTime(final int totalSeconds) {
        final int m = totalSeconds / 60;
        final int s = totalSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
