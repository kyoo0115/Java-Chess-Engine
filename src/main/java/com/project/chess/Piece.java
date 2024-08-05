package com.project.chess;

import java.util.Collection;

public abstract class Piece {

  protected final int piecePosition;
  protected final Alliance pieceAlliance;
  protected final boolean isFirstMove;

  protected Piece(final int piecePosition, final Alliance pieceAlliance) {
    this.piecePosition = piecePosition;
    this.pieceAlliance = pieceAlliance;
    this.isFirstMove = false;
  }

  public Alliance getPieceAlliance() {
    return this.pieceAlliance;
  }

  public boolean isFirstMove() {
    return this.isFirstMove;
  }

  public abstract Collection<Move> calculateLegalMoves(final Board board);
}