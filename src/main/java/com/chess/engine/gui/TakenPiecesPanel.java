package com.chess.engine.gui;

import com.chess.engine.board.Move;
import com.chess.engine.pieces.Piece;
import com.google.common.primitives.Ints;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static com.chess.engine.gui.Table.MoveLog;

/**
 * Side panel — chess.com style captured-piece layout.
 * <p>
 * Viewing as White (board NORMAL):
 * TOP    → opponent's captures  (white pieces Black took)
 * BOTTOM → your captures        (black pieces White took)
 * <p>
 * Each half shows piece icons + total points + "+N" advantage for the leading side.
 */
public class TakenPiecesPanel extends JPanel {

    private static final int PAWN_VAL = 1;
    private static final int KNIGHT_VAL = 3;
    private static final int BISHOP_VAL = 3;
    private static final int ROOK_VAL = 5;
    private static final int QUEEN_VAL = 9;

    private static final EtchedBorder PANEL_BORDER = new EtchedBorder(EtchedBorder.RAISED);
    private static final Color PANEL_COLOR = Color.decode("0xFFFDE6");
    private static final Dimension TAKEN_PIECES_DIMENSION = new Dimension(72, 600);
    private static final int TAKEN_PIECE_SIZE = 26;

    /**
     * Scaled icon cache — loaded once, never re-read from disk on each redo().
     */
    private static final Map<String, ImageIcon> ICON_CACHE = buildIconCache();
    // TOP  = Black's captures (white pieces Black took) — opponent's side of screen
    private final JPanel topPieces;
    private final JLabel topScore;
    private final JLabel topAdv;
    // BOTTOM = White's captures (black pieces White took) — your side of screen
    private final JPanel bottomPieces;
    private final JLabel bottomScore;
    private final JLabel bottomAdv;
    public TakenPiecesPanel() {
        super(new BorderLayout(0, 4));
        setBackground(PANEL_COLOR);
        setBorder(PANEL_BORDER);

        topScore = scoreLabel();
        topPieces = piecesGrid();
        topAdv = advLabel();

        bottomScore = scoreLabel();
        bottomPieces = piecesGrid();
        bottomAdv = advLabel();

        add(section(topScore, topPieces, topAdv), BorderLayout.NORTH);
        add(section(bottomScore, bottomPieces, bottomAdv), BorderLayout.SOUTH);
        setPreferredSize(TAKEN_PIECES_DIMENSION);
    }

    private static Map<String, ImageIcon> buildIconCache() {
        final Map<String, ImageIcon> cache = new HashMap<>();
        for (final String a : new String[]{"W", "B"})
            for (final String s : new String[]{"K", "Q", "R", "B", "N", "P"}) {
                final String key = a + s;
                try {
                    final BufferedImage raw = ImageIO.read(new File("images/" + key + ".png"));
                    final Image scaled = raw.getScaledInstance(TAKEN_PIECE_SIZE, TAKEN_PIECE_SIZE, Image.SCALE_SMOOTH);
                    cache.put(key, new ImageIcon(scaled));
                } catch (final IOException e) {
                    System.err.println("TakenPiecesPanel: missing image " + key + ".png");
                }
            }
        return Collections.unmodifiableMap(cache);
    }

    // ── Factory helpers ───────────────────────────────────────────────

    private static JLabel scoreLabel() {
        final JLabel l = new JLabel("", SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(new Color(60, 60, 60));
        return l;
    }

    private static JLabel advLabel() {
        final JLabel l = new JLabel("", SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(new Color(40, 120, 40));
        return l;
    }

    private static JPanel piecesGrid() {
        final JPanel p = new JPanel(new GridLayout(0, 2, 0, 0));
        p.setBackground(Color.decode("0xFFFDE6"));
        return p;
    }

    private static JPanel section(final JLabel score, final JPanel pieces, final JLabel adv) {
        final JPanel s = new JPanel(new BorderLayout(0, 1));
        s.setBackground(Color.decode("0xFFFDE6"));
        s.add(score, BorderLayout.NORTH);
        s.add(pieces, BorderLayout.CENTER);
        s.add(adv, BorderLayout.SOUTH);
        return s;
    }

    // ── Public refresh ────────────────────────────────────────────────

    private static void renderPieces(final List<Piece> pieces, final JPanel panel) {
        for (final Piece piece : pieces) {
            final String key = String.valueOf(piece.getPieceAlliance().toString().charAt(0)) + piece;
            final ImageIcon icon = ICON_CACHE.get(key);
            if (icon != null) panel.add(new JLabel(icon));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static int materialScore(final List<Piece> pieces) {
        int total = 0;
        for (final Piece p : pieces) {
            total += switch (p.getPieceType()) {
                case PAWN -> PAWN_VAL;
                case KNIGHT -> KNIGHT_VAL;
                case BISHOP -> BISHOP_VAL;
                case ROOK -> ROOK_VAL;
                case QUEEN -> QUEEN_VAL;
                default -> 0;
            };
        }
        return total;
    }

    public void redo(final MoveLog moveLog) {
        topPieces.removeAll();
        bottomPieces.removeAll();

        // whiteTaken = black pieces White captured → White's captures → BOTTOM (your side)
        // blackTaken = white pieces Black captured → Black's captures → TOP    (opponent's side)
        final List<Piece> whiteTaken = new ArrayList<>();
        final List<Piece> blackTaken = new ArrayList<>();

        for (final Move move : moveLog.getMoves()) {
            if (!move.isAttack()) continue;
            final Piece taken = move.getAttackedPiece();
            if (taken.getPieceAlliance().isBlack()) whiteTaken.add(taken);
            else blackTaken.add(taken);
        }

        whiteTaken.sort((a, b) -> Ints.compare(b.getPieceValue(), a.getPieceValue()));
        blackTaken.sort((a, b) -> Ints.compare(b.getPieceValue(), a.getPieceValue()));

        renderPieces(whiteTaken, bottomPieces);   // White's captures at bottom
        renderPieces(blackTaken, topPieces);      // Black's captures at top

        final int ws = materialScore(whiteTaken);
        final int bs = materialScore(blackTaken);

        bottomScore.setText(ws > 0 ? ws + " pts" : "");
        topScore.setText(bs > 0 ? bs + " pts" : "");

        final int diff = ws - bs;
        if (diff > 0) {
            bottomAdv.setText("+" + diff);
            topAdv.setText("");
        } else if (diff < 0) {
            bottomAdv.setText("");
            topAdv.setText("+" + (-diff));
        } else {
            bottomAdv.setText("");
            topAdv.setText("");
        }

        validate();
        repaint();
    }
}
