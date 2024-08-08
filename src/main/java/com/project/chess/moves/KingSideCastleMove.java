package com.project.chess.moves;

import com.project.chess.board.Board;
import com.project.chess.pieces.Piece;
import com.project.chess.pieces.Rook;
import lombok.Getter;

@Getter
public final class KingSideCastleMove extends CastleMove {

  public KingSideCastleMove(final Board board,
      final Piece movedPiece,
      final int destinationCoordinate,
      final Rook castleRook,
      final int castleRookStart,
      final int castleRookDestination) {
    super(board, movedPiece, destinationCoordinate, castleRook, castleRookStart,
        castleRookDestination);
  }

  @Override
  public String toString() {
    return "O-O";
  }
}
