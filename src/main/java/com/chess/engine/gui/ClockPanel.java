package com.chess.engine.gui;

import com.chess.engine.board.MoveLog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ClockPanel extends JPanel {

    private final JPanel whiteCard;
    private final JPanel blackCard;
    private final JLabel whiteTimeLabel;
    private final JLabel blackTimeLabel;
    private final JLabel whiteNameLabel;
    private final JLabel blackNameLabel;
    private final JLabel whiteDot;
    private final JLabel blackDot;
    private final TakenPiecesPanel whiteTakenPanel;
    private final TakenPiecesPanel blackTakenPanel;
    private final Runnable onWhiteTimeout;
    private final Runnable onBlackTimeout;
    private final Timer ticker;

    /** Remaining time in milliseconds for each player. */
    private long whiteMs = 600_000;
    private long blackMs = 600_000;
    private boolean whiteActive = false;
    private boolean enabled = true;
    /** Wall-clock time (ms) at which the current player's turn started, or -1 if not running. */
    private long turnStartMs = -1;
    /** Milliseconds remaining for the active player at the moment their turn started. */
    private long activeMsAtTurnStart = 0;

    public ClockPanel(final Runnable onWhiteTimeout, final Runnable onBlackTimeout) {
        super(new GridLayout(2, 1, 0, 10));
        this.onWhiteTimeout = onWhiteTimeout;
        this.onBlackTimeout = onBlackTimeout;

        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        // Black Card components
        blackNameLabel = makeNameLabel("Black");
        blackDot = makeStatusDot();
        blackTimeLabel = makeTimeLabel();
        blackTakenPanel = new TakenPiecesPanel(false);
        blackCard = makePlayerCard(false, blackNameLabel, blackDot, blackTimeLabel, blackTakenPanel);

        // White Card components
        whiteNameLabel = makeNameLabel("White");
        whiteDot = makeStatusDot();
        whiteTimeLabel = makeTimeLabel();
        whiteTakenPanel = new TakenPiecesPanel(true);
        whiteCard = makePlayerCard(true, whiteNameLabel, whiteDot, whiteTimeLabel, whiteTakenPanel);

        add(blackCard);
        add(whiteCard);

        ticker = new Timer(100, e -> tick());
        ticker.setInitialDelay(100);

        UITheme.addThemeListener(this::refreshStyles);
        refreshStyles();
    }

    /**
     * Formats milliseconds as {@code mm:ss} when ≥ 1 minute, or {@code ss.d} when under 1 minute.
     */
    private static String formatTime(final long totalMs) {
        if (totalMs <= 0) return "00:00.0";
        final long minutes = totalMs / 60_000;
        if (minutes >= 1) {
            final long secs = (totalMs % 60_000) / 1000;
            return String.format("%02d:%02d", minutes, secs);
        }
        final long secs = totalMs / 1000;
        final long tenths = (totalMs % 1000) / 100;
        return String.format("%02d.%d", secs, tenths);
    }

    private JLabel makeNameLabel(String name) {
        JLabel l = new JLabel(name);
        l.setFont(new Font("SansSerif", Font.BOLD, 14));
        return l;
    }

    private JLabel makeStatusDot() {
        return new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.getActiveGreen());
                g2.fillOval(2, (getHeight() - 8) / 2, 8, 8);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(12, 16);
            }
        };
    }

    private JLabel makeTimeLabel() {
        JLabel l = new JLabel("10:00");
        l.setFont(new Font("SansSerif", Font.BOLD, 30));
        return l;
    }

    private JPanel makePlayerCard(boolean isWhite, JLabel nameLabel, JLabel dot, JLabel timeLabel, TakenPiecesPanel takenPanel) {
        JPanel card = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean active = (isWhite && whiteActive) || (!isWhite && !whiteActive && ticker.isRunning());
                g2.setColor(UITheme.getCardBg());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                if (active) {
                    g2.setColor(UITheme.getActiveGreen());
                    g2.setStroke(new BasicStroke(2.0f));
                    g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 15, 15);
                } else {
                    g2.setColor(UITheme.getCardBorder());
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));

        // Left avatar
        JLabel avatar = new JLabel(ChessIcons.getUserAvatarIcon(44, isWhite));
        card.add(avatar, BorderLayout.WEST);

        // Center content:
        // Top: Name + Dot + Captured pieces strip
        // Bottom: Big Time Display
        JPanel textPanel = new JPanel(new BorderLayout(0, 2));
        textPanel.setOpaque(false);

        JPanel headerRow = new JPanel(new BorderLayout(6, 0));
        headerRow.setOpaque(false);

        JPanel nameAndDot = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        nameAndDot.setOpaque(false);
        nameAndDot.add(nameLabel);
        nameAndDot.add(dot);

        headerRow.add(nameAndDot, BorderLayout.WEST);
        headerRow.add(takenPanel, BorderLayout.CENTER);

        textPanel.add(headerRow, BorderLayout.NORTH);
        textPanel.add(timeLabel, BorderLayout.CENTER);

        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    public void redoTakenPieces(MoveLog moveLog) {
        whiteTakenPanel.redo(moveLog);
        blackTakenPanel.redo(moveLog);
    }

    public void refreshStyles() {
        Color fg = UITheme.getTextPrimary();
        Color sec = UITheme.getTextSecondary();

        whiteNameLabel.setForeground(sec);
        blackNameLabel.setForeground(sec);
        whiteTimeLabel.setForeground(fg);
        blackTimeLabel.setForeground(fg);

        updateLabels();
        repaint();
    }

    public void configure(final boolean on, final int minutes) {
        ticker.stop();
        enabled = on;
        final long ms = (long) minutes * 60_000;
        whiteMs = ms;
        blackMs = ms;
        whiteActive = false;
        turnStartMs = -1;
        activeMsAtTurnStart = 0;
        whiteTakenPanel.clear();
        blackTakenPanel.clear();
        updateLabels();
        setVisible(on);
    }

    public void reset(final boolean on, final int minutes) {
        configure(on, minutes);
    }

    public void onMoveMade(final com.chess.engine.Alliance nowToMove) {
        if (!enabled) return;
        // Commit any elapsed time from the previous turn before switching sides
        commitElapsed();
        whiteActive = nowToMove.isWhite();
        activeMsAtTurnStart = whiteActive ? whiteMs : blackMs;
        turnStartMs = System.currentTimeMillis();
        if (!ticker.isRunning()) ticker.start();
        updateLabels();
        repaint();
    }

    public void stop() {
        commitElapsed();
        turnStartMs = -1;
        ticker.stop();
        repaint();
    }

    public void pause() {
        if (ticker.isRunning()) {
            commitElapsed();
            turnStartMs = -1;
            ticker.stop();
        } else if (enabled) {
            activeMsAtTurnStart = whiteActive ? whiteMs : blackMs;
            turnStartMs = System.currentTimeMillis();
            ticker.start();
        }
        repaint();
    }

    public boolean isRunning() {
        return ticker.isRunning();
    }

    /** Saves elapsed wall-clock time back into the active player's counter. */
    private void commitElapsed() {
        if (turnStartMs < 0) return;
        final long elapsed = System.currentTimeMillis() - turnStartMs;
        if (whiteActive) {
            whiteMs = Math.max(0, activeMsAtTurnStart - elapsed);
        } else {
            blackMs = Math.max(0, activeMsAtTurnStart - elapsed);
        }
    }

    private void tick() {
        if (turnStartMs < 0) return;
        commitElapsed();
        updateLabels();
        if (whiteActive && whiteMs == 0) {
            ticker.stop();
            turnStartMs = -1;
            onWhiteTimeout.run();
        } else if (!whiteActive && blackMs == 0) {
            ticker.stop();
            turnStartMs = -1;
            onBlackTimeout.run();
        }
    }

    private void updateLabels() {
        whiteTimeLabel.setText(formatTime(whiteMs));
        blackTimeLabel.setText(formatTime(blackMs));

        boolean showWhiteDot = whiteActive && ticker.isRunning();
        boolean showBlackDot = !whiteActive && ticker.isRunning();
        whiteDot.setVisible(showWhiteDot);
        blackDot.setVisible(showBlackDot);

        repaint();
    }
}
