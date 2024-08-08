package com.project.chess.moves;

import com.project.chess.board.Board;
import com.project.chess.pieces.Piece;
import lombok.Getter;

/**
 * MajorMove 클래스는 공격이 아닌 기본 이동을 나타냅니다.
 */
@Getter
public final class MajorMove extends Move {

  public MajorMove(final Board board, final Piece movedPiece, final int destinationCoordinate) {
    super(board, movedPiece, destinationCoordinate);
  }
}
