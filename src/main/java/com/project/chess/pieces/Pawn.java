package com.project.chess.pieces;

import static com.project.chess.board.BoardUtil.EIGHTH_COLUMN;
import static com.project.chess.board.BoardUtil.FIRST_COLUMN;
import static com.project.chess.board.BoardUtil.SEVENTH_RANK;
import static com.project.chess.board.BoardUtil.SECOND_RANK;
import static com.project.chess.board.BoardUtil.isValidCoordinate;

import com.google.common.collect.ImmutableList;
import com.project.chess.Alliance;
import com.project.chess.board.Board;
import com.project.chess.moves.Move;
import com.project.chess.moves.Move.MajorMove;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 체스의 폰(병사)을 나타냅니다.
 */
public class Pawn extends Piece {

  // 폰의 가능한 이동 오프셋
  private static final int[] CANDIDATE_MOVE_COORDINATES = {7, 8, 9, 16};

  /**
   * 폰 조각의 생성자.
   *
   * @param piecePosition 조각의 위치.
   * @param pieceAlliance 조각의 연합(색깔).
   */
  public Pawn(final int piecePosition, final Alliance pieceAlliance) {
    super(PieceType.PAWN, piecePosition, pieceAlliance);
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
      int candidateDestinationCoordinate =
          this.piecePosition + (this.pieceAlliance.getDirection() * currentCandidateOffset);

      // 후보 좌표가 유효한지 확인
      if (!isValidCoordinate(candidateDestinationCoordinate)) {
        continue;
      }

      // 한 칸 앞으로 이동
      if (currentCandidateOffset == 8 && !board.getTile(candidateDestinationCoordinate)
          .isTileOccupied()) {
        // TODO: 폰 프로모션 처리
        legalMoves.add(new MajorMove(board, this, candidateDestinationCoordinate));
      }
      // 두 칸 앞으로 이동
      else if (currentCandidateOffset == 16 && this.isFirstMove() &&
          ((SEVENTH_RANK[this.piecePosition] && this.pieceAlliance.isBlack()) ||
              (SECOND_RANK[this.piecePosition] && this.pieceAlliance.isWhite()))) {
        final int behindCandidateDestinationCoordinate =
            this.piecePosition + (this.pieceAlliance.getDirection() * 8);
        if (!board.getTile(behindCandidateDestinationCoordinate).isTileOccupied() &&
            !board.getTile(candidateDestinationCoordinate).isTileOccupied()) {
          legalMoves.add(new MajorMove(board, this, candidateDestinationCoordinate));
        }
      }
      // 대각선 왼쪽 이동 (공격)
      else if (currentCandidateOffset == 7 &&
          !((EIGHTH_COLUMN[this.piecePosition] && this.pieceAlliance.isWhite()) ||
              (FIRST_COLUMN[this.piecePosition] && this.pieceAlliance.isBlack()))) {
        if (board.getTile(candidateDestinationCoordinate).isTileOccupied()) {
          final Piece pieceOnCandidate = board.getTile(candidateDestinationCoordinate).getPiece();
          if (this.pieceAlliance != pieceOnCandidate.getPieceAlliance()) {
            legalMoves.add(new MajorMove(board, this, candidateDestinationCoordinate));
          }
        }
      }
      // 대각선 오른쪽 이동 (공격)
      else if (currentCandidateOffset == 9 &&
          !((FIRST_COLUMN[this.piecePosition] && this.pieceAlliance.isWhite()) ||
              (EIGHTH_COLUMN[this.piecePosition] && this.pieceAlliance.isBlack()))) {
        if (board.getTile(candidateDestinationCoordinate).isTileOccupied()) {
          final Piece pieceOnCandidate = board.getTile(candidateDestinationCoordinate).getPiece();
          if (this.pieceAlliance != pieceOnCandidate.getPieceAlliance()) {
            legalMoves.add(new MajorMove(board, this, candidateDestinationCoordinate));
          }
        }
      }
    }
    return ImmutableList.copyOf(legalMoves);
  }

  @Override
  public Pawn movePiece(Move move) {
    return new Pawn(move.getDestinationCoordinate(), move.getMovedPiece().getPieceAlliance());
  }

  @Override
  public String toString() {
    return PieceType.PAWN.toString();
  }
}