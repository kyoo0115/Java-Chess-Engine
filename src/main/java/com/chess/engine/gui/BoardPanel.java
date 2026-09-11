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

/**
 * The 8×8 board panel. Delegates all rendering state reads to {@link TableContext}.
 * Move events are reported back to Table via callbacks.
 */
class BoardPanel extends JPanel {

    private static final Font COORD_FONT = new Font("SansSerif", Font.BOLD, 10);
    private static final Color ANNOTATION_COLOR = new Color(50, 200, 50, 200);

    final List<TilePanel> boardTiles;
    private final TableContext ctx;

    // Callbacks into Table
    private final Runnable onRebuildCaches;
    private final BiConsumer<Integer, Integer> onMove;   // fromId, toId
    private final Runnable onRightClick;
    private final Consumer<Integer> onHover;             // hovered tileId

    // Right-click annotations: each int[2] = {fromId, toId}; fromId==toId means circle
    private final List<int[]> annotations = new ArrayList<>();
    private int rcPressId = -1;  // tile pressed on right-click (for drag detection)

    BoardPanel(final TableContext ctx,
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

    // Allow external (Table) press handling by exposing tileIdAtPoint publicly
    // The drag-point update on mouseDragged is handled here too
    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        // gameOver / click-to-move handled entirely by callbacks
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

    int tileIdAtPoint(final Point p) {
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

    void clearAnnotations() {
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
                    // Toggle: remove existing annotation with same coordinates, or add new one
                    final int from = rcPressId;
                    final int to = releaseId;
                    boolean removed = annotations.removeIf(a -> a[0] == from && a[1] == to);
                    if (!removed) annotations.add(new int[]{from, to});
                    rcPressId = -1;
                    repaint();
                    // Don't propagate to Table's right-click handler when annotating
                    return;
                }
                rcPressId = -1;
                onRightClick.run();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
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

        // ── Drag ghost ────────────────────────────────────────────────
        final BufferedImage drag = ctx.getDragImage();
        final Point dragPt = ctx.getDragPoint();
        if (drag != null && dragPt != null) {
            final int tileW = getWidth() / 8;
            final int tileH = getHeight() / 8;
            g2.drawImage(drag, dragPt.x - tileW / 2, dragPt.y - tileH / 2, tileW, tileH, this);
        }

        // ── AI piece animation ────────────────────────────────────────
        final BufferedImage anim = ctx.getAnimPiece();
        if (anim != null) {
            final int tileW = getWidth() / 8;
            final int tileH = getHeight() / 8;
            final float t = 1f - (1f - ctx.getAnimProgress()) * (1f - ctx.getAnimProgress());
            final int px = Math.round(ctx.getAnimFromX() + (ctx.getAnimToX() - ctx.getAnimFromX()) * t);
            final int py = Math.round(ctx.getAnimFromY() + (ctx.getAnimToY() - ctx.getAnimFromY()) * t);
            g2.drawImage(anim, px, py, tileW, tileH, this);
        }

        // ── Move arrow ────────────────────────────────────────────────
        if (ctx.getArrowSource() >= 0 && ctx.getArrowDest() >= 0)
            paintMoveArrow(g2, ctx.getArrowSource(), ctx.getArrowDest());

        // ── Right-click annotations ───────────────────────────────────
        g2.setColor(ANNOTATION_COLOR);
        for (final int[] ann : annotations) {
            if (ann[0] == ann[1]) paintAnnotationCircle(g2, ann[0]);
            else paintAnnotationArrow(g2, ann[0], ann[1]);
        }

        // ── Coordinate labels ─────────────────────────────────────────
        if (ctx.isShowCoordinates()) paintCoordinates(g2);

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

        g2.setColor(new Color(235, 165, 25, 185));

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

    private void paintCoordinates(final Graphics2D g2) {
        final int w = getWidth();
        final int h = getHeight();
        final int tw = w / 8;
        final int th = h / 8;

        g2.setFont(COORD_FONT);
        final FontMetrics fm = g2.getFontMetrics();

        final String[] files = {"a", "b", "c", "d", "e", "f", "g", "h"};
        final String[] ranks = {"8", "7", "6", "5", "4", "3", "2", "1"};

        final String[] fLabels = ctx.getBoardDirection() == BoardDirection.FLIPPED
                ? new String[]{"h", "g", "f", "e", "d", "c", "b", "a"} : files;
        final String[] rLabels = ctx.getBoardDirection() == BoardDirection.FLIPPED
                ? new String[]{"1", "2", "3", "4", "5", "6", "7", "8"} : ranks;

        for (int i = 0; i < 8; i++) {
            final String fl = fLabels[i];
            final int fx = i * tw + tw - fm.stringWidth(fl) - 3;
            final int fy = h - 3;
            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawString(fl, fx + 1, fy + 1);
            g2.setColor(isLightTileAt(i, 7) ? ctx.getBoardTheme().dark : ctx.getBoardTheme().light);
            g2.drawString(fl, fx, fy);

            final String rl = rLabels[i];
            final int rx = 3;
            final int ry = i * th + fm.getAscent() + 2;
            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawString(rl, rx + 1, ry + 1);
            g2.setColor(isLightTileAt(0, i) ? ctx.getBoardTheme().dark : ctx.getBoardTheme().light);
            g2.drawString(rl, rx, ry);
        }
    }

    private boolean isLightTileAt(int col, int row) {
        return (col + row) % 2 == 0;
    }
}
