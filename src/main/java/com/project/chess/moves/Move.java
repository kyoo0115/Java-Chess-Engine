package com.project.chess.moves;

import com.project.chess.board.Board;
import com.project.chess.board.Builder;
import com.project.chess.pieces.Piece;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 체스 이동을 나타내는 추상 클래스입니다.
 */
@Getter
@AllArgsConstructor
public abstract class Move {

  final Board board;
  final Piece movedPiece;
  final int destinationCoordinate;

  public static final Move NULL_MOVE = new NullMove();

  public Board execute() {

    final Builder builder = new Builder();

    for (final Piece piece : this.board.getCurrentPlayer().getActivePieces()) {
      if (!this.movedPiece.equals(piece)) {
        builder.setPiece(piece);
      }
    }

    for (final Piece piece : this.board.getCurrentPlayer().getOpponent().getActivePieces()) {
      builder.setPiece(piece);
    }

    builder.setPiece(this.movedPiece.movePiece(this));
    builder.setMoveMaker(this.board.getCurrentPlayer().getOpponent().getAlliance());
    return builder.build();

  }

  public int getCurrentCoordinate() {
    return this.getMovedPiece().getPiecePosition();
  }

  public boolean isAttack() {
    return false;
  }

  public boolean isCastlingMove() {
    return false;
  }

  public Piece getAttackedPiece() {
    return null;
  }

  /**
   * MajorMove 클래스는 공격이 아닌 기본 이동을 나타냅니다.
   */
  @Getter
  public static final class MajorMove extends Move {

    public MajorMove(final Board board, final Piece movedPiece, final int destinationCoordinate) {
      super(board, movedPiece, destinationCoordinate);
    }
  }

  /**
   * AttackMove 클래스는 공격 이동을 나타내며, 공격된 조각을 포함합니다.
   */
  @Getter
  public static final class AttackMove extends Move {

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

}