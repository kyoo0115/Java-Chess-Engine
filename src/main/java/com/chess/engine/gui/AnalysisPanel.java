package com.chess.engine.gui;

import com.chess.engine.Alliance;
import com.chess.engine.player.ai.AnalysisResult;
import com.chess.engine.player.ai.MoveClassification;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel that shows post-game / live analysis results alongside the move history.
 *
 * <p>Layout (top to bottom):
 * <pre>
 *   ┌─────────────────────────────┐
 *   │  Accuracy bar  W% / B%      │  ← accuracy summary (shown after full analysis)
 *   ├─────────────────────────────┤
 *   │  # │ Move  │ Eval │ Class   │  ← scrollable move rows
 *   │    │  …    │  …   │  …      │
 *   └─────────────────────────────┘
 * </pre>
 *
 * <p>Thread safety: all public mutator methods must be called on the EDT.
 */
public class AnalysisPanel extends JPanel {

    // ── Sizing ────────────────────────────────────────────────────────────────
    private static final int ROW_HEIGHT = 34;
    private static final int NUM_W = 30;
    private static final int MOVE_W = 62;
    private static final int EVAL_W = 58;
    private static final int CLASS_W = 80; // filled last, takes remaining space
    private static final int H_PAD = 8;
    private static final int PILL_ARC = 8;
    private static final int BADGE_H = 18;

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font ROW_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font MOVE_FONT = new Font("SansSerif", Font.BOLD, 12);
    private static final Font BADGE_FONT = new Font("SansSerif", Font.BOLD, 10);
    private static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 12);
    private static final Font ACCURACY_FONT = new Font("SansSerif", Font.BOLD, 13);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 11);

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<AnalysisRow> rows = new ArrayList<>();
    // ── Sub-components ────────────────────────────────────────────────────────
    private final AccuracyBar accuracyBar;
    private final MoveListPanel listPanel;
    private final JScrollPane scrollPane;
    private final JLabel progressLabel;
    /**
     * Per-move centipawn-loss accumulators for accuracy calculation.
     */
    private int whiteTotalCpLoss = 0;
    private int blackTotalCpLoss = 0;
    private int whiteMoveCount = 0;
    private int blackMoveCount = 0;
    /**
     * Whether a full analysis has completed (shows accuracy summary).
     */
    private boolean analysisComplete = false;
    /**
     * Progress label — shown while analysis is running.
     */
    private String progressText = null;

    public AnalysisPanel() {
        super(new BorderLayout(0, 0));
        setOpaque(false);

        // ── Accuracy / Progress header ────────────────────────────────────────
        accuracyBar = new AccuracyBar();
        accuracyBar.setVisible(false);

        progressLabel = new JLabel("", SwingConstants.CENTER);
        progressLabel.setFont(LABEL_FONT);
        progressLabel.setForeground(UITheme.getTextMuted());
        progressLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        progressLabel.setVisible(false);

        final JPanel topArea = new JPanel(new BorderLayout());
        topArea.setOpaque(false);
        topArea.add(accuracyBar, BorderLayout.NORTH);
        topArea.add(progressLabel, BorderLayout.CENTER);

        // ── Move list ─────────────────────────────────────────────────────────
        listPanel = new MoveListPanel();
        scrollPane = new JScrollPane(listPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);

        final JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(final Graphics g) {
                final Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.getCardBg());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(UITheme.getCardBorder());
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(6, 6, 6, 6));
        card.add(topArea, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);

        UITheme.addThemeListener(this::repaint);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Standard centipawn-loss accuracy formula:
     * {@code accuracy = 103.1668 * exp(-0.04354 * avgCpLoss) - 3.1669}
     * clamped to [0, 100].
     */
    private static double accuracy(final int totalCpLoss, final int moves) {
        if (moves == 0) return 100.0;
        final double avg = (double) totalCpLoss / moves;
        final double raw = 103.1668 * Math.exp(-0.04354 * avg) - 3.1669;
        return Math.max(0, Math.min(100, raw));
    }

    /**
     * Removes all rows and resets accuracy counters. Call on EDT.
     */
    public void clear() {
        rows.clear();
        whiteTotalCpLoss = 0;
        blackTotalCpLoss = 0;
        whiteMoveCount = 0;
        blackMoveCount = 0;
        analysisComplete = false;
        progressText = null;
        progressLabel.setVisible(false);
        accuracyBar.setVisible(false);
        listPanel.setPreferredHeight(0);
        listPanel.revalidate();
        listPanel.repaint();
    }

    /**
     * Appends one analysed half-move row. Call on EDT (e.g. from SwingWorker.process()).
     *
     * @param moveNumber   1-based full-move number
     * @param moveNotation SAN or coordinate notation of the move played
     * @param alliance     which side played the move
     * @param evalBefore   evaluation (White cp) BEFORE the move was played
     * @param result       Stockfish's analysis of the position AFTER the move
     */
    public void addRow(final int moveNumber,
                       final String moveNotation,
                       final Alliance alliance,
                       final int evalBefore,
                       final AnalysisResult result) {
        final int evalAfter = (result != null) ? result.scoreWhiteCp() : evalBefore;
        final MoveClassification cls = AnalysisResult.classify(evalBefore, evalAfter, alliance.isWhite());
        final String evalStr = (result != null) ? result.evalString() : "?";
        final String bestMove = (result != null) ? result.bestUci() : null;

        // Accumulate cp loss for accuracy (both scores are White-relative):
        //   White's loss  = evalBefore - evalAfter  (positive when White's position worsened)
        //   Black's loss  = evalAfter  - evalBefore  (positive when position improved for White = bad for Black)
        final int cpLoss = Math.max(0,
                alliance.isWhite() ? (evalBefore - evalAfter) : (evalAfter - evalBefore));
        if (alliance.isWhite()) {
            whiteTotalCpLoss += cpLoss;
            whiteMoveCount++;
        } else {
            blackTotalCpLoss += cpLoss;
            blackMoveCount++;
        }

        rows.add(new AnalysisRow(moveNumber, moveNotation, alliance, evalStr, cls, bestMove));
        listPanel.setPreferredHeight(rows.size() * ROW_HEIGHT);
        listPanel.revalidate();
        listPanel.repaint();

        // Auto-scroll to newest row — double invokeLater lets the layout pass
        // triggered by revalidate() complete before we read the new maximum.
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
            final JScrollBar vsb = scrollPane.getVerticalScrollBar();
            vsb.setValue(vsb.getMaximum());
        }));
    }

    /**
     * Updates the "Analysing move X/Y…" progress text. Pass {@code null} to hide.
     */
    public void setProgress(final String text) {
        progressText = text;
        if (text != null) {
            progressLabel.setText(text);
            progressLabel.setVisible(true);
        } else {
            progressLabel.setVisible(false);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Called when the full analysis is done — shows the accuracy summary bar.
     */
    public void onAnalysisComplete() {
        analysisComplete = true;
        progressLabel.setVisible(false);
        final double whiteAcc = accuracy(whiteTotalCpLoss, whiteMoveCount);
        final double blackAcc = accuracy(blackTotalCpLoss, blackMoveCount);
        accuracyBar.update(whiteAcc, blackAcc);
        accuracyBar.setVisible(true);
    }

    // ── Inner classes ─────────────────────────────────────────────────────────

    private record AnalysisRow(
            int moveNumber,
            String notation,
            Alliance alliance,
            String evalStr,
            MoveClassification classification,
            String bestUci) {
    }

    // ── Accuracy Bar ─────────────────────────────────────────────────────────

    private static class AccuracyBar extends JPanel {
        private double whiteAcc = 0;
        private double blackAcc = 0;

        AccuracyBar() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 54));
            setBorder(new EmptyBorder(6, 8, 6, 8));
        }

        void update(final double wAcc, final double bAcc) {
            this.whiteAcc = wAcc;
            this.blackAcc = bAcc;
            repaint();
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            final int w = getWidth() - 16;
            final int barH = 8;
            final int barY = 32;
            final int barX = 8;

            // Header label
            g2.setFont(HEADER_FONT);
            g2.setColor(UITheme.getTextPrimary());
            g2.drawString("Accuracy", barX, 16);

            // White accuracy text
            g2.setFont(ACCURACY_FONT);
            g2.setColor(Color.WHITE);
            final String wStr = String.format("%.0f%%", whiteAcc);
            final String bStr = String.format("%.0f%%", blackAcc);

            // Bar background
            g2.setColor(UITheme.getCardBorder());
            g2.fillRoundRect(barX, barY, w, barH, barH, barH);

            // White fill (from left)
            final int whiteW = (int) (w * whiteAcc / 100.0);
            g2.setColor(new Color(220, 220, 220));
            g2.fillRoundRect(barX, barY, Math.max(barH, whiteW), barH, barH, barH);

            // Black fill (from right)
            final int blackW = (int) (w * blackAcc / 100.0);
            g2.setColor(new Color(60, 60, 60));
            g2.fillRoundRect(barX + w - Math.max(barH, blackW), barY, Math.max(barH, blackW), barH, barH, barH);

            // Labels
            final FontMetrics fm = g2.getFontMetrics();
            // White label (left)
            g2.setColor(UITheme.getTextPrimary());
            g2.setFont(LABEL_FONT);
            g2.drawString("W " + wStr, barX, barY + barH + 14);
            // Black label (right)
            final int bLabelW = fm.stringWidth("B " + bStr);
            g2.drawString("B " + bStr, barX + w - bLabelW, barY + barH + 14);

            g2.dispose();
        }
    }

    // ── Move List Panel ───────────────────────────────────────────────────────

    private class MoveListPanel extends JPanel {
        private int preferredHeight = 0;

        MoveListPanel() {
            setOpaque(false);
        }

        void setPreferredHeight(final int h) {
            this.preferredHeight = h;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(230, Math.max(preferredHeight, 60));
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            if (rows.isEmpty()) return;

            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            final int w = getWidth();

            for (int i = 0; i < rows.size(); i++) {
                final AnalysisRow row = rows.get(i);
                final int y = i * ROW_HEIGHT;

                // Alternating row tint
                if (i % 2 == 1) {
                    g2.setColor(UITheme.getRowAlt());
                    g2.fillRoundRect(2, y + 1, w - 4, ROW_HEIGHT - 2, 6, 6);
                }

                int x = H_PAD;

                // ── Move number ─────────────────────────────────────────────
                g2.setFont(ROW_FONT);
                g2.setColor(UITheme.getTextMuted());
                final String numStr = row.moveNumber() + (row.alliance().isWhite() ? "." : "…");
                drawCentred(g2, numStr, 0, y, NUM_W);
                x = NUM_W;

                // ── Move notation (white = light bg pill, black = dark) ─────
                final Color moveBg = row.alliance().isWhite()
                        ? new Color(230, 230, 230, 100)
                        : new Color(60, 60, 60, 100);
                g2.setFont(MOVE_FONT);
                final FontMetrics mfm = g2.getFontMetrics();
                final int mw = mfm.stringWidth(row.notation());
                final int pillW = Math.max(mw + H_PAD * 2, 44);
                g2.setColor(moveBg);
                g2.fillRoundRect(x + 2, y + (ROW_HEIGHT - 20) / 2, pillW, 20, 6, 6);
                g2.setColor(UITheme.getTextPrimary());
                g2.drawString(row.notation(), x + 2 + (pillW - mw) / 2,
                        y + (ROW_HEIGHT - mfm.getHeight()) / 2 + mfm.getAscent());
                x += MOVE_W;

                // ── Eval string ─────────────────────────────────────────────
                g2.setFont(ROW_FONT);
                g2.setColor(evalColor(row.evalStr()));
                drawCentred(g2, row.evalStr(), x, y, EVAL_W);
                x += EVAL_W;

                // ── Classification badge ─────────────────────────────────────
                final MoveClassification cls = row.classification();
                final String badgeText = cls.label;
                g2.setFont(BADGE_FONT);
                final FontMetrics bfm = g2.getFontMetrics();
                final int badgeW = bfm.stringWidth(badgeText) + H_PAD * 2;
                final int badgeX = x + 2;
                final int badgeY = y + (ROW_HEIGHT - BADGE_H) / 2;
                g2.setColor(new Color(cls.color.getRed(), cls.color.getGreen(), cls.color.getBlue(), 40));
                g2.fillRoundRect(badgeX, badgeY, badgeW, BADGE_H, PILL_ARC, PILL_ARC);
                g2.setColor(cls.color);
                g2.drawRoundRect(badgeX, badgeY, badgeW, BADGE_H, PILL_ARC, PILL_ARC);
                g2.drawString(badgeText, badgeX + H_PAD,
                        badgeY + (BADGE_H - bfm.getHeight()) / 2 + bfm.getAscent());
            }
            g2.dispose();
        }

        private Color evalColor(final String eval) {
            if (eval.startsWith("+")) return new Color(70, 180, 100);
            if (eval.startsWith("-")) return new Color(200, 80, 80);
            return UITheme.getTextSecondary();
        }

        private void drawCentred(final Graphics2D g2, final String text,
                                 final int x, final int y, final int cellW) {
            final FontMetrics fm = g2.getFontMetrics();
            final int tw = fm.stringWidth(text);
            final int cx = x + (cellW - tw) / 2;
            final int baseline = y + (ROW_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, cx, baseline);
        }
    }
}
