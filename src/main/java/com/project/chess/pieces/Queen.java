package com.project.chess.pieces;

import com.project.chess.Piece;

public class Queen extends Piece {

  public Queen(boolean isWhite, int row, int col) {
    super(isWhite, row, col);
  }

  @Override
  public boolean isValidMove(int destRow, int destCol) {
    return false;
  }
}
