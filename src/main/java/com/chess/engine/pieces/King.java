package com.chess.engine.pieces;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.Move.MajorAttackMove;
import com.chess.engine.board.Tile;
import com.chess.engine.util.BoardUtils;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import static com.chess.engine.board.Move.MajorMove;

public final class King extends Piece {

    private final static int[] KING_MOVE_OFFSETS = {-9, -8, -7, -1, 1, 7, 8, 9};

    public King(final int piecePosition, final Alliance pieceAlliance) {
        super(PieceType.KING, piecePosition, pieceAlliance, true);
    }

    public King(final Alliance pieceAlliance, final int piecePosition, final boolean isFirstMove) {
        super(PieceType.KING, piecePosition, pieceAlliance, isFirstMove);
    }

    private static boolean isColumnExclusion(final int position, final int offset) {
        return (BoardUtils.FIRST_COLUMN[position] && (offset == -9 || offset == -1 || offset == 7))
                || (BoardUtils.EIGHTH_COLUMN[position] && (offset == -7 || offset == 1 || offset == 9));
    }

    @Override
    public List<Move> calculateLegalMoves(final Board board) {
        final List<Move> legalMoves = new ArrayList<>();

        for (final int offset : KING_MOVE_OFFSETS) {

            if (isColumnExclusion(this.piecePosition, offset)) continue;

            final int destinationCoordinate = this.piecePosition + offset;

            if (!BoardUtils.isValidTileCoordinate(destinationCoordinate)) continue;

            final Tile destinationTile = board.getTile(destinationCoordinate);

            if (!destinationTile.isTileOccupied()) {
                legalMoves.add(new MajorMove(board, this, destinationCoordinate));
                continue;
            }

            final Piece pieceAtDestination = destinationTile.getPiece();

            if (this.pieceAlliance != pieceAtDestination.getPieceAlliance()) {
                legalMoves.add(new MajorAttackMove(board, this, destinationCoordinate, pieceAtDestination));
            }
        }

        return ImmutableList.copyOf(legalMoves);
    }

    @Override
    public King movePiece(final Move move) {
        return new King(move.getDestinationCoordinate(), move.getMovedPiece().getPieceAlliance());
    }

    @Override
    public String toString() {
        return PieceType.KING.toString();
    }
}
