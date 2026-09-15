package com.chess.engine.board;

import com.chess.engine.board.Board.Builder;
import com.chess.engine.pieces.Pawn;
import com.chess.engine.pieces.Piece;
import com.chess.engine.pieces.Rook;
import com.chess.engine.util.BoardUtils;

public abstract class Move {

    private static final Move NULL_MOVE = new NullMove();
    protected final Board board;
    protected final Piece movedPiece;
    protected final int destinationCoordinate;
    protected final boolean isFirstMove;

    private Move(final Board board, final Piece movedPiece, final int destinationCoordinate) {
        this.board = board;
        this.movedPiece = movedPiece;
        this.destinationCoordinate = destinationCoordinate;
        this.isFirstMove = movedPiece.isFirstMove();
    }

    private Move(final Board board, final int destinationCoordinate) {
        this.board = board;
        this.destinationCoordinate = destinationCoordinate;
        this.movedPiece = null;
        this.isFirstMove = false;
    }

    @Override
    public int hashCode() {
        if (movedPiece == null) return destinationCoordinate;
        final int prime = 31;
        int result = 1;
        result = prime * result + this.destinationCoordinate;
        result = prime * result + this.movedPiece.hashCode();
        result = prime * result + this.movedPiece.getPiecePosition();
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;

        if (!(other instanceof Move otherMove)) {
            return false;
        }

        return getCurrentCoordinate() == otherMove.getCurrentCoordinate() &&
                getDestinationCoordinate() == otherMove.getDestinationCoordinate() &&
                getMovedPiece().equals(otherMove.getMovedPiece());
    }

    public Board getBoard() {
        return this.board;
    }

    public int getDestinationCoordinate() {
        return this.destinationCoordinate;
    }

    public int getCurrentCoordinate() {
        return this.getMovedPiece().getPiecePosition();
    }

    public Piece getMovedPiece() {
        return this.movedPiece;
    }

    public boolean isAttack() {
        return false;
    }

    public boolean isCastlingMove() {
        return false;
    }

    public Piece getAttackedPiece() {
        return null;
    }

    public Board execute() {

        final Builder builder = new Builder();

        for (final Piece piece : this.board.getCurrentPlayer().getActivePieces()) {
            if (!this.movedPiece.equals(piece)) {
                builder.setPiece(piece);
            }
        }

        for (final Piece piece : this.board.getCurrentPlayer().getOpponent().getActivePieces()) {
            builder.setPiece(piece);
        }

        builder.setPiece(this.movedPiece.movePiece(this));
        builder.setMoveMaker(this.board.getCurrentPlayer().getOpponent().getAlliance());
        // Half-move clock: reset on pawn move or capture, else increment
        builder.setHalfMoveClock(
                (isAttack() || movedPiece.getPieceType() == Piece.PieceType.PAWN)
                        ? 0 : this.board.getHalfMoveClock() + 1);

        return builder.build();
    }

    public static class MajorMove extends Move {

        public MajorMove(final Board board, final Piece piece, final int destinationCoordinate) {
            super(board, piece, destinationCoordinate);
        }

        @Override
        public boolean equals(final Object other) {
            return this == other || other instanceof MajorMove && super.equals(other);
        }

        @Override
        public String toString() {
            return movedPiece.getPieceType().toString() + BoardUtils.getPositionAtCoordinate(this.destinationCoordinate);
        }
    }

    public static class MajorAttackMove extends AttackMove {

        public MajorAttackMove(final Board board, final Piece movedPiece, final int destinationCoordinate,
                               final Piece pieceAttacked) {
            super(board, movedPiece, destinationCoordinate, pieceAttacked);
        }

        @Override
        public boolean equals(final Object other) {
            return this == other || other instanceof MajorAttackMove && super.equals(other);
        }

        @Override
        public String toString() {
            return movedPiece.getPieceType() + BoardUtils.getPositionAtCoordinate(this.destinationCoordinate);
        }
    }

    public static class AttackMove extends Move {

        final Piece attackedPiece;

        public AttackMove(final Board board, final Piece piece, final int destinationCoordinate, final Piece attackedPiece) {
            super(board, piece, destinationCoordinate);
            this.attackedPiece = attackedPiece;
        }

        @Override
        public int hashCode() {
            return this.attackedPiece.hashCode() + super.hashCode();
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) return true;

            if (!(other instanceof AttackMove otherAttackMove)) return false;

            return super.equals(otherAttackMove) && getAttackedPiece().equals(otherAttackMove.getAttackedPiece());
        }

        @Override
        public boolean isAttack() {
            return true;
        }

