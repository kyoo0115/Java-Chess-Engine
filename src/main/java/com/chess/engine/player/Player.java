package com.chess.engine.player;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.pieces.King;
import com.chess.engine.pieces.Piece;
import com.chess.engine.util.BoardUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.Collection;

public abstract class Player {

    protected final Board board;
    protected final King playerKing;
    protected final Collection<Move> legalMoves;
    private final boolean inCheck;

    protected Player(Board board, Collection<Move> legalMoves, Collection<Move> opponentMoves,
                     Alliance opponentAlliance) {
        this.board = board;
        this.playerKing = findKing();
        this.inCheck = isSquareAttackedBy(board, playerKing.getPiecePosition(), opponentAlliance);
        this.legalMoves = ImmutableList.copyOf(Iterables.concat(legalMoves, calculateKingCastles(legalMoves, opponentMoves)));
    }

    /**
     * Returns true if {@code square} is attacked by any piece of {@code attacker} alliance
     * on the given board, using proper piece-based attack detection.
     * <p>
     * Unlike calculateAttacksOnTile(), this correctly handles:
     * - Pawns attacking empty diagonal squares (not captured in move lists)
     * - Excludes pawn forward moves (which are not attacks)
     * <p>
     * Used for castling transit/destination square validation.
     */
    public static boolean isSquareAttackedBy(final Board board, final int square, final Alliance attacker) {
        for (final Piece piece : (attacker.isWhite() ? board.getWhitePieces() : board.getBlackPieces())) {
            if (pieceAttacksSquare(board, piece, square)) return true;
        }
        return false;
    }

