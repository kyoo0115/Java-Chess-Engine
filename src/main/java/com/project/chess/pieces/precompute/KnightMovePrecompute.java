package com.project.chess.pieces.precompute;

import com.google.common.collect.ImmutableMap;
import com.project.util.BoardUtil;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class KnightMovePrecompute {

  private static final int[] CANDIDATE_MOVE_COORDINATES = {-17, -15, -10, -6, 6, 10, 15, 17};
  private static final ImmutableMap<Integer, List<Integer>> KNIGHT_MOVES_CACHE;

  static {
    Map<Integer, List<Integer>> movesMap = new HashMap<>();
    for (int i = 0; i < 64; i++) {
      movesMap.put(i, calculateKnightMoves(i));
    }
    KNIGHT_MOVES_CACHE = ImmutableMap.copyOf(movesMap);
  }

  private static List<Integer> calculateKnightMoves(int position) {
    List<Integer> legalMoves = new ArrayList<>();
    for (int offset : CANDIDATE_MOVE_COORDINATES) {
      int candidateCoordinate = position + offset;
      if (isValidCoordinate(candidateCoordinate)) {
        if (isFirstColumnExclusion(position, offset) ||
            isSecondColumnExclusion(position, offset) ||
            isSeventhColumnExclusion(position, offset) ||
            isEighthColumnExclusion(position, offset)) {
          continue;
        }
        legalMoves.add(candidateCoordinate);
      }
    }
    return legalMoves;
  }

  private static boolean isFirstColumnExclusion(int currentPosition, int candidateOffset) {
    return BoardUtil.FIRST_COLUMN[currentPosition] &&
        (candidateOffset == -17 || candidateOffset == -10 || candidateOffset == 6 || candidateOffset == 15);
  }

  private static boolean isSecondColumnExclusion(int currentPosition, int candidateOffset) {
    return BoardUtil.SECOND_COLUMN[currentPosition] &&
        (candidateOffset == -10 || candidateOffset == 6);
  }

  private static boolean isSeventhColumnExclusion(int currentPosition, int candidateOffset) {
    return BoardUtil.SEVENTH_COLUMN[currentPosition] &&
        (candidateOffset == -6 || candidateOffset == 10);
  }

  private static boolean isEighthColumnExclusion(int currentPosition, int candidateOffset) {
    return BoardUtil.EIGHTH_COLUMN[currentPosition] &&
        (candidateOffset == -15 || candidateOffset == -6 || candidateOffset == 10 || candidateOffset == 17);
  }

  private static boolean isValidCoordinate(final int coordinate) {
    return coordinate >= 0 && coordinate < 64;
  }

  public static List<Integer> getKnightMoves(int position) {
    return KNIGHT_MOVES_CACHE.get(position);
  }

}