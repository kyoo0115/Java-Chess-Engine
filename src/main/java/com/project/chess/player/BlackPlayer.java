package com.project.chess.player;

import com.google.common.collect.ImmutableList;
import com.project.chess.Alliance;
import com.project.chess.board.Board;
import com.project.chess.board.Tile;
import com.project.chess.moves.KingSideCastleMove;
import com.project.chess.moves.Move;
import com.project.chess.moves.QueenSideCastleMove;
import com.project.chess.pieces.Piece;
import com.project.chess.pieces.Rook;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Getter;

/**
 * 블랙 플레이어를 나타내는 클래스입니다.
 */
@Getter
public class BlackPlayer extends Player {

  // 캐슬링을 위한 상수들
  private static final int KING_SIDE_CASTLE_KING_FINAL_POSITION = 6;
  private static final int KING_SIDE_CASTLE_ROOK_FINAL_POSITION = 5;
  private static final int KING_SIDE_ROOK_POSITION = 7;

  private static final int QUEEN_SIDE_CASTLE_KING_FINAL_POSITION = 2;
  private static final int QUEEN_SIDE_CASTLE_ROOK_FINAL_POSITION = 3;
  private static final int QUEEN_SIDE_ROOK_POSITION = 0;

  private static final int[] KING_SIDE_CASTLE_EMPTY_TILES = {5, 6};
  private static final int[] QUEEN_SIDE_CASTLE_EMPTY_TILES = {1, 2, 3};

  /**
   * 블랙 플레이어 생성자.
   *
   * @param board                   현재 보드 상태
   * @param whiteStandardLegalMoves 화이트 플레이어의 합법적인 이동들
   * @param blackStandardLegalMoves 블랙 플레이어의 합법적인 이동들
   */
  public BlackPlayer(final Board board,
      final Collection<Move> whiteStandardLegalMoves,
      final Collection<Move> blackStandardLegalMoves) {
    super(board, blackStandardLegalMoves, whiteStandardLegalMoves);
  }

  @Override
  public Collection<Piece> getActivePieces() {
    return this.board.getBlackPieces();
  }

  @Override
  public Alliance getAlliance() {
    return Alliance.BLACK;
  }

  @Override
  public Player getOpponent() {
    return this.board.getWhitePlayer();
  }

  @Override
  protected Collection<Move> calculateKingCastles(final Collection<Move> playerLegals,
      final Collection<Move> opponentLegals) {

    final List<Move> kingCastles = new ArrayList<>();

    if (this.playerKing.isFirstMove() && !this.isInCheck()) {
      // 블랙 킹 캐슬링
      // 킹사이드 캐슬링
      addKingSideCastle(playerLegals, opponentLegals, kingCastles);
      // 퀸사이드 캐슬링
      addQueenSideCastle(playerLegals, opponentLegals, kingCastles);
    }

    return ImmutableList.copyOf(kingCastles);
  }

  /**
   * 킹사이드 캐슬링을 추가합니다.
   *
   * @param playerLegals   플레이어의 합법적인 이동들
   * @param opponentLegals 상대방의 합법적인 이동들
   * @param kingCastles    캐슬링 이동 목록
   */
  private void addKingSideCastle(final Collection<Move> playerLegals,
      final Collection<Move> opponentLegals,
      final List<Move> kingCastles) {

    if (areTilesEmpty(KING_SIDE_CASTLE_EMPTY_TILES) &&
        !isUnderAttack(KING_SIDE_CASTLE_EMPTY_TILES, opponentLegals)) {
      final Tile rookTile = this.board.getTile(KING_SIDE_ROOK_POSITION);

      if (isRookFirstMove(rookTile)) {
        kingCastles.add(new KingSideCastleMove(
            this.board,
            this.playerKing,
            KING_SIDE_CASTLE_KING_FINAL_POSITION,
            (Rook) rookTile.getPiece(),
            rookTile.getTileCoordinate(),
            KING_SIDE_CASTLE_ROOK_FINAL_POSITION));
      }
    }
  }

  /**
   * 퀸사이드 캐슬링을 추가합니다.
   *
   * @param playerLegals   플레이어의 합법적인 이동들
   * @param opponentLegals 상대방의 합법적인 이동들
   * @param kingCastles    캐슬링 이동 목록
   */
  private void addQueenSideCastle(final Collection<Move> playerLegals,
      final Collection<Move> opponentLegals,
      final List<Move> kingCastles) {

    if (areTilesEmpty(QUEEN_SIDE_CASTLE_EMPTY_TILES) &&
        !isUnderAttack(QUEEN_SIDE_CASTLE_EMPTY_TILES, opponentLegals)) {
      final Tile rookTile = this.board.getTile(QUEEN_SIDE_ROOK_POSITION);

      if (isRookFirstMove(rookTile)) {
        kingCastles.add(new QueenSideCastleMove(
            this.board,
            this.playerKing,
            QUEEN_SIDE_CASTLE_KING_FINAL_POSITION,
            (Rook) rookTile.getPiece(),
            rookTile.getTileCoordinate(),
            QUEEN_SIDE_CASTLE_ROOK_FINAL_POSITION));
      }
    }
  }

  /**
   * 주어진 타일들이 비어있는지 확인합니다.
   *
   * @param positions 확인할 타일 위치들
   * @return 타일들이 비어있으면 true, 아니면 false
   */
  private boolean areTilesEmpty(int[] positions) {
    for (int position : positions) {
      if (this.board.getTile(position).isTileOccupied()) {
        return false;
      }
    }
    return true;
  }

  /**
   * 주어진 타일들이 공격받고 있는지 확인합니다.
   *
   * @param positions      확인할 타일 위치들
   * @param opponentLegals 상대방의 합법적인 이동들
   * @return 타일들이 공격받고 있으면 true, 아니면 false
   */
  private boolean isUnderAttack(int[] positions, Collection<Move> opponentLegals) {
    for (int position : positions) {
      if (!Player.calculateAttacksOnTile(position, opponentLegals).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  /**
   * 주어진 타일의 룩이 첫 이동인지 확인합니다.
   *
   * @param rookTile 확인할 타일
   * @return 룩이 첫 이동이면 true, 아니면 false
   */
  private boolean isRookFirstMove(Tile rookTile) {
    return rookTile.isTileOccupied() &&
        rookTile.getPiece().isFirstMove() &&
        rookTile.getPiece().getPieceType().isRook();
  }
}