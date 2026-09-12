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

public final class Bishop extends Piece {

    private static final int[] BISHOP_MOVE_OFFSETS = {-9, -7, 7, 9};

    public Bishop(final int piecePosition, final Alliance pieceAlliance) {
        super(PieceType.BISHOP, piecePosition, pieceAlliance, true);
    }

    public Bishop(final Alliance pieceAlliance, final int piecePosition, final boolean isFirstMove) {
        super(PieceType.BISHOP, piecePosition, pieceAlliance, isFirstMove);
    }

    private static boolean isColumnExclusion(final int position, final int offset) {
        return (BoardUtils.FIRST_COLUMN[position] && (offset == -9 || offset == 7))
                || (BoardUtils.EIGHTH_COLUMN[position] && (offset == -7 || offset == 9));
    }

    /* ==================== Column Exclusions ==================== */

    @Override
    public List<Move> calculateLegalMoves(final Board board) {

        final List<Move> legalMoves = new ArrayList<>();

        for (final int offset : BISHOP_MOVE_OFFSETS) {

            int destinationCoordinate = this.piecePosition;

            while (true) {

                if (isColumnExclusion(destinationCoordinate, offset)) {
                    break;
                }

                destinationCoordinate += offset;

                if (!BoardUtils.isValidTileCoordinate(destinationCoordinate)) {
                    break;
                }

                final Tile destinationTile = board.getTile(destinationCoordinate);

                if (!destinationTile.isTileOccupied()) {
                    legalMoves.add(new MajorMove(board, this, destinationCoordinate));
                } else {
                    final Piece pieceAtDestination = destinationTile.getPiece();

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
    public Bishop movePiece(final Move move) {
        return new Bishop(move.getMovedPiece().getPieceAlliance(), move.getDestinationCoordinate(), false);
    }

    @Override
    public String toString() {
        return PieceType.BISHOP.toString();
    }
}
