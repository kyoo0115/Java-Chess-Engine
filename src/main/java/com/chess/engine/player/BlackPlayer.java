package com.chess.engine.player;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.Move.KingSideCastleMove;
import com.chess.engine.board.Move.QueenSideCastleMove;
import com.chess.engine.board.Tile;
import com.chess.engine.pieces.Piece;
import com.chess.engine.pieces.Rook;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BlackPlayer extends Player {
    public BlackPlayer(final Board board, final Collection<Move> whiteStandardLegalMoves,
                       final Collection<Move> blackStandardLegalMoves) {
        super(board, blackStandardLegalMoves, whiteStandardLegalMoves);
    }

    @Override
    public Collection<Piece> getActivePieces() {
        return this.board.getBlackPieces();
    }

    @Override
    public Alliance getAlliance() {
        return Alliance.BLACK;
    }

    @Override
    public Player getOpponent() {
        return this.board.getWhitePlayer();
    }

    @Override
    protected Collection<Move> calculateKingCastles(Collection<Move> playerLegals, Collection<Move> opponentLegals) {
        final List<Move> kingCastles = new ArrayList<>();

        if (!this.playerKing.isFirstMove() || isInCheck() || this.playerKing.getPiecePosition() != 4) {
            return ImmutableList.copyOf(kingCastles); // king cannot castle
        }

        // King-side castle
        addKingSideCastle(kingCastles, opponentLegals);

        // Queen-side castle
        addQueenSideCastle(kingCastles, opponentLegals);

        return ImmutableList.copyOf(kingCastles);
    }

    private void addKingSideCastle(final List<Move> kingCastles,
                                   final Collection<Move> opponentLegals) {

        final int f8 = 5;
        final int g8 = 6;
        final int rookH8 = 7;

        final Tile f8Tile = this.board.getTile(f8);
        final Tile g8Tile = this.board.getTile(g8);
        final Tile rookTile = this.board.getTile(rookH8);

        if (!f8Tile.isTileOccupied()
                && !g8Tile.isTileOccupied()
                && isRookEligibleForCastle(rookTile)
                && !Player.isSquareAttackedBy(this.board, f8, Alliance.WHITE)
                && !Player.isSquareAttackedBy(this.board, g8, Alliance.WHITE)
                && rookTile.getPiece().getPieceType().isRook()) {

            kingCastles.add(new KingSideCastleMove(this.board, this.playerKing, 6,
                    (Rook) rookTile.getPiece(), rookTile.getTileCoordinate(), 5));
        }
    }


    private void addQueenSideCastle(final List<Move> kingCastles,
                                    final Collection<Move> opponentLegals) {

        final int b8 = 1;
        final int c8 = 2;
        final int d8 = 3;
        final int rookA8 = 0;

        final Tile b8Tile = this.board.getTile(b8);
        final Tile c8Tile = this.board.getTile(c8);
        final Tile d8Tile = this.board.getTile(d8);
        final Tile rookTile = this.board.getTile(rookA8);

        if (!b8Tile.isTileOccupied()
                && !c8Tile.isTileOccupied()
                && !d8Tile.isTileOccupied()
                && isRookEligibleForCastle(rookTile)
                && !Player.isSquareAttackedBy(this.board, c8, Alliance.WHITE)
                && !Player.isSquareAttackedBy(this.board, d8, Alliance.WHITE)
                && rookTile.getPiece().getPieceType().isRook()) {

            kingCastles.add(new QueenSideCastleMove(this.board, this.playerKing, 2,
                    (Rook) rookTile.getPiece(), rookTile.getTileCoordinate(), 3));
        }
    }


    private boolean isRookEligibleForCastle(Tile rookTile) {
        return rookTile.isTileOccupied() && rookTile.getPiece().isFirstMove();
    }
}
