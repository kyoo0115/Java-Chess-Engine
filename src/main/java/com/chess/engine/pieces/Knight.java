package com.chess.engine.pieces;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.Tile;
import com.chess.engine.util.BoardUtils;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import static com.chess.engine.board.Move.MajorAttackMove;
import static com.chess.engine.board.Move.MajorMove;

public final class Knight extends Piece {

    private static final int[] KNIGHT_MOVE_OFFSETS = {
            -17, -15, -10, -6, 6, 10, 15, 17
    };

    public Knight(final int piecePosition, final Alliance pieceAlliance) {
        super(PieceType.KNIGHT, piecePosition, pieceAlliance, true);
    }

    public Knight(final Alliance pieceAlliance, final int piecePosition, final boolean isFirstMove) {
        super(PieceType.KNIGHT, piecePosition, pieceAlliance, isFirstMove);
    }

    private static boolean isColumnExclusion(final int position, final int offset) {
        return isFirstColumnExclusion(position, offset)
                || isSecondColumnExclusion(position, offset)
                || isSeventhColumnExclusion(position, offset)
                || isEighthColumnExclusion(position, offset);
    }

    private static boolean isFirstColumnExclusion(final int position, final int offset) {
        return BoardUtils.FIRST_COLUMN[position]
                && (offset == -17 || offset == -10 || offset == 6 || offset == 15);
    }

    private static boolean isSecondColumnExclusion(final int position, final int offset) {
        return BoardUtils.SECOND_COLUMN[position]
                && (offset == -10 || offset == 6);
    }

    private static boolean isSeventhColumnExclusion(final int position, final int offset) {
        return BoardUtils.SEVENTH_COLUMN[position]
                && (offset == -6 || offset == 10);
    }

    private static boolean isEighthColumnExclusion(final int position, final int offset) {
        return BoardUtils.EIGHTH_COLUMN[position]
                && (offset == -15 || offset == -6 || offset == 10 || offset == 17);
    }

    @Override
    public List<Move> calculateLegalMoves(final Board board) {

        final List<Move> legalMoves = new ArrayList<>();

        for (final int offset : KNIGHT_MOVE_OFFSETS) {

            if (isColumnExclusion(this.piecePosition, offset)) {
                continue;
            }

            final int destinationCoordinate = this.piecePosition + offset;

            if (!BoardUtils.isValidTileCoordinate(destinationCoordinate)) {
                continue;
            }

            final Tile destinationTile = board.getTile(destinationCoordinate);

            if (!destinationTile.isTileOccupied()) {
                legalMoves.add(new MajorMove(board, this, destinationCoordinate));
                continue;
            }

            final Piece destinationPiece = destinationTile.getPiece();

            if (this.pieceAlliance != destinationPiece.getPieceAlliance()) {
                legalMoves.add(new MajorAttackMove(board, this, destinationCoordinate, destinationPiece));
            }
        }

        return ImmutableList.copyOf(legalMoves);
    }

    @Override
    public Knight movePiece(final Move move) {
        return new Knight(move.getMovedPiece().getPieceAlliance(), move.getDestinationCoordinate(), false);
    }

    @Override
    public String toString() {
        return PieceType.KNIGHT.toString();
    }
}
