package com.project.chess.moves;

import com.project.chess.board.Board;
import com.project.chess.pieces.Piece;
import lombok.Getter;

@Getter
public final class PawnEnpassantAttackMove extends Move {

  public PawnEnpassantAttackMove(final Board board, final Piece movedPiece,
      final int destinationCoordinate,
      final Piece attackedPiece) {
    super(board, movedPiece, destinationCoordinate);
  }
}