    private static boolean pieceAttacksSquare(final Board board, final Piece piece, final int square) {
        final int pos = piece.getPiecePosition();
        switch (piece.getPieceType()) {
            case PAWN -> {
                // Pawn attacks its two diagonal squares regardless of occupancy
                final int dir = piece.getPieceAlliance().getDirection();
                final int leftAttack = pos + dir * 7;
                final int rightAttack = pos + dir * 9;
                // Left diagonal (wraps excluded)
                if (leftAttack == square && !isLeftColumnExclusion(piece.getPieceAlliance(), pos)) return true;
                // Right diagonal (wraps excluded)
                return rightAttack == square && !isRightColumnExclusion(piece.getPieceAlliance(), pos);
            }
            case KNIGHT -> {
                final int[] KNIGHT_OFFSETS = {-17, -15, -10, -6, 6, 10, 15, 17};
                for (final int offset : KNIGHT_OFFSETS) {
                    final int dest = pos + offset;
                    if (dest == square && BoardUtils.isValidTileCoordinate(dest)
                            && !isKnightColumnExclusion(pos, offset)) return true;
                }
                return false;
            }
            case BISHOP -> {
                return isDiagonalAttack(board, pos, square);
            }
            case ROOK -> {
                return isStraightAttack(board, pos, square);
            }
            case QUEEN -> {
                return isDiagonalAttack(board, pos, square) || isStraightAttack(board, pos, square);
            }
            case KING -> {
                final int[] KING_OFFSETS = {-9, -8, -7, -1, 1, 7, 8, 9};
                for (final int offset : KING_OFFSETS) {
                    final int dest = pos + offset;
                    if (dest == square && BoardUtils.isValidTileCoordinate(dest)
                            && !isKingColumnExclusion(pos, offset)) return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Pawn left-column exclusion: piece on file A (black) or file H (white) can't attack left
     */
    private static boolean isLeftColumnExclusion(final Alliance alliance, final int pos) {
        return (BoardUtils.EIGHTH_COLUMN[pos] && alliance.isWhite())
                || (BoardUtils.FIRST_COLUMN[pos] && alliance.isBlack());
    }

    /**
     * Pawn right-column exclusion: piece on file A (white) or file H (black) can't attack right
     */
    private static boolean isRightColumnExclusion(final Alliance alliance, final int pos) {
        return (BoardUtils.FIRST_COLUMN[pos] && alliance.isWhite())
                || (BoardUtils.EIGHTH_COLUMN[pos] && alliance.isBlack());
    }

    private static boolean isKnightColumnExclusion(final int pos, final int offset) {
        return (BoardUtils.FIRST_COLUMN[pos] && (offset == -17 || offset == -10 || offset == 6 || offset == 15))
                || (BoardUtils.SECOND_COLUMN[pos] && (offset == -10 || offset == 6))
                || (BoardUtils.SEVENTH_COLUMN[pos] && (offset == -6 || offset == 10))
                || (BoardUtils.EIGHTH_COLUMN[pos] && (offset == -15 || offset == -6 || offset == 10 || offset == 17));
    }

    private static boolean isKingColumnExclusion(final int pos, final int offset) {
        return (BoardUtils.FIRST_COLUMN[pos] && (offset == -9 || offset == -1 || offset == 7))
                || (BoardUtils.EIGHTH_COLUMN[pos] && (offset == -7 || offset == 1 || offset == 9));
    }

    /**
     * Ray-trace along diagonals from {@code from} to see if {@code target} is attacked.
     */
    private static boolean isDiagonalAttack(final Board board, final int from, final int target) {
        for (final int dir : new int[]{-9, -7, 7, 9}) {
            int sq = from;
            while (true) {
                if (BoardUtils.FIRST_COLUMN[sq] && (dir == -9 || dir == 7)) break;
                if (BoardUtils.EIGHTH_COLUMN[sq] && (dir == -7 || dir == 9)) break;
                sq += dir;
                if (!BoardUtils.isValidTileCoordinate(sq)) break;
                if (sq == target) return true;
                if (board.getTile(sq).isTileOccupied()) break; // blocked
            }
        }
        return false;
    }

    /**
     * Ray-trace along ranks/files from {@code from} to see if {@code target} is attacked.
     */
    private static boolean isStraightAttack(final Board board, final int from, final int target) {
        // Rank (horizontal)
        for (final int dir : new int[]{-1, 1}) {
            int sq = from;
            while (true) {
                if (dir == -1 && BoardUtils.FIRST_COLUMN[sq]) break;
                if (dir == 1 && BoardUtils.EIGHTH_COLUMN[sq]) break;
                sq += dir;
                if (!BoardUtils.isValidTileCoordinate(sq)) break;
                if (sq == target) return true;
                if (board.getTile(sq).isTileOccupied()) break;
            }
        }
        // File (vertical)
        for (final int dir : new int[]{-8, 8}) {
            int sq = from;
            while (true) {
                sq += dir;
                if (!BoardUtils.isValidTileCoordinate(sq)) break;
                if (sq == target) return true;
                if (board.getTile(sq).isTileOccupied()) break;
            }
        }
        return false;
    }

    public King getPlayerKing() {
        return this.playerKing;
    }

    public Collection<Move> getLegalMoves() {
        return this.legalMoves;
    }

    private King findKing() {
        return getActivePieces().stream()
                .filter(piece -> piece.getPieceType().isKing())
                .map(piece -> (King) piece)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Player must have a king!"));
    }

    public boolean isMoveLegal(Move move) {
        return legalMoves.contains(move);
    }

    public boolean isInCheck() {
        return inCheck;
    }

    public boolean isCheckMate() {
        return inCheck && !hasEscapeMoves();
    }

    public boolean isStaleMate() {
        return !inCheck && !hasEscapeMoves();
    }

    public boolean isCastled() {
        return false;
    }

    protected boolean hasEscapeMoves() {
        return legalMoves.stream().anyMatch(move -> makeMove(move).getMoveStatus().isDone());
    }

    public MoveTransition makeMove(final Move move) {
        if (!isMoveLegal(move)) {
            return new MoveTransition(this.board, move, MoveStatus.ILLEGAL_MOVE);
        }

        final Board transitionBoard = move.execute();

        final int kingSquare = transitionBoard.getCurrentPlayer().getOpponent().getPlayerKing().getPiecePosition();
        if (isSquareAttackedBy(transitionBoard, kingSquare, transitionBoard.getCurrentPlayer().getAlliance())) {
            return new MoveTransition(this.board, move, MoveStatus.LEAVE_PLAYER_IN_CHECK);
        }

        return new MoveTransition(transitionBoard, move, MoveStatus.DONE);
    }

    public abstract Collection<Piece> getActivePieces();

    public abstract Alliance getAlliance();

    public abstract Player getOpponent();

    protected abstract Collection<Move> calculateKingCastles(Collection<Move> playerLegals, Collection<Move> opponentLegals);
}
