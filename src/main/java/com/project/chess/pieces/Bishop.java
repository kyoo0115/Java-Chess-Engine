package com.project.chess.pieces;

import com.project.chess.Piece;

public class Bishop extends Piece {

  public Bishop(boolean isWhite, int row, int col) {
    super(isWhite, row, col);
  }

  @Override
  public boolean isValidMove(int destRow, int destCol) {
    return false;
  }
}
