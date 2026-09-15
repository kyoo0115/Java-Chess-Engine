package com.chess.engine.gui;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.pieces.Piece;
import com.chess.engine.util.BoardUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * A single tile on the board. Reads rendering state from {@link TableContext}.
 */
class TilePanel extends JPanel {

    private static final Dimension TILE_PANEL_DIMENSION = new Dimension(10, 10);

    private final int tileId;
    private final TableContext ctx;
    // Permanent label — icon is updated in-place instead of removeAll()+add() each draw.
    private final JLabel pieceLabel = new JLabel();
    private boolean showLegalDot = false;
    private boolean isCaptureDot = false;
    private boolean showCheckOverlay = false;
    // Cached stroke — recomputed only when tile width changes, not on every paint.
    private int lastStrokeWidth = -1;
    private BasicStroke cachedStroke;

    TilePanel(final int tileId, final TableContext ctx) {
        super(new GridBagLayout());
        this.tileId = tileId;
        this.ctx = ctx;
        setPreferredSize(TILE_PANEL_DIMENSION);
        setOpaque(true);
        add(pieceLabel);
    }

    void drawTile(final Board board) {
        assignTileColor();
        assignTilePieceIcon(board);
        highlightLastMove();
        highlightSelected();
        highlightCheck(board);
        highlightLegals(board);
    }

    void refreshDots(final Board board) {
        highlightLegals(board);
        repaint();
    }

    private void assignTileColor() {
        setBackground(isLightTile() ? ctx.getBoardTheme().light : ctx.getBoardTheme().dark);
    }

    private boolean isLightTile() {
        if (BoardUtils.isEighthRank(tileId) || BoardUtils.isSixthRank(tileId)
                || BoardUtils.isFourthRank(tileId) || BoardUtils.isSecondRank(tileId))
            return tileId % 2 == 0;
        return tileId % 2 != 0;
    }

    private void highlightLastMove() {
        if (tileId == ctx.getLastMoveSource() || tileId == ctx.getLastMoveDest())
            setBackground(isLightTile() ? ctx.getBoardTheme().lastLight : ctx.getBoardTheme().lastDark);
    }

    private void highlightSelected() {
        if ((ctx.getSourceTile() != null && tileId == ctx.getSourceTile().getTileCoordinate())
                || tileId == ctx.getDragSourceTileId())
            setBackground(ctx.getBoardTheme().selected);
    }

    private void highlightCheck(final Board board) {
        showCheckOverlay = board.getCurrentPlayer().isInCheck()
                && tileId == board.getCurrentPlayer().getPlayerKing().getPiecePosition();
    }

    private void highlightLegals(final Board board) {
        showLegalDot = false;
        isCaptureDot = false;

        final boolean dragActive = ctx.isHighlightLegalMoves()
                && ctx.getHumanMovedPiece() != null
                && ctx.getDragSourceTileId() >= 0;
        final boolean hoverActive = ctx.isHoverHighlight()
                && ctx.getHumanMovedPiece() != null
                && ctx.getHoverTileId() >= 0
                && ctx.getDragSourceTileId() < 0;
        if (!dragActive && !hoverActive) return;

        for (final Move move : legalMovesOfSelected(board)) {
            if (move.getDestinationCoordinate() != tileId) continue;
            showLegalDot = true;
            isCaptureDot = move.isAttack();
            break;
        }
    }

    private Collection<Move> legalMovesOfSelected(final Board board) {
        final Piece p = ctx.getHumanMovedPiece();
        if (p == null || p.getPieceAlliance() != board.getCurrentPlayer().getAlliance())
            return Collections.emptyList();
        return board.getCurrentPlayer().getLegalMoves().stream()
                .filter(m -> m.getMovedPiece().equals(p))
                .toList();
    }

    private void assignTilePieceIcon(final Board board) {
        if (tileId == ctx.getDragSourceTileId() || !board.getTile(tileId).isTileOccupied()) {
            pieceLabel.setIcon(null);
            return;
        }
        final Piece piece = board.getTile(tileId).getPiece();
        final String key = piece.getPieceAlliance().toString().charAt(0) + piece.toString();
        final Map<String, BufferedImage> scaled = ctx.getScaledImageCache();
        final Map<String, BufferedImage> raw = ctx.getRawImageCache();
        final BufferedImage img = scaled.getOrDefault(key, raw.get(key));
        pieceLabel.setIcon(img != null ? new ImageIcon(img) : null);
    }

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
            final int strokeWidth = Math.max(3, w / 12);
            // Reuse cached stroke unless tile size changed
            if (strokeWidth != lastStrokeWidth) {
                cachedStroke = new BasicStroke(strokeWidth);
                lastStrokeWidth = strokeWidth;
            }
            g2.setColor(new Color(0, 0, 0, 90));
            g2.setStroke(cachedStroke);
            final int inset = strokeWidth / 2 + 1;
            g2.drawOval(inset, inset, w - inset * 2, h - inset * 2);
        } else {
            final int r = w / 8;
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillOval(w / 2 - r, h / 2 - r, r * 2, r * 2);
        }
    }
}
