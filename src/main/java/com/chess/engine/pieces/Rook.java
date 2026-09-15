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

public final class Rook extends Piece {

    private static final int[] ROOK_MOVE_OFFSETS = {-8, -1, 1, 8};

    public Rook(final int piecePosition, final Alliance pieceAlliance) {
        super(PieceType.ROOK, piecePosition, pieceAlliance, true);
    }

    public Rook(final Alliance pieceAlliance, final int piecePosition, final boolean isFirstMove) {
        super(PieceType.ROOK, piecePosition, pieceAlliance, isFirstMove);
    }

    private static boolean isColumnExclusion(final int position, final int offset) {
        return (BoardUtils.isFirstColumn(position) && (offset == -1))
                || (BoardUtils.isEighthColumn(position) && (offset == 1));
    }

    @Override
    public List<Move> calculateLegalMoves(final Board board) {

        final List<Move> legalMoves = new ArrayList<>();

        for (final int offset : ROOK_MOVE_OFFSETS) {

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
                    final Piece destinationPiece = destinationTile.getPiece();

                    if (this.pieceAlliance != destinationPiece.getPieceAlliance()) {
                        legalMoves.add(new MajorAttackMove(board, this, destinationCoordinate, destinationPiece));
                    }
                    break;
                }
            }
        }

        return ImmutableList.copyOf(legalMoves);
    }

    @Override
    public Rook movePiece(final Move move) {
        return new Rook(move.getMovedPiece().getPieceAlliance(), move.getDestinationCoordinate(), false);
    }

    @Override
    public String toString() {
        return PieceType.ROOK.toString();
    }
}
