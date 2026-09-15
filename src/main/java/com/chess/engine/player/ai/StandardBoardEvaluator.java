package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.Player;

public final class StandardBoardEvaluator implements BoardEvaluator {

    private static final int CHECK_BONUS = 45;
    private static final int CHECK_MATE_BONUS = 10000;
    private static final int DEPTH_BONUS = 100;
    private static final int CASTLE_BONUS = 60;
    /**
     * Penalty applied when the king has moved (forfeiting castling rights) but has
     * not actually castled, and we are still in the middlegame.
     * Larger than CASTLE_BONUS so the engine strongly prefers castling over king moves.
     */
    private static final int CASTLING_RIGHTS_LOST_PENALTY = -80;

    // ── Pawn structure penalties / bonuses ────────────────────────────
    private static final int DOUBLED_PAWN_PENALTY  = -20;
    private static final int ISOLATED_PAWN_PENALTY = -15;
    /** Passed-pawn bonus indexed by how many ranks from promotion (0 = next rank, 2 = rank 5). */
    private static final int[] PASSED_PAWN_BONUS = { 0, 0, 30, 50, 70, 0, 0, 0 };

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

    /**
     * Endgame king — centralise; avoid edges and corners.
     */
    private static final int[] KING_ENDGAME_TABLE = {
            -50, -40, -30, -20, -20, -30, -40, -50,
            -30, -20, -10, 0, 0, -10, -20, -30,
            -30, -10, 20, 30, 30, 20, -10, -30,
            -30, -10, 30, 40, 40, 30, -10, -30,
            -30, -10, 30, 40, 40, 30, -10, -30,
            -30, -10, 20, 30, 30, 20, -10, -30,
            -30, -30, 0, 0, 0, 0, -30, -30,
            -50, -30, -30, -30, -30, -30, -30, -50
    };

    /**
     * Material threshold below which the endgame king table is used (~no queens + few pieces).
     */
    private static final int ENDGAME_MATERIAL_THRESHOLD = 1300;

    // ─────────────────────────────────────────────────────────────────

    private static int castled(final Player player) {
        return player.isCastled() ? CASTLE_BONUS : 0;
    }

