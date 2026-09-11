package com.chess.engine.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class ToggleSwitch extends JComponent {

    private boolean selected;
    private Consumer<Boolean> onToggle;
    private float animPos = 0f;
    private Timer animTimer;

    public ToggleSwitch(boolean initial, Consumer<Boolean> onToggle) {
        this.selected = initial;
        this.onToggle = onToggle;
        this.animPos = initial ? 1.0f : 0.0f;
        setPreferredSize(new Dimension(42, 24));
        setMinimumSize(new Dimension(42, 24));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setSelected(!selected);
                if (ToggleSwitch.this.onToggle != null) {
                    ToggleSwitch.this.onToggle.accept(selected);
                }
            }
        });
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            startAnimation();
        }
    }

    private void startAnimation() {
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        final float target = selected ? 1.0f : 0.0f;
        animTimer = new Timer(15, e -> {
            if (Math.abs(animPos - target) < 0.08f) {
                animPos = target;
                animTimer.stop();
            } else {
                animPos += (target - animPos) * 0.35f;
            }
            repaint();
        });
        animTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int trackW = 40;
        int trackH = 22;
        int x = (w - trackW) / 2;
        int y = (h - trackH) / 2;

        Color offCol = UITheme.getToggleTrackOff();
        Color onCol = UITheme.getToggleTrackOn();
        int r = (int) (offCol.getRed() + (onCol.getRed() - offCol.getRed()) * animPos);
        int gr = (int) (offCol.getGreen() + (onCol.getGreen() - offCol.getGreen()) * animPos);
        int b = (int) (offCol.getBlue() + (onCol.getBlue() - offCol.getBlue()) * animPos);
        g2.setColor(new Color(r, gr, b));
        g2.fillRoundRect(x, y, trackW, trackH, trackH, trackH);

        // Knob
        int knobSize = trackH - 4;
        int knobMinX = x + 2;
        int knobMaxX = x + trackW - 2 - knobSize;
        int knobX = knobMinX + (int) ((knobMaxX - knobMinX) * animPos);
        int knobY = y + 2;

        g2.setColor(Color.WHITE);
        g2.fillOval(knobX, knobY, knobSize, knobSize);

        g2.dispose();
    }
}
