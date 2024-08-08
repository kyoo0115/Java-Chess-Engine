package com.project.chess.board;

import com.project.chess.Alliance;
import com.project.chess.pieces.Pawn;
import com.project.chess.pieces.Piece;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * 보드 빌더 클래스입니다.
 */
@Getter
@Setter
public class Builder {

  private final Map<Integer, Piece> boardConfig;
  private Alliance nextMoveMaker;
  private Pawn enpassantPawn;

  public Builder() {
    this.boardConfig = new HashMap<>();
  }

  /**
   * 보드 설정에 조각을 추가합니다.
   *
   * @param piece 추가할 조각
   * @return 현재 빌더 객체
   */
  public Builder setPiece(final Piece piece) {
    this.boardConfig.put(piece.getPiecePosition(), piece);
    return this;
  }

  /**
   * 다음 턴을 누가 시작할지 설정합니다.
   *
   * @param moveMaker 다음 턴을 시작할 연합
   */
  public Builder setMoveMaker(final Alliance moveMaker) {
    this.nextMoveMaker = moveMaker;
    return this;
  }

  /**
   * 현재 빌더 설정을 사용하여 보드를 빌드합니다.
   *
   * @return 생성된 보드 객체
   */
  public Board build() {
    return new Board(this);
  }
}
