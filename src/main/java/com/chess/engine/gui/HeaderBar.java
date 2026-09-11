package com.chess.engine.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class HeaderBar extends JPanel {

    private final JPanel centerPillsPanel;
    private final Consumer<Integer> onTabChanged;
    private int selectedTab = 0; // 0 = Game, 1 = Analysis, 2 = Settings

    public HeaderBar(Consumer<Integer> onTabChanged, Runnable onThemeToggle) {
        super(new BorderLayout());
        this.onTabChanged = onTabChanged;

        setOpaque(false);
        setBorder(new EmptyBorder(12, 16, 12, 20));

        // Center Pill Tabs: [ Game ] [ Analysis ] [ Settings ]
        centerPillsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        centerPillsPanel.setOpaque(false);

        centerPillsPanel.add(makeTabPill("Game", 0));
        centerPillsPanel.add(makeTabPill("Analysis", 1));
        centerPillsPanel.add(makeTabPill("Settings", 2));

        add(centerPillsPanel, BorderLayout.CENTER);

        // Right quick controls: Light/Dark Mode Switch pill + Window mock indicators
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);

        // Theme Toggle Button
        JButton themeBtn = new JButton(UITheme.isDark() ? "☀️ Light" : "🌙 Dark") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.getControlBtnBg());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(UITheme.getControlBtnBorder());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        themeBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        themeBtn.setForeground(UITheme.getTextSecondary());
        themeBtn.setFocusPainted(false);
        themeBtn.setContentAreaFilled(false);
        themeBtn.setBorderPainted(false);
        themeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeBtn.addActionListener(e -> {
            if (UITheme.isDark()) {
                UITheme.setMode(UITheme.Mode.LIGHT);
                themeBtn.setText("🌙 Dark");
            } else {
                UITheme.setMode(UITheme.Mode.DARK);
                themeBtn.setText("☀️ Light");
            }
            if (onThemeToggle != null) onThemeToggle.run();
        });

        rightPanel.add(themeBtn);

        add(rightPanel, BorderLayout.EAST);

        UITheme.addThemeListener(() -> {
            themeBtn.setForeground(UITheme.getTextSecondary());
            centerPillsPanel.repaint();
            repaint();
        });
    }

    private JComponent makeTabPill(String text, int index) {
        JLabel pill = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean active = (index == selectedTab);
                if (active) {
                    g2.setColor(UITheme.getAccentActiveBg());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(new Font("SansSerif", Font.BOLD, 13));
        pill.setPreferredSize(new Dimension(84, 32));
        pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pill.setForeground(index == selectedTab ? UITheme.getAccentBlue() : UITheme.getTextSecondary());

        pill.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setSelectedTab(index);
                if (onTabChanged != null) onTabChanged.accept(index);
            }
        });

        return pill;
    }

    public void setSelectedTab(int index) {
        this.selectedTab = index;
        Component[] comps = centerPillsPanel.getComponents();
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] instanceof JLabel) {
                comps[i].setForeground(i == selectedTab ? UITheme.getAccentBlue() : UITheme.getTextSecondary());
            }
        }
        centerPillsPanel.repaint();
    }
}
