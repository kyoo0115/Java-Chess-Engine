package com.chess.engine.pieces;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.util.BoardUtils;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public final class Rook extends Piece {

    private static final int[] ROOK_MOVE_OFFSETS = {-8, -1, 1, 8};

    public Rook(final int piecePosition, final Alliance pieceAlliance) {
        super(PieceType.ROOK, piecePosition, pieceAlliance, true);
    }

    public Rook(final Alliance pieceAlliance, final int piecePosition, final boolean isFirstMove) {
        super(PieceType.ROOK, piecePosition, pieceAlliance, isFirstMove);
    }

    @Override
    public List<Move> calculateLegalMoves(final Board board) {
        final List<Move> legalMoves = new ArrayList<>();
        addSlidingMoves(board, this, this.piecePosition, ROOK_MOVE_OFFSETS,
                (pos, off) -> (BoardUtils.isFirstColumn(pos) && off == -1)
                           || (BoardUtils.isEighthColumn(pos) && off == 1),
                legalMoves);
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
