package com.project.chess.moves;

import com.project.chess.board.Board;
import com.project.chess.pieces.Piece;
import lombok.Getter;

/**
 * AttackMove 클래스는 공격 이동을 나타내며, 공격된 조각을 포함합니다.
 */
@Getter
public final class AttackMove extends Move {

  final Piece attackedPiece;

  public AttackMove(final Board board, final Piece movedPiece, final int destinationCoordinate,
      final Piece attackedPiece) {
    super(board, movedPiece, destinationCoordinate);
    this.attackedPiece = attackedPiece;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AttackMove otherAttackMove)) {
      return false;
    }
    return super.equals(otherAttackMove) && getAttackedPiece().equals(
        otherAttackMove.getAttackedPiece());
  }

  @Override
  public int hashCode() {
    return this.attackedPiece.hashCode();
  }

  @Override
  public boolean isAttack() {
    return true;
  }
}
