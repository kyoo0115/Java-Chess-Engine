package com.chess.engine.player;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.Tile;
import com.chess.engine.pieces.Piece;
import com.chess.engine.pieces.Rook;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.chess.engine.board.Move.KingSideCastleMove;
import static com.chess.engine.board.Move.QueenSideCastleMove;

public class WhitePlayer extends Player {
    public WhitePlayer(final Board board, final Collection<Move> whiteStandardLegalMoves,
                       final Collection<Move> blackStandardLegalMoves) {
        super(board, whiteStandardLegalMoves, blackStandardLegalMoves, Alliance.BLACK);
    }

    @Override
    public Collection<Piece> getActivePieces() {
        return this.board.getWhitePieces();
    }

    @Override
    public Alliance getAlliance() {
        return Alliance.WHITE;
    }

    @Override
    public Player getOpponent() {
        return this.board.getBlackPlayer();
    }

    @Override
    protected Collection<Move> calculateKingCastles(Collection<Move> playerLegals, Collection<Move> opponentLegals) {
        final List<Move> kingCastles = new ArrayList<>();

        if (!this.playerKing.isFirstMove() || isInCheck() || this.playerKing.getPiecePosition() != 60) {
            return ImmutableList.copyOf(kingCastles); // king cannot castle
        }

        // King-side castle
        addKingSideCastle(kingCastles, opponentLegals);

        // Queen-side castle
        addQueenSideCastle(kingCastles, opponentLegals);

        return ImmutableList.copyOf(kingCastles);
    }

    private void addKingSideCastle(List<Move> kingCastles, Collection<Move> opponentLegals) {
        Tile fTile = this.board.getTile(61);
        Tile gTile = this.board.getTile(62);
        Tile rookTile = this.board.getTile(63);

        if (!fTile.isTileOccupied() && !gTile.isTileOccupied() && isRookEligibleForCastle(rookTile)) {
            if (!Player.isSquareAttackedBy(this.board, 61, Alliance.BLACK) &&
                    !Player.isSquareAttackedBy(this.board, 62, Alliance.BLACK) &&
                    rookTile.getPiece().getPieceType().isRook()) {
                kingCastles.add(new KingSideCastleMove(this.board, this.playerKing, 62,
                        (Rook) rookTile.getPiece(), rookTile.getTileCoordinate(), 61) {
                });
            }
        }
    }

    private void addQueenSideCastle(List<Move> kingCastles, Collection<Move> opponentLegals) {
        Tile bTile = this.board.getTile(57);
        Tile cTile = this.board.getTile(58);
        Tile dTile = this.board.getTile(59);
        Tile rookTile = this.board.getTile(56);

        if (!bTile.isTileOccupied() &&
                !cTile.isTileOccupied() &&
                !dTile.isTileOccupied() && isRookEligibleForCastle(rookTile)) {

            if (!Player.isSquareAttackedBy(this.board, 58, Alliance.BLACK) &&
                    !Player.isSquareAttackedBy(this.board, 59, Alliance.BLACK) &&
                    rookTile.getPiece().getPieceType().isRook()) {

                kingCastles.add(new QueenSideCastleMove(this.board, this.playerKing, 58,
                        (Rook) rookTile.getPiece(), rookTile.getTileCoordinate(), 59));
            }
        }
    }

    private boolean isRookEligibleForCastle(Tile rookTile) {
        return rookTile.isTileOccupied() && rookTile.getPiece().isFirstMove();
    }
}
