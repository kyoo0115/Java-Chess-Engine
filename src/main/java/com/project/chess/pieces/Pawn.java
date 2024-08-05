package com.project.chess.pieces;

import static com.project.util.BoardUtil.SECOND_ROW;
import static com.project.util.BoardUtil.SEVENTH_ROW;
import static com.project.util.BoardUtil.isValidCoordinate;

import com.google.common.collect.ImmutableList;
import com.project.chess.Alliance;
import com.project.chess.Board;
import com.project.chess.Move;
import com.project.chess.Move.MajorMove;
import com.project.chess.Piece;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 체스의 폰(병사)을 나타냅니다.
 */
public class Pawn extends Piece {

  // 폰의 가능한 이동 오프셋
  private static final int[] CANDIDATE_MOVE_COORDINATES = {8, 16};

  /**
   * 폰 조각의 생성자.
   *
   * @param piecePosition 조각의 위치.
   * @param pieceAlliance 조각의 연합(색깔).
   */
  protected Pawn(final int piecePosition, final Alliance pieceAlliance) {
    super(piecePosition, pieceAlliance);
  }

  /**
   * 주어진 보드에서 폰의 합법적인 이동을 계산합니다.
   *
   * @param board 이동을 계산할 보드.
   * @return 합법적인 이동 목록.
   */
  @Override
  public Collection<Move> calculateLegalMoves(final Board board) {

    final List<Move> legalMoves = new ArrayList<>();

    for (final int currentCandidateOffset : CANDIDATE_MOVE_COORDINATES) {
      // 이동 후보 좌표 계산
      int candidateDestinationCoordinate = this.piecePosition + (this.pieceAlliance.getDirection() * currentCandidateOffset);

      if (!isValidCoordinate(candidateDestinationCoordinate)) {
        continue;
      }

      // 한 칸 앞으로 이동
      if (currentCandidateOffset == 8 && !board.getTile(candidateDestinationCoordinate).isTileOccupied()) {
        legalMoves.add(new MajorMove(board, this, candidateDestinationCoordinate));
      }
      // 두 칸 앞으로 이동
      else if (currentCandidateOffset == 16 && this.isFirstMove() &&
          ((SECOND_ROW[this.piecePosition] && this.pieceAlliance.isBlack()) ||
              (SEVENTH_ROW[this.piecePosition] && this.pieceAlliance.isWhite()))) {
        final int behindCandidateDestinationCoordinate = this.piecePosition + (this.pieceAlliance.getDirection() * 8);
        if (!board.getTile(behindCandidateDestinationCoordinate).isTileOccupied() &&
            !board.getTile(candidateDestinationCoordinate).isTileOccupied()) {
          legalMoves.add(new MajorMove(board, this, candidateDestinationCoordinate));
        }
      }
    }
    return ImmutableList.copyOf(legalMoves);
  }
}