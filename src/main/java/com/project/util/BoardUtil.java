package com.project.util;

/**
 * 체스 보드에 대한 유틸리티 클래스입니다.
 */
public class BoardUtil {

  public static final boolean[] FIRST_COLUMN = initColumn(0);
  public static final boolean[] SECOND_COLUMN = initColumn(1);
  public static final boolean[] SEVENTH_COLUMN = initColumn(6);
  public static final boolean[] EIGHTH_COLUMN = initColumn(7);

  public static final int NUM_TILES = 64;
  public static final int NUM_TILES_PER_ROW = 8;

  /**
   * 지정된 열의 모든 위치를 표시하는 배열을 초기화합니다.
   *
   * @param columnNumber 표시할 열.
   * @return 지정된 열의 위치에 true가 있는 배열.
   */
  private static boolean[] initColumn(int columnNumber) {
    final boolean[] column = new boolean[NUM_TILES];
    do {
      column[columnNumber] = true;
      columnNumber += NUM_TILES_PER_ROW;
    } while (columnNumber < NUM_TILES);
    return column;
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