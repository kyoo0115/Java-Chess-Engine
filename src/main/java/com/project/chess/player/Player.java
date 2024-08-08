package com.project.chess.player;

import static com.project.chess.player.MoveStatus.DONE;
import static com.project.chess.player.MoveStatus.ILLEGAL_MOVE;
import static com.project.chess.player.MoveStatus.LEAVES_PLAYER_IN_CHECK;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.project.chess.Alliance;
import com.project.chess.board.Board;
import com.project.chess.moves.Move;
import com.project.chess.pieces.King;
import com.project.chess.pieces.Piece;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Getter;

/**
 * 체스 플레이어를 나타내는 추상 클래스입니다.
 */
@Getter
public abstract class Player {

  protected final Board board;
  protected final King playerKing;
  protected final Collection<Move> legalMoves;
  private final boolean isInCheck;

  /**
   * 플레이어 생성자.
   *
   * @param board         현재 보드 상태
   * @param legalMoves    플레이어의 합법적인 이동들
   * @param opponentMoves 상대방의 이동들
   */
  protected Player(final Board board,
      final Collection<Move> legalMoves,
      final Collection<Move> opponentMoves) {
    this.board = board;
    this.playerKing = establishKing();
    this.legalMoves = ImmutableList.copyOf(
        Iterables.concat(legalMoves, calculateKingCastles(legalMoves, opponentMoves)));
    this.isInCheck = !calculateAttacksOnTile(this.playerKing.getPiecePosition(),
        opponentMoves).isEmpty();
  }

  /**
   * 주어진 타일에 대한 공격을 계산합니다.
   *
   * @param piecePosition 공격을 계산할 타일 위치
   * @param moves         이동들
   * @return 공격 이동들의 컬렉션
   */
  static Collection<Move> calculateAttacksOnTile(final int piecePosition,
      final Collection<Move> moves) {
    final List<Move> attackMoves = new ArrayList<>();
    for (final Move move : moves) {
      if (piecePosition == move.getDestinationCoordinate()) {
        attackMoves.add(move);
      }
    }
    return ImmutableList.copyOf(attackMoves);
  }

  /**
   * 플레이어의 왕을 찾습니다.
   *
   * @return 플레이어의 왕 조각
   */
  private King establishKing() {
    for (final Piece piece : getActivePieces()) {
      if (piece.getPieceType().isKing()) {
        return (King) piece;
      }
    }
    throw new RuntimeException("Error: unable to establish the player's king!");
  }

  /**
   * 플레이어가 체크 상태인지 확인합니다.
   *
   * @return 체크 상태이면 true, 아니면 false
   */
  public boolean isInCheck() {
    return this.isInCheck;
  }

  /**
   * 플레이어가 체크메이트 상태인지 확인합니다.
   *
   * @return 체크메이트 상태이면 true, 아니면 false
   */
  public boolean isInCheckmate() {
    return this.isInCheck && !hasEscapeMoves();
  }

  /**
   * 플레이어가 스테일메이트 상태인지 확인합니다.
   *
   * @return 스테일메이트 상태이면 true, 아니면 false
   */
  public boolean isInStalemate() {
    return !this.isInCheck && !hasEscapeMoves();
  }

  /**
   * 플레이어가 탈출할 수 있는 이동이 있는지 확인합니다.
   *
   * @return 탈출 이동이 있으면 true, 아니면 false
   */
  protected boolean hasEscapeMoves() {
    for (final Move move : this.legalMoves) {
      final MoveTransition moveTransition = makeMove(move);
      if (moveTransition.getMoveStatus().isDone()) {
        return true;
      }
    }
    return false;
  }

  /**
   * 플레이어가 캐슬링했는지 확인합니다.
   *
   * @return 캐슬링했으면 true, 아니면 false
   */
  public boolean isCastled() {
    return false;
  }

  /**
   * 이동이 합법적인지 확인합니다.
   *
   * @param move 확인할 이동
   * @return 합법적이면 true, 아니면 false
   */
  public boolean isMoveLegal(final Move move) {
    return this.legalMoves.contains(move);
  }

  /**
   * 주어진 이동을 수행합니다.
   *
   * @param move 수행할 이동
   * @return 이동 결과를 포함하는 MoveTransition 객체
   */
  public MoveTransition makeMove(final Move move) {
    if (!isMoveLegal(move)) {
      return new MoveTransition(this.board, move, ILLEGAL_MOVE);
    }

    final Board transitionBoard = move.execute();

    final Collection<Move> kingAttacks = calculateAttacksOnTile(
        transitionBoard.getCurrentPlayer().getOpponent().getPlayerKing().getPiecePosition(),
        transitionBoard.getCurrentPlayer().getLegalMoves());

    if (!kingAttacks.isEmpty()) {
      return new MoveTransition(transitionBoard, move, LEAVES_PLAYER_IN_CHECK);
    }

    return new MoveTransition(transitionBoard, move, DONE);
  }

  /**
   * 활성화된 기물들을 반환합니다.
   *
   * @return 활성화된 기물들의 컬렉션
   */
  public abstract Collection<Piece> getActivePieces();

  /**
   * 플레이어의 연합(색깔)을 반환합니다.
   *
   * @return 연합(색깔)
   */
  public abstract Alliance getAlliance();

  /**
   * 상대방 플레이어를 반환합니다.
   *
   * @return 상대방 플레이어
   */
  public abstract Player getOpponent();

  /**
   * 킹 캐슬링을 계산합니다.
   *
   * @param playerLegals   플레이어의 합법적인 이동들
   * @param opponentLegals 상대방의 합법적인 이동들
   * @return 킹 캐슬링 이동들의 컬렉션
   */
  protected abstract Collection<Move> calculateKingCastles(Collection<Move> playerLegals,
      Collection<Move> opponentLegals);
}