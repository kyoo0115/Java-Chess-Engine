package com.chess.engine.gui;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.pieces.*;
import com.chess.engine.util.BoardUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Modal dialog that lets the user place / remove pieces to build a custom position.
 *
 * <p>Left-click palette  → select piece to place
 * <br>Left-click board tile → place selected piece (or erase if eraser is active)
 * <br>Right-click board tile → erase piece on that tile
 * <br>"Done" → builds a {@link Board} via {@link Board.Builder} and returns it via {@link #getResultBoard()}.
 */
class BoardEditorDialog extends JDialog {

    // Palette entry: null key means "eraser"
    private static final String ERASER_KEY = "__ERASE__";

    private final Map<String, BufferedImage> scaledCache;
    private final Map<String, BufferedImage> rawCache;
    private final BoardDirection boardDirection;

    // 64-element array: null = empty; otherwise the piece image key ("WQ", "BK", …)
    private final String[] tileKeys = new String[BoardUtils.NUM_TILES];

    private String selectedPaletteKey = null;   // null = eraser
    private Alliance sideToMove = Alliance.WHITE;
    private Board resultBoard = null;

    // UI
    private EditorBoardPanel boardPanel;
    private JLabel selectionLabel;

    BoardEditorDialog(final JFrame owner,
                      final Board startBoard,
                      final Map<String, BufferedImage> scaledCache,
                      final Map<String, BufferedImage> rawCache,
                      final BoardDirection boardDirection) {
        super(owner, "Edit Position", true);
        this.scaledCache = scaledCache;
        this.rawCache = rawCache;
        this.boardDirection = boardDirection;

        // Seed tileKeys from startBoard
        for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
            final var tile = startBoard.getTile(i);
            if (tile.isTileOccupied()) {
                final Piece p = tile.getPiece();
                final String key = allianceChar(p.getPieceAlliance()) + p.toString();
                tileKeys[i] = key;
            }
        }
        sideToMove = startBoard.getCurrentPlayer().getAlliance();

        buildUI();
        pack();
        setMinimumSize(new Dimension(560, 480));
        setResizable(true);
        setLocationRelativeTo(owner);
    }

    /** Returns the built board after "Done", or null if the user cancelled. */
    Board getResultBoard() {
        return resultBoard;
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        setLayout(new BorderLayout(6, 6));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        boardPanel = new EditorBoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        // ── Right sidebar: palette + options ─────────────────────────────
        final JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        sidebar.add(buildPaletteRow("White", Alliance.WHITE));
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(buildPaletteRow("Black", Alliance.BLACK));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(buildEraserButton());
        sidebar.add(Box.createVerticalStrut(10));

        selectionLabel = new JLabel("Selected: Eraser");
        selectionLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        selectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(selectionLabel);
        sidebar.add(Box.createVerticalStrut(14));

        sidebar.add(buildSideToMovePanel());
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(buildClearButton());
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(buildButtonRow());

        add(sidebar, BorderLayout.EAST);
    }

    private JPanel buildPaletteRow(final String label, final Alliance alliance) {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(3));

        final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        final char ac = allianceChar(alliance);
        for (final String suffix : new String[]{"K", "Q", "R", "B", "N", "P"}) {
            final String key = ac + suffix;
            final JButton btn = buildPieceButton(key);
            row.add(btn);
        }
        panel.add(row);
        return panel;
    }

    private JButton buildPieceButton(final String key) {
        final BufferedImage img = scaledCache.getOrDefault(key, rawCache.get(key));
        final JButton btn;
        if (img != null) {
            final Image scaled = img.getScaledInstance(36, 36, Image.SCALE_SMOOTH);
            btn = new JButton(new ImageIcon(scaled));
            btn.setPreferredSize(new Dimension(42, 42));
            btn.setToolTipText(key);
        } else {
            btn = new JButton(key);
            btn.setPreferredSize(new Dimension(42, 42));
        }
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(1, 1, 1, 1));
        btn.addActionListener(e -> selectPaletteKey(key));
        return btn;
    }

    private JButton buildEraserButton() {
        final JButton btn = new JButton("✕ Eraser");
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(e -> selectPaletteKey(ERASER_KEY));
        return btn;
    }

    private JPanel buildSideToMovePanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel lbl = new JLabel("Side to move:");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);

        final ButtonGroup bg = new ButtonGroup();
        final JRadioButton white = new JRadioButton("White", sideToMove == Alliance.WHITE);
        final JRadioButton black = new JRadioButton("Black", sideToMove == Alliance.BLACK);
        white.setFont(new Font("SansSerif", Font.PLAIN, 12));
        black.setFont(new Font("SansSerif", Font.PLAIN, 12));
        white.addActionListener(e -> sideToMove = Alliance.WHITE);
        black.addActionListener(e -> sideToMove = Alliance.BLACK);
        bg.add(white);
        bg.add(black);
        panel.add(white);
        panel.add(black);
        return panel;
    }

    private JButton buildClearButton() {
        final JButton btn = new JButton("Clear Board");
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(e -> {
            java.util.Arrays.fill(tileKeys, null);
            boardPanel.repaint();
        });
        return btn;
    }

    private JPanel buildButtonRow() {
        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        final JButton done = new JButton("Done");
        done.setFont(new Font("SansSerif", Font.BOLD, 12));
        done.addActionListener(e -> {
            final Board built = tryBuildBoard();
            if (built == null) return;   // validation error already shown
            resultBoard = built;
            dispose();
        });

        panel.add(cancel);
        panel.add(done);
        return panel;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void selectPaletteKey(final String key) {
        selectedPaletteKey = ERASER_KEY.equals(key) ? null : key;
        final String name = (selectedPaletteKey == null) ? "Eraser" : selectedPaletteKey;
        selectionLabel.setText("Selected: " + name);
    }

    private static char allianceChar(final Alliance a) {
        return a.isWhite() ? 'W' : 'B';
    }

    /**
     * Builds the Board from the current tileKeys array.
     * Returns null and shows an error dialog if both kings are not present.
     */
    private Board tryBuildBoard() {
        boolean hasWhiteKing = false, hasBlackKing = false;
        for (final String k : tileKeys) {
            if ("WK".equals(k)) hasWhiteKing = true;
            if ("BK".equals(k)) hasBlackKing = true;
        }
        if (!hasWhiteKing || !hasBlackKing) {
            JOptionPane.showMessageDialog(this,
                    "Both kings must be present on the board.",
                    "Invalid Position", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        final Board.Builder builder = new Board.Builder();
        builder.setMoveMaker(sideToMove);

        for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
            final String key = tileKeys[i];
            if (key == null) continue;
            final Alliance a = key.charAt(0) == 'W' ? Alliance.WHITE : Alliance.BLACK;
            final Piece piece = switch (key.charAt(1)) {
                case 'K' -> new King(i, a);
                case 'Q' -> new Queen(i, a);
                case 'R' -> new Rook(i, a);
                case 'B' -> new Bishop(i, a);
                case 'N' -> new Knight(i, a);
                case 'P' -> new Pawn(i, a);
                default  -> null;
            };
            if (piece != null) builder.setPiece(piece);
        }
        return builder.build();
    }

    // ── Inner board panel ──────────────────────────────────────────────────────

    private class EditorBoardPanel extends JPanel {

        EditorBoardPanel() {
            setPreferredSize(new Dimension(400, 400));
            setMinimumSize(new Dimension(240, 240));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    final int tileId = tileIdAt(e.getPoint());
                    if (tileId < 0) return;

                    if (SwingUtilities.isRightMouseButton(e)) {
                        // Right-click always erases
                        tileKeys[tileId] = null;
                        repaint();
                        return;
                    }
                    if (!SwingUtilities.isLeftMouseButton(e)) return;

                    if (selectedPaletteKey == null) {
                        // Eraser selected
                        tileKeys[tileId] = null;
                    } else {
                        tileKeys[tileId] = selectedPaletteKey;
                    }
                    repaint();
                }
            });
        }

        private int tileIdAt(final Point p) {
            if (getWidth() == 0 || getHeight() == 0) return -1;
            final int tw = getWidth() / 8;
            final int th = getHeight() / 8;
            int col = p.x / tw;
            int row = p.y / th;
            col = Math.max(0, Math.min(7, col));
            row = Math.max(0, Math.min(7, row));
            if (boardDirection == BoardDirection.FLIPPED) {
                col = 7 - col;
                row = 7 - row;
            }
            return row * 8 + col;
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            final int w = getWidth();
            final int h = getHeight();
            final int tw = w / 8;
            final int th = h / 8;

            for (int tileId = 0; tileId < BoardUtils.NUM_TILES; tileId++) {
                final int dispId = (boardDirection == BoardDirection.FLIPPED) ? (63 - tileId) : tileId;
                final int col = dispId % 8;
                final int row = dispId / 8;
                final int x = col * tw;
                final int y = row * th;

                // Tile colour
                final boolean light = isLightTile(tileId);
                g2.setColor(light ? new Color(240, 217, 181) : new Color(181, 136, 99));
                g2.fillRect(x, y, tw, th);

                // Piece image
                final String key = tileKeys[tileId];
                if (key != null) {
                    final BufferedImage img = scaledCache.getOrDefault(key, rawCache.get(key));
                    if (img != null) {
                        g2.drawImage(img, x, y, tw, th, this);
                    }
                }

                // Grid border
                g2.setColor(new Color(0, 0, 0, 30));
                g2.drawRect(x, y, tw - 1, th - 1);
            }

            g2.dispose();
        }

        private boolean isLightTile(final int tileId) {
            final int row = tileId / 8;
            final int col = tileId % 8;
            return (row + col) % 2 == 0;
        }
    }
}
