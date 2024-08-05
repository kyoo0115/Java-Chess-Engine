package com.project.chess;

import com.google.common.collect.ImmutableMap;
import com.project.util.BoardUtil;
import java.util.HashMap;
import java.util.Map;

public abstract class Tile {

  protected final int coordinate;

  private static final Map<Integer, EmptyTile> EMPTY_TILES_CACHE = createAllPossibleEmptyTiles();

  /**
   * Creates a map of all possible empty tiles.
   *
   * @return an immutable map of empty tiles.
   */
  private static Map<Integer, EmptyTile> createAllPossibleEmptyTiles() {
    final Map<Integer, EmptyTile> emptyTiles = new HashMap<>();

    for (int i = 0; i < BoardUtil.NUM_TILES; i++) {
      emptyTiles.put(i, new EmptyTile(i));
    }

    return ImmutableMap.copyOf(emptyTiles);
  }

  /**
   * Factory method to create a Tile.
   *
   * @param coordinate the coordinate of the tile.
   * @param piece the piece on the tile, if any.
   * @return an OccupiedTile if a piece is provided, otherwise an EmptyTile.
   */
  public static Tile createTile(final int coordinate, final Piece piece) {
    return piece != null ? new OccupiedTile(coordinate, piece) : EMPTY_TILES_CACHE.get(coordinate);
  }

  private Tile(final int coordinate) {
    this.coordinate = coordinate;
  }

  public int getCoordinate() {
    return coordinate;
  }

  public abstract boolean isTileOccupied();

  public abstract Piece getPiece();

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
  }

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
  }
}