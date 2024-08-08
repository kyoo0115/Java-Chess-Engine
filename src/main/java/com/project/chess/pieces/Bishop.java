package com.project.chess.pieces;

import com.google.common.collect.ImmutableList;
import com.project.chess.Alliance;
import com.project.chess.board.Board;
import com.project.chess.board.Tile;
import com.project.chess.moves.Move;
import com.project.chess.moves.Move.AttackMove;
import com.project.chess.moves.Move.MajorMove;
import com.project.chess.pieces.precompute.BishopBitboard;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Bishop extends Piece {

  public Bishop(final int piecePosition, final Alliance pieceAlliance) {
    super(PieceType.BISHOP, piecePosition, pieceAlliance);
  }

  @Override
  public Collection<Move> calculateLegalMoves(final Board board) {
    final List<Move> legalMoves = new ArrayList<>();
    long possibleMoves = BishopBitboard.getBishopMoves(this.piecePosition);

    for (int i = 0; i < 64; i++) {
      if ((possibleMoves & (1L << i)) != 0) {
        final Tile candidateDestinationTile = board.getTile(i);
        if (!candidateDestinationTile.isTileOccupied()) {
          legalMoves.add(new MajorMove(board, this, i));
        } else {
          final Piece pieceAtDestination = candidateDestinationTile.getPiece();
          final Alliance pieceAlliance = pieceAtDestination.getPieceAlliance();
          if (this.pieceAlliance != pieceAlliance) {
            legalMoves.add(new AttackMove(board, this, i, pieceAtDestination));
          }
        }
      }
    }

    return ImmutableList.copyOf(legalMoves);
  }

  @Override
  public Bishop movePiece(Move move) {
    return new Bishop(move.getDestinationCoordinate(), move.getMovedPiece().getPieceAlliance());
  }

  @Override
  public String toString() {
    return PieceType.BISHOP.toString();
  }
}