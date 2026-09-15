package com.chess.engine.gui;

import javax.swing.*;
import java.awt.*;

public class BoardContainer extends JPanel {

    private static final int MARGIN = 28;
    private static final int MARGIN      = 28;
    /** Extra pixels added to the LEFT margin to accommodate the eval bar. */
    private static final int LEFT_EXTRA  = 20;
    /** Width of the live eval bar, painted on the right edge of the left margin. */
    private static final int EVAL_BAR_W  = 14;
    /** Gap between the eval bar right edge and the board left edge. */
    private static final int EVAL_BAR_GAP = 4;

    private static final Font COORD_FONT = new Font("SansSerif", Font.BOLD, 12);
    private static final Font EVAL_FONT  = new Font("SansSerif", Font.BOLD, 10);

    private final BoardPanel boardPanel;
    private final TableContext ctx;

    /**
     * Current evaluation in centipawns from White's perspective.
     * {@code Integer.MIN_VALUE} means "not available" (bar hidden).
     */
    private int liveEvalCp = Integer.MIN_VALUE;

    public BoardContainer(BoardPanel boardPanel, TableContext ctx) {
        super(null); // manual layout so we can enforce a square board
        this.boardPanel = boardPanel;
        this.ctx = ctx;

        setOpaque(false);
        add(boardPanel);

        UITheme.addThemeListener(this::repaint);
    }

    /**
     * Updates the live evaluation bar.  Pass {@link Integer#MIN_VALUE} to hide it.
     * Must be called on the EDT.
     *
     * @param cp centipawns from White's perspective (positive = White better)
     */
    public void setLiveEval(final int cp) {
        this.liveEvalCp = cp;
        repaint();
    }

    /**
     * Force the inner BoardPanel to always be a perfect square.
     */
    @Override
    public void doLayout() {
        final int w = getWidth();
        final int h = getHeight();
        final int leftMargin = MARGIN + LEFT_EXTRA;
        // available space after margins
        final int availW = w - leftMargin - MARGIN;
        final int availH = h - MARGIN - MARGIN;
        final int side = Math.max(0, Math.min(availW, availH));
        // centre the square inside the available area
        final int x = leftMargin + (availW - side) / 2;
        final int y = MARGIN + (availH - side) / 2;
        boardPanel.setBounds(x, y, side, side);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Draw outer rounded card background for the board area
        g2.setColor(UITheme.getBoardFrameBg());
        g2.fillRoundRect(0, 0, w, h, 20, 20);

        g2.setColor(UITheme.getCardBorder());
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 20, 20);

        // 2. Live eval bar (right side of left margin, along the board height)
        if (liveEvalCp != Integer.MIN_VALUE) {
            paintEvalBar(g2);
        }

        // 3. Coordinates
        if (ctx.isShowCoordinates()) {
            g2.setFont(COORD_FONT);
            g2.setColor(UITheme.getTextSecondary());
            FontMetrics fm = g2.getFontMetrics();

            int bx = boardPanel.getX();
            int by = boardPanel.getY();
            int bw = boardPanel.getWidth();
            int bh = boardPanel.getHeight();

            if (bw > 0 && bh > 0) {
                int tw = bw / 8;
                int th = bh / 8;

                String[] files = {"a", "b", "c", "d", "e", "f", "g", "h"};
                String[] ranks = {"8", "7", "6", "5", "4", "3", "2", "1"};

                String[] fLabels = ctx.getBoardDirection() == BoardDirection.FLIPPED
                        ? new String[]{"h", "g", "f", "e", "d", "c", "b", "a"} : files;
                String[] rLabels = ctx.getBoardDirection() == BoardDirection.FLIPPED
                        ? new String[]{"1", "2", "3", "4", "5", "6", "7", "8"} : ranks;

                // Rank numbers along the left margin
                for (int i = 0; i < 8; i++) {
                    String rl = rLabels[i];
                    int rx = (MARGIN - fm.stringWidth(rl)) / 2;
                    int ry = by + i * th + th / 2 + fm.getAscent() / 2 - 2;
                    g2.drawString(rl, rx, ry);
                }

                // File letters along the bottom margin
                for (int i = 0; i < 8; i++) {
                    String fl = fLabels[i];
                    int fx = bx + i * tw + tw / 2 - fm.stringWidth(fl) / 2;
                    int fy = by + bh + (MARGIN + fm.getAscent()) / 2 - 2;
                    g2.drawString(fl, fx, fy);
                }
            }
        }

