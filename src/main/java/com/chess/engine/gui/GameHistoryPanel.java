package com.chess.engine.gui;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.MoveLog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GameHistoryPanel extends JPanel {

    private static final int ROW_HEIGHT = 28;
    private static final int NUM_COL_WIDTH = 32;
    private static final int CELL_PAD_H = 8;
    private static final int PILL_ARC = 8;

    private static final Font NUM_FONT = new Font("SansSerif", Font.BOLD, 12);
    private static final Font MOVE_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font LAST_MOVE_FONT = new Font("SansSerif", Font.BOLD, 13);

    private final List<MoveRow> rows = new ArrayList<>();
    private final MoveListPanel listPanel;
    private final JScrollPane scrollPane;
    // Toggle options
    private final ToggleSwitch legalMovesSwitch;
    private final ToggleSwitch lastMoveSwitch;
    private final ToggleSwitch flipBoardSwitch;
    private int lastRowIdx = -1;
    private int lastHalf = -1; // 0 = white cell, 1 = black cell
    // Controls bar callbacks
    private Runnable onFirstMove;
    private Runnable onPrevMove;
    private Runnable onPlayPause;
    private Runnable onNextMove;
    private Runnable onLastMove;

    public GameHistoryPanel(
            Runnable onFirstMove,
            Runnable onPrevMove,
            Runnable onPlayPause,
            Runnable onNextMove,
            Runnable onLastMove,
            boolean initLegalMoves,
            boolean initLastMove,
            boolean initFlipBoard,
            java.util.function.Consumer<Boolean> onToggleLegalMoves,
            java.util.function.Consumer<Boolean> onToggleLastMove,
            java.util.function.Consumer<Boolean> onToggleFlipBoard
    ) {
        super(new BorderLayout(0, 10));
        this.onFirstMove = onFirstMove;
        this.onPrevMove = onPrevMove;
        this.onPlayPause = onPlayPause;
        this.onNextMove = onNextMove;
        this.onLastMove = onLastMove;

        setOpaque(false);

        // 1. Move History Card (Center)
        JPanel historyCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.getCardBg());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(UITheme.getCardBorder());
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        historyCard.setOpaque(false);
        historyCard.setBorder(new EmptyBorder(8, 8, 8, 8));

        listPanel = new MoveListPanel();
        scrollPane = new JScrollPane(listPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);

        historyCard.add(scrollPane, BorderLayout.CENTER);
        add(historyCard, BorderLayout.CENTER);

        // 2. Bottom Controls & Toggles Card
        JPanel bottomCard = new JPanel(new BorderLayout(0, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.getCardBg());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(UITheme.getCardBorder());
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bottomCard.setOpaque(false);
        bottomCard.setBorder(new EmptyBorder(12, 14, 14, 14));

        // Playback Buttons Row: [ |< ] [ < ] [ ▶ ] [ > ] [ >| ]
        JPanel playbackRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        playbackRow.setOpaque(false);

        JButton btnFirst = makeNavButton("|<", onFirstMove);
        JButton btnPrev = makeNavButton("<", onPrevMove);
        JButton btnPlay = makePlayButton(onPlayPause);
        JButton btnNext = makeNavButton(">", onNextMove);
        JButton btnLast = makeNavButton(">|", onLastMove);

        playbackRow.add(btnFirst);
        playbackRow.add(btnPrev);
        playbackRow.add(btnPlay);
        playbackRow.add(btnNext);
        playbackRow.add(btnLast);

        bottomCard.add(playbackRow, BorderLayout.NORTH);

        // Toggle rows
        JPanel togglesPanel = new JPanel(new GridLayout(3, 1, 0, 8));
        togglesPanel.setOpaque(false);

        legalMovesSwitch = new ToggleSwitch(initLegalMoves, onToggleLegalMoves);
        lastMoveSwitch = new ToggleSwitch(initLastMove, onToggleLastMove);
        flipBoardSwitch = new ToggleSwitch(initFlipBoard, onToggleFlipBoard);

        togglesPanel.add(makeToggleRow(ChessIcons.getGridIcon(16, null), "Show legal moves", legalMovesSwitch));
        togglesPanel.add(makeToggleRow(ChessIcons.getExpandArrowIcon(16, null), "Highlight last move", lastMoveSwitch));
        togglesPanel.add(makeToggleRow(ChessIcons.getSettingsIcon(16, null), "Flip board", flipBoardSwitch));

        bottomCard.add(togglesPanel, BorderLayout.CENTER);

        add(bottomCard, BorderLayout.SOUTH);

        UITheme.addThemeListener(this::repaint);
    }

    private static String checkSuffix(final Board board, final List<Move> moves, final int idx) {
        if (idx != moves.size() - 1) return "";
        if (board.getCurrentPlayer().isCheckMate()) return "#";
        if (board.getCurrentPlayer().isInCheck()) return "+";
        return "";
    }

    private JButton makeNavButton(String text, Runnable action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? UITheme.getAccentActiveBg() : UITheme.getControlBtnBg());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(UITheme.getControlBtnBorder());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setPreferredSize(new Dimension(38, 34));
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(UITheme.getTextSecondary());
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (action != null) action.run();
        });
        return btn;
    }

    private JButton makePlayButton(Runnable action) {
        JButton btn = new JButton(ChessIcons.getPlayIcon(16, Color.WHITE)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? UITheme.getAccentHover() : UITheme.getAccentBlue());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setPreferredSize(new Dimension(46, 36));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (action != null) action.run();
        });
        return btn;
    }

    private JPanel makeToggleRow(Icon icon, String labelText, ToggleSwitch toggleSwitch) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel iconLabel = new JLabel(icon);
        JLabel textLabel = new JLabel(labelText);
        textLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        textLabel.setForeground(UITheme.getTextPrimary());

        UITheme.addThemeListener(() -> {
            textLabel.setForeground(UITheme.getTextPrimary());
        });

        left.add(iconLabel);
        left.add(textLabel);

        row.add(left, BorderLayout.CENTER);
        row.add(toggleSwitch, BorderLayout.EAST);
        return row;
    }

    public void setFlipBoardState(boolean flipped) {
        flipBoardSwitch.setSelected(flipped);
    }

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
                if (current == null) {
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

    private static class MoveRow {
        final int number;
        String white = "";
        String black = "";

        MoveRow(final int n) {
            this.number = n;
        }
    }

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
            return new Dimension(220, Math.max(preferredHeight, 180));
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

                if (i % 2 == 1) {
                    g2.setColor(UITheme.getRowAlt());
                    g2.fillRoundRect(2, y + 1, w - 4, ROW_HEIGHT - 2, 6, 6);
                }

                // Number
                g2.setFont(NUM_FONT);
                g2.setColor(UITheme.getTextMuted());
                drawCentred(g2, row.number + ".", 0, y, NUM_COL_WIDTH);

                // White move
                drawMoveCell(g2, row.white, colWhite, y, colBlack - colWhite,
                        i == lastRowIdx && lastHalf == 0);

                // Black move
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
                // Blue pill highlighting the last played move
                final FontMetrics fm = g2.getFontMetrics(LAST_MOVE_FONT);
                final int textW = fm.stringWidth(text);
                final int pillW = Math.max(textW + CELL_PAD_H * 2, 48);
                final int pillH = ROW_HEIGHT - 6;
                final int pillX = x + CELL_PAD_H / 2;
                final int pillY = y + 3;

                g2.setColor(UITheme.getAccentBlue());
                g2.fillRoundRect(pillX, pillY, pillW, pillH, PILL_ARC, PILL_ARC);

                g2.setFont(LAST_MOVE_FONT);
                g2.setColor(Color.WHITE);
                final int baseline = pillY + (pillH - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, pillX + (pillW - textW) / 2, baseline);
            } else {
                g2.setFont(MOVE_FONT);
                g2.setColor(UITheme.getTextPrimary());
                final FontMetrics fm = g2.getFontMetrics();
                final int baseline = y + (ROW_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, x + CELL_PAD_H, baseline);
            }
        }

        private void drawCentred(final Graphics2D g2, final String text,
                                 final int x, final int y, final int cellW) {
            final FontMetrics fm = g2.getFontMetrics();
            final int textW = fm.stringWidth(text);
            final int cx = x + (cellW - textW) / 2;
            final int baseline = y + (ROW_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, cx, baseline);
        }
    }
}
