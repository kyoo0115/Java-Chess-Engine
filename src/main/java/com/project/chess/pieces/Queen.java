package com.project.chess.pieces;

import static com.project.chess.board.BoardUtil.isValidCoordinate;

import com.google.common.collect.ImmutableList;
import com.project.chess.Alliance;
import com.project.chess.board.Board;
import com.project.chess.board.BoardUtil;
import com.project.chess.board.Tile;
import com.project.chess.moves.Move;
import com.project.chess.moves.Move.AttackMove;
import com.project.chess.moves.Move.MajorMove;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Queen extends Piece {

  private static final int[] CANDIDATE_MOVE_VECTOR_COORDINATES = {-9, -8, -7, -1, 1, 7, 8, 9};

  public Queen(final int piecePosition, final Alliance pieceAlliance) {
    super(PieceType.QUEEN, piecePosition, pieceAlliance);
  }

  @Override
  public Collection<Move> calculateLegalMoves(final Board board) {
    final List<Move> legalMoves = new ArrayList<>();

    // 가능한 모든 이동 벡터 오프셋을 확인합니다.
    for (final int candidateOffset : CANDIDATE_MOVE_VECTOR_COORDINATES) {
      int candidateDestinationCoordinate = this.piecePosition;

      // 후보 목적지 좌표가 유효한지 확인합니다.
      while (isValidCoordinate(candidateDestinationCoordinate)) {

        // 특정 열에서의 특수 케이스를 처리합니다.
        if (isFirstColumnExclusion(candidateDestinationCoordinate, candidateOffset) ||
            isEighthColumnExclusion(candidateDestinationCoordinate, candidateOffset)) {
          break; // 이 경우는 무시합니다.
        }

        candidateDestinationCoordinate += candidateOffset;

        if (isValidCoordinate(candidateDestinationCoordinate)) {
          // 후보 목적지 타일을 가져옵니다.
          final Tile candidateDestinationTile = board.getTile(candidateDestinationCoordinate);

          // 후보 목적지 타일이 비어 있는지 확인합니다.
          if (!candidateDestinationTile.isTileOccupied()) {
            // 타일이 비어 있으면 MajorMove를 추가합니다.
            legalMoves.add(new MajorMove(board, this, candidateDestinationCoordinate));
          } else {
            // 타일에 조각이 있는 경우
            final Piece pieceAtDestination = candidateDestinationTile.getPiece();
            final Alliance pieceAlliance = pieceAtDestination.getPieceAlliance();

            // 목적지의 조각이 적군 조각인 경우 AttackMove를 추가합니다.
            if (this.pieceAlliance != pieceAlliance) {
              legalMoves.add(
                  new AttackMove(board, this, candidateDestinationCoordinate, pieceAtDestination));
            }
            // 비숍은 조각을 뛰어넘을 수 없으므로 루프를 종료합니다.
            break;
          }
        } else {
          // 목적지 좌표가 유효하지 않으면 루프를 종료합니다.
          break;
        }
      }
    }
    // ImmutableList로 변환하여 반환합니다.
    return ImmutableList.copyOf(legalMoves);
  }

  /**
   * 현재 위치가 첫 번째 열에 있고 후보 오프셋이 유효하지 않은 이동을 초래할 경우 확인합니다.
   *
   * @param currentPosition 조각의 현재 위치.
   * @param candidateOffset 후보 이동 오프셋.
   * @return 첫 번째 열 배제에 의해 이동이 유효하지 않은 경우 true, 그렇지 않으면 false.
   */
  private static boolean isFirstColumnExclusion(final int currentPosition,
      final int candidateOffset) {
    return BoardUtil.FIRST_COLUMN[currentPosition] && (candidateOffset == -1
        || candidateOffset == -9 || candidateOffset == 7);
  }

  /**
   * 현재 위치가 여덟 번째 열에 있고 후보 오프셋이 유효하지 않은 이동을 초래할 경우 확인합니다.
   *
   * @param currentPosition 조각의 현재 위치.
   * @param candidateOffset 후보 이동 오프셋.
   * @return 여덟 번째 열 배제에 의해 이동이 유효하지 않은 경우 true, 그렇지 않으면 false.
   */
  private static boolean isEighthColumnExclusion(final int currentPosition,
      final int candidateOffset) {
    return BoardUtil.EIGHTH_COLUMN[currentPosition] && (candidateOffset == 1
        || candidateOffset == -7 || candidateOffset == 9);
  }

  @Override
  public Queen movePiece(Move move) {
    return new Queen(move.getDestinationCoordinate(), move.getMovedPiece().getPieceAlliance());
  }

  @Override
  public String toString() {
    return PieceType.QUEEN.toString();
  }
}
