package com.project.chess;

import java.util.Collection;

public abstract class Piece {

  protected final int piecePosition;
  protected final Alliance pieceAlliance;

  protected Piece(final int piecePosition, final Alliance pieceAlliance) {
    this.piecePosition = piecePosition;
    this.pieceAlliance = pieceAlliance;
  }

  public Alliance getPieceAlliance() {
    return this.pieceAlliance;
  }

  public abstract Collection<Move> calculateLegalMoves(final Board board);
}