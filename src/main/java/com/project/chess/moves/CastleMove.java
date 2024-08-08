package com.project.chess.moves;

import com.project.chess.board.Board;
import com.project.chess.board.Builder;
import com.project.chess.pieces.Piece;
import com.project.chess.pieces.Rook;
import lombok.Getter;

@Getter
public abstract class CastleMove extends Move {

  protected final Rook castleRook;
  protected final int castleRookStart;
  protected final int castleRookDestination;

  public CastleMove(final Board board,
      final Piece movedPiece,
      final int destinationCoordinate,
      final Rook castleRook,
      final int castleRookStart,
      final int castleRookDestination) {
    super(board, movedPiece, destinationCoordinate);
    this.castleRook = castleRook;
    this.castleRookStart = castleRookStart;
    this.castleRookDestination = castleRookDestination;
  }

  @Override
  public boolean isCastlingMove() {
    return true;
  }

  @Override
  public Board execute() {

    final Builder builder = new Builder();
    for (final Piece piece : this.board.getCurrentPlayer().getActivePieces()) {
      if (!this.movedPiece.equals(piece) && !this.castleRook.equals(piece)) {
        builder.setPiece(piece);
      }
    }

    for (final Piece piece : this.board.getCurrentPlayer().getOpponent().getActivePieces()) {
      builder.setPiece(piece);
    }

    builder.setPiece(this.movedPiece.movePiece(this));
    builder.setPiece(new Rook(this.castleRookDestination, this.castleRook.getPieceAlliance()));
    builder.setMoveMaker(this.board.getCurrentPlayer().getOpponent().getAlliance());

    return builder.build();
  }
}
