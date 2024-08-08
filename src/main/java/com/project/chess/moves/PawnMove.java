package com.project.chess.moves;

import com.project.chess.board.Board;
import com.project.chess.pieces.Piece;
import lombok.Getter;

@Getter
public final class PawnMove extends Move {

  public PawnMove(final Board board, final Piece movedPiece, final int destinationCoordinate) {
    super(board, movedPiece, destinationCoordinate);
  }
}
