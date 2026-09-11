package com.chess.engine.gui;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.MoveLog;
import com.chess.engine.board.Move;
import com.chess.engine.board.Tile;
import com.chess.engine.pieces.*;
import com.chess.engine.player.MoveTransition;
import com.chess.engine.player.ai.Minimax;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;

public class Table implements TableContext {

    // ── Constants ─────────────────────────────────────────────────────
    private static final Dimension OUTER_FRAME_DIMENSION = new Dimension(880, 680);
    private static final String PIECE_ICON_PATH = "images/";
    // ── Static raw image cache (loaded once) ──────────────────────────
    private static final Map<String, BufferedImage> RAW_IMAGE_CACHE = loadRawCache();
    // ── Persistent preferences ────────────────────────────────────────
    private static final Preferences PREFS = Preferences.userNodeForPackage(Table.class);
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
    private Map<String, BufferedImage> scaledImageCache = new HashMap<>();
    private Map<String, BufferedImage> dragImageCache = new HashMap<>();
    private int lastScaledTileSize = -1;
    private Board chessBoard;
    private BoardDirection boardDirection;
    private BoardTheme boardTheme = BoardTheme.CLASSIC;
    // Preferences flags
    private boolean highlightLegalMoves = false;
    private boolean hoverHighlight = false;
    private boolean showCoordinates = false;
    private boolean gameOver = false;
    // Selection / drag state
    private Tile sourceTile;
    private Tile destinationTile;
    private Piece humanMovedPiece;
    private BufferedImage dragImage;
    private Point dragPoint;
    private int dragSourceTileId = -1;
    private int hoverTileId = -1;
    // Last-move highlights
    private int lastMoveSource = -1;
    private int lastMoveDest = -1;
    // Move arrow overlay
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

        boardPanel = new BoardPanel(this,
                this::rebuildScaledCaches,
                this::onMoveAttempt,
                this::onRightClick,
                this::onHover);
        installBoardPressListener();

