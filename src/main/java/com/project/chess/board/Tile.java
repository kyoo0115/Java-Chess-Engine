package com.project.chess.board;

import com.google.common.collect.ImmutableMap;
import com.project.chess.pieces.Piece;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * 체스 보드의 타일을 나타내는 추상 클래스입니다.
 */
@Getter
public abstract class Tile {

  protected final int tileCoordinate;
  private static final Map<Integer, EmptyTile> EMPTY_TILES_CACHE = createAllPossibleEmptyTiles();

  /**
   * 모든 가능한 빈 타일의 맵을 생성합니다.
   *
   * @return 빈 타일의 불변 맵
   */
  private static Map<Integer, EmptyTile> createAllPossibleEmptyTiles() {
    final Map<Integer, EmptyTile> emptyTiles = new HashMap<>();
    for (int i = 0; i < BoardUtil.NUM_TILES; i++) {
      emptyTiles.put(i, new EmptyTile(i));
    }
    return ImmutableMap.copyOf(emptyTiles);
  }

  /**
   * 타일을 생성하는 팩토리 메서드입니다.
   *
   * @param coordinate 타일의 좌표
   * @param piece      타일에 있는 조각, 없을 경우 null
   * @return 조각이 있으면 OccupiedTile, 없으면 EmptyTile을 반환
   */
  public static Tile createTile(final int coordinate, final Piece piece) {
    return piece != null ? new OccupiedTile(coordinate, piece) : EMPTY_TILES_CACHE.get(coordinate);
  }

  private Tile(final int tileCoordinate) {
    this.tileCoordinate = tileCoordinate;
  }

  public abstract boolean isTileOccupied();

  public abstract Piece getPiece();

  /**
   * 빈 타일을 나타내는 클래스입니다.
   */
  public static final class EmptyTile extends Tile {

    EmptyTile(final int coordinate) {
      super(coordinate);
    }

    @Override
    public boolean isTileOccupied() {
      return false;
    }

    @Override
    public Piece getPiece() {
      return null;
    }

    @Override
    public String toString() {
      return "-";
    }
  }

  /**
   * 조각이 있는 타일을 나타내는 클래스입니다.
   */
  public static final class OccupiedTile extends Tile {

    private final Piece pieceOnTile;

    OccupiedTile(final int coordinate, final Piece pieceOnTile) {
      super(coordinate);
      this.pieceOnTile = pieceOnTile;
    }

    @Override
    public boolean isTileOccupied() {
      return true;
    }

    @Override
    public Piece getPiece() {
      return pieceOnTile;
    }

    @Override
    public String toString() {
      return getPiece().getPieceAlliance().isBlack() ? getPiece().toString().toLowerCase()
          : getPiece().toString();
    }
  }
}