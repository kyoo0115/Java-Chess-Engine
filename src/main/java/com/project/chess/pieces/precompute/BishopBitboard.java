package com.project.chess.pieces.precompute;

import java.util.HashMap;
import java.util.Map;

public class BishopBitboard {

  private static final Map<Integer, Long> BISHOP_MOVE_MASKS = new HashMap<>();

  static {
    for (int i = 0; i < 64; i++) {
      BISHOP_MOVE_MASKS.put(i, computeBishopMoves(i));
    }
  }

  private static long computeBishopMoves(int position) {
    long moves = 0L;
    int[] directions = {-9, -7, 7, 9};
    for (int direction : directions) {
      int currentPos = position;
      while (isValidCoordinate(currentPos)) {
        currentPos += direction;
        if (isFirstColumnExclusion(currentPos, direction) || isEighthColumnExclusion(currentPos,
            direction)) {
          break;
        }
        if (isValidCoordinate(currentPos)) {
          moves |= 1L << currentPos;
        }
      }
    }
    return moves;
  }

  private static boolean isValidCoordinate(int coordinate) {
    return coordinate >= 0 && coordinate < 64;
  }

  private static boolean isFirstColumnExclusion(int currentPosition, int candidateOffset) {
    return (currentPosition % 8 == 0) && (candidateOffset == -9 || candidateOffset == 7);
  }

  private static boolean isEighthColumnExclusion(int currentPosition, int candidateOffset) {
    return (currentPosition % 8 == 7) && (candidateOffset == -7 || candidateOffset == 9);
  }

  public static long getBishopMoves(int position) {
    return BISHOP_MOVE_MASKS.get(position);
  }
}