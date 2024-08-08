package com.project.chess.moves;

import com.project.chess.board.Board;
import com.project.chess.pieces.Piece;
import lombok.Getter;

@Getter
public final class PawnAttackMove extends Move {

  public PawnAttackMove(final Board board, final Piece movedPiece, final int destinationCoordinate,
      final Piece attackedPiece) {
    super(board, movedPiece, destinationCoordinate);
  }
}
