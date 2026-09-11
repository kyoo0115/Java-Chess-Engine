package com.chess.engine.gui;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.MoveLog;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * • No table header — move number acts as the label
 * • Three columns per row: move# | White | Black
 * • Last move is highlighted with a soft blue pill
 * • Comfortable row height with clean SansSerif font
 */
public class GameHistoryPanel extends JPanel {

    // ── Dimensions ────────────────────────────────────────────────────────
    private static final Dimension PREFERRED_SIZE = new Dimension(160, 400);
    private static final int ROW_HEIGHT = 26;
    private static final int NUM_COL_WIDTH = 28;
    private static final int CELL_PAD_H = 6;
    private static final int PILL_ARC = 6;

    // ── Light-theme palette ───────────────────────────────────────────────
    private static final Color BG = new Color(248, 248, 250);
    private static final Color ROW_ALT = new Color(240, 241, 245);
    private static final Color NUM_FG = new Color(150, 150, 160);
    private static final Color MOVE_FG = new Color(25, 25, 35);
    private static final Color LAST_PILL_BG = new Color(210, 228, 255);   // Lichess-style blue tint
    private static final Color LAST_PILL_FG = new Color(20, 80, 180);
    private static final Font NUM_FONT = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font MOVE_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font LAST_MOVE_FONT = new Font("SansSerif", Font.BOLD, 13);

    // ── Data ──────────────────────────────────────────────────────────────
    /**
     * Each entry = one full move-pair row: [moveNumber, whiteMove, blackMove]
     */
    private final List<MoveRow> rows = new ArrayList<>();
    // ── The custom list panel ─────────────────────────────────────────────
    private final MoveListPanel listPanel;
    private final JScrollPane scrollPane;
    /**
     * Index into rows of the last highlighted move, and which half (0=white,1=black).
     */
    private int lastRowIdx = -1;
    private int lastHalf = -1;   // 0 = white cell, 1 = black cell

    GameHistoryPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);

        listPanel = new MoveListPanel();
        scrollPane = new JScrollPane(listPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BG);
        scrollPane.setBackground(BG);
        scrollPane.setPreferredSize(PREFERRED_SIZE);

        add(scrollPane, BorderLayout.CENTER);
    }

    // ── Public refresh ────────────────────────────────────────────────────

    /**
     * Returns check/checkmate suffix only for the move at position {@code idx}
     * if it is the last move in the list (so we don't annotate mid-game moves
     * that were later superseded — matching how chess.com renders it).
     */
    private static String checkSuffix(final Board board, final List<Move> moves, final int idx) {
        if (idx != moves.size() - 1) return "";
        if (board.getCurrentPlayer().isCheckMate()) return "#";
        if (board.getCurrentPlayer().isInCheck()) return "+";
        return "";
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    public void redo(final Board board, final MoveLog moveLog) {
        rows.clear();
        lastRowIdx = -1;
        lastHalf = -1;

        final List<Move> moves = moveLog.getMoves();
        MoveRow current = null;
        int moveNum = 1;

        for (int i = 0; i < moves.size(); i++) {
            final Move move = moves.get(i);
            final String notation = move.toString() + checkSuffix(board, moves, i);

            if (move.getMovedPiece().getPieceAlliance().isWhite()) {
                current = new MoveRow(moveNum++);
                current.white = notation;
                rows.add(current);
                lastRowIdx = rows.size() - 1;
                lastHalf = 0;
            } else {
                if (current == null) {           // black moves first (rare edge case)
                    current = new MoveRow(moveNum++);
                    rows.add(current);
                }
                current.black = notation;
                lastRowIdx = rows.size() - 1;
                lastHalf = 1;
                current = null;
            }
        }

        listPanel.setPreferredHeight(rows.size() * ROW_HEIGHT);
        listPanel.revalidate();
        listPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            final JScrollBar vsb = scrollPane.getVerticalScrollBar();
            vsb.setValue(vsb.getMaximum());
        });
    }

    // ── Inner data class ──────────────────────────────────────────────────

    private static class MoveRow {
        final int number;
        String white = "";
        String black = "";

        MoveRow(final int n) {
            this.number = n;
        }
    }

    // ── Custom-painted list panel ─────────────────────────────────────────

    private class MoveListPanel extends JPanel {

        private int preferredHeight = 0;

        MoveListPanel() {
            setBackground(BG);
            setOpaque(true);
        }

        void setPreferredHeight(final int h) {
            this.preferredHeight = h;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(PREFERRED_SIZE.width, Math.max(preferredHeight, PREFERRED_SIZE.height));
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            final int w = getWidth();
            final int colWhite = NUM_COL_WIDTH;
            final int colBlack = NUM_COL_WIDTH + (w - NUM_COL_WIDTH) / 2;

            for (int i = 0; i < rows.size(); i++) {
                final MoveRow row = rows.get(i);
                final int y = i * ROW_HEIGHT;

                // ── Row background (alternating) ──────────────────────
                g2.setColor(i % 2 == 0 ? BG : ROW_ALT);
                g2.fillRect(0, y, w, ROW_HEIGHT);

                // ── Move number ───────────────────────────────────────
                g2.setFont(NUM_FONT);
                g2.setColor(NUM_FG);
                drawCentred(g2, row.number + ".", 0, y, NUM_COL_WIDTH);

                // ── White move ────────────────────────────────────────
                drawMoveCell(g2, row.white, colWhite, y, colBlack - colWhite,
                        i == lastRowIdx && lastHalf == 0);

                // ── Black move ────────────────────────────────────────
                drawMoveCell(g2, row.black, colBlack, y, w - colBlack,
                        i == lastRowIdx && lastHalf == 1);
            }

            g2.dispose();
        }

        private void drawMoveCell(final Graphics2D g2, final String text,
                                  final int x, final int y, final int cellW,
                                  final boolean isLast) {
            if (text == null || text.isEmpty()) return;

            if (isLast) {
                // Draw rounded pill background
                final FontMetrics fm = g2.getFontMetrics(LAST_MOVE_FONT);
                final int textW = fm.stringWidth(text);
                final int pillW = textW + CELL_PAD_H * 2;
                final int pillH = ROW_HEIGHT - 4;
                final int pillX = x + CELL_PAD_H;
                final int pillY = y + 2;
                g2.setColor(LAST_PILL_BG);
                g2.fillRoundRect(pillX, pillY, pillW, pillH, PILL_ARC, PILL_ARC);

                g2.setFont(LAST_MOVE_FONT);
                g2.setColor(LAST_PILL_FG);
                final int baseline = pillY + (pillH - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, pillX + CELL_PAD_H, baseline);
            } else {
                g2.setFont(MOVE_FONT);
                g2.setColor(MOVE_FG);
                final FontMetrics fm = g2.getFontMetrics();
                final int baseline = y + (ROW_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, x + CELL_PAD_H, baseline);
            }
        }

        /**
         * Centre text horizontally within a column.
         */
        private void drawCentred(final Graphics2D g2, final String text,
                                 final int x, final int y, final int colW) {
            final FontMetrics fm = g2.getFontMetrics();
            final int textW = fm.stringWidth(text);
            final int baseline = y + (ROW_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, x + (colW - textW) / 2, baseline);
        }
    }
}
