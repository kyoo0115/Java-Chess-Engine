package com.project.chess.board;

/**
 * 체스 보드에 대한 유틸리티 클래스입니다.
 */
public final class BoardUtil {

  public static final boolean[] FIRST_COLUMN = initColumn(0);
  public static final boolean[] SECOND_COLUMN = initColumn(1);
  public static final boolean[] SEVENTH_COLUMN = initColumn(6);
  public static final boolean[] EIGHTH_COLUMN = initColumn(7);

  public static final boolean[] EIGHTH_RANK = initRow(0);
  public static final boolean[] SEVENTH_RANK = initRow(1);
  public static final boolean[] SIXTH_RANK = initRow(2);
  public static final boolean[] FIFTH_RANK = initRow(3);
  public static final boolean[] FOURTH_RANK = initRow(4);
  public static final boolean[] THIRD_RANK = initRow(5);
  public static final boolean[] SECOND_RANK = initRow(6);
  public static final boolean[] FIRST_RANK = initRow(7);

  public static final int NUM_TILES = 64;
  public static final int NUM_TILES_PER_ROW = 8;

  /**
   * 유틸리티 클래스의 인스턴스화를 방지합니다.
   */
  private BoardUtil() {
    throw new RuntimeException("이 클래스는 인스턴스화할 수 없습니다.");
  }

  /**
   * 지정된 열의 모든 위치를 표시하는 배열을 초기화합니다.3
   *
   * @param columnNumber 표시할 열.
   * @return 지정된 열의 위치에 true가 있는 배열.
   */
  private static boolean[] initColumn(final int columnNumber) {
    final boolean[] column = new boolean[NUM_TILES];
    int tempColumnNumber = columnNumber;
    do {
      column[tempColumnNumber] = true;
      tempColumnNumber += NUM_TILES_PER_ROW;
    } while (tempColumnNumber < NUM_TILES);
    return column;
  }

  /**
   * 지정된 행의 모든 위치를 표시하는 배열을 초기화합니다.
   *
   * @param rowNumber 표시할 행.
   * @return 지정된 행의 위치에 true가 있는 배열.
   */
  private static boolean[] initRow(final int rowNumber) {
    final boolean[] row = new boolean[NUM_TILES];
    int startIndex = rowNumber * NUM_TILES_PER_ROW;
    int endIndex = startIndex + NUM_TILES_PER_ROW;
    for (int i = startIndex; i < endIndex; i++) {
      row[i] = true;
    }
    return row;
  }

  /**
   * 좌표가 보드의 유효 범위 내에 있는지 확인합니다.
   *
   * @param coordinate 확인할 좌표.
   * @return 좌표가 유효한 경우 true, 그렇지 않으면 false.
   */
  public static boolean isValidCoordinate(final int coordinate) {
    return coordinate >= 0 && coordinate < NUM_TILES;
  }
}