    /**
     * Returns {@link #CASTLING_RIGHTS_LOST_PENALTY} when the king has already moved
     * (forfeiting castling rights) without having castled, during the middlegame.
     * Zero in the endgame — king centralisation matters more than castling then.
     */
    private static int castlingRightsLost(final Player player, final Board board) {
        if (totalNonKingMaterial(board) < ENDGAME_MATERIAL_THRESHOLD) return 0;
        if (player.isCastled()) return 0;
        if (player.getPlayerKing().isFirstMove()) return 0; // rights still intact
        return CASTLING_RIGHTS_LOST_PENALTY;
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
    private static int pieceSquareBonus(final Player player, final Board board) {
        int bonus = 0;
        final boolean isWhite = player.getAlliance().isWhite();
        final boolean endgame = totalNonKingMaterial(board) < ENDGAME_MATERIAL_THRESHOLD;

        for (final Piece piece : player.getActivePieces()) {
            final int pos = piece.getPiecePosition();
            // Mirror for black: row r → row (7-r)
            final int tableIndex = isWhite ? mirrorForWhite(pos) : pos;
            bonus += pstFor(piece, endgame)[tableIndex];
        }
        return bonus;
    }

    private static int totalNonKingMaterial(final Board board) {
        int total = 0;
        for (final Piece p : board.getWhitePlayer().getActivePieces()) {
            if (!p.getPieceType().isKing()) total += p.getPieceValue() * 100;
        }
        for (final Piece p : board.getBlackPlayer().getActivePieces()) {
            if (!p.getPieceType().isKing()) total += p.getPieceValue() * 100;
        }
        return total;
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

    private static int[] pstFor(final Piece piece, final boolean endgame) {
        return switch (piece.getPieceType()) {
            case PAWN -> PAWN_TABLE;
            case KNIGHT -> KNIGHT_TABLE;
            case BISHOP -> BISHOP_TABLE;
            case ROOK -> ROOK_TABLE;
            case QUEEN -> QUEEN_TABLE;
            case KING -> endgame ? KING_ENDGAME_TABLE : KING_TABLE;
        };
    }

    @Override
    public int evaluate(final Board board, final int depth) {
        // Terminal states: return a decisive score immediately.
        // The current player is the one whose turn it is — if they are in checkmate
        // or stalemate, return from the loser's perspective.
        if (board.getCurrentPlayer().isCheckMate()) {
            // The current player lost — score is hugely negative for them.
            // Positive = good for White, so: if White is mated → very negative; Black mated → very positive.
            return board.getCurrentPlayer().getAlliance().isWhite()
                    ? -(CHECK_MATE_BONUS * depthBonus(depth))
                    : (CHECK_MATE_BONUS * depthBonus(depth));
        }
        if (board.getCurrentPlayer().isStaleMate()) {
            return 0;
        }
        return scorePlayer(board, board.getWhitePlayer(), depth) -
                scorePlayer(board, board.getBlackPlayer(), depth);
    }

    private int scorePlayer(final Board board, final Player player, final int depth) {
        return pieceValue(player) +
                pieceSquareBonus(player, board) +
                mobility(player) +
                check(player) +
                castled(player) +
                pawnStructure(player, board);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Pawn structure evaluation
    // ─────────────────────────────────────────────────────────────────

    /**
     * Scores pawn structure for one player.
     * <ul>
     *   <li>Doubled pawns: −20 cp per extra pawn on the same file.</li>
     *   <li>Isolated pawns: −15 cp per pawn with no friendly pawn on adjacent files.</li>
     *   <li>Passed pawns: +30/50/70 cp depending on how advanced they are.</li>
     * </ul>
     * All values are from the player's own perspective (positive = good for this player).
     */
    private static int pawnStructure(final Player player, final Board board) {
        final boolean isWhite = player.getAlliance().isWhite();

        // Collect pawn files and ranks (board tile 0 = a8, tile 63 = h1).
        // pawnsOnFile[0..7] = count of this player's pawns on that file.
        final int[] pawnsOnFile = new int[8];
        // filePawnRanks[file] = the most-advanced rank index (closest to promotion).
        // For White, promotion is rank 0 (top); for Black, promotion is rank 7 (bottom).
        // We store the raw board rank (0–7) of each pawn per file (the most advanced one).
        final int[] bestRankOnFile = new int[8];
        java.util.Arrays.fill(bestRankOnFile, -1);

        for (final Piece p : player.getActivePieces()) {
            if (p.getPieceType() != Piece.PieceType.PAWN) continue;
            final int pos  = p.getPiecePosition();
            final int file = pos % 8;
            final int rank = pos / 8; // 0 = rank 8 (top), 7 = rank 1 (bottom)
            pawnsOnFile[file]++;
            if (isWhite) {
                // White advances toward rank 0 — smaller rank = more advanced
                if (bestRankOnFile[file] == -1 || rank < bestRankOnFile[file])
                    bestRankOnFile[file] = rank;
            } else {
                // Black advances toward rank 7 — larger rank = more advanced
                if (bestRankOnFile[file] == -1 || rank > bestRankOnFile[file])
                    bestRankOnFile[file] = rank;
            }
        }

        // Opponent pawn files — needed for passed-pawn check.
        final int[] opponentPawnsOnFile = new int[8];
        for (final Piece p : player.getOpponent().getActivePieces()) {
            if (p.getPieceType() == Piece.PieceType.PAWN)
                opponentPawnsOnFile[p.getPiecePosition() % 8]++;
        }

        int score = 0;

        for (int file = 0; file < 8; file++) {
            final int count = pawnsOnFile[file];
            if (count == 0) continue;

            // ── Doubled pawns ─────────────────────────────────────────
            if (count > 1) score += (count - 1) * DOUBLED_PAWN_PENALTY;

            // ── Isolated pawns ────────────────────────────────────────
            final boolean leftEmpty  = (file == 0) || (pawnsOnFile[file - 1] == 0);
            final boolean rightEmpty = (file == 7) || (pawnsOnFile[file + 1] == 0);
            if (leftEmpty && rightEmpty) score += count * ISOLATED_PAWN_PENALTY;

            // ── Passed pawns ──────────────────────────────────────────
            // A pawn is passed if no opponent pawn is on the same or adjacent files
            // ahead of it. We use the most-advanced pawn on this file.
            final boolean noOpponentAhead =
                    opponentPawnsOnFile[file] == 0
                    && (file == 0 || opponentPawnsOnFile[file - 1] == 0)
                    && (file == 7 || opponentPawnsOnFile[file + 1] == 0);
            if (noOpponentAhead && bestRankOnFile[file] != -1) {
                // ranksFromPromotion: 0 = on the promotion rank (already queened),
                // 1 = one step away, etc.
                final int rank = bestRankOnFile[file];
                final int ranksFromPromo = isWhite ? rank : (7 - rank);
                if (ranksFromPromo < PASSED_PAWN_BONUS.length)
                    score += PASSED_PAWN_BONUS[ranksFromPromo];
            }
        }

        return score;
    }
}
