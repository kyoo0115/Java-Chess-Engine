package com.project.chess.pieces;

import com.project.chess.Alliance;
import com.project.chess.board.Board;
import com.project.chess.moves.Move;
import java.util.Collection;
import java.util.Objects;
import lombok.Getter;

/**
 * 체스 조각을 나타내는 추상 클래스입니다.
 */
@Getter
public abstract class Piece {

  protected final PieceType pieceType;
  protected final int piecePosition;
  protected final Alliance pieceAlliance;
  protected final boolean isFirstMove;
  private final int cachedHashCode;

  protected Piece(final PieceType pieceType, final int piecePosition,
      final Alliance pieceAlliance) {
    this.pieceType = pieceType;
    this.piecePosition = piecePosition;
    this.pieceAlliance = pieceAlliance;
    this.isFirstMove = false;
    this.cachedHashCode = Objects.hash(this.pieceType, this.piecePosition, this.pieceAlliance);
  }

  public boolean isFirstMove() {
    return this.isFirstMove;
  }

  /**
   * 주어진 보드에서 조각의 합법적인 이동을 계산합니다.
   *
   * @param board 이동을 계산할 보드
   * @return 합법적인 이동 컬렉션
   */
  public abstract Collection<Move> calculateLegalMoves(final Board board);

  public abstract Piece movePiece(Move move);

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Piece)) {
      return false;
    }
    final Piece piece = (Piece) o;
    return piecePosition == piece.getPiecePosition() &&
        isFirstMove == piece.isFirstMove() &&
        pieceType == piece.getPieceType() &&
        pieceAlliance == piece.getPieceAlliance();
  }

  @Override
  public int hashCode() {
    return this.cachedHashCode;
  }

  /**
   * 체스 조각의 타입을 나타내는 열거형입니다.
   */
  public enum PieceType {

    PAWN("P") {
      @Override
      public boolean isKing() {
        return false;
      }

      @Override
      public boolean isRook() {
        return false;
      }
    },
    ROOK("R") {
      @Override
      public boolean isKing() {
        return false;
      }

      @Override
      public boolean isRook() {
        return true;
      }
    },
    KNIGHT("N") {
      @Override
      public boolean isKing() {
        return false;
      }

      @Override
      public boolean isRook() {
        return false;
      }
    },
    BISHOP("B") {
      @Override
      public boolean isKing() {
        return false;
      }

      @Override
      public boolean isRook() {
        return false;
      }
    },
    QUEEN("Q") {
      @Override
      public boolean isKing() {
        return false;
      }

      @Override
      public boolean isRook() {
        return false;
      }
    },
    KING("K") {
      @Override
      public boolean isKing() {
        return true;
      }

      @Override
      public boolean isRook() {
        return false;
      }
    };

    private final String pieceName;

    PieceType(final String pieceName) {
      this.pieceName = pieceName;
    }

    @Override
    public String toString() {
      return this.pieceName;
    }

    /**
     * 해당 조각이 왕인지 확인합니다.
     *
     * @return 조각이 왕인 경우 true, 그렇지 않으면 false
     */
    public abstract boolean isKing();

    public abstract boolean isRook();
  }
}