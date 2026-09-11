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

public final class Queen extends Piece {

    private static final int[] QUEEN_MOVE_OFFSETS = {-9, -8, -7, -1, 1, 7, 8, 9};

    public Queen(final int piecePosition, final Alliance pieceAlliance) {
        super(PieceType.QUEEN, piecePosition, pieceAlliance, true);
    }

    public Queen(final Alliance pieceAlliance, final int piecePosition, final boolean isFirstMove) {
        super(PieceType.QUEEN, piecePosition, pieceAlliance, isFirstMove);
    }

    private static boolean isColumnExclusion(final int position, final int offset) {
        return (BoardUtils.FIRST_COLUMN[position] && (offset == -9 || offset == -1 || offset == 7))
                || (BoardUtils.EIGHTH_COLUMN[position] && (offset == -7 || offset == 1 || offset == 9));
    }

    @Override
    public List<Move> calculateLegalMoves(final Board board) {

        final List<Move> legalMoves = new ArrayList<>();

        for (final int offset : QUEEN_MOVE_OFFSETS) {

            int destinationCoordinate = this.piecePosition;

            while (true) {

                if (isColumnExclusion(destinationCoordinate, offset)) {
                    break;
                }

                destinationCoordinate += offset;

                if (!BoardUtils.isValidTileCoordinate(destinationCoordinate)) {
                    break;
                }

                final Tile candidateTile = board.getTile(destinationCoordinate);

                if (!candidateTile.isTileOccupied()) {
                    legalMoves.add(new MajorMove(board, this, destinationCoordinate));
                } else {
                    final Piece pieceAtDestination = candidateTile.getPiece();

                    if (this.pieceAlliance != pieceAtDestination.getPieceAlliance()) {
                        legalMoves.add(new MajorAttackMove(board, this, destinationCoordinate, pieceAtDestination));
                    }
                    break;
                }
            }

        }

        return ImmutableList.copyOf(legalMoves);
    }

    @Override
    public Queen movePiece(final Move move) {
        return new Queen(move.getDestinationCoordinate(), move.getMovedPiece().getPieceAlliance());
    }

    @Override
    public String toString() {
        return PieceType.QUEEN.toString();
    }
}
