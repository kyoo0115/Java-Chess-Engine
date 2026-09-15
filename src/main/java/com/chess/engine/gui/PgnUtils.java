package com.chess.engine.gui;

import com.chess.engine.PlayerType;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.MoveLog;
import com.chess.engine.player.MoveTransition;

import javax.swing.*;
import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Static helpers for saving and loading PGN files.
 */
public final class PgnUtils {

    private PgnUtils() {
    }

    /** Derives the PGN result tag from the current board state. */
    private static String gameResult(final Board board) {
        if (board.getCurrentPlayer().isCheckMate()) {
            // The player to move is mated — the opponent wins
            return board.getCurrentPlayer().getAlliance().isWhite() ? "0-1" : "1-0";
        }
        if (board.getCurrentPlayer().isStaleMate()) {
            return "1/2-1/2";
        }
        return "*";
    }

    public static void saveGame(final JFrame parent, final Board board, final MoveLog moveLog, final GameSetup gameSetup) {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Game as PGN");
        fc.setSelectedFile(new File("game.pgn"));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        final String result = gameResult(board);
        final File file = fc.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("[Event \"JChess Game\"]");
            pw.println("[Date \"" + LocalDate.now() + "\"]");
            pw.println("[White \"" + (gameSetup.getWhitePlayerType() == PlayerType.HUMAN ? "Human" : "Computer") + "\"]");
            pw.println("[Black \"" + (gameSetup.getBlackPlayerType() == PlayerType.HUMAN ? "Human" : "Computer") + "\"]");
            pw.println("[Result \"" + result + "\"]");
            pw.println();

            final StringBuilder sb = new StringBuilder();
            final List<Move> moves = moveLog.getMoves();
            for (int i = 0; i < moves.size(); i++) {
                if (i % 2 == 0) sb.append(i / 2 + 1).append(". ");
                sb.append(moves.get(i).toString()).append(' ');
            }
            // Append the result token after the move list (required by the PGN spec)
            if (!result.equals("*")) sb.append(result);
            final String text = sb.toString().trim();
            int pos = 0;
            while (pos < text.length()) {
                int end = Math.min(pos + 80, text.length());
                if (end < text.length()) {
                    final int space = text.lastIndexOf(' ', end);
                    if (space > pos) end = space;
                }
                pw.println(text.substring(pos, end).trim());
                pos = end;
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Failed to save: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads a PGN file and returns the list of replayed moves, or null on failure.
     */
    public static List<Move> loadGame(final JFrame parent) {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load PGN File");
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return null;

        final File file = fc.getSelectedFile();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            final StringBuilder moveSb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                final String trimmed = line.trim();
                if (trimmed.startsWith("[") || trimmed.isEmpty()) continue;
                moveSb.append(trimmed).append(' ');
            }

            final String[] tokens = moveSb.toString().split("\\s+");
            Board replayBoard = Board.createStandardBoard();
            final List<Move> loaded = new ArrayList<>();
            for (final String token : tokens) {
                if (token.isEmpty()) continue;
                if (token.matches("\\d+\\.+")) continue;
                if (token.matches("\\$\\d+")) continue;
                if (token.matches("[01½][-/][01½]|\\*")) continue;
                final String san = token.replaceAll("[+#!?]", "");
                Move matched = null;
                for (final Move m : replayBoard.getCurrentPlayer().getLegalMoves()) {
                    if (m.toString().replaceAll("[+#!?]", "").equals(san)) {
                        matched = m;
                        break;
                    }
                }
                if (matched == null) {
                    // Could not match token — warn the user about partial load
                    JOptionPane.showMessageDialog(parent,
                            "Could not parse move token \"" + token + "\" — the game was loaded up to move " + loaded.size() + ".",
                            "Partial Load", JOptionPane.WARNING_MESSAGE);
                    break;
                }
                final MoveTransition t = replayBoard.getCurrentPlayer().makeMove(matched);
                if (!t.getMoveStatus().isDone()) {
                    JOptionPane.showMessageDialog(parent,
                            "Illegal move \"" + token + "\" at move " + (loaded.size() + 1) + " — the game was loaded up to that point.",
                            "Partial Load", JOptionPane.WARNING_MESSAGE);
                    break;
                }
                replayBoard = t.getTransitionBoard();
                loaded.add(matched);
            }

            if (loaded.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "No valid moves found in file.",
                        "Load Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return loaded;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Failed to load: " + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
}