        g2.dispose();
    }

    /**
     * Paints the live evaluation bar to the left of the board.
     *
     * <p>The bar is split: Black's colour (dark) fills from the top, White's
     * colour (light) fills from the bottom.  The eval score is drawn rotated
     * inside the winning side's section, and the numeric value (+1.23 / -0.50)
     * is shown in small text below the bar in the bottom margin.
     */
    private void paintEvalBar(final Graphics2D g2) {
        final int bx = boardPanel.getX();
        final int by = boardPanel.getY();
        final int bh = boardPanel.getHeight();
        final int bBottom = by + bh;
        if (bh <= 0) return;

        final int barX = bx - EVAL_BAR_W - EVAL_BAR_GAP;
        final int barY = by;
        final int barH = bh;

        // ── Clamp & fraction ──────────────────────────────────────────────────
        final int clamped = Math.max(-600, Math.min(600, liveEvalCp));
        // whiteFrac: 0.0 = Black fully winning, 0.5 = equal, 1.0 = White fully winning
        final float whiteFrac = (clamped + 600) / 1200f;
        final int whiteH = Math.round(whiteFrac * barH);
        final int blackH = barH - whiteH;

        // ── Black section (top) ───────────────────────────────────────────────
        g2.setColor(UITheme.isDark() ? new Color(38, 38, 44) : new Color(52, 52, 58));
        g2.fillRoundRect(barX, barY, EVAL_BAR_W, barH, 5, 5);

        // ── White section (bottom, overlaid) ─────────────────────────────────
        final int whiteY = barY + blackH;
        g2.setColor(UITheme.isDark() ? new Color(215, 215, 215) : new Color(242, 242, 242));
        // Bottom-rounded only: draw a full rect then re-draw top corners in white
        g2.fillRoundRect(barX, whiteY, EVAL_BAR_W, whiteH, 5, 5);
        // Cover the top-left / top-right rounded bumps of the white fill so the
        // boundary between the two colours is a clean straight line, not a gap.
        if (whiteH > 5) g2.fillRect(barX, whiteY, EVAL_BAR_W, 5);

        // ── Midpoint tick ─────────────────────────────────────────────────────
        g2.setColor(new Color(128, 128, 128, 100));
        g2.setStroke(new BasicStroke(1f));
        final int midY = barY + barH / 2;
        g2.drawLine(barX, midY, barX + EVAL_BAR_W, midY);

        // ── Eval label rotated inside the bar ────────────────────────────────
        // Build the display string
        final String evalStr = evalDisplayString();

        g2.setFont(EVAL_FONT);
        final FontMetrics fm = g2.getFontMetrics();
        final int textW = fm.stringWidth(evalStr);

        // Decide which section to draw in (winning side), and what colour
        final boolean whiteAhead = liveEvalCp >= 0;
        // Only draw label when that section is tall enough to fit text
        final int sectionH = whiteAhead ? whiteH : blackH;
        if (sectionH >= textW + 4) {
            final int sectionTop = whiteAhead ? whiteY : barY;
            // Centre the rotated text in the section
            final int textCentreY = sectionTop + sectionH / 2;
            final int textCentreX = barX + EVAL_BAR_W / 2;

            final Color labelColor = whiteAhead
                    ? new Color(40, 40, 40)
                    : new Color(210, 210, 210);

            g2.setColor(labelColor);
            final java.awt.geom.AffineTransform saved = g2.getTransform();
            // Rotate -90° so text reads bottom-to-top
            g2.translate(textCentreX, textCentreY);
            g2.rotate(-Math.PI / 2);
            g2.drawString(evalStr, -textW / 2, fm.getAscent() / 2);
            g2.setTransform(saved);
        }

        // ── Numeric score below bar (bottom margin) ───────────────────────────
        // E.g. "+1.23" in accent colour beneath the bar, centred horizontally
        g2.setFont(EVAL_FONT);
        final FontMetrics fm2 = g2.getFontMetrics();
        final int labelW = fm2.stringWidth(evalStr);
        final int labelX = barX + (EVAL_BAR_W - labelW) / 2;
        final int labelY = bBottom + (MARGIN + fm2.getAscent()) / 2 - 1;
        // Colour: green when White ahead, muted red when Black ahead, grey for ≈even
        final Color scoreColor;
        if (liveEvalCp > 20)        scoreColor = new Color(70, 180, 100);
        else if (liveEvalCp < -20)  scoreColor = new Color(210, 80, 80);
        else                        scoreColor = UITheme.getTextMuted();
        g2.setColor(scoreColor);
        g2.drawString(evalStr, labelX, labelY);
    }

    /** Formats {@code liveEvalCp} as "+1.23", "-0.50", "+M", "-M" etc. */
    private String evalDisplayString() {
        // Mate scores stored as ±MATE_SCORE sentinel
        final int MATE = com.chess.engine.player.ai.AnalysisResult.MATE_SCORE;
        if (liveEvalCp >= MATE)  return "+M";
        if (liveEvalCp <= -MATE) return "-M";
        final float pawns = liveEvalCp / 100f;
        return (pawns >= 0 ? "+" : "") + String.format("%.2f", pawns);
    }
}
