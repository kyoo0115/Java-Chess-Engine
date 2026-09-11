package com.chess.engine.gui;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.Tile;
import com.chess.engine.pieces.*;
import com.chess.engine.player.MoveTransition;
import com.chess.engine.player.ai.Minimax;
import com.chess.engine.util.BoardUtils;
import com.google.common.collect.Lists;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;

public class Table {

    // ── Constants ─────────────────────────────────────────────────────
    private static final Dimension OUTER_FRAME_DIMENSION = new Dimension(880, 680);
    private static final Dimension TILE_PANEL_DIMENSION = new Dimension(10, 10);
    private static final String PIECE_ICON_PATH = "images/";
    // ── Static raw image cache (loaded once) ──────────────────────────
    private static final Map<String, BufferedImage> RAW_IMAGE_CACHE = loadRawCache();
    // ── Persistent preferences ────────────────────────────────────────
    private static final Preferences PREFS = Preferences.userNodeForPackage(Table.class);
    // ── Coordinate label font (drawn on board edge) ───────────────────
    private static final Font COORD_FONT = new Font("SansSerif", Font.BOLD, 10);
    // ── Instance fields ───────────────────────────────────────────────
    private final JFrame gameFrame;
    private final BoardPanel boardPanel;
    private final MoveLog moveLog;
    private final GameHistoryPanel gameHistoryPanel;
    private final TakenPiecesPanel takenPiecesPanel;
    private final GameSetup gameSetup;
    private final JLabel statusLabel;
    private final ClockPanel clockPanel;
    // ── Per-instance scaled caches (rebuilt on board resize) ──────────
    private Map<String, BufferedImage> SCALED_IMAGE_CACHE = new HashMap<>();
    private Map<String, BufferedImage> DRAG_IMAGE_CACHE = new HashMap<>();
    private int lastScaledTileSize = -1;
    private Board chessBoard;
    private BoardDirection boardDirection;
    private BoardTheme boardTheme = BoardTheme.CLASSIC;
    // Preferences flags
    private boolean highlightLegalMoves = false;
    private boolean hoverHighlight = false;   // show legal dots on hover (no click)
    private boolean showCoordinates = false;
    private boolean gameOver = false;
    // Selection / drag state
    private Tile sourceTile;
    private Tile destinationTile;
    private Piece humanMovedPiece;
    private BufferedImage dragImage;
    private Point dragPoint;
    private int dragSourceTileId = -1;
    private int hoverTileId = -1;   // tile the cursor is over
    // Last-move highlights
    private int lastMoveSource = -1;
    private int lastMoveDest = -1;
    // AI move arrow overlay
    private int arrowSource = -1;
    private int arrowDest = -1;
    // AI piece animation
    private BufferedImage animPiece = null;
    private float animFromX, animFromY, animToX, animToY;
    private float animProgress = 0f;
    private javax.swing.Timer animTimer = null;

