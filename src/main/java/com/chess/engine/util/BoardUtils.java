package com.chess.engine.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BoardUtils {

    public static final int NUM_TILES = 64;
    public static final int NUM_TILES_PER_ROW = 8;
    public static final int START_TILE_INDEX = 0;
    public static final List<String> ALGEBRAIC_NOTATION = initializeAlgebraicNotation();
    public static final Map<String, Integer> POSITION_TO_COORDINATE = initializePositionToCoordinateMap();
    private static final boolean[] FIRST_COLUMN = initColumn(0);
    private static final boolean[] SECOND_COLUMN = initColumn(1);
    private static final boolean[] SEVENTH_COLUMN = initColumn(6);
    private static final boolean[] EIGHTH_COLUMN = initColumn(7);
    private static final boolean[] EIGHTH_RANK = initRow(0);
    private static final boolean[] SEVENTH_RANK = initRow(8);
    private static final boolean[] SIXTH_RANK = initRow(16);
    private static final boolean[] FIFTH_RANK = initRow(24);
    private static final boolean[] FOURTH_RANK = initRow(32);
    private static final boolean[] THIRD_RANK = initRow(40);
    private static final boolean[] SECOND_RANK = initRow(48);
    private static final boolean[] FIRST_RANK = initRow(56);

    private BoardUtils() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    // Package-visible accessors for column/rank membership
    public static boolean isFirstColumn(int sq) {
        return FIRST_COLUMN[sq];
    }

    public static boolean isSecondColumn(int sq) {
        return SECOND_COLUMN[sq];
    }

    public static boolean isSeventhColumn(int sq) {
        return SEVENTH_COLUMN[sq];
    }

    public static boolean isEighthColumn(int sq) {
        return EIGHTH_COLUMN[sq];
    }

    public static boolean isFirstRank(int sq) {
        return FIRST_RANK[sq];
    }

    public static boolean isSecondRank(int sq) {
        return SECOND_RANK[sq];
    }

    public static boolean isThirdRank(int sq) {
        return THIRD_RANK[sq];
    }

    public static boolean isFourthRank(int sq) {
        return FOURTH_RANK[sq];
    }

    public static boolean isFifthRank(int sq) {
        return FIFTH_RANK[sq];
    }

    public static boolean isSixthRank(int sq) {
        return SIXTH_RANK[sq];
    }

    public static boolean isSeventhRank(int sq) {
        return SEVENTH_RANK[sq];
    }

    public static boolean isEighthRank(int sq) {
        return EIGHTH_RANK[sq];
    }

    private static boolean[] initColumn(int columnIndex) {
        final boolean[] column = new boolean[NUM_TILES];

        for (int i = columnIndex; i < NUM_TILES; i += NUM_TILES_PER_ROW) {
            column[i] = true;
        }

        return column;
    }

    private static boolean[] initRow(int rowNumber) {
        final boolean[] row = new boolean[NUM_TILES];

        do {
            row[rowNumber] = true;
            rowNumber++;

        } while (rowNumber % NUM_TILES_PER_ROW != 0);

        return row;
    }

    private static Map<String, Integer> initializePositionToCoordinateMap() {
        final Map<String, Integer> positionToCoordinate = new HashMap<>();
        for (int i = START_TILE_INDEX; i < NUM_TILES; i++) {
            positionToCoordinate.put(ALGEBRAIC_NOTATION.get(i), i);
        }
        return Collections.unmodifiableMap(positionToCoordinate);
    }

    private static List<String> initializeAlgebraicNotation() {
        return List.of("a8", "b8", "c8", "d8", "e8", "f8", "g8", "h8",
                "a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7",
                "a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6",
                "a5", "b5", "c5", "d5", "e5", "f5", "g5", "h5",
                "a4", "b4", "c4", "d4", "e4", "f4", "g4", "h4",
                "a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3",
                "a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2",
                "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1");
    }

    public static boolean isValidTileCoordinate(final int coordinate) {
        return coordinate >= 0 && coordinate < NUM_TILES;
    }

    public static int getCoordinateAtPosition(final String position) {
        return POSITION_TO_COORDINATE.get(position);
    }

    public static String getPositionAtCoordinate(final int coordinate) {
        return ALGEBRAIC_NOTATION.get(coordinate);
    }

}
