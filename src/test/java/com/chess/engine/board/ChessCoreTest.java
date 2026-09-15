package com.chess.engine.board;

import com.chess.engine.Alliance;
import com.chess.engine.pieces.*;
import com.chess.engine.player.MoveTransition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for core chess engine logic.
 *
 * <p>Coverage:
 * <ol>
 *   <li>Move generation — 20 legal moves from the starting position.</li>
 *   <li>FEN round-trip — {@code Board.toFEN()} followed by {@code Board.fromFEN()} reproduces the same position.</li>
 *   <li>PGN save/load round-trip — serialise moves to PGN text and re-parse them.</li>
 *   <li>{@code Board.fromFEN} edge cases — castling rights, en-passant, side to move.</li>
 * </ol>
 */
class ChessCoreTest {

    // ── 1. Move generation ────────────────────────────────────────────────────

    @Test
    @DisplayName("Starting position has exactly 20 legal moves")
    void startingPositionHas20LegalMoves() {
        final Board board = Board.createStandardBoard();
        assertEquals(20, board.getCurrentPlayer().getLegalMoves().size());
    }

    @Test
    @DisplayName("Starting position legal moves are all pawn or knight moves")
    void startingMovesArePawnsAndKnights() {
        final Board board = Board.createStandardBoard();
        for (final Move move : board.getCurrentPlayer().getLegalMoves()) {
            final Piece.PieceType t = move.getMovedPiece().getPieceType();
            assertTrue(t == Piece.PieceType.PAWN || t == Piece.PieceType.KNIGHT,
                    "Unexpected piece type in opening: " + t);
        }
    }

    // ── 2. FEN round-trip ─────────────────────────────────────────────────────

    @Test
    @DisplayName("FEN round-trip: standard starting position")
    void fenRoundTripStartingPosition() {
        final Board board = Board.createStandardBoard();
        final String fen = board.toFEN();
        final Board restored = Board.fromFEN(fen);
        assertBoardsEqual(board, restored);
    }

    @Test
    @DisplayName("FEN round-trip: Kiwipete position")
    void fenRoundTripKiwipete() {
        final String kiwipete = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
        final Board board = Board.fromFEN(kiwipete);
        final String fen = board.toFEN();
        final Board restored = Board.fromFEN(fen);
        assertBoardsEqual(board, restored);
    }

    @Test
    @DisplayName("FEN round-trip: endgame KRK position")
    void fenRoundTripEndgame() {
        final String endgame = "k7/8/1K6/8/8/8/8/R7 w - -";
        final Board board = Board.fromFEN(endgame);
        final String fen = board.toFEN();
        final Board restored = Board.fromFEN(fen);
        assertBoardsEqual(board, restored);
    }

    // ── 3. PGN save/load round-trip ───────────────────────────────────────────

    @Test
    @DisplayName("PGN round-trip: 1.e4 e5 2.Nf3 Nc6 3.Bb5")
    void pgnRoundTripOpeningMoves() {
        // Play a few known moves from the start
        final List<Move> played = playMoves(Board.createStandardBoard(),
                "e2e4", "e7e5", "g1f3", "b8c6", "f1b5");

        final String pgn = movesToPgn(played);
        final List<Move> loaded = movesFromPgn(pgn);

        assertEquals(played.size(), loaded.size(), "PGN round-trip move count mismatch");
        for (int i = 0; i < played.size(); i++) {
            assertEquals(played.get(i).getCurrentCoordinate(),  loaded.get(i).getCurrentCoordinate(),
                    "Move " + (i + 1) + " from-square mismatch");
            assertEquals(played.get(i).getDestinationCoordinate(), loaded.get(i).getDestinationCoordinate(),
                    "Move " + (i + 1) + " to-square mismatch");
        }
    }

