package com.chess.engine.gui;

import com.chess.engine.board.Move;
import com.chess.engine.board.MoveLog;
import com.chess.engine.pieces.Piece;
import com.google.common.primitives.Ints;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Chess.com-style captured pieces: two thin horizontal strips placed
 * above and below the board.
 * <p>
 * BLACK strip (top)  — white pieces Black captured
 * WHITE strip (bottom) — black pieces White captured
 * <p>
 * Each strip paints piece icons overlapping slightly, with a green "+N"
 * advantage pill inline after the last piece.
 */
public class TakenPiecesPanel {

    // ── Piece values ──────────────────────────────────────────────────────
    private static final int PAWN_VAL = 1;
    private static final int KNIGHT_VAL = 3;
    private static final int BISHOP_VAL = 3;
    private static final int ROOK_VAL = 5;
    private static final int QUEEN_VAL = 9;

    // ── Layout constants ──────────────────────────────────────────────────
    private static final int STRIP_H = 32;   // strip height in pixels
    private static final int PIECE_SIZE = 22;   // icon render size
    private static final int OVERLAP = 6;    // each icon overlaps previous by this much
    private static final int PAD_LEFT = 6;    // left padding before first piece
    private static final int PILL_H = 16;
    private static final int PILL_ARC = 8;
    private static final int PILL_PAD = 5;    // horizontal pill text padding

    // ── Light-theme palette ───────────────────────────────────────────────
    private static final Color BG_TOP = new Color(245, 245, 248);
    private static final Color BG_BOT = new Color(245, 245, 248);
    private static final Color BORDER_COL = new Color(210, 211, 216);
    private static final Color ADV_PILL_BG = new Color(200, 235, 200);
    private static final Color ADV_PILL_FG = new Color(25, 100, 25);
    private static final Font PILL_FONT = new Font("SansSerif", Font.BOLD, 11);

    // ── Icon cache (raw — scaled at paint time for crisp rendering) ───────
    private static final Map<String, BufferedImage> ICON_CACHE = buildIconCache();
    // ── The two strip panels ──────────────────────────────────────────────
    private final Strip topStrip;    // BLACK's captures (white pieces) — above board
    private final Strip bottomStrip; // WHITE's captures (black pieces) — below board
    // ── State ─────────────────────────────────────────────────────────────
    private List<Piece> whiteTaken = new ArrayList<>(); // black pieces White captured
    private List<Piece> blackTaken = new ArrayList<>(); // white pieces Black captured
    private int whiteMaterial = 0;
    private int blackMaterial = 0;

    public TakenPiecesPanel() {
        topStrip = new Strip(BG_TOP,  /* topBorder */ false);
        bottomStrip = new Strip(BG_BOT,  /* topBorder */ true);
    }

    private static Map<String, BufferedImage> buildIconCache() {
        final Map<String, BufferedImage> cache = new HashMap<>();
        for (final String a : new String[]{"W", "B"})
            for (final String s : new String[]{"K", "Q", "R", "B", "N", "P"}) {
                final String key = a + s;
                try {
                    cache.put(key, ImageIO.read(new File("images/" + key + ".png")));
                } catch (final IOException e) {
                    System.err.println("TakenPiecesPanel: missing image " + key + ".png");
                }
            }
        return Collections.unmodifiableMap(cache);
    }

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

    // ── Public refresh ────────────────────────────────────────────────────

    /**
     * The panel to place above the board (shows Black player's captured pieces).
     */
    public JPanel getTopStrip() {
        return topStrip;
    }

    // ── Icon cache ────────────────────────────────────────────────────────

    /**
     * The panel to place below the board (shows White player's captured pieces).
     */
    public JPanel getBottomStrip() {
        return bottomStrip;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    public void redo(final MoveLog moveLog) {
        whiteTaken = new ArrayList<>();
        blackTaken = new ArrayList<>();

        for (final Move move : moveLog.getMoves()) {
            if (!move.isAttack()) continue;
            final Piece taken = move.getAttackedPiece();
            if (taken.getPieceAlliance().isBlack()) whiteTaken.add(taken);
            else blackTaken.add(taken);
        }

        whiteTaken.sort((a, b) -> Ints.compare(b.getPieceValue(), a.getPieceValue()));
        blackTaken.sort((a, b) -> Ints.compare(b.getPieceValue(), a.getPieceValue()));

        whiteMaterial = materialScore(whiteTaken);
        blackMaterial = materialScore(blackTaken);

        topStrip.repaint();
        bottomStrip.repaint();
    }

    // ── Inner strip panel ─────────────────────────────────────────────────

    private class Strip extends JPanel {

        private final boolean topBorder;

        Strip(final Color bg, final boolean topBorder) {
            this.topBorder = topBorder;
            setBackground(bg);
            setOpaque(true);
            setPreferredSize(new Dimension(0, STRIP_H));
        }

        /**
         * True when this strip renders White's captures (bottom strip).
         */
        private boolean isWhiteStrip() {
            return topBorder;
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);

            final List<Piece> pieces = isWhiteStrip() ? whiteTaken : blackTaken;
            final int material = isWhiteStrip() ? whiteMaterial : blackMaterial;
            final int opponent = isWhiteStrip() ? blackMaterial : whiteMaterial;
            final int advantage = material - opponent;

            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // ── Border line ───────────────────────────────────────────
            g2.setColor(BORDER_COL);
            if (topBorder) g2.drawLine(0, 0, getWidth(), 0);
            else g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

            // ── Piece icons ───────────────────────────────────────────
            final int iconY = (STRIP_H - PIECE_SIZE) / 2;
            int x = PAD_LEFT;
            for (final Piece piece : pieces) {
                final String key = String.valueOf(
                        piece.getPieceAlliance().toString().charAt(0)) + piece;
                final BufferedImage img = ICON_CACHE.get(key);
                if (img != null) {
                    g2.drawImage(img, x, iconY, PIECE_SIZE, PIECE_SIZE, null);
                    x += PIECE_SIZE - OVERLAP;
                }
            }

            // ── Advantage pill ────────────────────────────────────────
            if (advantage > 0) {
                x += OVERLAP + 4;
                final String text = "+" + advantage;
                g2.setFont(PILL_FONT);
                final FontMetrics fm = g2.getFontMetrics();
                final int pillW = fm.stringWidth(text) + PILL_PAD * 2;
                final int pillY = (STRIP_H - PILL_H) / 2;
                g2.setColor(ADV_PILL_BG);
                g2.fillRoundRect(x, pillY, pillW, PILL_H, PILL_ARC, PILL_ARC);
                g2.setColor(ADV_PILL_FG);
                g2.drawString(text, x + PILL_PAD,
                        pillY + (PILL_H - fm.getHeight()) / 2 + fm.getAscent());
            }

            g2.dispose();
        }
    }
}
