package com.project.gui;

import com.project.chess.Board;
import com.project.chess.Tile;
import com.project.chess.Piece;

import com.project.chess.pieces.Bishop;
import com.project.chess.pieces.King;
import com.project.chess.pieces.Knight;
import com.project.chess.pieces.Pawn;
import com.project.chess.pieces.Queen;
import com.project.chess.pieces.Rook;
import com.google.common.collect.ImmutableMap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Table {
    private final JFrame gameFrame;
    private BoardPanel boardPanel;
    private final Board chessBoard;

    private static final int BOARD_SIZE = 8;
    private static final int TILE_SIZE = 75;

    private final JPanel glassPane = new JPanel(null);
    private JLabel draggedPieceLabel;
    private Point dragOffset;
    private Piece draggedPiece;

    public Table() {
        this.gameFrame = new JFrame("Chess");
        this.gameFrame.setLayout(new BorderLayout());
        this.chessBoard = new Board();

        setupMenu();

        // Initialize board panel with white at bottom by default
        this.boardPanel = new BoardPanel(true);
        this.gameFrame.add(this.boardPanel, BorderLayout.CENTER);
        this.gameFrame.setSize(BOARD_SIZE * TILE_SIZE + 50, BOARD_SIZE * TILE_SIZE + 50);  // Make the frame larger
        this.gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set up glass pane for dragging
        glassPane.setVisible(false);
        glassPane.setOpaque(false);
        gameFrame.setGlassPane(glassPane);
        glassPane.setVisible(true);

        this.gameFrame.setVisible(true);

        chessBoard.setupBoard(true);  // Default to white at bottom
        boardPanel.refreshBoard();
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu optionsMenu = new JMenu("Options");

        JMenuItem startAsWhite = new JMenuItem("Start as White");
        startAsWhite.addActionListener(e -> {
            chessBoard.setupBoard(true);
            refreshBoardPanel(true);
        });

        JMenuItem startAsBlack = new JMenuItem("Start as Black");
        startAsBlack.addActionListener(e -> {
            chessBoard.setupBoard(false);
            refreshBoardPanel(false);
        });

        optionsMenu.add(startAsWhite);
        optionsMenu.add(startAsBlack);
        menuBar.add(optionsMenu);

        gameFrame.setJMenuBar(menuBar);
    }

    private void refreshBoardPanel(boolean whiteAtBottom) {
        this.gameFrame.remove(this.boardPanel);
        this.boardPanel = new BoardPanel(whiteAtBottom);
        this.gameFrame.add(this.boardPanel, BorderLayout.CENTER);
        boardPanel.refreshBoard();
        this.gameFrame.validate();
    }

    private class BoardPanel extends JPanel {
        final TilePanel[][] boardTiles;
        private final boolean whiteAtBottom;

        BoardPanel(boolean whiteAtBottom) {
            super(new GridLayout(BOARD_SIZE, BOARD_SIZE));
            this.whiteAtBottom = whiteAtBottom;
            this.boardTiles = new TilePanel[BOARD_SIZE][BOARD_SIZE];
            initializeBoard();
        }

        private void initializeBoard() {
            removeAll();
            for (int row = 0; row < BOARD_SIZE; row++) {
                for (int col = 0; col < BOARD_SIZE; col++) {
                    TilePanel tilePanel = new TilePanel(row, col, whiteAtBottom);
                    this.boardTiles[row][col] = tilePanel;
                    add(tilePanel);
                }
            }
            validate();
            repaint();
        }

        public void refreshBoard() {
            ImmutableMap<Tile, Piece> boardState = chessBoard.getBoardState();
            for (int row = 0; row < BOARD_SIZE; row++) {
                for (int col = 0; col < BOARD_SIZE; col++) {
                    Tile tile = chessBoard.getTile(row, col);
                    Piece piece = boardState.get(tile);
                    boardTiles[row][col].assignTilePiece(piece);
                }
            }
            revalidate();
            repaint();
        }

        private class TilePanel extends JPanel {
            private final int row;
            private final int col;
            private final boolean whiteAtBottom;

            TilePanel(int row, int col, boolean whiteAtBottom) {
                super(new GridBagLayout());
                this.row = row;
                this.col = col;
                this.whiteAtBottom = whiteAtBottom;
                setPreferredSize(new Dimension(TILE_SIZE, TILE_SIZE));
                assignTileColor();
                assignTilePiece();
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        handleMousePress(e);
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        handleMouseRelease(e);
                    }
                });
                addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        handleMouseDrag(e);
                    }
                });
                validate();
            }

            private void assignTileColor() {
                boolean isWhiteTile;
                if (whiteAtBottom) {
                    isWhiteTile = (row + col) % 2 == 0;
                } else {
                    isWhiteTile = (row + col) % 2 != 0;
                }
                setBackground(isWhiteTile ? Color.WHITE : Color.GRAY);
            }

            private void assignTilePiece() {
                removeAll(); // Clear the panel first
                Tile tile = chessBoard.getTile(row, col);
                Piece piece = tile.getPiece();
                if (piece != null) {
                    try {
                        BufferedImage pieceImage = ImageIO.read(getClass().getResourceAsStream("/images/" + getPieceImageName(piece)));
                        Image scaledImage = pieceImage.getScaledInstance(TILE_SIZE, TILE_SIZE, Image.SCALE_SMOOTH);
                        add(new JLabel(new ImageIcon(scaledImage)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                validate();
            }

            private void assignTilePiece(Piece piece) {
                removeAll(); // Clear the panel first
                if (piece != null) {
                    try {
                        BufferedImage pieceImage = ImageIO.read(getClass().getResourceAsStream("/images/" + getPieceImageName(piece)));
                        Image scaledImage = pieceImage.getScaledInstance(TILE_SIZE, TILE_SIZE, Image.SCALE_SMOOTH);
                        add(new JLabel(new ImageIcon(scaledImage)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                validate();
            }

            private String getPieceImageName(Piece piece) {
                String pieceColor = piece.isWhite() ? "White" : "Black";
                String pieceName = "";
                if (piece instanceof King) {
                    pieceName = "King";
                } else if (piece instanceof Queen) {
                    pieceName = "Queen";
                } else if (piece instanceof Rook) {
                    pieceName = "Rook";
                } else if (piece instanceof Bishop) {
                    pieceName = "Bishop";
                } else if (piece instanceof Knight) {
                    pieceName = "Knight";
                } else if (piece instanceof Pawn) {
                    pieceName = "Pawn";
                }
                return pieceColor + "_" + pieceName + ".png";
            }

            private void handleMousePress(MouseEvent e) {
                Tile tile = chessBoard.getTile(row, col);
                if (tile.isOccupied()) {
                    draggedPiece = tile.getPiece();
                    dragOffset = e.getPoint();

                    // Create a visual representation of the dragged piece
                    try {
                        BufferedImage pieceImage = ImageIO.read(getClass().getResourceAsStream("/images/" + getPieceImageName(draggedPiece)));
                        Image scaledImage = pieceImage.getScaledInstance(TILE_SIZE, TILE_SIZE, Image.SCALE_SMOOTH);
                        draggedPieceLabel = new JLabel(new ImageIcon(scaledImage));
                        draggedPieceLabel.setSize(TILE_SIZE, TILE_SIZE);
                        glassPane.add(draggedPieceLabel);
                        Point glassPanePoint = SwingUtilities.convertPoint(this, e.getPoint(), glassPane);
                        draggedPieceLabel.setLocation(glassPanePoint);
                        glassPane.repaint();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    tile.setPiece(null);
                    refreshBoard();
                }
            }

            private void handleMouseRelease(MouseEvent e) {
                if (draggedPiece != null) {
                    Point glassPanePoint = SwingUtilities.convertPoint(this, e.getPoint(), glassPane);
                    int destRow = glassPanePoint.y / TILE_SIZE;
                    int destCol = glassPanePoint.x / TILE_SIZE;
                    if (destRow >= 0 && destRow < BOARD_SIZE && destCol >= 0 && destCol < BOARD_SIZE) {
                        chessBoard.makeMove(row, col, destRow, destCol);
                    } else {
                        chessBoard.getTile(row, col).setPiece(draggedPiece); // revert move
                    }
                    draggedPiece = null;
                    glassPane.remove(draggedPieceLabel);
                    draggedPieceLabel = null;
                    refreshBoard();
                    glassPane.repaint();
                }
            }

            private void handleMouseDrag(MouseEvent e) {
                if (draggedPieceLabel != null) {
                    Point glassPanePoint = SwingUtilities.convertPoint(this, e.getPoint(), glassPane);
                    glassPanePoint.translate(-dragOffset.x, -dragOffset.y);
                    draggedPieceLabel.setLocation(glassPanePoint);
                    glassPane.repaint();
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Table::new);
    }
}