    @Test
    @DisplayName("PGN round-trip: Scholar's mate (4 moves)")
    void pgnRoundTripScholarsMate() {
        final List<Move> played = playMoves(Board.createStandardBoard(),
                "e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "a7a6", "h5f7");

        final String pgn = movesToPgn(played);
        final List<Move> loaded = movesFromPgn(pgn);

        assertEquals(played.size(), loaded.size());
    }

    // ── 4. Board.fromFEN edge cases ───────────────────────────────────────────

    @Test
    @DisplayName("fromFEN: side to move is Black")
    void fenSideToMoveBlack() {
        final Board board = Board.fromFEN("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq -");
        assertEquals(Alliance.BLACK, board.getCurrentPlayer().getAlliance());
    }

    @Test
    @DisplayName("fromFEN: side to move is White")
    void fenSideToMoveWhite() {
        final Board board = Board.fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -");
        assertEquals(Alliance.WHITE, board.getCurrentPlayer().getAlliance());
    }

    @Test
    @DisplayName("fromFEN: castling rights preserved — all four")
    void fenCastlingAllFour() {
        final Board board = Board.fromFEN("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq -");
        assertTrue(board.getWhitePlayer().getPlayerKing().isFirstMove(), "White king should have castling right");
        assertTrue(board.getBlackPlayer().getPlayerKing().isFirstMove(), "Black king should have castling right");
    }

    @Test
    @DisplayName("fromFEN: castling rights stripped — none")
    void fenCastlingNone() {
        final Board board = Board.fromFEN("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w - -");
        assertFalse(board.getWhitePlayer().getPlayerKing().isFirstMove(), "White king should have no castling right");
        assertFalse(board.getBlackPlayer().getPlayerKing().isFirstMove(), "Black king should have no castling right");
    }

    @Test
    @DisplayName("fromFEN: en-passant target square set correctly")
    void fenEnPassant() {
        // After 1.e4 (e2e4): ep target is e3, meaning white pawn is on e4 (tile 36: rank-index 4 * 8 + file-index 4)
        final Board board = Board.fromFEN("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3");
        assertNotNull(board.getEnPassantPawn(), "En-passant pawn should be set");
        assertEquals(36, board.getEnPassantPawn().getPiecePosition(), "En-passant pawn should be on e4 (tile 36)");
    }

    @Test
    @DisplayName("toFEN: en-passant field correct after pawn jump (e2e4 → e3, not e5)")
    void toFenEnPassantCorrect() {
        // Play 1.e4 from starting position — white pawn jumps to e4, ep target must be e3
        final List<Move> moves = playMoves(Board.createStandardBoard(), "e2e4");
        final Board after = Board.fromFEN("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq -");
        // Replay through makeMove so the enPassantPawn is set on the board
        final Board afterE4 = Board.createStandardBoard()
                .getCurrentPlayer().makeMove(moves.getFirst()).getTransitionBoard();
        final String fen = afterE4.toFEN();
        assertTrue(fen.contains(" e3 ") || fen.endsWith(" e3") || fen.contains(" e3"),
                "EP target should be e3 after 1.e4, but FEN was: " + fen);
        assertFalse(fen.contains(" e5"),
                "EP target must NOT be e5 after 1.e4, but FEN was: " + fen);
    }

    @Test
    @DisplayName("fromFEN: no en-passant when field is dash")
    void fenNoEnPassant() {
        final Board board = Board.fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -");
        assertNull(board.getEnPassantPawn(), "No en-passant pawn expected");
    }

    @Test
    @DisplayName("fromFEN: rejects FEN with wrong number of ranks")
    void fenInvalidRankCount() {
        assertThrows(IllegalArgumentException.class,
                () -> Board.fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq -"));
    }

    @Test
    @DisplayName("fromFEN: piece counts match starting position")
    void fenPieceCountsStarting() {
        final Board board = Board.fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -");
        assertEquals(16, board.getWhitePieces().size(), "White should have 16 pieces");
        assertEquals(16, board.getBlackPieces().size(), "Black should have 16 pieces");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Plays a sequence of moves in coordinate notation from a starting board.
     * Fails the test if any move is illegal.
     */
    private static List<Move> playMoves(Board board, final String... coords) {
        final List<Move> played = new ArrayList<>();
        for (final String coord : coords) {
            final int fromFile = coord.charAt(0) - 'a';
            final int fromRank = '8' - coord.charAt(1);
            final int toFile   = coord.charAt(2) - 'a';
            final int toRank   = '8' - coord.charAt(3);
            final int fromId = fromRank * 8 + fromFile;
            final int toId   = toRank   * 8 + toFile;

            Move found = null;
            for (final Move m : board.getCurrentPlayer().getLegalMoves()) {
                if (m.getCurrentCoordinate() == fromId && m.getDestinationCoordinate() == toId) {
                    found = m;
                    break;
                }
            }
            assertNotNull(found, "Illegal move in test sequence: " + coord);
            final MoveTransition t = board.getCurrentPlayer().makeMove(found);
            assertTrue(t.getMoveStatus().isDone(), "Move not done: " + coord);
            board = t.getTransitionBoard();
            played.add(found);
        }
        return played;
    }

    /** Serialises a move list to PGN move-text (no headers). */
    private static String movesToPgn(final List<Move> moves) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < moves.size(); i++) {
            if (i % 2 == 0) sb.append(i / 2 + 1).append(". ");
            sb.append(moves.get(i).toString()).append(' ');
        }
        return sb.toString().trim();
    }

    /** Re-parses PGN move-text back into a move list (mirrors PgnUtils.loadGame logic). */
    private static List<Move> movesFromPgn(final String pgn) {
        Board board = Board.createStandardBoard();
        final List<Move> loaded = new ArrayList<>();
        for (final String token : pgn.split("\\s+")) {
            if (token.isEmpty() || token.matches("\\d+\\.+")) continue;
            final String san = token.replaceAll("[+#!?]", "");
            Move matched = null;
            for (final Move m : board.getCurrentPlayer().getLegalMoves()) {
                if (m.toString().replaceAll("[+#!?]", "").equals(san)) {
                    matched = m;
                    break;
                }
            }
            if (matched == null) break;
            final MoveTransition t = board.getCurrentPlayer().makeMove(matched);
            if (!t.getMoveStatus().isDone()) break;
            board = t.getTransitionBoard();
            loaded.add(matched);
        }
        return loaded;
    }

    /**
     * Asserts that two boards have identical piece placement, side to move,
     * and en-passant state (the parts that define a chess position).
     */
    private static void assertBoardsEqual(final Board expected, final Board actual) {
        // Compare via FEN piece-placement + side fields for a compact diff message
        final String fenA = expected.toFEN();
        final String fenB = actual.toFEN();
        // Compare first two fields: placement and side to move
        final String prefixA = fenA.substring(0, fenA.indexOf(' ', fenA.indexOf(' ') + 1));
        final String prefixB = fenB.substring(0, fenB.indexOf(' ', fenB.indexOf(' ') + 1));
        assertEquals(prefixA, prefixB, "Board position mismatch after FEN round-trip");

        // Also compare en-passant pawn presence
        assertEquals(expected.getEnPassantPawn() != null,
                actual.getEnPassantPawn() != null,
                "En-passant pawn presence mismatch");
    }
}
