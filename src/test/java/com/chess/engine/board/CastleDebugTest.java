package com.chess.engine.board;

import com.chess.engine.player.MoveTransition;
import org.junit.jupiter.api.Test;

import java.util.Collection;

/**
 * Find the remaining 59 phantom castles after the isInCheck ordering fix.
 * These pass makeMove but shouldn't be legal. Check:
 * 1. King in check before (already ruled out = 0)
 * 2. King in check after  (already ruled out = 0)
 * 3. King passes through an attacked transit square
 * 4. Some other condition
 */
class CastleDebugTest {

    private static final String KIWIPETE =
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";

    @Test
    void findRemaining59() {
        Board root = Board.fromFEN(KIWIPETE);
        int transitAttacked = 0;
        int other = 0;

        for (Move m1 : root.getCurrentPlayer().getLegalMoves()) {
            MoveTransition t1 = root.getCurrentPlayer().makeMove(m1);
            if (!t1.getMoveStatus().isDone()) continue;
            Board b1 = t1.getTransitionBoard();

            for (Move m2 : b1.getCurrentPlayer().getLegalMoves()) {
                MoveTransition t2 = b1.getCurrentPlayer().makeMove(m2);
                if (!t2.getMoveStatus().isDone()) continue;
                Board b2 = t2.getTransitionBoard();

                for (Move m3 : b2.getCurrentPlayer().getLegalMoves()) {
                    MoveTransition t3 = b2.getCurrentPlayer().makeMove(m3);
                    if (!t3.getMoveStatus().isDone()) continue;
                    Board b3 = t3.getTransitionBoard();

                    for (Move m4 : b3.getCurrentPlayer().getLegalMoves()) {
                        if (!m4.isCastlingMove()) continue;
                        MoveTransition t4 = b3.getCurrentPlayer().makeMove(m4);
                        if (!t4.getMoveStatus().isDone()) continue;

                        // Skip previously-confirmed-ok cases
                        if (b3.getCurrentPlayer().isInCheck()) continue;
                        Board b4 = t4.getTransitionBoard();
                        if (b4.getCurrentPlayer().getOpponent().isInCheck()) continue;

                        // Check if the transit square is attacked by the opponent's
                        // FULL legal moves (including castling), vs what calculateKingCastles
                        // uses (standard moves only).
                        // Transit square = midpoint between king start and end
                        int kingFrom = m4.getCurrentCoordinate();
                        int kingTo = m4.getDestinationCoordinate();
                        int transit = (kingFrom + kingTo) / 2;

                        // Get opponent's FULL legal moves on b3
                        Collection<Move> opponentFullMoves = b3.getCurrentPlayer().getOpponent().getLegalMoves();
                        boolean transitAttackedByFull = !com.chess.engine.player.Player
                                .calculateAttacksOnTile(transit, opponentFullMoves).isEmpty();
                        boolean kingAttackedByFull = !com.chess.engine.player.Player
                                .calculateAttacksOnTile(kingFrom, opponentFullMoves).isEmpty();

                        // Get opponent's standard moves (what calculateKingCastles actually uses)
                        // These are the opponent's legalMoves minus castle moves
                        Collection<Move> opponentStdMoves = b3.getCurrentPlayer().getOpponent().getLegalMoves()
                                .stream()
                                .filter(mv -> !mv.isCastlingMove())
                                .toList();
                        boolean transitAttackedByStd = !com.chess.engine.player.Player
                                .calculateAttacksOnTile(transit, opponentStdMoves).isEmpty();

                        if (transitAttackedByFull && !transitAttackedByStd) {
                            // Transit square is attacked only by a castle move of the opponent
                            transitAttacked++;
                            System.out.printf("TRANSIT-CASTLE-ATTACK: %-8s %-8s %-8s %-8s | king %d->%d transit=%d%n",
                                    m1, m2, m3, m4, kingFrom, kingTo, transit);
                        } else if (transitAttackedByStd) {
                            // Transit IS attacked by standard moves — this should have been blocked!
                            System.out.printf("MISSED-TRANSIT-STD: %-8s %-8s %-8s %-8s | king %d->%d transit=%d%n",
                                    m1, m2, m3, m4, kingFrom, kingTo, transit);
                        } else {
                            // Transit not attacked — why is this a phantom?
                            other++;
                            System.out.printf("UNKNOWN: %-8s %-8s %-8s %-8s | king %d->%d transit=%d inCheck=%b%n",
                                    m1, m2, m3, m4, kingFrom, kingTo, transit,
                                    b3.getCurrentPlayer().isInCheck());
                        }
                    }
                }
            }
        }
        System.out.println("Transit attacked by castle only: " + transitAttacked);
        System.out.println("Other unexplained: " + other);
        System.out.println("Total remaining: " + (transitAttacked + other) + " (expected 59)");
    }
}
