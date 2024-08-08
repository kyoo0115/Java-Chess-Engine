package com.project.chess.pieces;

import static com.project.chess.board.BoardUtil.isValidCoordinate;

import com.google.common.collect.ImmutableList;
import com.project.chess.Alliance;
import com.project.chess.board.Board;
import com.project.chess.board.Tile;
import com.project.chess.moves.Move;
import com.project.chess.moves.Move.AttackMove;
import com.project.chess.moves.Move.MajorMove;
import com.project.chess.pieces.precompute.KnightMovePrecompute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 체스의 나이트(말)를 나타냅니다.
 */
public class Knight extends Piece {

  /**
   * 나이트 조각의 생성자.
   *
   * @param piecePosition 조각의 위치.
   * @param pieceAlliance 조각의 연합(색깔).
   */
  public Knight(final int piecePosition, final Alliance pieceAlliance) {
    super(PieceType.KNIGHT, piecePosition, pieceAlliance);
  }

  /**
   * 주어진 보드에서 나이트의 합법적인 이동을 계산합니다.
   *
   * @param board 이동을 계산할 보드.
   * @return 합법적인 이동 목록.
   */
  @Override
  public Collection<Move> calculateLegalMoves(final Board board) {
    final List<Move> legalMoves = new ArrayList<>();

    // Precomputed possible moves for the current position
    List<Integer> precomputedMoves = KnightMovePrecompute.getKnightMoves(this.piecePosition);

    // Check each precomputed move
    for (final int candidateDestinationCoordinate : precomputedMoves) {
      if (isValidCoordinate(candidateDestinationCoordinate)) {
        final Tile candidateDestinationTile = board.getTile(candidateDestinationCoordinate);

        // If the destination tile is not occupied, add a major move
        if (!candidateDestinationTile.isTileOccupied()) {
          legalMoves.add(new MajorMove(board, this, candidateDestinationCoordinate));
        } else {
          // If the destination tile is occupied, check the alliance of the piece
          final Piece pieceAtDestination = candidateDestinationTile.getPiece();
          final Alliance pieceAlliance = pieceAtDestination.getPieceAlliance();

          // If the piece is an enemy piece, add an attack move
          if (this.pieceAlliance != pieceAlliance) {
            legalMoves.add(
                new AttackMove(board, this, candidateDestinationCoordinate, pieceAtDestination));
          }
        }
      }
    }

    // Return the list of legal moves
    return ImmutableList.copyOf(legalMoves);
  }

  @Override
  public Knight movePiece(Move move) {
    return new Knight(move.getDestinationCoordinate(), move.getMovedPiece().getPieceAlliance());
  }

  @Override
  public String toString() {
    return PieceType.KNIGHT.toString();
  }
}