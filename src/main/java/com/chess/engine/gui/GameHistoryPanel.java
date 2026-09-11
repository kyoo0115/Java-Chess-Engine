package com.chess.engine.gui;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.MoveLog;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GameHistoryPanel extends JPanel {

    private static final Dimension HISTORY_PANEL_DIMENSION = new Dimension(140, 400);
    private static final Color SIDEBAR_BG   = new Color(30, 30, 30);
    private static final Color ROW_ODD      = new Color(38, 38, 38);
    private static final Color ROW_EVEN     = new Color(45, 45, 45);
    private static final Color TEXT_COLOR   = new Color(200, 200, 200);
    private static final Color HEADER_BG    = new Color(25, 25, 25);
    private static final Color HEADER_FG    = new Color(140, 140, 140);
    private static final Color SEL_BG       = new Color(60, 90, 60);

    private final DataModel model;
    private final JScrollPane scrollPane;

    GameHistoryPanel() {
        this.setLayout(new BorderLayout());
        this.model = new DataModel();

        final JTable table = new JTable(model);
        table.setRowHeight(18);
        table.setBackground(ROW_ODD);
        table.setForeground(TEXT_COLOR);
        table.setGridColor(new Color(55, 55, 55));
        table.setSelectionBackground(SEL_BG);
        table.setSelectionForeground(TEXT_COLOR);
        table.setFont(new Font("Monospaced", Font.PLAIN, 12));
        table.setShowVerticalLines(false);

        // Alternating row renderer
        final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(final JTable t, final Object value,
                    final boolean isSelected, final boolean hasFocus, final int row, final int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                    setForeground(TEXT_COLOR);
                }
                setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
                return this;
            }
        };
        table.setDefaultRenderer(String.class, renderer);

        // Header styling
        final JTableHeader header = table.getTableHeader();
        header.setBackground(HEADER_BG);
        header.setForeground(HEADER_FG);
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setReorderingAllowed(false);

        this.scrollPane = new JScrollPane(table);
        scrollPane.setColumnHeaderView(header);
        scrollPane.setPreferredSize(HISTORY_PANEL_DIMENSION);
        scrollPane.getViewport().setBackground(ROW_ODD);
        scrollPane.setBackground(SIDEBAR_BG);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.add(scrollPane, BorderLayout.CENTER);
        this.setBackground(SIDEBAR_BG);
        this.setVisible(true);
    }

    void redo(final Board board, final MoveLog moveHistory) {
        int currentRow = 0;
        this.model.clear();

        for (final Move move : moveHistory.getMoves()) {
            final String moveText = move.toString();

            if (move.getMovedPiece().getPieceAlliance().isWhite()) {
                this.model.setValueAt(moveText, currentRow, 0);
            } else if (move.getMovedPiece().getPieceAlliance().isBlack()) {
                this.model.setValueAt(moveText, currentRow, 1);
                currentRow++;
            }
        }

        if (!moveHistory.getMoves().isEmpty()) {
            final Move lastMove = moveHistory.getMoves().get(moveHistory.size() - 1);
            final String moveText = lastMove.toString();

            if (lastMove.getMovedPiece().getPieceAlliance().isWhite()) {
                this.model.setValueAt(moveText + calculateCheckAndCheckMateHash(board), currentRow, 0);
            } else if (lastMove.getMovedPiece().getPieceAlliance().isBlack()) {
                this.model.setValueAt(moveText + calculateCheckAndCheckMateHash(board), currentRow - 1, 1);
            }
        }

        final JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    private String calculateCheckAndCheckMateHash(final Board board) {
        if (board.getCurrentPlayer().isCheckMate()) {
            return "#";
        } else if (board.getCurrentPlayer().isInCheck()) {
            return "+";
        }

        return "";
    }

    private static class DataModel extends DefaultTableModel {

        private static final String[] NAMES = {"White", "Black"};
        private final List<Row> values;

        DataModel() {
            this.values = new ArrayList<>();
        }

        public void clear() {
            this.values.clear();
            setRowCount(0);
        }

        @Override
        public int getRowCount() {
            if (this.values == null) {
                return 0;
            }

            return this.values.size();
        }

        @Override
        public int getColumnCount() {
            return NAMES.length;
        }

        @Override
        public Object getValueAt(final int row, final int column) {
            final Row currentRow = this.values.get(row);

            if (column == 0) {
                return currentRow.getWhiteMove();
            } else if (column == 1) {
                return currentRow.getBlackMove();
            }

            return null;
        }

        @Override
        public void setValueAt(final Object aValue, final int row, final int column) {
            final Row currentRow;
            if (this.values.size() <= row) {
                currentRow = new Row();
                this.values.add(currentRow);
            } else {
                currentRow = this.values.get(row);
            }

            if (column == 0) {
                currentRow.setWhiteMove((String) aValue);
                fireTableRowsInserted(row, row);
            } else if (column == 1) {
                currentRow.setBlackMove((String) aValue);
                fireTableCellUpdated(row, column);
            }
        }

        @Override
        public Class<?> getColumnClass(final int column) {
            return String.class;
        }

        @Override
        public String getColumnName(final int column) {
            return NAMES[column];
        }
    }

    private static class Row {

        private String whiteMove;
        private String blackMove;

        Row() {

        }

        public String getWhiteMove() {
            return this.whiteMove;
        }

        public void setWhiteMove(final String move) {
            this.whiteMove = move;
        }

        public String getBlackMove() {
            return this.blackMove;
        }

        public void setBlackMove(final String move) {
            this.blackMove = move;
        }
    }
}
