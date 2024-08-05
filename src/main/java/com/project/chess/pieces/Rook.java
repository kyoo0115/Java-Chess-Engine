package com.project.chess.pieces;

import com.project.chess.Piece;

public class Rook extends Piece {

  public Rook(boolean isWhite, int row, int col) {
    super(isWhite, row, col);
  }

  @Override
  public boolean isValidMove(int destRow, int destCol) {
    return false;
  }
}