    public Table() {
        gameFrame = new JFrame("JChess");
        gameFrame.setLayout(new BorderLayout());

        chessBoard = Board.createStandardBoard();
        moveLog = new MoveLog();

        gameHistoryPanel = new GameHistoryPanel();
        takenPiecesPanel = new TakenPiecesPanel();
        gameSetup = new GameSetup(gameFrame, true);

        // ── Load persisted preferences before building UI ─────────────
        boardTheme = BoardTheme.fromName(PREFS.get("boardTheme", BoardTheme.CLASSIC.name()));
        highlightLegalMoves = PREFS.getBoolean("highlightLegalMoves", false);
        hoverHighlight = PREFS.getBoolean("hoverHighlight", false);
        showCoordinates = PREFS.getBoolean("showCoordinates", false);
        SoundManager.setEnabled(PREFS.getBoolean("soundEnabled", true));
        gameSetup.setDifficulty(PREFS.getInt("difficultyIndex", 1), PREFS.getInt("customDepth", 4));

        gameFrame.setJMenuBar(createTableMenuBar());
        gameFrame.setSize(OUTER_FRAME_DIMENSION);
        gameFrame.setMinimumSize(new Dimension(640, 520));

        boardPanel = new BoardPanel();
        clockPanel = new ClockPanel();

        statusLabel = new JLabel("White to move", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        gameFrame.add(takenPiecesPanel, BorderLayout.WEST);
        gameFrame.add(boardPanel, BorderLayout.CENTER);
        gameFrame.add(gameHistoryPanel, BorderLayout.EAST);
        gameFrame.add(statusLabel, BorderLayout.SOUTH);
        gameFrame.add(clockPanel, BorderLayout.NORTH);

        boardDirection = BoardDirection.NORMAL;

        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameFrame.setLocationRelativeTo(null); // centre on screen
        gameFrame.setVisible(true);

        // Initial draw — must happen after setVisible so panel sizes are known.
        // Build scaled caches at actual tile size, then draw + optionally fire AI.
        SwingUtilities.invokeLater(() -> {
            rebuildScaledCaches();
            boardPanel.drawBoard(chessBoard);
            updateStatus();
            if (gameSetup.isAIPlayer(chessBoard.getCurrentPlayer())) fireAIThinkTank();
        });
    }

    private static Map<String, BufferedImage> loadRawCache() {
        final Map<String, BufferedImage> cache = new HashMap<>();
        for (final String a : new String[]{"W", "B"})
            for (final String s : new String[]{"K", "Q", "R", "B", "N", "P"}) {
                final String key = a + s;
                try {
                    cache.put(key, ImageIO.read(new File(PIECE_ICON_PATH + key + ".png")));
                } catch (IOException e) {
                    System.err.println("Missing piece image: " + key + ".png");
                }
            }
        return Collections.unmodifiableMap(cache);
    }

    /**
     * Rebuilds SCALED_IMAGE_CACHE and DRAG_IMAGE_CACHE at the current tile pixel size.
     * No-ops if the tile size hasn't changed since last call.
     */
    private void rebuildScaledCaches() {
        final int tw = boardPanel.getWidth() / 8;
        final int th = boardPanel.getHeight() / 8;
        final int tileSize = Math.max(16, Math.min(tw, th));
        if (tileSize == lastScaledTileSize) return;
        lastScaledTileSize = tileSize;

        final Map<String, BufferedImage> sc = new HashMap<>();
        final Map<String, BufferedImage> dc = new HashMap<>();
        for (final Map.Entry<String, BufferedImage> entry : RAW_IMAGE_CACHE.entrySet()) {
            try {
                final BufferedImage scaled = Scalr.resize(entry.getValue(),
                        Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, tileSize, tileSize, Scalr.OP_ANTIALIAS);
                sc.put(entry.getKey(), scaled);
                dc.put(entry.getKey(), scaled);
            } catch (Exception e) {
                sc.put(entry.getKey(), entry.getValue());
                dc.put(entry.getKey(), entry.getValue());
            }
        }
        SCALED_IMAGE_CACHE = Collections.unmodifiableMap(sc);
        DRAG_IMAGE_CACHE = Collections.unmodifiableMap(dc);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────

    private JMenuBar createTableMenuBar() {
        final JMenuBar bar = new JMenuBar();
        bar.add(createFileMenu());
        bar.add(createPreferencesMenu());
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Menu bar
    // ─────────────────────────────────────────────────────────────────

    private JMenu createFileMenu() {
        final JMenu menu = new JMenu("File");

        final JMenuItem newGame = new JMenuItem("New Game");
        newGame.addActionListener(e -> resetGame());
        menu.add(newGame);

        final JMenuItem undo = new JMenuItem("Undo");
        undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        undo.addActionListener(e -> undoLastMove());
        menu.add(undo);

        final JMenuItem setup = new JMenuItem("Game Setup...");
        setup.addActionListener(e -> {
            gameSetup.promptUser();
            setupAfterGameSetup();
        });
        menu.add(setup);

        menu.addSeparator();

        final JMenuItem save = new JMenuItem("Save Game…");
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        save.addActionListener(e -> saveGame());
        menu.add(save);

        final JMenuItem load = new JMenuItem("Load Game…");
        load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        load.addActionListener(e -> loadGame());
        menu.add(load);

        menu.addSeparator();
        final JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> System.exit(0));
        menu.add(exit);
        return menu;
    }

    private JMenu createPreferencesMenu() {
        final JMenu menu = new JMenu("Preferences");

        // ── Flip Board ──────────────────────────────────────────────
        final JMenuItem flip = new JMenuItem("Flip Board");
        flip.addActionListener(e -> {
            boardDirection = boardDirection.opposite();
            boardPanel.drawBoard(chessBoard);
        });
        menu.add(flip);
        menu.addSeparator();

        // ── Highlight Legal Moves (click) ───────────────────────────
        final JCheckBoxMenuItem hiLegal = new JCheckBoxMenuItem("Highlight Legal Moves", highlightLegalMoves);
        hiLegal.addActionListener(e -> {
            highlightLegalMoves = hiLegal.isSelected();
            PREFS.putBoolean("highlightLegalMoves", highlightLegalMoves);
            boardPanel.drawBoard(chessBoard);
        });
        menu.add(hiLegal);

        // ── Hover highlights ────────────────────────────────────────
        final JCheckBoxMenuItem hiHover = new JCheckBoxMenuItem("Highlight on Hover", hoverHighlight);
        hiHover.addActionListener(e -> {
            hoverHighlight = hiHover.isSelected();
            PREFS.putBoolean("hoverHighlight", hoverHighlight);
            boardPanel.repaint();
        });
        menu.add(hiHover);

        // ── Coordinate labels ────────────────────────────────────────
        final JCheckBoxMenuItem coords = new JCheckBoxMenuItem("Show Coordinates", showCoordinates);
        coords.addActionListener(e -> {
            showCoordinates = coords.isSelected();
            PREFS.putBoolean("showCoordinates", showCoordinates);
            boardPanel.repaint();
        });
        menu.add(coords);

        menu.addSeparator();

        // ── Board Theme submenu ──────────────────────────────────────
        final JMenu themeMenu = new JMenu("Board Theme");
        final ButtonGroup themeGroup = new ButtonGroup();
        for (final BoardTheme theme : BoardTheme.values()) {
            final JRadioButtonMenuItem item = new JRadioButtonMenuItem(theme.label, theme == boardTheme);
            themeGroup.add(item);
            item.addActionListener(e -> {
                boardTheme = theme;
                PREFS.put("boardTheme", theme.name());
                boardPanel.drawBoard(chessBoard);
            });
            themeMenu.add(item);
        }
        menu.add(themeMenu);

        menu.addSeparator();

        // ── Sound toggle ─────────────────────────────────────────────
        final JCheckBoxMenuItem sound = new JCheckBoxMenuItem("Sound Effects", SoundManager.isEnabled());
        sound.addActionListener(e -> {
            SoundManager.setEnabled(sound.isSelected());
            PREFS.putBoolean("soundEnabled", sound.isSelected());
        });
        menu.add(sound);

        return menu;
    }

    private void undoLastMove() {
        if (moveLog.size() == 0) return;

        // In Human vs Computer: undo 2 plies (AI reply + human move).
        // In Human vs Human: undo 1 ply.
        final boolean vsComputer = gameSetup.isAIPlayer(chessBoard.getCurrentPlayer())
                || gameSetup.isAIPlayer(chessBoard.getCurrentPlayer().getOpponent());
        final int movesToPop = (vsComputer && moveLog.size() >= 2) ? 2 : 1;

        for (int i = 0; i < movesToPop; i++) {
            moveLog.removeMove(moveLog.size() - 1);
        }

        // Replay from scratch
        chessBoard = Board.createStandardBoard();
        for (final Move move : moveLog.getMoves()) {
            chessBoard = chessBoard.getCurrentPlayer().makeMove(move).getTransitionBoard();
        }

        // Restore last-move highlight from new tail of log
        if (moveLog.size() > 0) {
            final Move last = moveLog.getMoves().get(moveLog.size() - 1);
            lastMoveSource = last.getCurrentCoordinate();
            lastMoveDest = last.getDestinationCoordinate();
        } else {
            lastMoveSource = -1;
            lastMoveDest = -1;
        }

        // Reset interaction state
        sourceTile = null;
        destinationTile = null;
        humanMovedPiece = null;
        dragImage = null;
        dragPoint = null;
        dragSourceTileId = -1;
        hoverTileId = -1;
        gameOver = false;

        SwingUtilities.invokeLater(() -> {
            gameHistoryPanel.redo(chessBoard, moveLog);
            takenPiecesPanel.redo(moveLog);
            boardPanel.drawBoard(chessBoard);
            updateStatus();
        });
    }

    private void saveGame() {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Game as PGN");
        fc.setSelectedFile(new File("game.pgn"));
        if (fc.showSaveDialog(gameFrame) != JFileChooser.APPROVE_OPTION) return;

        final File file = fc.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            // PGN headers
            pw.println("[Event \"JChess Game\"]");
            pw.println("[Date \"" + LocalDate.now() + "\"]");
            pw.println("[White \"" + (gameSetup.getWhitePlayerType() == Table.PlayerType.HUMAN ? "Human" : "Computer") + "\"]");
            pw.println("[Black \"" + (gameSetup.getBlackPlayerType() == Table.PlayerType.HUMAN ? "Human" : "Computer") + "\"]");
            pw.println("[Result \"*\"]");
            pw.println();

            // Move text: "1. e4 e5 2. Nf3 ..."
            final StringBuilder sb = new StringBuilder();
            final List<Move> moves = moveLog.getMoves();
            for (int i = 0; i < moves.size(); i++) {
                if (i % 2 == 0) sb.append(i / 2 + 1).append(". ");
                sb.append(moves.get(i).toString()).append(' ');
            }
            // Wrap at ~80 chars
            final String text = sb.toString().trim();
            int pos = 0;
            while (pos < text.length()) {
                int end = Math.min(pos + 80, text.length());
                if (end < text.length()) {
                    final int space = text.lastIndexOf(' ', end);
                    if (space > pos) end = space;
                }
                pw.println(text.substring(pos, end).trim());
                pos = end;
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(gameFrame, "Failed to save: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadGame() {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load PGN File");
        if (fc.showOpenDialog(gameFrame) != JFileChooser.APPROVE_OPTION) return;

        final File file = fc.getSelectedFile();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            // Collect non-header lines
            final StringBuilder moveSb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                final String trimmed = line.trim();
                if (trimmed.startsWith("[") || trimmed.isEmpty()) continue;
                moveSb.append(trimmed).append(' ');
            }

            // Strip move numbers (e.g. "1." "12."), result tokens, and annotations
            final String[] tokens = moveSb.toString().split("\\s+");
            Board replayBoard = Board.createStandardBoard();
            final List<Move> loaded = new ArrayList<>();
            for (final String token : tokens) {
                if (token.isEmpty()) continue;
                if (token.matches("\\d+\\.+")) continue;           // move numbers
                if (token.matches("\\$\\d+")) continue;            // NAG annotations
                if (token.matches("[01½][-/][01½]|\\*")) continue; // results
                // Strip trailing check/checkmate symbols for matching
                final String san = token.replaceAll("[+#!?]", "");
                // Find the legal move whose toString() (also stripped) matches
                Move matched = null;
                for (final Move m : replayBoard.getCurrentPlayer().getLegalMoves()) {
                    if (m.toString().replaceAll("[+#!?]", "").equals(san)) {
                        matched = m;
                        break;
                    }
                }
                if (matched == null) break; // unrecognised token — stop
                final MoveTransition t = replayBoard.getCurrentPlayer().makeMove(matched);
                if (!t.getMoveStatus().isDone()) break;
                replayBoard = t.getTransitionBoard();
                loaded.add(matched);
            }

            if (loaded.isEmpty()) {
                JOptionPane.showMessageDialog(gameFrame, "No valid moves found in file.",
                        "Load Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Apply loaded game
            chessBoard = replayBoard;
            moveLog.clear();
            for (final Move m : loaded) moveLog.addMove(m);
            final Move last = loaded.get(loaded.size() - 1);
            lastMoveSource = last.getCurrentCoordinate();
            lastMoveDest   = last.getDestinationCoordinate();
            arrowSource = lastMoveSource;
            arrowDest   = lastMoveDest;
            sourceTile = null; destinationTile = null; humanMovedPiece = null;
            dragImage = null; dragPoint = null; dragSourceTileId = -1;
            hoverTileId = -1; gameOver = false;
            SwingUtilities.invokeLater(() -> {
                gameHistoryPanel.redo(chessBoard, moveLog);
                takenPiecesPanel.redo(moveLog);
                boardPanel.drawBoard(chessBoard);
                updateStatus();
            });
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(gameFrame, "Failed to load: " + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetGame() {
        chessBoard = Board.createStandardBoard();
        moveLog.clear();
        sourceTile = null;
        destinationTile = null;
        humanMovedPiece = null;
        dragImage = null;
        dragPoint = null;
        dragSourceTileId = -1;
        hoverTileId = -1;
        lastMoveSource = -1;
        lastMoveDest = -1;
        arrowSource = -1;
        arrowDest = -1;
        gameOver = false;
        clockPanel.reset(gameSetup.isClockEnabled(), gameSetup.getClockMinutes());
        SwingUtilities.invokeLater(() -> {
            gameHistoryPanel.redo(chessBoard, moveLog);
            takenPiecesPanel.redo(moveLog);
            boardPanel.drawBoard(chessBoard);
            updateStatus();
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Game control
    // ─────────────────────────────────────────────────────────────────

    private void setupAfterGameSetup() {
        PREFS.putInt("difficultyIndex", gameSetup.getDifficultyIndex());
        PREFS.putInt("customDepth", gameSetup.getCustomDepth());
        clockPanel.configure(gameSetup.isClockEnabled(), gameSetup.getClockMinutes());
        SwingUtilities.invokeLater(() -> {
            if (gameSetup.isAIPlayer(chessBoard.getCurrentPlayer())) fireAIThinkTank();
        });
    }

    private void updateStatus() {
        if (chessBoard.getCurrentPlayer().isCheckMate()) {
            statusLabel.setText("Checkmate!  " + chessBoard.getCurrentPlayer().getOpponent().getAlliance() + " wins.");
        } else if (chessBoard.getCurrentPlayer().isStaleMate()) {
            statusLabel.setText("Stalemate — Draw.");
        } else if (chessBoard.getCurrentPlayer().isInCheck()) {
            statusLabel.setText(chessBoard.getCurrentPlayer().getAlliance() + " is in Check!");
        } else {
            statusLabel.setText(chessBoard.getCurrentPlayer().getAlliance() + " to move");
        }
    }

    private void fireAIThinkTank() {
        statusLabel.setText("Computer thinking…");
        gameFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new AIThinkTank().execute();
    }

    private void tryMove(final int fromId, final int toId) {
        arrowSource = -1;
        arrowDest = -1;
        Move move = Move.MoveFactory.createMove(chessBoard, fromId, toId);
        // If this is a pawn promotion and it's a human player's move, ask which piece
        if (move instanceof Move.PawnPromotion
                && !gameSetup.isAIPlayer(chessBoard.getCurrentPlayer())) {
            move = askPromotionChoice((Move.PawnPromotion) move);
        }
        final MoveTransition t = chessBoard.getCurrentPlayer().makeMove(move);
        if (!t.getMoveStatus().isDone()) return;

        if (move.isCastlingMove()) SoundManager.play(SoundManager.SoundType.CASTLE);
        else if (move.isAttack()) SoundManager.play(SoundManager.SoundType.CAPTURE);
        else SoundManager.play(SoundManager.SoundType.MOVE);

        lastMoveSource = fromId;
        lastMoveDest = toId;
        chessBoard = t.getTransitionBoard();
        moveLog.addMove(move);

        if (chessBoard.getCurrentPlayer().isInCheck()) SoundManager.play(SoundManager.SoundType.CHECK);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Move execution
    // ─────────────────────────────────────────────────────────────────

    /**
     * Shows a dialog asking the human which piece to promote to.
     * Returns a new PawnPromotion carrying the chosen piece.
     * Shows a dialog with piece-icon buttons; falls back to Queen if dismissed.
     */
    private Move askPromotionChoice(final Move.PawnPromotion original) {
        final com.chess.engine.Alliance alliance = original.getMovedPiece().getPieceAlliance();
        final int dest = original.getDestinationCoordinate();
        final char allianceChar = alliance.toString().charAt(0);

        // piece labels and keys matching SCALED_IMAGE_CACHE / RAW_IMAGE_CACHE
        final String[] labels = {"Queen", "Rook", "Bishop", "Knight"};
        final String[] keys = {allianceChar + "Q", allianceChar + "R", allianceChar + "B", allianceChar + "N"};

        final int[] chosen = {0};   // default: Queen

        final JDialog dialog = new JDialog(gameFrame, "Pawn Promotion", true);
        dialog.setLayout(new FlowLayout(FlowLayout.CENTER, 12, 12));
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            final BufferedImage img = SCALED_IMAGE_CACHE.getOrDefault(keys[i], RAW_IMAGE_CACHE.get(keys[i]));
            final JButton btn = new JButton(labels[i]);
            if (img != null) {
                // Scale icon to ~60 px so the button is large enough to click comfortably
                final int iconSize = 60;
                final Image scaled = img.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
                btn.setIcon(new ImageIcon(scaled));
                btn.setHorizontalTextPosition(SwingConstants.CENTER);
                btn.setVerticalTextPosition(SwingConstants.BOTTOM);
            }
            btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                chosen[0] = idx;
                dialog.dispose();
            });
            dialog.add(btn);
        }

        dialog.pack();
        dialog.setLocationRelativeTo(gameFrame);
        dialog.setVisible(true);   // blocks until disposed

        final Piece promotedTo = switch (chosen[0]) {
            case 1 -> new Rook(alliance, dest, false);
            case 2 -> new Bishop(alliance, dest, false);
            case 3 -> new Knight(alliance, dest, false);
            default -> new Queen(alliance, dest, false);
        };
        return new Move.PawnPromotion(original.getDecoratedMove(), promotedTo);
    }

    private void afterMoveRefresh() {
        gameHistoryPanel.redo(chessBoard, moveLog);
        takenPiecesPanel.redo(moveLog);
        boardPanel.drawBoard(chessBoard);
        updateStatus();
        checkGameOver();
        if (!gameOver) clockPanel.onMoveMade(chessBoard.getCurrentPlayer().getAlliance());
        if (!gameOver && gameSetup.isAIPlayer(chessBoard.getCurrentPlayer())) fireAIThinkTank();
    }

    private void checkGameOver() {
        if (gameOver) return;
        final boolean mate = chessBoard.getCurrentPlayer().isCheckMate();
        final boolean stale = chessBoard.getCurrentPlayer().isStaleMate();
        if (!mate && !stale) return;

        gameOver = true;
        clockPanel.stop();
        SoundManager.play(SoundManager.SoundType.GAME_END);

        final String msg = mate
                ? "Checkmate!  " + chessBoard.getCurrentPlayer().getOpponent().getAlliance() + " wins!"
                : "Stalemate!  The game is a draw.";

        final int choice = JOptionPane.showOptionDialog(gameFrame, msg, "Game Over",
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, new Object[]{"New Game", "Close"}, "New Game");
        if (choice == JOptionPane.YES_OPTION) resetGame();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Game over
    // ─────────────────────────────────────────────────────────────────

    // ── Board themes ──────────────────────────────────────────────────
    public enum BoardTheme {
        CLASSIC("Classic", "#F0D9B5", "#B58863", "#F6F669", "#CDD16E", "#AABA44"),
        GREEN("Green", "#FFFFDD", "#86A666", "#F6F669", "#B8CF67", "#8FAE36"),
        BLUE("Blue", "#DEE3E6", "#788A9B", "#EAF04E", "#A2B870", "#7D9F3A");

        final String label;
        final Color light, dark, selected, lastLight, lastDark;

        BoardTheme(String label, String light, String dark,
                   String selected, String lastLight, String lastDark) {
            this.label = label;
            this.light = Color.decode(light);
            this.dark = Color.decode(dark);
            this.selected = Color.decode(selected);
            this.lastLight = Color.decode(lastLight);
            this.lastDark = Color.decode(lastDark);
        }

        static BoardTheme fromName(final String name) {
            for (final BoardTheme t : values()) {
                if (t.name().equals(name)) return t;
            }
            return CLASSIC;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  AI worker
    // ─────────────────────────────────────────────────────────────────

    public enum BoardDirection {
        NORMAL {
            @Override
            List<TilePanel> traverse(List<TilePanel> t) {
                return t;
            }

            @Override
            BoardDirection opposite() {
                return FLIPPED;
            }
        },
        FLIPPED {
            @Override
            List<TilePanel> traverse(List<TilePanel> t) {
                return Lists.reverse(t);
            }

            @Override
            BoardDirection opposite() {
                return NORMAL;
            }
        };

        abstract List<TilePanel> traverse(List<TilePanel> boardTiles);

        abstract BoardDirection opposite();
    }

    // ─────────────────────────────────────────────────────────────────
    //  BoardDirection / MoveLog / PlayerType
    // ─────────────────────────────────────────────────────────────────

    public enum PlayerType {HUMAN, COMPUTER}

    public static class MoveLog {
        private final List<Move> moves = new ArrayList<>();

        public List<Move> getMoves() {
            return moves;
        }

        public void addMove(Move m) {
            moves.add(m);
        }

        public int size() {
            return moves.size();
        }

        public void clear() {
            moves.clear();
        }

        public void removeMove(int i) {
            moves.remove(i);
        }

        public boolean removeMove(Move m) {
            return moves.remove(m);
        }
    }

    private class AIThinkTank extends SwingWorker<Move, Void> {
        @Override
        protected Move doInBackground() {
            return new Minimax(gameSetup.getSearchDepth()).execute(chessBoard);
        }

        @Override
        protected void done() {
            gameFrame.setCursor(Cursor.getDefaultCursor());
            try {
                final Move m = get();
                if (m == null) { afterMoveRefresh(); return; }

                // ── Set up animation before committing the move ───────
                final int fromId = m.getCurrentCoordinate();
                final int toId   = m.getDestinationCoordinate();
                final Piece piece = chessBoard.getTile(fromId).getPiece();
                final String key  = String.valueOf(piece.getPieceAlliance().toString().charAt(0)) + piece;
                animPiece = SCALED_IMAGE_CACHE.getOrDefault(key, RAW_IMAGE_CACHE.get(key));

                final int tw = boardPanel.getWidth() / 8;
                final int th = boardPanel.getHeight() / 8;
                final int fromDisp = (boardDirection == BoardDirection.FLIPPED) ? (63 - fromId) : fromId;
                final int toDisp   = (boardDirection == BoardDirection.FLIPPED) ? (63 - toId)   : toId;
                animFromX = (fromDisp % 8) * tw;
                animFromY = (fromDisp / 8) * th;
                animToX   = (toDisp % 8) * tw;
                animToY   = (toDisp / 8) * th;
                animProgress = 0f;

                // Hide piece from its source tile during animation
                dragSourceTileId = fromId;
                boardPanel.drawBoard(chessBoard);

                final int FRAMES = 10;
                final float step = 1f / FRAMES;
                animTimer = new javax.swing.Timer(15, null);
                animTimer.addActionListener(ae -> {
                    animProgress = Math.min(1f, animProgress + step);
                    boardPanel.repaint();
                    if (animProgress >= 1f) {
                        animTimer.stop();
                        animPiece = null;
                        dragSourceTileId = -1;
                        tryMove(fromId, toId);
                        arrowSource = fromId;
                        arrowDest   = toId;
                        afterMoveRefresh();
                    }
                });
                animTimer.start();

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                afterMoveRefresh();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  BoardPanel — single mouse listener, ghost overlay, coord labels
    // ─────────────────────────────────────────────────────────────────

    private class BoardPanel extends JPanel {

        final List<TilePanel> boardTiles;

        BoardPanel() {
            super(new GridLayout(8, 8));
            boardTiles = new ArrayList<>();
            for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
                final TilePanel tp = new TilePanel(i);
                boardTiles.add(tp);
                add(tp);
            }
            // No setPreferredSize — BorderLayout.CENTER expands to fill all available space.
            // Rebuild scaled caches and redraw whenever the panel is resized/maximized.
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    rebuildScaledCaches();
                    drawBoard(chessBoard);
                }
            });
            installMouseListener();
            validate();
        }

        // ── Single listener for press/drag/release/hover ─────────────

        private void installMouseListener() {
            final MouseAdapter adapter = new MouseAdapter() {

                // ── Press: pick up piece, start drag ─────────────────
                @Override
                public void mousePressed(MouseEvent e) {
                    if (gameOver || !isLeftMouseButton(e)) return;
                    final int tileId = tileIdAtPoint(e.getPoint());
                    if (tileId < 0) return;
                    final Tile tile = chessBoard.getTile(tileId);
                    final Piece piece = tile.getPiece();
                    if (piece == null || piece.getPieceAlliance() != chessBoard.getCurrentPlayer().getAlliance())
                        return;

                    sourceTile = tile;
                    humanMovedPiece = piece;
                    dragSourceTileId = tileId;
                    dragPoint = e.getPoint();

                    final String key = String.valueOf(piece.getPieceAlliance().toString().charAt(0)) + piece;
                    dragImage = DRAG_IMAGE_CACHE.getOrDefault(key, RAW_IMAGE_CACHE.get(key));
                    repaint();
                }

                // ── Drag: move ghost ──────────────────────────────────
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragImage == null) return;
                    dragPoint = e.getPoint();
                    repaint();
                }

                // ── Release: drop piece ───────────────────────────────
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (dragImage == null) return;
                    final int toId = tileIdAtPoint(e.getPoint());
                    dragImage = null;
                    dragPoint = null;
                    final int fromId = dragSourceTileId;
                    dragSourceTileId = -1;

                    if (toId == fromId) {
                        repaint();
                        return;
                    } // tap — keep selection

                    sourceTile = null;
                    humanMovedPiece = null;
                    tryMove(fromId, toId);
                    if (lastMoveSource == fromId) { arrowSource = fromId; arrowDest = toId; }
                    afterMoveRefresh();
                }

                // ── Hover: update dot flags only — never call drawBoard here ─────
                @Override
                public void mouseMoved(MouseEvent e) {
                    final int tid = tileIdAtPoint(e.getPoint());
                    if (tid == hoverTileId) return;
                    hoverTileId = tid;

                    // Only update humanMovedPiece when no piece is already selected (click-mode)
                    if (sourceTile == null) {
                        final Piece p = (tid >= 0) ? chessBoard.getTile(tid).getPiece() : null;
                        humanMovedPiece = (hoverHighlight && p != null
                                && p.getPieceAlliance() == chessBoard.getCurrentPlayer().getAlliance())
                                ? p : null;
                    }

                    // Refresh only the legal-move dot flags on each tile, then repaint.
                    // drawBoard() is NOT called here — it removes/re-adds all tile components
                    // which causes flicker on every mouse movement.
                    for (final TilePanel tp : boardTiles) {
                        tp.refreshDots(chessBoard);
                    }
                    repaint();
                }
            };
            addMouseListener(adapter);
            addMouseMotionListener(adapter);
        }

        // ── Click-to-move (second click) ─────────────────────────────

        @Override
        protected void processMouseEvent(MouseEvent e) {
            super.processMouseEvent(e);
            if (e.getID() != MouseEvent.MOUSE_CLICKED || gameOver) return;

            if (isRightMouseButton(e)) {
                sourceTile = null;
                destinationTile = null;
                humanMovedPiece = null;
                dragSourceTileId = -1;
                drawBoard(chessBoard);
                return;
            }
            if (!isLeftMouseButton(e)) return;

            if (sourceTile != null && dragImage == null && dragSourceTileId == -1) {
                final int toId = tileIdAtPoint(e.getPoint());
                if (toId < 0) return;
                final int fromId = sourceTile.getTileCoordinate();
                sourceTile = null;
                humanMovedPiece = null;
                tryMove(fromId, toId);
                if (lastMoveSource == fromId) { arrowSource = fromId; arrowDest = toId; }
                afterMoveRefresh();
            }
        }

        // ── Board layout ─────────────────────────────────────────────

        public void drawBoard(final Board board) {
            removeAll();
            for (final TilePanel tp : boardDirection.traverse(boardTiles)) {
                tp.drawTile(board);
                add(tp);
            }
            validate();
            repaint();
        }

        /**
         * Map a point on this panel to tile index 0-63, or -1.
         */
        int tileIdAtPoint(final Point p) {
            if (getWidth() == 0 || getHeight() == 0) return -1;
            int col = p.x * 8 / getWidth();
            int row = p.y * 8 / getHeight();
            col = Math.max(0, Math.min(7, col));
            row = Math.max(0, Math.min(7, row));
            if (boardDirection == BoardDirection.FLIPPED) {
                col = 7 - col;
                row = 7 - row;
            }
            return row * 8 + col;
        }

        /**
         * Paint drag ghost + optional coordinate labels on top of tiles.
         * No layout work happens here — pure graphics.
         */
        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // ── Drag ghost — sized to current tile dimensions ─────────
            if (dragImage != null && dragPoint != null) {
                final int tileW = getWidth() / 8;
                final int tileH = getHeight() / 8;
                g2.drawImage(dragImage, dragPoint.x - tileW / 2, dragPoint.y - tileH / 2,
                        tileW, tileH, this);
            }

            // ── AI piece animation ────────────────────────────────────
            if (animPiece != null) {
                final int tileW = getWidth() / 8;
                final int tileH = getHeight() / 8;
                // Ease-out: smooth deceleration towards destination
                final float t = 1f - (1f - animProgress) * (1f - animProgress);
                final int px = Math.round(animFromX + (animToX - animFromX) * t);
                final int py = Math.round(animFromY + (animToY - animFromY) * t);
                g2.drawImage(animPiece, px, py, tileW, tileH, this);
            }

            // ── AI move arrow ─────────────────────────────────────────
            if (arrowSource >= 0 && arrowDest >= 0) paintMoveArrow(g2, arrowSource, arrowDest);

            // ── Coordinate labels ─────────────────────────────────────
            if (showCoordinates) paintCoordinates(g2);

            g2.dispose();
        }

        private void paintMoveArrow(final Graphics2D g2, final int fromId, final int toId) {
            final int tw = getWidth() / 8;
            final int th = getHeight() / 8;

            // Centre of source and destination tiles (accounting for board flip)
            final int fromDisplay = (boardDirection == BoardDirection.FLIPPED) ? (63 - fromId) : fromId;
            final int toDisplay   = (boardDirection == BoardDirection.FLIPPED) ? (63 - toId)   : toId;

            final int x1 = (fromDisplay % 8) * tw + tw / 2;
            final int y1 = (fromDisplay / 8) * th + th / 2;
            final int x2 = (toDisplay % 8) * tw + tw / 2;
            final int y2 = (toDisplay / 8) * th + th / 2;

            final double angle = Math.atan2(y2 - y1, x2 - x1);
            final double cos = Math.cos(angle);
            final double sin = Math.sin(angle);

            // chess.com style: orange, semi-transparent
            g2.setColor(new Color(235, 165, 25, 185));

            // Shaft — filled rectangle along the line, stopping before the arrowhead base
            final int shaftW = Math.max(2, tw / 10);
            final int headLen = Math.max(8, tw / 4);
            final int headW = Math.max(5, tw / 4);
            // shaft end point (pulled back by headLen so it doesn't overlap the head)
            final int sx2 = (int) (x2 - headLen * cos);
            final int sy2 = (int) (y2 - headLen * sin);
            final int[] shaftXs = {
                (int) (x1 - shaftW * sin), (int) (x1 + shaftW * sin),
                (int) (sx2 + shaftW * sin), (int) (sx2 - shaftW * sin)
            };
            final int[] shaftYs = {
                (int) (y1 + shaftW * cos), (int) (y1 - shaftW * cos),
                (int) (sy2 - shaftW * cos), (int) (sy2 + shaftW * cos)
            };
            g2.fillPolygon(shaftXs, shaftYs, 4);

            // Arrowhead — wide triangle from headBase to tip
            final int[] headXs = {
                x2,
                (int) (sx2 - headW * sin),
                (int) (sx2 + headW * sin)
            };
            final int[] headYs = {
                y2,
                (int) (sy2 + headW * cos),
                (int) (sy2 - headW * cos)
            };
            g2.fillPolygon(headXs, headYs, 3);
        }

        /**
         * Draws rank (1-8) and file (a-h) labels inside the first/last column/row of tiles.
         */
        private void paintCoordinates(final Graphics2D g2) {
            final int w = getWidth();
            final int h = getHeight();
            final int tw = w / 8;   // tile width
            final int th = h / 8;   // tile height

            g2.setFont(COORD_FONT);
            final FontMetrics fm = g2.getFontMetrics();

            final String[] files = {"a", "b", "c", "d", "e", "f", "g", "h"};
            final String[] ranks = {"8", "7", "6", "5", "4", "3", "2", "1"};

            // When flipped the labels reverse
            final String[] fLabels = boardDirection == BoardDirection.FLIPPED
                    ? new String[]{"h", "g", "f", "e", "d", "c", "b", "a"} : files;
            final String[] rLabels = boardDirection == BoardDirection.FLIPPED
                    ? new String[]{"1", "2", "3", "4", "5", "6", "7", "8"} : ranks;

            for (int i = 0; i < 8; i++) {
                // File labels along the bottom edge
                final String fl = fLabels[i];
                final int fx = i * tw + tw - fm.stringWidth(fl) - 3;
                final int fy = h - 3;
                // shadow
                g2.setColor(new Color(0, 0, 0, 120));
                g2.drawString(fl, fx + 1, fy + 1);
                // label — alternate colour to match the tile below it
                g2.setColor(isLightTileAt(i, 7) ? boardTheme.dark : boardTheme.light);
                g2.drawString(fl, fx, fy);

                // Rank labels along the left edge
                final String rl = rLabels[i];
                final int rx = 3;
                final int ry = i * th + fm.getAscent() + 2;
                g2.setColor(new Color(0, 0, 0, 120));
                g2.drawString(rl, rx + 1, ry + 1);
                g2.setColor(isLightTileAt(0, i) ? boardTheme.dark : boardTheme.light);
                g2.drawString(rl, rx, ry);
            }
        }

        /**
         * Whether the tile at (col, row) is a light tile (before theme colouring).
         */
        private boolean isLightTileAt(int col, int row) {
            return (col + row) % 2 == 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  TilePanel — pure display, theme-aware colours
    // ─────────────────────────────────────────────────────────────────

    private class TilePanel extends JPanel {

        private final int tileId;
        private boolean showLegalDot = false;
        private boolean isCaptureDot = false;
        private boolean showCheckOverlay = false;

        // ── Colour ───────────────────────────────────────────────────

        TilePanel(final int tileId) {
            super(new GridBagLayout());
            this.tileId = tileId;
            setPreferredSize(TILE_PANEL_DIMENSION);
            setOpaque(true);
        }

        void drawTile(final Board board) {
            assignTileColor();
            assignTilePieceIcon(board);
            highlightLastMove();
            highlightSelected();
            highlightCheck(board);
            highlightLegals(board);
            validate();
            repaint();
        }

        /**
         * Lightweight update used by hover — only recomputes dot flags, no icon reload.
         */
        void refreshDots(final Board board) {
            highlightLegals(board);
            repaint();
        }

        private void assignTileColor() {
            setBackground(isLightTile() ? boardTheme.light : boardTheme.dark);
        }

        private boolean isLightTile() {
            if (BoardUtils.EIGHTH_RANK[tileId] || BoardUtils.SIXTH_RANK[tileId]
                    || BoardUtils.FOURTH_RANK[tileId] || BoardUtils.SECOND_RANK[tileId])
                return tileId % 2 == 0;
            return tileId % 2 != 0;
        }

        // ── Legal-move dot flag (painted in paintComponent, never adds a child) ──

        private void highlightLastMove() {
            if (tileId == lastMoveSource || tileId == lastMoveDest)
                setBackground(isLightTile() ? boardTheme.lastLight : boardTheme.lastDark);
        }

        private void highlightSelected() {
            if ((sourceTile != null && tileId == sourceTile.getTileCoordinate())
                    || tileId == dragSourceTileId)
                setBackground(boardTheme.selected);
        }

        private void highlightCheck(final Board board) {
            showCheckOverlay = board.getCurrentPlayer().isInCheck()
                    && tileId == board.getCurrentPlayer().getPlayerKing().getPiecePosition();
        }

        private void highlightLegals(final Board board) {
            showLegalDot = false;
            isCaptureDot = false;

            final boolean clickActive = highlightLegalMoves && humanMovedPiece != null && sourceTile != null;
            final boolean hoverActive = hoverHighlight && humanMovedPiece != null && hoverTileId >= 0 && sourceTile == null;
            if (!clickActive && !hoverActive) return;

            for (final Move move : legalMovesOfSelected(board)) {
                if (move.getDestinationCoordinate() != tileId) continue;
                showLegalDot = true;
                isCaptureDot = move.isAttack();
                break;
            }
        }

        private Collection<Move> legalMovesOfSelected(final Board board) {
            if (humanMovedPiece != null
                    && humanMovedPiece.getPieceAlliance() == board.getCurrentPlayer().getAlliance())
                return humanMovedPiece.calculateLegalMoves(board);
            return Collections.emptyList();
        }

        // ── Piece icon ───────────────────────────────────────────────

        private void assignTilePieceIcon(final Board board) {
            removeAll();
            if (tileId == dragSourceTileId) return;
            if (!board.getTile(tileId).isTileOccupied()) return;

            final Piece piece = board.getTile(tileId).getPiece();
            final String key = String.valueOf(piece.getPieceAlliance().toString().charAt(0)) + piece;
            final BufferedImage img = SCALED_IMAGE_CACHE.getOrDefault(key, RAW_IMAGE_CACHE.get(key));
            if (img != null) add(new JLabel(new ImageIcon(img)));
        }

        // ── Rendering quality + legal-move dot overlay ───────────────

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (showCheckOverlay) {
                g2.setColor(new Color(220, 0, 0, 120));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            if (!showLegalDot) return;

            final int w = getWidth();
            final int h = getHeight();

            if (isCaptureDot) {
                // Capture: translucent ring around the tile edge
                final int stroke = Math.max(3, w / 12);
                g2.setColor(new Color(0, 0, 0, 90));
                g2.setStroke(new BasicStroke(stroke));
                final int inset = stroke / 2 + 1;
                g2.drawOval(inset, inset, w - inset * 2, h - inset * 2);
            } else {
                // Move: small filled circle in centre
                final int r = w / 4;
                final int cx = w / 2 - r;
                final int cy = h / 2 - r;
                g2.setColor(new Color(0, 0, 0, 80));
                g2.fillOval(cx, cy, r * 2, r * 2);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ClockPanel — two countdown clocks, one per side
    // ─────────────────────────────────────────────────────────────────

    private class ClockPanel extends JPanel {

        private final JLabel whiteLabel = new JLabel("10:00", SwingConstants.CENTER);
        private final JLabel blackLabel = new JLabel("10:00", SwingConstants.CENTER);
        private int whiteSeconds = 600;
        private int blackSeconds = 600;
        private boolean whiteActive = false; // whose clock is ticking
        private boolean enabled = true;
        private final javax.swing.Timer ticker;

        ClockPanel() {
            super(new GridLayout(1, 2));
            final Font f = new Font("Monospaced", Font.BOLD, 16);
            whiteLabel.setFont(f);
            blackLabel.setFont(f);
            whiteLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            blackLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            add(new JLabel("White", SwingConstants.RIGHT));
            add(whiteLabel);
            add(blackLabel);
            add(new JLabel("Black", SwingConstants.LEFT));
            setLayout(new GridLayout(1, 4));
            ticker = new javax.swing.Timer(1000, e -> tick());
            ticker.setInitialDelay(1000);
        }

        /** Called by setupAfterGameSetup when settings change mid-game. */
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

        /** Called by resetGame — resets to new configured time and stops. */
        void reset(final boolean on, final int minutes) {
            configure(on, minutes);
        }

        /** Called after each move to swap the active clock. */
        void onMoveMade(final com.chess.engine.Alliance nowToMove) {
            if (!enabled) return;
            // The player who just moved is the opponent of nowToMove
            whiteActive = nowToMove.isWhite(); // white's turn → white clock ticks
            if (!ticker.isRunning()) ticker.start();
        }

        void stop() {
            ticker.stop();
        }

        private void tick() {
            if (!enabled) return;
            if (whiteActive) {
                whiteSeconds--;
                if (whiteSeconds <= 0) {
                    whiteSeconds = 0;
                    ticker.stop();
                    updateLabels();
                    onTimeout(true);
                    return;
                }
            } else {
                blackSeconds--;
                if (blackSeconds <= 0) {
                    blackSeconds = 0;
                    ticker.stop();
                    updateLabels();
                    onTimeout(false);
                    return;
                }
            }
            updateLabels();
        }

        private void onTimeout(final boolean whiteTimedOut) {
            gameOver = true;
            SoundManager.play(SoundManager.SoundType.GAME_END);
            final String winner = whiteTimedOut ? "Black" : "White";
            final int choice = JOptionPane.showOptionDialog(gameFrame,
                    (whiteTimedOut ? "White" : "Black") + " ran out of time!  " + winner + " wins!",
                    "Time Out", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, new Object[]{"New Game", "Close"}, "New Game");
            if (choice == JOptionPane.YES_OPTION) resetGame();
        }

        private void updateLabels() {
            whiteLabel.setText(formatTime(whiteSeconds));
            blackLabel.setText(formatTime(blackSeconds));
            // Highlight active clock; flash red when under 10 seconds
            final Color activeCol = new Color(200, 240, 200);
            final Color warnCol   = new Color(255, 100, 100);
            final Color idleCol   = getBackground();
            whiteLabel.setBackground(whiteActive
                    ? (whiteSeconds <= 10 ? warnCol : activeCol) : idleCol);
            blackLabel.setBackground(!whiteActive
                    ? (blackSeconds <= 10 ? warnCol : activeCol) : idleCol);
            whiteLabel.setOpaque(whiteActive || whiteSeconds <= 10);
            blackLabel.setOpaque(!whiteActive || blackSeconds <= 10);
        }

        private String formatTime(final int totalSeconds) {
            final int m = totalSeconds / 60;
            final int s = totalSeconds % 60;
            return String.format("%02d:%02d", m, s);
        }
    }
}
