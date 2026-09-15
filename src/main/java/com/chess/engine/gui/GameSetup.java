package com.chess.engine.gui;

import com.chess.engine.Alliance;
import com.chess.engine.PlayerType;
import com.chess.engine.player.Player;
import com.chess.engine.player.ai.StockfishEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class GameSetup extends JDialog {

    private static final String[] DIFFICULTY_LABELS = {"Easy", "Medium", "Hard", "Master", "Custom"};
    /**
     * Movetime in ms for each difficulty preset. -1 = custom.
     */
    private static final int[] DIFFICULTY_MOVETIMES = {100, 500, 2000, 5000, -1};

    private static final int[] PRESET_MINUTES = {1, 3, 5, 10, 15, 30, -1};
    private static final String[] TIME_LABELS = {"1 min", "3 min", "5 min", "10 min", "15 min", "30 min", "Custom"};

    private final List<JLabel> diffPills = new ArrayList<>();
    private final List<JLabel> timePills = new ArrayList<>();
    private final JSpinner customDepthSpinner;
    private final JPanel customSpinnerRow;
    private final JSpinner customMinutesSpinner;
    private final JPanel customMinutesRow;
    private final ToggleSwitch clockEnabledSwitch;

    private PlayerType whitePlayerType = PlayerType.HUMAN;
    private PlayerType blackPlayerType = PlayerType.COMPUTER;

    private int selectedMinutes = 10;
    private boolean isCustomTime = false;
    private int selectedWhiteTypeIdx = 0; // 0 = Human, 1 = AI Engine
    private int selectedBlackTypeIdx = 1; // 0 = Human, 1 = AI Engine
    private int selectedDiffIdx = 1;     // 1 = Medium (Depth 4)

    public GameSetup(final JFrame frame, final boolean modal) {
        super(frame, "Game Settings", modal);
        setUndecorated(true);
        setResizable(false);
        setBackground(new Color(0, 0, 0, 0));

        // Window drag support
        final Point[] dragPoint = new Point[1];

        // Outer container with rounded border and clean shadow aesthetic
        final JPanel outerCard = new JPanel(new BorderLayout(0, 18)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.getCardBg());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(UITheme.getCardBorder());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 23, 23);
                g2.dispose();
            }
        };
        outerCard.setOpaque(false);
        outerCard.setBorder(new EmptyBorder(22, 26, 24, 26));

        // Title bar (Draggable + Close 'X' button)
        final JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);

        final JPanel titleLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleLeft.setOpaque(false);
        final JLabel logoIcon = new JLabel(ChessIcons.getSettingsIcon(20, UITheme.getAccentBlue()));
        final JLabel titleLabel = new JLabel("Game & AI Settings");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        titleLabel.setForeground(UITheme.getTextPrimary());
        titleLeft.add(logoIcon);
        titleLeft.add(titleLabel);
        titleBar.add(titleLeft, BorderLayout.WEST);

        final JLabel closeBtn = new JLabel("✕", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getForeground() == UITheme.getAccentBlue()) {
                    g2.setColor(UITheme.getAccentActiveBg());
                    g2.fillOval(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        closeBtn.setPreferredSize(new Dimension(28, 28));
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        closeBtn.setForeground(UITheme.getTextMuted());
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(UITheme.getAccentBlue());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(UITheme.getTextMuted());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                setVisible(false);
            }
        });
        titleBar.add(closeBtn, BorderLayout.EAST);

        // Window drag handler
        titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragPoint[0] = e.getPoint();
            }
        });
        titleBar.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragPoint[0] != null) {
                    Point curr = getLocation();
                    setLocation(curr.x + e.getX() - dragPoint[0].x, curr.y + e.getY() - dragPoint[0].y);
                }
            }
        });

        outerCard.add(titleBar, BorderLayout.NORTH);

        // Center Content Body
        final JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setOpaque(false);

        // 1. Player Sides Card (White & Black)
        final JPanel playersCard = createCardContainer();
        playersCard.setLayout(new GridLayout(1, 2, 16, 0));
        playersCard.add(createPlayerColumn("White Player", true));
        playersCard.add(createPlayerColumn("Black Player", false));
        bodyPanel.add(playersCard);
        bodyPanel.add(Box.createVerticalStrut(12));

        // 2. AI Difficulty Card
        final JPanel diffCard = createCardContainer();
        diffCard.setLayout(new BorderLayout(0, 10));

        final JLabel diffHeader = new JLabel("AI Difficulty Level");
        diffHeader.setFont(new Font("SansSerif", Font.BOLD, 13));
        diffHeader.setForeground(UITheme.getTextSecondary());
        diffCard.add(diffHeader, BorderLayout.NORTH);

        final JPanel diffPillsRow = new JPanel(new GridLayout(1, 5, 6, 0));
        diffPillsRow.setOpaque(false);

        for (int idx = 0; idx < DIFFICULTY_LABELS.length; idx++) {
            final JLabel pill = makeDifficultyPill(DIFFICULTY_LABELS[idx], idx);
            diffPills.add(pill);
            diffPillsRow.add(pill);
        }
        diffCard.add(diffPillsRow, BorderLayout.CENTER);

        // Custom search depth row (shown only when 'Custom' is selected)
        customDepthSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 30000, 100));
        styleSpinner(customDepthSpinner);
        customSpinnerRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        customSpinnerRow.setOpaque(false);
        final JLabel depthLbl = new JLabel("Custom Think Time (ms):");
        depthLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        depthLbl.setForeground(UITheme.getTextSecondary());
        customSpinnerRow.add(depthLbl);
        customSpinnerRow.add(customDepthSpinner);
        customSpinnerRow.setVisible(false);
        diffCard.add(customSpinnerRow, BorderLayout.SOUTH);

        bodyPanel.add(diffCard);
        bodyPanel.add(Box.createVerticalStrut(12));

        // 3. Time Control Card
        final JPanel clockCard = createCardContainer();
        clockCard.setLayout(new BorderLayout(0, 12));

        final JPanel clockHeader = new JPanel(new BorderLayout());
        clockHeader.setOpaque(false);
        final JLabel clockTitle = new JLabel("Clock & Time Control");
        clockTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        clockTitle.setForeground(UITheme.getTextSecondary());
        clockHeader.add(clockTitle, BorderLayout.WEST);

        final JPanel timePillsRow = new JPanel(new GridLayout(1, PRESET_MINUTES.length, 6, 0));
        timePillsRow.setOpaque(false);

        for (int i = 0; i < PRESET_MINUTES.length; i++) {
            final int mins = PRESET_MINUTES[i];
            final JLabel pill = makeTimePill(TIME_LABELS[i], mins);
            timePills.add(pill);
            timePillsRow.add(pill);
        }

        // Custom minutes row (shown only when 'Custom' is selected)
        customMinutesSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 180, 1));
        styleSpinner(customMinutesSpinner);
        customMinutesRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        customMinutesRow.setOpaque(false);
        final JLabel minsLbl = new JLabel("Custom Minutes per player:");
        minsLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        minsLbl.setForeground(UITheme.getTextSecondary());
        customMinutesRow.add(minsLbl);
        customMinutesRow.add(customMinutesSpinner);
        customMinutesRow.setVisible(false);

        clockEnabledSwitch = new ToggleSwitch(true, enabled -> {
            timePillsRow.setVisible(enabled);
            customMinutesRow.setVisible(enabled && isCustomTime);
            pack();
        });
        clockHeader.add(clockEnabledSwitch, BorderLayout.EAST);
        clockCard.add(clockHeader, BorderLayout.NORTH);

        clockCard.add(timePillsRow, BorderLayout.CENTER);
        clockCard.add(customMinutesRow, BorderLayout.SOUTH);
        bodyPanel.add(clockCard);

        outerCard.add(bodyPanel, BorderLayout.CENTER);

        // Bottom Action Buttons
        final JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setOpaque(false);

        final JButton btnCancel = makeActionButton("Cancel", false, () -> setVisible(false));
        final JButton btnSave = makeActionButton("Start Game", true, () -> {
            whitePlayerType = selectedWhiteTypeIdx == 1 ? PlayerType.COMPUTER : PlayerType.HUMAN;
            blackPlayerType = selectedBlackTypeIdx == 1 ? PlayerType.COMPUTER : PlayerType.HUMAN;
            setVisible(false);
        });

        bottomBar.add(btnCancel);
        bottomBar.add(btnSave);
        outerCard.add(bottomBar, BorderLayout.SOUTH);

        getContentPane().add(outerCard);
        pack();
        setLocationRelativeTo(frame);
    }

    private JPanel createCardContainer() {
        final JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.Border() {
                    @Override
                    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(UITheme.isDark() ? new Color(24, 32, 48) : new Color(248, 250, 253));
                        g2.fillRoundRect(x, y, width, height, 16, 16);
                        g2.setColor(UITheme.getCardBorder());
                        g2.setStroke(new BasicStroke(1.0f));
                        g2.drawRoundRect(x, y, width - 1, height - 1, 16, 16);
                        g2.dispose();
                    }

                    @Override
                    public Insets getBorderInsets(Component c) {
                        return new Insets(12, 14, 12, 14);
                    }

                    @Override
                    public boolean isBorderOpaque() {
                        return false;
                    }
                },
                new EmptyBorder(0, 0, 0, 0)
        ));
        return panel;
    }

    private JPanel createPlayerColumn(String title, boolean isWhite) {
        final JPanel col = new JPanel(new BorderLayout(0, 8));
        col.setOpaque(false);

        final JLabel header = new JLabel(title);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setForeground(UITheme.getTextSecondary());
        col.add(header, BorderLayout.NORTH);

        final JPanel pillsRow = new JPanel(new GridLayout(1, 2, 6, 0));
        pillsRow.setOpaque(false);

        final JLabel humanBtn = makePlayerPill("👤 Human", 0, isWhite);
        final JLabel aiBtn = makePlayerPill("🤖 AI Engine", 1, isWhite);

        pillsRow.add(humanBtn);
        pillsRow.add(aiBtn);
        col.add(pillsRow, BorderLayout.CENTER);

        return col;
    }

    private JLabel makePlayerPill(String label, int typeIdx, boolean isWhite) {
        final JLabel pill = new JLabel(label, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean active = isWhite ? (selectedWhiteTypeIdx == typeIdx) : (selectedBlackTypeIdx == typeIdx);
                if (active) {
                    g2.setColor(UITheme.getAccentActiveBg());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(UITheme.getAccentBlue());
                    g2.setStroke(new BasicStroke(1.6f));
                    g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 9, 9);
                } else {
                    g2.setColor(UITheme.getCardBg());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(UITheme.getCardBorder());
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(new Font("SansSerif", Font.BOLD, 12));
        pill.setPreferredSize(new Dimension(84, 34));
        pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pill.setForeground(UITheme.getTextPrimary());

        pill.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isWhite) selectedWhiteTypeIdx = typeIdx;
                else selectedBlackTypeIdx = typeIdx;
                pill.getParent().repaint();
            }
        });

        return pill;
    }

    private JLabel makeDifficultyPill(String label, int diffIdx) {
        final JLabel pill = new JLabel(label, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean active = (selectedDiffIdx == diffIdx);
                if (active) {
                    g2.setColor(UITheme.getAccentActiveBg());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(UITheme.getAccentBlue());
                    g2.setStroke(new BasicStroke(1.6f));
                    g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 9, 9);
                } else {
                    g2.setColor(UITheme.getCardBg());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(UITheme.getCardBorder());
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(new Font("SansSerif", Font.BOLD, 12));
        pill.setPreferredSize(new Dimension(66, 32));
        pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pill.setForeground(UITheme.getTextPrimary());

        pill.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedDiffIdx = diffIdx;
                boolean isCustom = (diffIdx == DIFFICULTY_LABELS.length - 1);
                customSpinnerRow.setVisible(isCustom);
                for (JLabel p : diffPills) p.repaint();
                pack();
            }
        });

        return pill;
    }

    private JLabel makeTimePill(String label, int mins) {
        final JLabel pill = new JLabel(label, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean active = (mins == -1) ? isCustomTime : (!isCustomTime && selectedMinutes == mins);
                if (active) {
                    g2.setColor(UITheme.getAccentActiveBg());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(UITheme.getAccentBlue());
                    g2.setStroke(new BasicStroke(1.6f));
                    g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 9, 9);
                } else {
                    g2.setColor(UITheme.getCardBg());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(UITheme.getCardBorder());
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(new Font("SansSerif", Font.BOLD, 12));
        pill.setPreferredSize(new Dimension(52, 32));
        pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pill.setForeground(UITheme.getTextPrimary());

        pill.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (mins == -1) {
                    isCustomTime = true;
                    customMinutesRow.setVisible(true);
                } else {
                    isCustomTime = false;
                    selectedMinutes = mins;
                    customMinutesRow.setVisible(false);
                }
                for (JLabel p : timePills) p.repaint();
                pack();
            }
        });

        return pill;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(new Font("SansSerif", Font.BOLD, 13));
        spinner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.getCardBorder(), 1, true),
                new EmptyBorder(2, 6, 2, 6)
        ));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(UITheme.getCardBg());
            tf.setForeground(UITheme.getTextPrimary());
            tf.setBorder(BorderFactory.createEmptyBorder());
        }
    }

    private JButton makeActionButton(String text, boolean isPrimary, Runnable action) {
        final JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (isPrimary) {
                    g2.setColor(getModel().isRollover() ? UITheme.getAccentHover() : UITheme.getAccentBlue());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                } else {
                    g2.setColor(getModel().isRollover() ? UITheme.getAccentActiveBg() : UITheme.getControlBtnBg());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setColor(UITheme.getControlBtnBorder());
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(isPrimary ? Color.WHITE : UITheme.getTextSecondary());
        btn.setPreferredSize(new Dimension(116, 38));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (action != null) action.run();
        });
        return btn;
    }

    public void promptUser() {
        selectedWhiteTypeIdx = (whitePlayerType == PlayerType.COMPUTER) ? 1 : 0;
        selectedBlackTypeIdx = (blackPlayerType == PlayerType.COMPUTER) ? 1 : 0;
        for (JLabel p : diffPills) p.repaint();
        for (JLabel p : timePills) p.repaint();
        repaint();
        setVisible(true);
    }

    public boolean isAIPlayer(final Player player) {
        if (player.getAlliance() == Alliance.WHITE) {
            return getWhitePlayerType() == PlayerType.COMPUTER;
        }
        return getBlackPlayerType() == PlayerType.COMPUTER;
    }

    /**
     * Returns the movetime in ms for the currently selected difficulty.
     */
    public int getMoveTimeMs() {
        if (selectedDiffIdx >= 0 && selectedDiffIdx < DIFFICULTY_MOVETIMES.length) {
            if (DIFFICULTY_MOVETIMES[selectedDiffIdx] == -1) {
                return (int) customDepthSpinner.getValue();
            }
            return DIFFICULTY_MOVETIMES[selectedDiffIdx];
        }
        return StockfishEngine.MOVETIME_MEDIUM;
    }

    public int getDifficultyIndex() {
        return selectedDiffIdx;
    }

    public int getCustomMoveTime() {
        return (int) customDepthSpinner.getValue();
    }

    public void setDifficulty(final int comboIndex, final int customMoveTime) {
        final int safeIndex = (comboIndex >= 0 && comboIndex < DIFFICULTY_LABELS.length) ? comboIndex : 1;
        selectedDiffIdx = safeIndex;
        customDepthSpinner.setValue(customMoveTime);
        final boolean isCustom = safeIndex == DIFFICULTY_LABELS.length - 1;
        customSpinnerRow.setVisible(isCustom);
        for (JLabel p : diffPills) p.repaint();
    }

    public boolean isClockEnabled() {
        return clockEnabledSwitch.isSelected();
    }

    public int getClockMinutes() {
        if (isCustomTime) {
            return (int) customMinutesSpinner.getValue();
        }
        return selectedMinutes;
    }

    public PlayerType getWhitePlayerType() {
        return this.whitePlayerType;
    }

    public void setWhitePlayerType(final PlayerType whitePlayerType) {
        this.whitePlayerType = whitePlayerType;
        this.selectedWhiteTypeIdx = (whitePlayerType == PlayerType.COMPUTER) ? 1 : 0;
    }

    public PlayerType getBlackPlayerType() {
        return this.blackPlayerType;
    }

    public void setBlackPlayerType(final PlayerType blackPlayerType) {
        this.blackPlayerType = blackPlayerType;
        this.selectedBlackTypeIdx = (blackPlayerType == PlayerType.COMPUTER) ? 1 : 0;
    }
}
