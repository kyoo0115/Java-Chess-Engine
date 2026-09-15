package com.chess.engine.player;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.pieces.Piece;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BlackPlayer extends Player {
    public BlackPlayer(final Board board, final Collection<Move> whiteStandardLegalMoves,
                       final Collection<Move> blackStandardLegalMoves, final boolean castled) {
        super(board, blackStandardLegalMoves, whiteStandardLegalMoves, Alliance.WHITE, castled);
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
            return ImmutableList.copyOf(kingCastles);
        }

        // King-side: f8=5, g8=6, rook=7 → rook dest 5
        addCastleIfLegal(kingCastles, 6, 7, 5, new int[]{5, 6}, new int[]{5, 6}, Alliance.WHITE, true);

        // Queen-side: b8=1, c8=2, d8=3, rook=0 → rook dest 3
        addCastleIfLegal(kingCastles, 2, 0, 3, new int[]{1, 2, 3}, new int[]{2, 3}, Alliance.WHITE, false);

        return ImmutableList.copyOf(kingCastles);
    }
}
