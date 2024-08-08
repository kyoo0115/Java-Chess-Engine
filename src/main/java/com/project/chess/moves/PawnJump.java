package com.project.chess.moves;

import com.project.chess.board.Board;
import com.project.chess.board.Builder;
import com.project.chess.pieces.Pawn;
import com.project.chess.pieces.Piece;
import lombok.Getter;

@Getter
public final class PawnJump extends Move {

  public PawnJump(final Board board, final Piece movedPiece, final int destinationCoordinate) {
    super(board, movedPiece, destinationCoordinate);
  }

  @Override
  public Board execute() {
    final Builder builder = new Builder();

    for (final Piece piece : this.board.getCurrentPlayer().getActivePieces()) {
      if (!this.movedPiece.equals(piece)) {
        builder.setPiece(piece);
      }
    }

    for (final Piece piece : this.board.getCurrentPlayer().getOpponent().getActivePieces()) {
      builder.setPiece(piece);
    }

    final Pawn movedPawn = (Pawn) this.movedPiece.movePiece(this);
    builder.setPiece(movedPawn);
    builder.setEnpassantPawn(movedPawn);
    builder.setMoveMaker(this.board.getCurrentPlayer().getOpponent().getAlliance());
    return builder.build();
  }
}
