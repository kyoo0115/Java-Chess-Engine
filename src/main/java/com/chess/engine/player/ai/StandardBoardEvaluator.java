package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.Player;

public final class StandardBoardEvaluator implements BoardEvaluator {

    private static final int CHECK_BONUS = 45;
    private static final int CHECK_MATE_BONUS = 10000;
    private static final int DEPTH_BONUS = 100;
    private static final int CASTLE_BONUS = 60;

    // ─────────────────────────────────────────────────────────────────
    //  Piece values in centipawns (standard chess engine convention)
    // ─────────────────────────────────────────────────────────────────
    // (Piece.PieceType values are 1/3/3/5/8/100 — we multiply by 100 here
    //  so that PST deltas (±50 cp) are meaningful relative to material.)

    // ─────────────────────────────────────────────────────────────────
    //  Piece-Square Tables  (from White's perspective, index 0 = a8)
    //  Source: standard tuned tables used widely in beginner engines.
    // ─────────────────────────────────────────────────────────────────

    private static final int[] PAWN_TABLE = {
            0, 0, 0, 0, 0, 0, 0, 0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5, 5, 10, 25, 25, 10, 5, 5,
            0, 0, 0, 20, 20, 0, 0, 0,
            5, -5, -10, 0, 0, -10, -5, 5,
            5, 10, 10, -20, -20, 10, 10, 5,
            0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] KNIGHT_TABLE = {
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20, 0, 0, 0, 0, -20, -40,
            -30, 0, 10, 15, 15, 10, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30,
            -30, 0, 15, 20, 20, 15, 0, -30,
            -30, 5, 10, 15, 15, 10, 5, -30,
            -40, -20, 0, 5, 5, 0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50
    };

    private static final int[] BISHOP_TABLE = {
            -20, -10, -10, -10, -10, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 10, 10, 5, 0, -10,
            -10, 5, 5, 10, 10, 5, 5, -10,
            -10, 0, 10, 10, 10, 10, 0, -10,
            -10, 10, 10, 10, 10, 10, 10, -10,
            -10, 5, 0, 0, 0, 0, 5, -10,
            -20, -10, -10, -10, -10, -10, -10, -20
    };

    private static final int[] ROOK_TABLE = {
            0, 0, 0, 0, 0, 0, 0, 0,
            5, 10, 10, 10, 10, 10, 10, 5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            0, 0, 0, 5, 5, 0, 0, 0
    };

    private static final int[] QUEEN_TABLE = {
            -20, -10, -10, -5, -5, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 5, 5, 5, 0, -10,
            -5, 0, 5, 5, 5, 5, 0, -5,
            0, 0, 5, 5, 5, 5, 0, -5,
            -10, 5, 5, 5, 5, 5, 0, -10,
            -10, 0, 5, 0, 0, 0, 0, -10,
            -20, -10, -10, -5, -5, -10, -10, -20
    };

    /**
     * King safety — prefer castled king behind pawns in the middlegame.
     */
    private static final int[] KING_TABLE = {
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            20, 20, 0, 0, 0, 0, 20, 20,
            20, 30, 10, 0, 0, 10, 30, 20
    };

    // ─────────────────────────────────────────────────────────────────

    private static int castled(final Player player) {
        return player.isCastled() ? CASTLE_BONUS : 0;
    }

    private static int depthBonus(final int depth) {
        return depth == 0 ? 1 : DEPTH_BONUS * depth;
    }

    // ─────────────────────────────────────────────────────────────────

    private static int check(final Player player) {
        return player.getOpponent().isInCheck() ? CHECK_BONUS : 0;
    }

    private static int mobility(final Player player) {
        return player.getLegalMoves().size();
    }

    private static int pieceValue(final Player player) {
        int score = 0;
        for (final Piece piece : player.getActivePieces()) {
            score += piece.getPieceValue() * 100; // convert to centipawns
        }
        return score;
    }

    /**
     * Lookup position bonus from a PST. White pieces read the table top-to-bottom (index as-is).
     * Black pieces mirror vertically (flip by row).
     */
    private static int pieceSquareBonus(final Player player) {
        int bonus = 0;
        final boolean isWhite = player.getAlliance().isWhite();

        for (final Piece piece : player.getActivePieces()) {
            final int pos = piece.getPiecePosition();
            // Mirror for black: row r → row (7-r)
            final int tableIndex = isWhite ? mirrorForWhite(pos) : pos;
            bonus += pstFor(piece)[tableIndex];
        }
        return bonus;
    }

    /**
     * The PST indices go from rank 8 (top, index 0) to rank 1 (bottom, index 56).
     * White pieces are physically at the bottom of the board (high indices).
     * We mirror vertically so that White's rank-1 uses row 7 of the table (the
     * "good" white king safety row).
     */
    private static int mirrorForWhite(final int position) {
        final int row = position / 8;
        final int col = position % 8;
        return (7 - row) * 8 + col;
    }

    private static int[] pstFor(final Piece piece) {
        return switch (piece.getPieceType()) {
            case PAWN -> PAWN_TABLE;
            case KNIGHT -> KNIGHT_TABLE;
            case BISHOP -> BISHOP_TABLE;
            case ROOK -> ROOK_TABLE;
            case QUEEN -> QUEEN_TABLE;
            case KING -> KING_TABLE;
        };
    }

    @Override
    public int evaluate(final Board board, final int depth) {
        return scorePlayer(board, board.getWhitePlayer(), depth) -
                scorePlayer(board, board.getBlackPlayer(), depth);
    }

    private int scorePlayer(final Board board, final Player player, final int depth) {
        return pieceValue(player) +
                pieceSquareBonus(player) +
                mobility(player) +
                check(player) +
                checkMate(player, depth) +
                castled(player);
    }

    private int checkMate(final Player player, final int depth) {
        return player.getOpponent().isCheckMate() ? CHECK_MATE_BONUS * depthBonus(depth) : 0;
    }
}
