package com.chess.engine.pieces;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.util.BoardUtils;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import static com.chess.engine.board.Move.*;

public final class Pawn extends Piece {

    private static final int MOVE_ONE_TILE = 8;
    private static final int MOVE_TWO_TILES = 16;
    private static final int ATTACK_LEFT = 7;
    private static final int ATTACK_RIGHT = 9;

    public Pawn(final int piecePosition, final Alliance pieceAlliance) {
        super(PieceType.PAWN, piecePosition, pieceAlliance, true);
    }

    public Pawn(final Alliance pieceAlliance, final int piecePosition, final boolean isFirstMove) {
        super(PieceType.PAWN, piecePosition, pieceAlliance, isFirstMove);
    }

    @Override
    public List<Move> calculateLegalMoves(Board board) {

        final List<Move> legalMoves = new ArrayList<>();

        for (final int candidateOffset : new int[]{MOVE_ONE_TILE, MOVE_TWO_TILES, ATTACK_LEFT, ATTACK_RIGHT}) {

            int candidateDestinationCoordinate = this.piecePosition + (this.getPieceAlliance().getDirection() * candidateOffset);

            if (!BoardUtils.isValidTileCoordinate(candidateDestinationCoordinate)) {
                continue;
            }

            /* ==================== Normal Move ==================== */
            if (candidateOffset == MOVE_ONE_TILE && !board.getTile(candidateDestinationCoordinate).isTileOccupied()) {

                if (this.pieceAlliance.isPawnPromotionSquare(candidateDestinationCoordinate)) {
                    legalMoves.add(new PawnPromotion(new PawnMove(board, this, candidateDestinationCoordinate)));
                } else {
                    legalMoves.add(new PawnMove(board, this, candidateDestinationCoordinate));
                }

                continue;
            }

            /* ==================== Two Tiles Move on First Move ==================== */
            if (candidateOffset == MOVE_TWO_TILES && this.isFirstMove()
                    && ((BoardUtils.SEVENTH_RANK[this.piecePosition] && this.pieceAlliance.isBlack())
                    || (BoardUtils.SECOND_RANK[this.piecePosition] && this.pieceAlliance.isWhite()))) {

                final int behindCoordinate = this.piecePosition + (this.pieceAlliance.getDirection() * MOVE_ONE_TILE);

                if (!board.getTile(behindCoordinate).isTileOccupied()
                        && !board.getTile(candidateDestinationCoordinate).isTileOccupied()) {

                    legalMoves.add(new PawnJump(board, this, candidateDestinationCoordinate));
                }

                continue;
            }

            /* ==================== Attacks ==================== */
            if (candidateOffset == ATTACK_LEFT
                    && !isLeftColumnExclusion(this.pieceAlliance)) {

                if (board.getTile(candidateDestinationCoordinate).isTileOccupied()) {
                    final Piece pieceOnCandidate = board.getTile(candidateDestinationCoordinate).getPiece();

                    if (this.pieceAlliance != pieceOnCandidate.getPieceAlliance()) {
                        if (this.pieceAlliance.isPawnPromotionSquare(candidateDestinationCoordinate)) {
                            legalMoves.add(new PawnPromotion(new PawnAttackMove(board, this, candidateDestinationCoordinate, pieceOnCandidate)));
                        } else {
                            legalMoves.add(new PawnAttackMove(board, this, candidateDestinationCoordinate, pieceOnCandidate));
                        }
                    }
                } else if (board.getEnPassantPawn() != null) {
                    if (board.getEnPassantPawn().getPiecePosition() == (this.piecePosition + (this.pieceAlliance.getOppositeDirection()))) {
                        final Piece pieceOnCandidate = board.getEnPassantPawn();
                        if (this.pieceAlliance != pieceOnCandidate.getPieceAlliance()) {
                            legalMoves.add(new PawnEnPassantAttackMove(board, this, candidateDestinationCoordinate, pieceOnCandidate));
                        }
                    }
                }
                continue;
            }

            if (candidateOffset == ATTACK_RIGHT
                    && !isRightColumnExclusion(this.pieceAlliance)) {

                if (board.getTile(candidateDestinationCoordinate).isTileOccupied()) {
                    final Piece pieceOnCandidate = board.getTile(candidateDestinationCoordinate).getPiece();

                    if (this.pieceAlliance != pieceOnCandidate.getPieceAlliance()) {
                        if (this.pieceAlliance.isPawnPromotionSquare(candidateDestinationCoordinate)) {
                            legalMoves.add(new PawnPromotion(new PawnAttackMove(board, this, candidateDestinationCoordinate, pieceOnCandidate)));
                        } else {
                            legalMoves.add(new PawnAttackMove(board, this, candidateDestinationCoordinate, pieceOnCandidate));
                        }
                    }
                } else if (board.getEnPassantPawn() != null) {
                    if (board.getEnPassantPawn().getPiecePosition() == (this.piecePosition - (this.pieceAlliance.getOppositeDirection()))) {
                        final Piece pieceOnCandidate = board.getEnPassantPawn();
                        if (this.pieceAlliance != pieceOnCandidate.getPieceAlliance()) {
                            legalMoves.add(new PawnEnPassantAttackMove(board, this, candidateDestinationCoordinate, pieceOnCandidate));
                        }
                    }
                }
            }

        }

        return ImmutableList.copyOf(legalMoves);
    }

    /* ==================== Column Exclusions ==================== */
    private boolean isLeftColumnExclusion(final Alliance alliance) {
        return (BoardUtils.EIGHTH_COLUMN[this.piecePosition] && alliance.isWhite())
                || (BoardUtils.FIRST_COLUMN[this.piecePosition] && alliance.isBlack());
    }

    private boolean isRightColumnExclusion(final Alliance alliance) {
        return (BoardUtils.FIRST_COLUMN[this.piecePosition] && alliance.isWhite())
                || (BoardUtils.EIGHTH_COLUMN[this.piecePosition] && alliance.isBlack());
    }

    @Override
    public Pawn movePiece(final Move move) {
        return new Pawn(move.getDestinationCoordinate(), move.getMovedPiece().getPieceAlliance());
    }

    @Override
    public String toString() {
        return PieceType.PAWN.toString();
    }

    public Piece getPromotionPiece() {
        return new Queen(this.pieceAlliance, this.piecePosition, false);
    }
}
