package com.chess.engine.gui;

import com.chess.engine.board.Move;
import com.chess.engine.board.MoveLog;
import com.chess.engine.pieces.Piece;
import com.google.common.primitives.Ints;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class TakenPiecesPanel extends JPanel {

    private static final int PIECE_SIZE = 16;
    private static final int OVERLAP = 4;
    private static final Map<String, BufferedImage> ICON_CACHE = buildIconCache();

    private final boolean isWhiteSide; // true = White player (shows black pieces captured by White)
    private List<Piece> takenPieces = new ArrayList<>();
    private int materialScore = 0;
    private int materialAdvantage = 0;

    public TakenPiecesPanel(boolean isWhiteSide) {
        this.isWhiteSide = isWhiteSide;
        setOpaque(false);
        setPreferredSize(new Dimension(100, 18));
    }

    private static Map<String, BufferedImage> buildIconCache() {
        final Map<String, BufferedImage> cache = new HashMap<>();
        for (final String a : new String[]{"W", "B"})
            for (final String s : new String[]{"K", "Q", "R", "B", "N", "P"}) {
                final String key = a + s;
                try (final java.io.InputStream is =
                             TakenPiecesPanel.class.getResourceAsStream("/images/" + key + ".png")) {
                    if (is == null) { System.err.println("TakenPiecesPanel: missing image " + key + ".png"); continue; }
                    cache.put(key, ImageIO.read(is));
                } catch (final IOException e) {
                    System.err.println("TakenPiecesPanel: missing image " + key + ".png");
                }
            }
        return Collections.unmodifiableMap(cache);
    }

    private static int calcMaterial(final List<Piece> pieces) {
        int total = 0;
        for (final Piece p : pieces) {
            final int v = p.getPieceType().getPieceValue();
            // King is not a capturable piece; skip it
            if (!p.getPieceType().isKing()) total += v;
        }
        return total;
    }

    public void redo(final MoveLog moveLog) {
        final List<Piece> whiteTaken = new ArrayList<>(); // black pieces White captured
        final List<Piece> blackTaken = new ArrayList<>(); // white pieces Black captured

        for (final Move move : moveLog.getMoves()) {
            if (!move.isAttack()) continue;
            final Piece taken = move.getAttackedPiece();
            if (taken.getPieceAlliance().isBlack()) whiteTaken.add(taken);
            else blackTaken.add(taken);
        }

        whiteTaken.sort((a, b) -> Ints.compare(b.getPieceValue(), a.getPieceValue()));
        blackTaken.sort((a, b) -> Ints.compare(b.getPieceValue(), a.getPieceValue()));

        final int whiteMat = calcMaterial(whiteTaken);
        final int blackMat = calcMaterial(blackTaken);

        if (isWhiteSide) {
            takenPieces = whiteTaken;
            materialScore = whiteMat;
            materialAdvantage = whiteMat - blackMat;
        } else {
            takenPieces = blackTaken;
            materialScore = blackMat;
            materialAdvantage = blackMat - whiteMat;
        }

        repaint();
    }

    public void clear() {
        takenPieces.clear();
        materialScore = 0;
        materialAdvantage = 0;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (takenPieces.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int x = 0;
        int y = (getHeight() - PIECE_SIZE) / 2;

        for (final Piece piece : takenPieces) {
            final String key = String.valueOf(piece.getPieceAlliance().toString().charAt(0)) + piece;
            final BufferedImage img = ICON_CACHE.get(key);
            if (img != null) {
                g2.drawImage(img, x, y, PIECE_SIZE, PIECE_SIZE, null);
                x += PIECE_SIZE - OVERLAP;
            }
        }

        // Draw +N advantage badge if this side has material lead
        if (materialAdvantage > 0) {
            x += OVERLAP + 4;
            String advText = "+" + materialAdvantage;
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int pillW = fm.stringWidth(advText) + 6;
            int pillH = 14;
            int pillY = (getHeight() - pillH) / 2;

            g2.setColor(UITheme.isDark() ? new Color(20, 50, 30) : new Color(220, 245, 225));
            g2.fillRoundRect(x, pillY, pillW, pillH, 6, 6);

            g2.setColor(UITheme.getActiveGreen());
            g2.drawString(advText, x + 3, pillY + fm.getAscent() + 1);
        }

        g2.dispose();
    }
}
