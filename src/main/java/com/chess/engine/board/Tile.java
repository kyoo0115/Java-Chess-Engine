package com.chess.engine.board;

import com.chess.engine.pieces.Piece;

public abstract class Tile {

    public static final int NUM_TILES = 64;
    // Plain array: direct index lookup, no boxing, no hashing.
    private static final EmptyTile[] EMPTY_TILES = initEmptyTiles();
    protected final int tileCoordinate;

    protected Tile(int tileCoordinate) {
        this.tileCoordinate = tileCoordinate;
    }

    private static EmptyTile[] initEmptyTiles() {
        final EmptyTile[] tiles = new EmptyTile[NUM_TILES];
        for (int coordinate = 0; coordinate < NUM_TILES; coordinate++) {
            tiles[coordinate] = new EmptyTile(coordinate);
        }
        return tiles;
    }

    public static Tile createTile(final int tileCoordinate, final Piece piece) {
        return piece == null
                ? EMPTY_TILES[tileCoordinate]
                : new OccupiedTile(tileCoordinate, piece);
    }

    public abstract boolean isTileOccupied();

    public abstract Piece getPiece();

    public int getTileCoordinate() {
        return this.tileCoordinate;
    }

    public static final class EmptyTile extends Tile {
        private EmptyTile(final int coordinate) {
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

    public static final class OccupiedTile extends Tile {

        private final Piece pieceOnTile;

        private OccupiedTile(int tileCoordinate, Piece pieceOnTile) {
            super(tileCoordinate);
            this.pieceOnTile = pieceOnTile;
        }

        @Override
        public boolean isTileOccupied() {
            return true;
        }

        @Override
        public Piece getPiece() {
            return this.pieceOnTile;
        }

        @Override
        public String toString() {
            return getPiece().getPieceAlliance().isBlack() ? getPiece().toString().toLowerCase() :
                    getPiece().toString();
        }
    }
}
