package com.chess.engine.gui;

import javax.swing.*;
import java.awt.*;

public class BoardContainer extends JPanel {

    private static final int MARGIN = 28;

    private static final Font COORD_FONT = new Font("SansSerif", Font.BOLD, 12);

    private final BoardPanel boardPanel;
    private final TableContext ctx;

    public BoardContainer(BoardPanel boardPanel, TableContext ctx) {
        super(null); // manual layout so we can enforce a square board
        this.boardPanel = boardPanel;
        this.ctx = ctx;

        setOpaque(false);
        add(boardPanel);

        UITheme.addThemeListener(this::repaint);
    }

    /** Force the inner BoardPanel to always be a perfect square. */
    @Override
    public void doLayout() {
        final int w = getWidth();
        final int h = getHeight();
        // available space after margins
        final int availW = w - MARGIN - MARGIN;
        final int availH = h - MARGIN - MARGIN;
        final int side = Math.max(0, Math.min(availW, availH));
        // centre the square inside the available area
        final int x = MARGIN + (availW - side) / 2;
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

        // 2. Coordinates
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
}
