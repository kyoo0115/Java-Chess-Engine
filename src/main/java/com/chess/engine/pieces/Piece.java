package com.chess.engine.pieces;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;

import java.util.List;

public abstract class Piece {

    protected final int piecePosition;
    protected final Alliance pieceAlliance;
    protected final boolean isFirstMove;
    protected final PieceType pieceType;
    private final int cachedHashCode;

    protected Piece(final PieceType pieceType, final int piecePosition, final Alliance pieceAlliance, final boolean isFirstMove) {
        this.pieceType = pieceType;
        this.pieceAlliance = pieceAlliance;
        this.piecePosition = piecePosition;
        this.isFirstMove = isFirstMove;
        this.cachedHashCode = computeHashCode();
    }

    /**
     * Shared sliding-ray generator used by Bishop, Rook, and Queen.
     * Walks from {@code startPosition} in each direction in {@code offsets},
     * stopping at board edges (via {@code columnExcluder}) or occupied squares.
     * Appends quiet moves and a single capture per ray to {@code legalMoves}.
     *
     * @param board          the current board
     * @param piece          the sliding piece that is moving
     * @param startPosition  the piece's current square index
     * @param offsets        ray directions (e.g. {-9,-7,7,9} for diagonals)
     * @param columnExcluder returns true when the current square + offset would wrap
     * @param legalMoves     output list to add generated moves to
     */
    protected static void addSlidingMoves(final Board board, final Piece piece,
                                          final int startPosition, final int[] offsets,
                                          final java.util.function.BiPredicate<Integer, Integer> columnExcluder,
                                          final List<Move> legalMoves) {
        for (final int offset : offsets) {
            int dest = startPosition;
            while (true) {
                if (columnExcluder.test(dest, offset)) break;
                dest += offset;
                if (!com.chess.engine.util.BoardUtils.isValidTileCoordinate(dest)) break;
                final com.chess.engine.board.Tile tile = board.getTile(dest);
                if (!tile.isTileOccupied()) {
                    legalMoves.add(new Move.MajorMove(board, piece, dest));
                } else {
                    final Piece occupant = tile.getPiece();
                    if (piece.getPieceAlliance() != occupant.getPieceAlliance()) {
                        legalMoves.add(new Move.MajorAttackMove(board, piece, dest, occupant));
                    }
                    break;
                }
            }
        }
    }

    private int computeHashCode() {
        int result = pieceType.hashCode();

        result = 31 * result + pieceAlliance.hashCode();
        result = 31 * result + piecePosition;
        result = 31 * result + (isFirstMove ? 1 : 0);

        return result;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;

        if (!(other instanceof Piece otherPiece)) return false;

        return piecePosition == otherPiece.piecePosition && pieceType == otherPiece.pieceType &&
                pieceAlliance == otherPiece.pieceAlliance && isFirstMove == otherPiece.isFirstMove;
    }

    @Override
    public int hashCode() {
        return this.cachedHashCode;
    }

    public PieceType getPieceType() {
        return this.pieceType;
    }

    public int getPiecePosition() {
        return this.piecePosition;
    }

    public Alliance getPieceAlliance() {
        return this.pieceAlliance;
    }

    public int getPieceValue() {
        return this.pieceType.getPieceValue();
    }

    public boolean isFirstMove() {
        return this.isFirstMove;
    }

    public abstract List<Move> calculateLegalMoves(final Board board);

    public abstract Piece movePiece(Move move);

    public enum PieceType {

        PAWN("P", false, false, 1),
        KNIGHT("N", false, false, 3),
        BISHOP("B", false, false, 3),
        ROOK("R", false, true, 5),
        QUEEN("Q", false, false, 9),
        KING("K", true, false, 100);

        private final boolean isKing;
        private final boolean isRook;
        private final String symbol;
        private final int pieceValue;

        PieceType(final String symbol, final boolean isKing, final boolean isRook, final int pieceValue) {
            this.symbol = symbol;
            this.pieceValue = pieceValue;
            this.isKing = isKing;
            this.isRook = isRook;
        }

        @Override
        public String toString() {
            return this.symbol;
        }

        public int getPieceValue() {
            return this.pieceValue;
        }

        public boolean isKing() {
            return isKing;
        }

        public boolean isRook() {
            return isRook;
        }
    }
}
