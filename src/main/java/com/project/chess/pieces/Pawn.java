package com.project.chess.pieces;

import static com.project.util.BoardUtil.isValidCoordinate;

import com.project.chess.Alliance;
import com.project.chess.Board;
import com.project.chess.Move;
import com.project.chess.Move.MajorMove;
import com.project.chess.Piece;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.units.qual.A;

public class Pawn extends Piece {

  private static final int[] CANDIDATE_MOVE_COORDINATES = {8};

  protected Pawn(int piecePosition, Alliance pieceAlliance) {
    super(piecePosition, pieceAlliance);
  }

  @Override
  public Collection<Move> calculateLegalMoves(Board board) {

    final List<Move> legalMoves = new ArrayList<>();

    for (final int currentCandidateOffset : CANDIDATE_MOVE_COORDINATES) {

      int candidateDestinationCoordinate = this.piecePosition + (this.pieceAlliance.getDirection() * currentCandidateOffset);

      if(!isValidCoordinate(candidateDestinationCoordinate)) {
        continue;
      }

      if(currentCandidateOffset == 8 && !board.getTile(candidateDestinationCoordinate).isTileOccupied()) {
        //todo
        if(!board.getTile(candidateDestinationCoordinate).isTileOccupied()) {
          legalMoves.add(new MajorMove(board, this, candidateDestinationCoordinate));
        }
      }


    }
  }
}
