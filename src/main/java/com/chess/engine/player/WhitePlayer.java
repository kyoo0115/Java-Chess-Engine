package com.chess.engine.player;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.pieces.Piece;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WhitePlayer extends Player {
    public WhitePlayer(final Board board, final Collection<Move> whiteStandardLegalMoves,
                       final Collection<Move> blackStandardLegalMoves, final boolean castled) {
        super(board, whiteStandardLegalMoves, blackStandardLegalMoves, Alliance.BLACK, castled);
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
            return ImmutableList.copyOf(kingCastles);
        }

        // King-side: f1=61, g1=62, rook=63 → rook dest 61
        addCastleIfLegal(kingCastles, 62, 63, 61, new int[]{61, 62}, new int[]{61, 62}, Alliance.BLACK, true);

        // Queen-side: b1=57, c1=58, d1=59, rook=56 → rook dest 59
        addCastleIfLegal(kingCastles, 58, 56, 59, new int[]{57, 58, 59}, new int[]{58, 59}, Alliance.BLACK, false);

        return ImmutableList.copyOf(kingCastles);
    }
}