        @Override
        public Piece getAttackedPiece() {
            return this.attackedPiece;
        }
    }

    public static final class PawnMove extends Move {

        public PawnMove(final Board board, final Piece movedPiece, final int destinationCoordinate) {
            super(board, movedPiece, destinationCoordinate);
        }

        public boolean equals(final Object other) {
            return (this == other || other instanceof PawnMove && super.equals(other));
        }

        @Override
        public String toString() {
            return BoardUtils.getPositionAtCoordinate(this.destinationCoordinate);
        }
    }

    public static class PawnAttackMove extends AttackMove {

        public PawnAttackMove(final Board board, final Piece piece, final int destinationCoordinate, final Piece attackedPiece) {
            super(board, piece, destinationCoordinate, attackedPiece);
        }

        @Override
        public boolean equals(final Object other) {
            return this == other || other instanceof PawnAttackMove && super.equals(other);
        }

        @Override
        public String toString() {
            return BoardUtils.getPositionAtCoordinate(this.movedPiece.getPiecePosition()).charAt(0) + "x" +
                    BoardUtils.getPositionAtCoordinate(this.destinationCoordinate);
        }
    }

    public static final class PawnEnPassantAttackMove extends PawnAttackMove {

        public PawnEnPassantAttackMove(final Board board, final Piece piece, final int destinationCoordinate, final Piece attackedPiece) {
            super(board, piece, destinationCoordinate, attackedPiece);
        }

        @Override
        public boolean equals(final Object other) {
            return this == other || other instanceof PawnEnPassantAttackMove && super.equals(other);
        }

        @Override
        public Board execute() {
            final Builder builder = new Builder();

            for (final Piece piece : this.board.getCurrentPlayer().getActivePieces()) {
                if (!this.movedPiece.equals(piece)) {
                    builder.setPiece(piece);
                }
            }

            for (final Piece piece : this.board.getCurrentPlayer().getOpponent().getActivePieces()) {
                if (!piece.equals(this.getAttackedPiece())) {
                    builder.setPiece(piece);
                }
            }

            builder.setPiece(this.movedPiece.movePiece(this));
            builder.setMoveMaker(this.board.getCurrentPlayer().getOpponent().getAlliance());
            builder.setHalfMoveClock(0); // capture always resets

            return builder.build();
        }

    }

    public static class PawnPromotion extends Move {

        final Move decorateMove;
        final Pawn promotedPawn;
        private final Piece promotionChoice; // null → default Queen via getPromotionPiece()

        public PawnPromotion(final Move decoratedMove) {
            super(decoratedMove.getBoard(), decoratedMove.getMovedPiece(), decoratedMove.getDestinationCoordinate());
            this.decorateMove = decoratedMove;
            this.promotedPawn = (Pawn) decorateMove.getMovedPiece();
            this.promotionChoice = null;
        }

        /**
         * Constructor used by the promotion-choice dialog — promoteTo is the piece to place.
         */
        public PawnPromotion(final Move decoratedMove, final Piece promoteTo) {
            super(decoratedMove.getBoard(), decoratedMove.getMovedPiece(), decoratedMove.getDestinationCoordinate());
            this.decorateMove = decoratedMove;
            this.promotedPawn = (Pawn) decorateMove.getMovedPiece();
            this.promotionChoice = promoteTo;
        }

        public Move getDecoratedMove() {
            return decorateMove;
        }

        /**
         * Returns the piece this pawn promotes to (Queen if not explicitly set).
         */
        public Piece getPromotionPiece() {
            return promotionChoice != null ? promotionChoice : promotedPawn.getPromotionPiece();
        }

        @Override
        public int hashCode() {
            int result = decorateMove.hashCode() + (31 * promotedPawn.hashCode());
            if (promotionChoice != null) result = 31 * result + promotionChoice.getPieceType().hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) return true;
            if (!(other instanceof PawnPromotion otherPP)) return false;
            if (!super.equals(otherPP)) return false;
            // Two promotions are only equal if they promote to the same piece type
            final Piece thisChoice = getPromotionPiece();
            final Piece otherChoice = otherPP.getPromotionPiece();
            return thisChoice.getPieceType() == otherChoice.getPieceType();
        }

        @Override
        public Board execute() {

            final Board pawnMovedBoard = this.decorateMove.execute();
            final Builder builder = new Builder();

            // After decorateMove.execute() the board has flipped sides, so the promoting
            // player is now pawnMovedBoard.getCurrentPlayer().getOpponent().
            for (final Piece piece : pawnMovedBoard.getCurrentPlayer().getOpponent().getActivePieces()) {
                if (!this.promotedPawn.equals(piece)) {
                    builder.setPiece(piece);
                }
            }

            for (final Piece piece : pawnMovedBoard.getCurrentPlayer().getActivePieces()) {
                builder.setPiece(piece);
            }

            final Piece pieceToPlace = (promotionChoice != null) ? promotionChoice : this.promotedPawn.getPromotionPiece();
            builder.setPiece(pieceToPlace.movePiece(this));
            builder.setMoveMaker(pawnMovedBoard.getCurrentPlayer().getAlliance());
            builder.setHalfMoveClock(0);

            return builder.build();
        }

        @Override
        public boolean isAttack() {
            return this.decorateMove.isAttack();
        }

        @Override
        public Piece getAttackedPiece() {
            return this.decorateMove.getAttackedPiece();
        }

        @Override
        public String toString() {
            // Use the single-letter PieceType abbreviation (P/N/B/R/Q/K) for the suffix
            final String suffix = (promotionChoice != null)
                    ? "=" + promotionChoice.getPieceType().toString()
                    : "=Q";
            return decorateMove.toString() + suffix;
        }
    }

    public static final class PawnJump extends Move {

        public PawnJump(final Board board, final Piece piece, final int destinationCoordinate) {
            super(board, piece, destinationCoordinate);
        }

        @Override
        public Board execute() {
            final Builder builder = new Builder();

            for (final Piece piece : this.board.getCurrentPlayer().getActivePieces()) {
                if (!this.movedPiece.equals(piece)) {
                    builder.setPiece(piece);
                }
            }

            for (final Piece piece : this.board.getCurrentPlayer().getOpponent().getActivePieces()) {
                builder.setPiece(piece);
            }

            final Pawn movedPawn = (Pawn) this.movedPiece.movePiece(this);
            builder.setPiece(movedPawn);
            builder.setEnPassantPawn(movedPawn);
            builder.setMoveMaker(this.board.getCurrentPlayer().getOpponent().getAlliance());
            builder.setHalfMoveClock(0); // pawn move always resets

            return builder.build();
        }

        @Override
        public String toString() {
            return BoardUtils.getPositionAtCoordinate(this.destinationCoordinate);
        }
    }

    static abstract class CastleMove extends Move {

        protected final Rook castleRook;
        protected final int castleRookStart;
        protected final int castleRookDestination;

        public CastleMove(final Board board, final Piece piece, final int destinationCoordinate,
                          final Rook castleRook, final int castleRookStart, final int castleRookDestination) {
            super(board, piece, destinationCoordinate);
            this.castleRook = castleRook;
            this.castleRookStart = castleRookStart;
            this.castleRookDestination = castleRookDestination;

        }

        public Rook getCastleRook() {
            return this.castleRook;
        }

        @Override
        public boolean isCastlingMove() {
            return true;
        }

        @Override
        public Board execute() {

            final Builder builder = new Builder();

            for (final Piece piece : this.board.getCurrentPlayer().getActivePieces()) {
                if (!this.movedPiece.equals(piece) && !this.castleRook.equals(piece)) {
                    builder.setPiece(piece);
                }
            }

            for (final Piece piece : this.board.getCurrentPlayer().getOpponent().getActivePieces()) {
                builder.setPiece(piece);
            }

            builder.setPiece(this.movedPiece.movePiece(this));
            builder.setPiece(new Rook(this.castleRook.getPieceAlliance(), this.castleRookDestination, false));
            builder.castledAlliance = this.board.getCurrentPlayer().getAlliance();
            builder.setMoveMaker(this.board.getCurrentPlayer().getOpponent().getAlliance());
            builder.setHalfMoveClock(this.board.getHalfMoveClock() + 1); // king move, not a capture
            return builder.build();

        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + this.castleRook.hashCode();
            result = prime * result + this.castleRookDestination;

            return result;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) return true;

            if (!(other instanceof CastleMove otherCastleMove)) return false;

            return super.equals(otherCastleMove) && this.castleRook.equals(otherCastleMove.getCastleRook());
        }
    }

    public static class KingSideCastleMove extends CastleMove {

        public KingSideCastleMove(final Board board, final Piece piece, final int destinationCoordinate,
                                  final Rook castleRook, final int castleRookStart, final int castleRookDestination) {
            super(board, piece, destinationCoordinate, castleRook, castleRookStart, castleRookDestination);
        }

        @Override
        public boolean equals(final Object other) {
            return this == other || other instanceof KingSideCastleMove && super.equals(other);
        }

        @Override
        public String toString() {
            return "O-O";
        }
    }

    public static final class QueenSideCastleMove extends CastleMove {

        public QueenSideCastleMove(final Board board, final Piece piece, final int destinationCoordinate,
                                   final Rook castleRook, final int castleRookStart, final int castleRookDestination) {
            super(board, piece, destinationCoordinate, castleRook, castleRookStart, castleRookDestination);
        }

        @Override
        public boolean equals(final Object other) {
            return this == other || other instanceof QueenSideCastleMove && super.equals(other);
        }

        @Override
        public String toString() {
            return "O-O-O";
        }
    }

    static class NullMove
            extends Move {

        NullMove() {
            super(null, -1);
        }

        @Override
        public int getCurrentCoordinate() {
            return -1;
        }

        @Override
        public int getDestinationCoordinate() {
            return -1;
        }

        @Override
        public Board execute() {
            throw new IllegalStateException("cannot execute null move!");
        }

        @Override
        public String toString() {
            return "Null Move";
        }
    }

    public static class MoveFactory {

        private MoveFactory() {
            throw new RuntimeException("Don't instantiate me!");
        }

        public static Move createMove(final Board board, final int currentCoordinate, final int destinationCoordinate) {

            for (final Move move : board.getAllLegalMoves()) {
                if (move.getCurrentCoordinate() == currentCoordinate && move.getDestinationCoordinate() == destinationCoordinate) {
                    return move;
                }
            }

            return NULL_MOVE;
        }
    }
}
