package com.chess.engine.gui;

import com.chess.engine.Alliance;
import com.chess.engine.player.Player;

import javax.swing.*;
import java.awt.*;

import static com.chess.engine.gui.Table.PlayerType;

public class GameSetup extends JDialog {

    private static final String HUMAN_TEXT = "Human";
    private static final String COMPUTER_TEXT = "Computer";

    private static final String[] DIFFICULTY_LABELS = {"Easy", "Medium", "Hard", "Master", "Custom"};
    private static final int[]    DIFFICULTY_DEPTHS  = {2,      4,       6,      8,        -1};

    // Defaults: White = Human, Black = Computer, depth = 4 (Medium)
    private PlayerType whitePlayerType = PlayerType.HUMAN;
    private PlayerType blackPlayerType = PlayerType.COMPUTER;
    private JComboBox<String> difficultyCombo;
    private JSpinner customDepthSpinner;
    private JPanel customSpinnerPanel;

    GameSetup(final JFrame frame,
              final boolean modal) {
        super(frame, modal);
        final JPanel myPanel = new JPanel(new GridLayout(0, 1));
        final JRadioButton whiteHumanButton = new JRadioButton(HUMAN_TEXT);
        final JRadioButton whiteComputerButton = new JRadioButton(COMPUTER_TEXT);
        final JRadioButton blackHumanButton = new JRadioButton(HUMAN_TEXT);
        final JRadioButton blackComputerButton = new JRadioButton(COMPUTER_TEXT);
        whiteHumanButton.setActionCommand(HUMAN_TEXT);

        final ButtonGroup whiteGroup = new ButtonGroup();
        whiteGroup.add(whiteHumanButton);
        whiteGroup.add(whiteComputerButton);
        whiteHumanButton.setSelected(true);   // White = Human by default

        final ButtonGroup blackGroup = new ButtonGroup();
        blackGroup.add(blackHumanButton);
        blackGroup.add(blackComputerButton);
        blackComputerButton.setSelected(true); // Black = Computer by default

        getContentPane().add(myPanel);
        myPanel.add(new JLabel("White"));
        myPanel.add(whiteHumanButton);
        myPanel.add(whiteComputerButton);
        myPanel.add(new JLabel("Black"));
        myPanel.add(blackHumanButton);
        myPanel.add(blackComputerButton);

        // ── Difficulty combo ─────────────────────────────────────────
        myPanel.add(new JLabel("Difficulty"));
        difficultyCombo = new JComboBox<>(DIFFICULTY_LABELS);
        difficultyCombo.setSelectedIndex(1); // Medium (depth 4) by default
        myPanel.add(difficultyCombo);

        // ── Custom depth spinner (hidden unless "Custom" is selected) ─
        customDepthSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 20, 1));
        customSpinnerPanel = new JPanel(new BorderLayout());
        customSpinnerPanel.add(new JLabel("Search Depth"), BorderLayout.WEST);
        customSpinnerPanel.add(customDepthSpinner, BorderLayout.CENTER);
        customSpinnerPanel.setVisible(false);
        myPanel.add(customSpinnerPanel);

        difficultyCombo.addActionListener(e -> {
            final boolean isCustom = difficultyCombo.getSelectedIndex() == DIFFICULTY_LABELS.length - 1;
            customSpinnerPanel.setVisible(isCustom);
            pack();
        });

        final JButton cancelButton = new JButton("Cancel");
        final JButton okButton = new JButton("OK");

        okButton.addActionListener(e -> {
            whitePlayerType = whiteComputerButton.isSelected() ? PlayerType.COMPUTER : PlayerType.HUMAN;
            blackPlayerType = blackComputerButton.isSelected() ? PlayerType.COMPUTER : PlayerType.HUMAN;
            GameSetup.this.setVisible(false);
        });

        cancelButton.addActionListener(e -> {
            System.out.println("Cancel");
            GameSetup.this.setVisible(false);
        });

        myPanel.add(cancelButton);
        myPanel.add(okButton);

        setLocationRelativeTo(frame);
        pack();
        setVisible(false);
    }

    void promptUser() {
        setVisible(true);
        repaint();
    }

    boolean isAIPlayer(final Player player) {
        if (player.getAlliance() == Alliance.WHITE) {
            return getWhitePlayerType() == PlayerType.COMPUTER;
        }

        return getBlackPlayerType() == PlayerType.COMPUTER;
    }

    int getSearchDepth() {
        final int idx = difficultyCombo.getSelectedIndex();
        if (DIFFICULTY_DEPTHS[idx] == -1) {
            // Custom: read from spinner
            return (int) customDepthSpinner.getValue();
        }
        return DIFFICULTY_DEPTHS[idx];
    }

    PlayerType getWhitePlayerType() {
        return this.whitePlayerType;
    }

    PlayerType getBlackPlayerType() {
        return this.blackPlayerType;
    }
}
