package com.chess.engine.gui;

import com.chess.engine.board.Board;
import com.chess.engine.util.BoardUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static javax.swing.SwingUtilities.isRightMouseButton;

public class BoardPanel extends JPanel {

    private static final Font COORD_FONT = new Font("SansSerif", Font.BOLD, 11);
    private static final Color ANNOTATION_COLOR = new Color(50, 200, 50, 200);

    final List<TilePanel> boardTiles;
    private final TableContext ctx;

    // Callbacks into Table
    private final Runnable onRebuildCaches;
    private final BiConsumer<Integer, Integer> onMove;
    private final Runnable onRightClick;
    private final Consumer<Integer> onHover;

    // Right-click annotations
    private final List<int[]> annotations = new ArrayList<>();
    private int rcPressId = -1;

    public BoardPanel(final TableContext ctx,
                      final Runnable onRebuildCaches,
                      final BiConsumer<Integer, Integer> onMove,
                      final Runnable onRightClick,
                      final Consumer<Integer> onHover) {
        super(new GridLayout(8, 8));
        this.ctx = ctx;
        this.onRebuildCaches = onRebuildCaches;
        this.onMove = onMove;
        this.onRightClick = onRightClick;
        this.onHover = onHover;

        setOpaque(false);

        boardTiles = new ArrayList<>();
        for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
            final TilePanel tp = new TilePanel(i, ctx);
            boardTiles.add(tp);
            add(tp);
        }

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                onRebuildCaches.run();
                drawBoard(ctx.getChessBoard());
            }
        });
        installMouseListener();
        validate();
    }

    public void drawBoard(final Board board) {
        removeAll();
        for (final TilePanel tp : ctx.getBoardDirection().traverse(boardTiles)) {
            tp.drawTile(board);
            add(tp);
        }
        validate();
        repaint();
    }

    public int tileIdAtPoint(final Point p) {
        if (getWidth() == 0 || getHeight() == 0) return -1;
        int col = p.x * 8 / getWidth();
        int row = p.y * 8 / getHeight();
        col = Math.max(0, Math.min(7, col));
        row = Math.max(0, Math.min(7, row));
        if (ctx.getBoardDirection() == BoardDirection.FLIPPED) {
            col = 7 - col;
            row = 7 - row;
        }
        return row * 8 + col;
    }

    public void clearAnnotations() {
        annotations.clear();
    }

    private void installMouseListener() {
        final MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isRightMouseButton(e)) {
                    rcPressId = tileIdAtPoint(e.getPoint());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!isRightMouseButton(e)) return;
                final int releaseId = tileIdAtPoint(e.getPoint());
                if (rcPressId >= 0) {
                    final int from = rcPressId;
                    final int to = releaseId;
                    boolean removed = annotations.removeIf(a -> a[0] == from && a[1] == to);
                    if (!removed) annotations.add(new int[]{from, to});
                    rcPressId = -1;
                    repaint();
                    return;
                }
                rcPressId = -1;
                onRightClick.run();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                for (final TilePanel tp : boardTiles) tp.refreshDots(ctx.getChessBoard());
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                onHover.accept(tileIdAtPoint(e.getPoint()));
                for (final TilePanel tp : boardTiles) tp.refreshDots(ctx.getChessBoard());
                repaint();
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Drag ghost
        final BufferedImage drag = ctx.getDragImage();
        final Point dragPt = ctx.getDragPoint();
        if (drag != null && dragPt != null) {
            final int tileW = getWidth() / 8;
            final int tileH = getHeight() / 8;
            g2.drawImage(drag, dragPt.x - tileW / 2, dragPt.y - tileH / 2, tileW, tileH, this);
        }

        // AI piece animation
        final BufferedImage anim = ctx.getAnimPiece();
        if (anim != null) {
            final int tileW = getWidth() / 8;
            final int tileH = getHeight() / 8;
            final float t = 1f - (1f - ctx.getAnimProgress()) * (1f - ctx.getAnimProgress());
            final int px = Math.round(ctx.getAnimFromX() + (ctx.getAnimToX() - ctx.getAnimFromX()) * t);
            final int py = Math.round(ctx.getAnimFromY() + (ctx.getAnimToY() - ctx.getAnimFromY()) * t);
            g2.drawImage(anim, px, py, tileW, tileH, this);
        }

        // Move arrow overlay
        if (ctx.getArrowSource() >= 0 && ctx.getArrowDest() >= 0) {
            paintMoveArrow(g2, ctx.getArrowSource(), ctx.getArrowDest());
        }

        // Right-click annotations
        g2.setColor(ANNOTATION_COLOR);
        for (final int[] ann : annotations) {
            if (ann[0] == ann[1]) paintAnnotationCircle(g2, ann[0]);
            else paintAnnotationArrow(g2, ann[0], ann[1]);
        }

        g2.dispose();
    }

    private void paintMoveArrow(final Graphics2D g2, final int fromId, final int toId) {
        final int tw = getWidth() / 8;
        final int th = getHeight() / 8;

        final int fromDisplay = (ctx.getBoardDirection() == BoardDirection.FLIPPED) ? (63 - fromId) : fromId;
        final int toDisplay = (ctx.getBoardDirection() == BoardDirection.FLIPPED) ? (63 - toId) : toId;

        final int x1 = (fromDisplay % 8) * tw + tw / 2;
        final int y1 = (fromDisplay / 8) * th + th / 2;
        final int x2 = (toDisplay % 8) * tw + tw / 2;
        final int y2 = (toDisplay / 8) * th + th / 2;

        final double angle = Math.atan2(y2 - y1, x2 - x1);
        final double cos = Math.cos(angle);
        final double sin = Math.sin(angle);

        // Gold arrow color from reference image (#f5a623 / #fac858 with transparency)
        g2.setColor(UITheme.getArrowColor());

        final int shaftW = Math.max(3, tw / 9);
        final int headLen = Math.max(10, tw / 3);
        final int headW = Math.max(8, tw / 3);
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

        final int[] headXs = {x2, (int) (sx2 - headW * sin), (int) (sx2 + headW * sin)};
        final int[] headYs = {y2, (int) (sy2 + headW * cos), (int) (sy2 - headW * cos)};
        g2.fillPolygon(headXs, headYs, 3);
    }

    private void paintAnnotationCircle(final Graphics2D g2, final int tileId) {
        final int tw = getWidth() / 8;
        final int th = getHeight() / 8;
        final int disp = (ctx.getBoardDirection() == BoardDirection.FLIPPED) ? (63 - tileId) : tileId;
        final int cx = (disp % 8) * tw + tw / 2;
        final int cy = (disp / 8) * th + th / 2;
        final int r = Math.min(tw, th) * 2 / 5;
        final int stroke = Math.max(2, Math.min(tw, th) / 12);
        final Graphics2D g3 = (Graphics2D) g2.create();
        g3.setStroke(new BasicStroke(stroke));
        g3.drawOval(cx - r, cy - r, r * 2, r * 2);
        g3.dispose();
    }

    private void paintAnnotationArrow(final Graphics2D g2, final int fromId, final int toId) {
        final int tw = getWidth() / 8;
        final int th = getHeight() / 8;
        final int fromDisp = (ctx.getBoardDirection() == BoardDirection.FLIPPED) ? (63 - fromId) : fromId;
        final int toDisp = (ctx.getBoardDirection() == BoardDirection.FLIPPED) ? (63 - toId) : toId;

        final int x1 = (fromDisp % 8) * tw + tw / 2;
        final int y1 = (fromDisp / 8) * th + th / 2;
        final int x2 = (toDisp % 8) * tw + tw / 2;
        final int y2 = (toDisp / 8) * th + th / 2;

        final double angle = Math.atan2(y2 - y1, x2 - x1);
        final double cos = Math.cos(angle);
        final double sin = Math.sin(angle);

        final int shaftW = Math.max(2, tw / 10);
        final int headLen = Math.max(8, tw / 4);
        final int headW = Math.max(5, tw / 4);
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

        final int[] headXs = {x2, (int) (sx2 - headW * sin), (int) (sx2 + headW * sin)};
        final int[] headYs = {y2, (int) (sy2 + headW * cos), (int) (sy2 - headW * cos)};
        g2.fillPolygon(headXs, headYs, 3);
    }
}
