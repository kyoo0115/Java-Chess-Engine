package com.chess.engine.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class LeftSidebar extends JPanel {

    private final JLabel statusDot;
    private final JLabel statusText;
    private final List<NavButton> navButtons = new ArrayList<>();
    private int selectedNavIndex = 0; // 0 = Play

    public LeftSidebar(
            Runnable onPlayClicked,
            Runnable onAnalysisClicked,
            Runnable onLearnClicked,
            Runnable onSettingsClicked
    ) {
        super(new BorderLayout());
        setPreferredSize(new Dimension(180, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 16, 20, 16));

        // Top Section: App Logo + Navigation Items
        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        // Logo & Title: [Knight Icon] Chess
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoPanel.setOpaque(false);
        logoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel logoIcon = new JLabel(ChessIcons.getChessKnightIcon(28, UITheme.getAccentBlue()));
        JLabel logoTitle = new JLabel("Chess");
        logoTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        logoTitle.setForeground(UITheme.getTextPrimary());
        logoPanel.add(logoIcon);
        logoPanel.add(logoTitle);

        topSection.add(logoPanel);
        topSection.add(Box.createVerticalStrut(28));

        // Navigation buttons
        NavButton btnPlay = new NavButton(ChessIcons.getPlayIcon(18, null), "Play", 0, onPlayClicked);
        NavButton btnAnalysis = new NavButton(ChessIcons.getAnalysisIcon(18, null), "Analysis", 1, onAnalysisClicked);
        NavButton btnLearn = new NavButton(ChessIcons.getLearnIcon(18, null), "Learn", 2, onLearnClicked);
        NavButton btnSettings = new NavButton(ChessIcons.getSettingsIcon(18, null), "Settings", 3, onSettingsClicked);

        navButtons.add(btnPlay);
        navButtons.add(btnAnalysis);
        navButtons.add(btnLearn);
        navButtons.add(btnSettings);

        for (NavButton btn : navButtons) {
            topSection.add(btn);
            topSection.add(Box.createVerticalStrut(6));
        }

        add(topSection, BorderLayout.NORTH);

        // Bottom Section: Status Indicator [User Icon] [● WHITE to move]
        JPanel bottomSection = new JPanel(new BorderLayout(8, 0));
        bottomSection.setOpaque(false);

        JLabel userIcon = new JLabel(ChessIcons.getUserAvatarIcon(24, true));

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        statusRow.setOpaque(false);

        statusDot = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.getActiveGreen());
                g2.fillOval(0, (getHeight() - 8) / 2, 8, 8);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(8, 20);
            }
        };

        statusText = new JLabel("WHITE to move");
        statusText.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusText.setForeground(UITheme.getTextSecondary());

        statusRow.add(statusDot);
        statusRow.add(statusText);

        bottomSection.add(userIcon, BorderLayout.WEST);
        bottomSection.add(statusRow, BorderLayout.CENTER);

        add(bottomSection, BorderLayout.SOUTH);

        UITheme.addThemeListener(() -> {
            logoTitle.setForeground(UITheme.getTextPrimary());
            statusText.setForeground(UITheme.getTextSecondary());
            for (NavButton b : navButtons) b.repaint();
            repaint();
        });
    }

    public void updateStatus(String status) {
        statusText.setText(status);
        repaint();
    }

    public void setSelectedNav(int idx) {
        this.selectedNavIndex = idx;
        for (NavButton b : navButtons) b.repaint();
    }

    private class NavButton extends JPanel {
        private final Icon icon;
        private final String text;
        private final int index;
        private boolean hover = false;

        NavButton(Icon icon, String text, int index, Runnable onClick) {
            super(new FlowLayout(FlowLayout.LEFT, 12, 10));
            this.icon = icon;
            this.text = text;
            this.index = index;

            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(160, 44));
            setPreferredSize(new Dimension(160, 44));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel iconLbl = new JLabel(icon);
            JLabel textLbl = new JLabel(text);
            textLbl.setFont(new Font("SansSerif", Font.BOLD, 14));

            add(iconLbl);
            add(textLbl);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    setSelectedNav(index);
                    if (onClick != null) onClick.run();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean selected = (index == selectedNavIndex);
            if (selected) {
                g2.setColor(UITheme.getAccentActiveBg());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } else if (hover) {
                g2.setColor(UITheme.getCardBorder());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }

            g2.dispose();
            super.paintComponent(g);

            // Update text color
            Component[] comps = getComponents();
            if (comps.length > 1 && comps[1] instanceof JLabel) {
                ((JLabel) comps[1]).setForeground(selected ? UITheme.getAccentBlue() : UITheme.getTextSecondary());
            }
        }
    }
}
