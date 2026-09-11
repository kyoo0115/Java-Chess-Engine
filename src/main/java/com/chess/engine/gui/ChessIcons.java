package com.chess.engine.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class ChessIcons {

    public static Icon getChessKnightIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                double s = size / 24.0;
                g2.scale(s, s);
                g2.setColor(color != null ? color : c.getForeground());

                // Stylized Chess Knight path
                Path2D p = new Path2D.Double();
                p.moveTo(19, 21);
                p.lineTo(5, 21);
                p.lineTo(6, 18);
                p.curveTo(6, 18, 5, 14, 7, 10);
                p.curveTo(8, 7, 10, 4, 14, 3);
                p.curveTo(15, 3, 16, 4, 15.5, 6);
                p.curveTo(17, 6, 19, 7.5, 19, 9);
                p.curveTo(19, 10, 18, 11, 16, 11.5);
                p.curveTo(17.5, 13, 18, 15, 18, 18);
                p.closePath();
                g2.fill(p);

                // Eye dot
                g2.setColor(UITheme.isDark() ? UITheme.DARK_APP_BG : Color.WHITE);
                g2.fillOval(12, 7, 2, 2);

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    public static Icon getPlayIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                double s = size / 24.0;
                g2.scale(s, s);
                g2.setColor(color != null ? color : c.getForeground());

                Path2D p = new Path2D.Double();
                p.moveTo(7, 5);
                p.lineTo(19, 12);
                p.lineTo(7, 19);
                p.closePath();
                g2.fill(p);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    public static Icon getAnalysisIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                double s = size / 24.0;
                g2.scale(s, s);
                g2.setColor(color != null ? color : c.getForeground());

                // 3 bar chart columns
                g2.fillRoundRect(4, 14, 3, 7, 2, 2);
                g2.fillRoundRect(10, 9, 3, 12, 2, 2);
                g2.fillRoundRect(16, 4, 3, 17, 2, 2);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    public static Icon getLearnIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                double s = size / 24.0;
                g2.scale(s, s);
                g2.setColor(color != null ? color : c.getForeground());

                // Open book shape
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D left = new Path2D.Double();
                left.moveTo(12, 6);
                left.curveTo(9, 4, 5, 4, 3, 5);
                left.lineTo(3, 19);
                left.curveTo(5, 18, 9, 18, 12, 20);
                left.closePath();
                g2.draw(left);

                Path2D right = new Path2D.Double();
                right.moveTo(12, 6);
                right.curveTo(15, 4, 19, 4, 21, 5);
                right.lineTo(21, 19);
                right.curveTo(19, 18, 15, 18, 12, 20);
                right.closePath();
                g2.draw(right);

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    public static Icon getSettingsIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                double s = size / 24.0;
                g2.scale(s, s);
                g2.setColor(color != null ? color : c.getForeground());

                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawOval(8, 8, 8, 8);

                for (int i = 0; i < 8; i++) {
                    double angle = i * Math.PI / 4;
                    int x1 = (int) (12 + 9 * Math.cos(angle));
                    int y1 = (int) (12 + 9 * Math.sin(angle));
                    int x2 = (int) (12 + 11.5 * Math.cos(angle));
                    int y2 = (int) (12 + 11.5 * Math.sin(angle));
                    g2.drawLine(x1, y1, x2, y2);
                }
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    public static Icon getGridIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                double s = size / 24.0;
                g2.scale(s, s);
                g2.setColor(color != null ? color : c.getForeground());

                g2.fillRect(4, 4, 7, 7);
                g2.fillRect(13, 4, 7, 7);
                g2.fillRect(4, 13, 7, 7);
                g2.fillRect(13, 13, 7, 7);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    public static Icon getExpandArrowIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                double s = size / 24.0;
                g2.scale(s, s);
                g2.setColor(color != null ? color : c.getForeground());

                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // Double-ended diagonal arrow
                g2.drawLine(5, 19, 19, 5);
                g2.drawLine(5, 14, 5, 19);
                g2.drawLine(5, 19, 10, 19);
                g2.drawLine(14, 5, 19, 5);
                g2.drawLine(19, 5, 19, 10);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    public static Icon getUserAvatarIcon(int size, boolean isWhite) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);

                // Background circle
                g2.setColor(UITheme.isDark() ? new Color(38, 48, 68) : new Color(240, 243, 248));
                g2.fillOval(0, 0, size, size);

                // User silhouette
                double s = size / 48.0;
                g2.scale(s, s);
                g2.setColor(isWhite
                        ? (UITheme.isDark() ? new Color(180, 190, 210) : new Color(100, 110, 130))
                        : (UITheme.isDark() ? new Color(140, 150, 170) : new Color(50, 55, 70)));

                // Head
                g2.fillOval(17, 10, 14, 14);

                // Shoulders
                Path2D shoulders = new Path2D.Double();
                shoulders.moveTo(10, 38);
                shoulders.curveTo(10, 28, 38, 28, 38, 38);
                shoulders.closePath();
                g2.fill(shoulders);

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }
}
