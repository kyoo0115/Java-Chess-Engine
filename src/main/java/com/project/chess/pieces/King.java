package com.project.chess.pieces;

import static com.project.chess.board.BoardUtil.FIRST_COLUMN;
import static com.project.chess.board.BoardUtil.isValidCoordinate;

import com.google.common.collect.ImmutableList;
import com.project.chess.Alliance;
import com.project.chess.board.Board;
import com.project.chess.board.Tile;
import com.project.chess.moves.Move;
import com.project.chess.moves.Move.AttackMove;
import com.project.chess.moves.Move.MajorMove;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class King extends Piece {

  private static final int[] CANDIDATE_MOVE_VECTOR_COORDINATES = {-9, -8, -7, -1, 1, 7, 8, 9};

  public King(final int piecePosition, final Alliance pieceAlliance) {
    super(PieceType.KING, piecePosition, pieceAlliance);
  }

  @Override
  public Collection<Move> calculateLegalMoves(Board board) {
    final List<Move> legalMoves = new ArrayList<>();

    for (final int currentCandidateOffset : CANDIDATE_MOVE_VECTOR_COORDINATES) {
      final int candidateDestinationCoordinate = this.piecePosition + currentCandidateOffset;

      if (isFirstColumnExclusion(this.piecePosition, currentCandidateOffset) ||
          isEighthColumnExclusion(this.piecePosition, currentCandidateOffset)) {
        continue;
      }

      if (isValidCoordinate(candidateDestinationCoordinate)) {
        final Tile candidateDestinationTile = board.getTile(candidateDestinationCoordinate);

        if (!candidateDestinationTile.isTileOccupied()) {
          legalMoves.add(new MajorMove(board, this, candidateDestinationCoordinate));
        } else {
          final Piece pieceAtDestination = candidateDestinationTile.getPiece();
          final Alliance pieceAlliance = pieceAtDestination.getPieceAlliance();

          if (this.pieceAlliance != pieceAlliance) {
            legalMoves.add(
                new AttackMove(board, this, candidateDestinationCoordinate, pieceAtDestination));
          }

        }
      }
    }

    return ImmutableList.copyOf(legalMoves);
  }

  private static boolean isFirstColumnExclusion(final int currentPosition,
      final int candidateOffset) {
    return FIRST_COLUMN[currentPosition] && (candidateOffset == -9 || candidateOffset == -1 ||
        candidateOffset == 7);
  }

  private static boolean isEighthColumnExclusion(final int currentPosition,
      final int candidateOffset) {
    return FIRST_COLUMN[currentPosition] && (candidateOffset == -7 || candidateOffset == 1 ||
        candidateOffset == 9);
  }

  @Override
  public King movePiece(Move move) {
    return new King(move.getDestinationCoordinate(), move.getMovedPiece().getPieceAlliance());
  }

  @Override
  public String toString() {
    return PieceType.KING.toString();
  }
}