        clockPanel = new ClockPanel(
                () -> onTimeout(true),
                () -> onTimeout(false));

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
        gameFrame.setLocationRelativeTo(null);
        gameFrame.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            rebuildScaledCaches();
            boardPanel.drawBoard(chessBoard);
            updateStatus();
            if (gameSetup.isAIPlayer(chessBoard.getCurrentPlayer())) fireAIThinkTank();
        });
    }

    // ── TableContext implementation ───────────────────────────────────

    @Override public Board getChessBoard()                          { return chessBoard; }
    @Override public BoardTheme getBoardTheme()                     { return boardTheme; }
    @Override public BoardDirection getBoardDirection()             { return boardDirection; }
    @Override public boolean isHighlightLegalMoves()               { return highlightLegalMoves; }
    @Override public boolean isHoverHighlight()                     { return hoverHighlight; }
    @Override public boolean isShowCoordinates()                    { return showCoordinates; }
    @Override public Tile getSourceTile()                           { return sourceTile; }
    @Override public Piece getHumanMovedPiece()                    { return humanMovedPiece; }
    @Override public BufferedImage getDragImage()                   { return dragImage; }
    @Override public Point getDragPoint()                           { return dragPoint; }
    @Override public int getDragSourceTileId()                      { return dragSourceTileId; }
    @Override public int getHoverTileId()                           { return hoverTileId; }
    @Override public int getLastMoveSource()                        { return lastMoveSource; }
    @Override public int getLastMoveDest()                          { return lastMoveDest; }
    @Override public int getArrowSource()                           { return arrowSource; }
    @Override public int getArrowDest()                             { return arrowDest; }
    @Override public BufferedImage getAnimPiece()                   { return animPiece; }
    @Override public float getAnimFromX()                           { return animFromX; }
    @Override public float getAnimFromY()                           { return animFromY; }
    @Override public float getAnimToX()                             { return animToX; }
    @Override public float getAnimToY()                             { return animToY; }
    @Override public float getAnimProgress()                        { return animProgress; }
    @Override public Map<String, BufferedImage> getScaledImageCache() { return scaledImageCache; }
    @Override public Map<String, BufferedImage> getRawImageCache()  { return RAW_IMAGE_CACHE; }

    // ── Image cache ───────────────────────────────────────────────────

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

    private void rebuildScaledCaches() {
        final int tw = boardPanel.getWidth() / 8;
        final int th = boardPanel.getHeight() / 8;
        final int tileSize = Math.max(16, Math.min(tw, th));
        if (tileSize == lastScaledTileSize) return;
        lastScaledTileSize = tileSize;

        final Map<String, BufferedImage> sc = new HashMap<>();
        for (final Map.Entry<String, BufferedImage> entry : RAW_IMAGE_CACHE.entrySet()) {
            try {
                sc.put(entry.getKey(), org.imgscalr.Scalr.resize(entry.getValue(),
                        org.imgscalr.Scalr.Method.QUALITY, org.imgscalr.Scalr.Mode.FIT_EXACT,
                        tileSize, tileSize, org.imgscalr.Scalr.OP_ANTIALIAS));
            } catch (Exception e) {
                sc.put(entry.getKey(), entry.getValue());
            }
        }
        scaledImageCache = Collections.unmodifiableMap(sc);
        dragImageCache = scaledImageCache;
    }

    // ── Menu bar ──────────────────────────────────────────────────────

    private JMenuBar createTableMenuBar() {
        final JMenuBar bar = new JMenuBar();
        bar.add(createFileMenu());
        bar.add(createPreferencesMenu());
        return bar;
    }

    private JMenu createFileMenu() {
        final JMenu menu = new JMenu("File");

        final JMenuItem newGame = new JMenuItem("New Game");
        newGame.addActionListener(e -> resetGame());
        menu.add(newGame);

        final JMenuItem undo = new JMenuItem("Undo");
        undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undo.addActionListener(e -> undoLastMove());
        menu.add(undo);

        final JMenuItem setup = new JMenuItem("Game Setup...");
        setup.addActionListener(e -> { gameSetup.promptUser(); setupAfterGameSetup(); });
        menu.add(setup);

        menu.addSeparator();

        final JMenuItem save = new JMenuItem("Save Game…");
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        save.addActionListener(e -> PgnUtils.saveGame(gameFrame, moveLog, gameSetup));
        menu.add(save);

        final JMenuItem load = new JMenuItem("Load Game…");
        load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        load.addActionListener(e -> loadGameFromPgn());
        menu.add(load);

        menu.addSeparator();
        final JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> System.exit(0));
        menu.add(exit);
        return menu;
    }

    private JMenu createPreferencesMenu() {
        final JMenu menu = new JMenu("Preferences");

        final JMenuItem flip = new JMenuItem("Flip Board");
        flip.addActionListener(e -> { boardDirection = boardDirection.opposite(); boardPanel.drawBoard(chessBoard); });
        menu.add(flip);
        menu.addSeparator();

        final JCheckBoxMenuItem hiLegal = new JCheckBoxMenuItem("Highlight Legal Moves", highlightLegalMoves);
        hiLegal.addActionListener(e -> {
            highlightLegalMoves = hiLegal.isSelected();
            PREFS.putBoolean("highlightLegalMoves", highlightLegalMoves);
            boardPanel.drawBoard(chessBoard);
        });
        menu.add(hiLegal);

        final JCheckBoxMenuItem hiHover = new JCheckBoxMenuItem("Highlight on Hover", hoverHighlight);
        hiHover.addActionListener(e -> {
            hoverHighlight = hiHover.isSelected();
            PREFS.putBoolean("hoverHighlight", hoverHighlight);
            boardPanel.repaint();
        });
        menu.add(hiHover);

        final JCheckBoxMenuItem coords = new JCheckBoxMenuItem("Show Coordinates", showCoordinates);
        coords.addActionListener(e -> {
            showCoordinates = coords.isSelected();
            PREFS.putBoolean("showCoordinates", showCoordinates);
            boardPanel.repaint();
        });
        menu.add(coords);

        menu.addSeparator();

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

        final JCheckBoxMenuItem sound = new JCheckBoxMenuItem("Sound Effects", SoundManager.isEnabled());
        sound.addActionListener(e -> {
            SoundManager.setEnabled(sound.isSelected());
            PREFS.putBoolean("soundEnabled", sound.isSelected());
        });
        menu.add(sound);

        return menu;
    }

    // ── Game control ──────────────────────────────────────────────────

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

    // ── Board event callbacks (from BoardPanel) ───────────────────────

    private void onMoveAttempt(final int fromId, final int toId) {
        if (gameOver) return;
        sourceTile = null;
        humanMovedPiece = null;
        dragImage = null;
        dragPoint = null;
        dragSourceTileId = -1;
        tryMove(fromId, toId);
        if (lastMoveSource == fromId) { arrowSource = fromId; arrowDest = toId; }
        afterMoveRefresh();
    }

    private void onRightClick() {
        sourceTile = null;
        destinationTile = null;
        humanMovedPiece = null;
        dragSourceTileId = -1;
        boardPanel.drawBoard(chessBoard);
    }

    private void onHover(final int tileId) {
        if (tileId == hoverTileId) return;
        hoverTileId = tileId;
        if (sourceTile == null) {
            final Piece p = (tileId >= 0) ? chessBoard.getTile(tileId).getPiece() : null;
            humanMovedPiece = (hoverHighlight && p != null
                    && p.getPieceAlliance() == chessBoard.getCurrentPlayer().getAlliance()) ? p : null;
        }
    }

    private void installBoardPressListener() {
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gameOver || !isLeftMouseButton(e)) return;
                final int tileId = boardPanel.tileIdAtPoint(e.getPoint());
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
                dragImage = dragImageCache.getOrDefault(key, RAW_IMAGE_CACHE.get(key));
                boardPanel.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (gameOver || !isLeftMouseButton(e)) {
                    if (isRightMouseButton(e)) onRightClick();
                    return;
                }
                if (sourceTile == null) return;
                final int toId = boardPanel.tileIdAtPoint(e.getPoint());
                if (toId >= 0 && toId != dragSourceTileId) {
                    final int fromId = dragSourceTileId;
                    onMoveAttempt(fromId, toId);
                } else {
                    // click-to-move: second click on same or no valid dest — reset
                    dragImage = null;
                    dragPoint = null;
                    dragSourceTileId = -1;
                    sourceTile = null;
                    humanMovedPiece = null;
                    boardPanel.repaint();
                }
            }
        });

        boardPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (sourceTile == null) return;
                dragPoint = e.getPoint();
                boardPanel.repaint();
            }
        });
    }

    private Move askPromotionChoice(final Move.PawnPromotion original) {
        final Alliance alliance = original.getMovedPiece().getPieceAlliance();
        final int dest = original.getDestinationCoordinate();
        final char allianceChar = alliance.toString().charAt(0);

        final String[] labels = {"Queen", "Rook", "Bishop", "Knight"};
        final String[] keys = {allianceChar + "Q", allianceChar + "R", allianceChar + "B", allianceChar + "N"};
        final int[] chosen = {0};

        final JDialog dialog = new JDialog(gameFrame, "Pawn Promotion", true);
        dialog.setLayout(new FlowLayout(FlowLayout.CENTER, 12, 12));
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            final BufferedImage img = scaledImageCache.getOrDefault(keys[i], RAW_IMAGE_CACHE.get(keys[i]));
            final JButton btn = new JButton(labels[i]);
            if (img != null) {
                final Image scaled = img.getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                btn.setIcon(new ImageIcon(scaled));
                btn.setHorizontalTextPosition(SwingConstants.CENTER);
                btn.setVerticalTextPosition(SwingConstants.BOTTOM);
            }
            btn.setFocusPainted(false);
            btn.addActionListener(e -> { chosen[0] = idx; dialog.dispose(); });
            dialog.add(btn);
        }
        dialog.pack();
        dialog.setLocationRelativeTo(gameFrame);
        dialog.setVisible(true);

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

    private void undoLastMove() {
        if (moveLog.size() == 0) return;
        final boolean vsComputer = gameSetup.isAIPlayer(chessBoard.getCurrentPlayer())
                || gameSetup.isAIPlayer(chessBoard.getCurrentPlayer().getOpponent());
        final int movesToPop = (vsComputer && moveLog.size() >= 2) ? 2 : 1;
        for (int i = 0; i < movesToPop; i++) moveLog.removeMove(moveLog.size() - 1);

        chessBoard = Board.createStandardBoard();
        for (final Move move : moveLog.getMoves())
            chessBoard = chessBoard.getCurrentPlayer().makeMove(move).getTransitionBoard();

        if (moveLog.size() > 0) {
            final Move last = moveLog.getMoves().get(moveLog.size() - 1);
            lastMoveSource = last.getCurrentCoordinate();
            lastMoveDest = last.getDestinationCoordinate();
        } else {
            lastMoveSource = -1;
            lastMoveDest = -1;
        }
        sourceTile = null; destinationTile = null; humanMovedPiece = null;
        dragImage = null; dragPoint = null; dragSourceTileId = -1;
        hoverTileId = -1; gameOver = false;
        SwingUtilities.invokeLater(() -> {
            gameHistoryPanel.redo(chessBoard, moveLog);
            takenPiecesPanel.redo(moveLog);
            boardPanel.drawBoard(chessBoard);
            updateStatus();
        });
    }

    private void loadGameFromPgn() {
        final List<Move> loaded = PgnUtils.loadGame(gameFrame);
        if (loaded == null) return;

        chessBoard = Board.createStandardBoard();
        for (final Move m : loaded) chessBoard = chessBoard.getCurrentPlayer().makeMove(m).getTransitionBoard();

        moveLog.clear();
        for (final Move m : loaded) moveLog.addMove(m);
        final Move last = loaded.getLast();
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
    }

    private void resetGame() {
        chessBoard = Board.createStandardBoard();
        moveLog.clear();
        sourceTile = null; destinationTile = null; humanMovedPiece = null;
        dragImage = null; dragPoint = null; dragSourceTileId = -1;
        hoverTileId = -1; lastMoveSource = -1; lastMoveDest = -1;
        arrowSource = -1; arrowDest = -1; gameOver = false;
        clockPanel.reset(gameSetup.isClockEnabled(), gameSetup.getClockMinutes());
        SwingUtilities.invokeLater(() -> {
            gameHistoryPanel.redo(chessBoard, moveLog);
            takenPiecesPanel.redo(moveLog);
            boardPanel.drawBoard(chessBoard);
            updateStatus();
        });
    }

    // ── AI worker ─────────────────────────────────────────────────────

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

                final int fromId = m.getCurrentCoordinate();
                final int toId   = m.getDestinationCoordinate();
                final Piece piece = chessBoard.getTile(fromId).getPiece();
                final String key  = String.valueOf(piece.getPieceAlliance().toString().charAt(0)) + piece;
                animPiece = scaledImageCache.getOrDefault(key, RAW_IMAGE_CACHE.get(key));

                final int tw = boardPanel.getWidth() / 8;
                final int th = boardPanel.getHeight() / 8;
                final int fromDisp = (boardDirection == BoardDirection.FLIPPED) ? (63 - fromId) : fromId;
                final int toDisp   = (boardDirection == BoardDirection.FLIPPED) ? (63 - toId)   : toId;
                animFromX = (fromDisp % 8) * tw;
                animFromY = (fromDisp / 8) * th;
                animToX   = (toDisp % 8) * tw;
                animToY   = (toDisp / 8) * th;
                animProgress = 0f;

                dragSourceTileId = fromId;
                boardPanel.drawBoard(chessBoard);

                final float step = 1f / 10;
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
}
