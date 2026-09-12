package com.chess.engine.board;

import com.chess.engine.Alliance;
import com.chess.engine.pieces.*;
import com.chess.engine.player.BlackPlayer;
import com.chess.engine.player.Player;
import com.chess.engine.player.WhitePlayer;
import com.chess.engine.util.BoardUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.*;
import java.util.regex.Pattern;

public class Board {

    private final List<Tile> gameBoard;
    private final Collection<Piece> whitePieces;
    private final Collection<Piece> blackPieces;

    private final WhitePlayer whitePlayer;
    private final BlackPlayer blackPlayer;
    private final Player currentPlayer;

    private final Pawn enPassantPawn;

    private Board(final Builder builder) {
        this.gameBoard = createGameBoard(builder);
        this.whitePieces = calculateActivePieces(this.gameBoard, Alliance.WHITE);
        this.blackPieces = calculateActivePieces(this.gameBoard, Alliance.BLACK);

        this.enPassantPawn = builder.enPassantPawn;

        final Collection<Move> whiteStandardLegalMoves = calculateLegalMoves(this.whitePieces);
        final Collection<Move> blackStandardLegalMoves = calculateLegalMoves(this.blackPieces);

        this.whitePlayer = new WhitePlayer(this, whiteStandardLegalMoves, blackStandardLegalMoves);
        this.blackPlayer = new BlackPlayer(this, whiteStandardLegalMoves, blackStandardLegalMoves);
        this.currentPlayer = builder.nextMoveMaker.choosePlayer(this.whitePlayer, this.blackPlayer);
    }

    private static List<Tile> createGameBoard(final Builder builder) {
        final Tile[] tiles = new Tile[BoardUtils.NUM_TILES];

        for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
            tiles[i] = Tile.createTile(i, builder.boardConfig.get(i));
        }

        return ImmutableList.copyOf(tiles);
    }

    public static Board createStandardBoard() {
        final Builder builder = new Builder();

        // black pieces
        builder.setPiece(new Rook(0, Alliance.BLACK));
        builder.setPiece(new Knight(1, Alliance.BLACK));
        builder.setPiece(new Bishop(2, Alliance.BLACK));
        builder.setPiece(new Queen(3, Alliance.BLACK));
        builder.setPiece(new King(4, Alliance.BLACK));
        builder.setPiece(new Bishop(5, Alliance.BLACK));
        builder.setPiece(new Knight(6, Alliance.BLACK));
        builder.setPiece(new Rook(7, Alliance.BLACK));

        builder.setPiece(new Pawn(8, Alliance.BLACK));
        builder.setPiece(new Pawn(9, Alliance.BLACK));
        builder.setPiece(new Pawn(10, Alliance.BLACK));
        builder.setPiece(new Pawn(11, Alliance.BLACK));
        builder.setPiece(new Pawn(12, Alliance.BLACK));
        builder.setPiece(new Pawn(13, Alliance.BLACK));
        builder.setPiece(new Pawn(14, Alliance.BLACK));
        builder.setPiece(new Pawn(15, Alliance.BLACK));

        // white pieces
        builder.setPiece(new Pawn(48, Alliance.WHITE));
        builder.setPiece(new Pawn(49, Alliance.WHITE));
        builder.setPiece(new Pawn(50, Alliance.WHITE));
        builder.setPiece(new Pawn(51, Alliance.WHITE));
        builder.setPiece(new Pawn(52, Alliance.WHITE));
        builder.setPiece(new Pawn(53, Alliance.WHITE));
        builder.setPiece(new Pawn(54, Alliance.WHITE));
        builder.setPiece(new Pawn(55, Alliance.WHITE));

        builder.setPiece(new Rook(56, Alliance.WHITE));
        builder.setPiece(new Knight(57, Alliance.WHITE));
        builder.setPiece(new Bishop(58, Alliance.WHITE));
        builder.setPiece(new Queen(59, Alliance.WHITE));
        builder.setPiece(new King(60, Alliance.WHITE));
        builder.setPiece(new Bishop(61, Alliance.WHITE));
        builder.setPiece(new Knight(62, Alliance.WHITE));
        builder.setPiece(new Rook(63, Alliance.WHITE));

        builder.setMoveMaker(Alliance.WHITE);

        return builder.build();
    }

    /**
     * Parses a FEN string and returns the corresponding Board.
     * Handles piece placement, active color, castling rights, and en-passant square.
     * Throws {@link IllegalArgumentException} if the FEN is malformed.
     */
    public static Board fromFEN(final String fen) {
        final String[] fields = fen.trim().split("\\s+");
        if (fields.length < 2) throw new IllegalArgumentException("FEN must have at least 2 fields");

        final Builder builder = new Builder();

        // ── Field 1: piece placement ──────────────────────────────────
        final String[] ranks = fields[0].split("/");
        if (ranks.length != 8) throw new IllegalArgumentException("FEN piece placement must have 8 ranks");

        // Castling rights: used below when placing kings/rooks
        final String castling = fields.length >= 3 ? fields[2] : "-";
        // En-passant target square
        final String epField = fields.length >= 4 ? fields[3] : "-";

        // Track whether we have seen the kings/rooks to set isFirstMove correctly
        // (isFirstMove = piece has castling right in the FEN)
        boolean whiteKingCanCastle = castling.contains("K") || castling.contains("Q");
        boolean blackKingCanCastle = castling.contains("k") || castling.contains("q");
        boolean whiteRookKingSide = castling.contains("K");
        boolean whiteRookQueenSide = castling.contains("Q");
        boolean blackRookKingSide = castling.contains("k");
        boolean blackRookQueenSide = castling.contains("q");

        for (int rankIdx = 0; rankIdx < 8; rankIdx++) {
            int file = 0;
            for (final char ch : ranks[rankIdx].toCharArray()) {
                if (Character.isDigit(ch)) {
                    file += ch - '0';
                } else {
                    final int square = rankIdx * 8 + file;
                    final Alliance alliance = Character.isUpperCase(ch) ? Alliance.WHITE : Alliance.BLACK;
                    final char lower = Character.toLowerCase(ch);
                    final Piece piece;
                    switch (lower) {
                        case 'p' -> piece = new Pawn(square, alliance);
                        case 'n' -> piece = new Knight(square, alliance);
                        case 'b' -> piece = new Bishop(square, alliance);
                        case 'q' -> piece = new Queen(square, alliance);
                        case 'r' -> {
                            boolean firstMove = false;
                            if (alliance == Alliance.WHITE) {
                                firstMove = (square == 63 && whiteRookKingSide)
                                        || (square == 56 && whiteRookQueenSide);
                            } else {
                                firstMove = (square == 7 && blackRookKingSide)
                                        || (square == 0 && blackRookQueenSide);
                            }
                            piece = new Rook(alliance, square, firstMove);
                        }
                        case 'k' -> {
                            boolean firstMove = (alliance == Alliance.WHITE)
                                    ? whiteKingCanCastle : blackKingCanCastle;
                            piece = new King(alliance, square, firstMove);
                        }
                        default -> throw new IllegalArgumentException("Unknown FEN piece char: " + ch);
                    }
                    builder.setPiece(piece);
                    file++;
                }
            }
        }

        // ── Field 2: active color ─────────────────────────────────────
        final Alliance sideToMove = fields[1].equals("b") ? Alliance.BLACK : Alliance.WHITE;
        builder.setMoveMaker(sideToMove);

        // ── Field 4: en-passant target square ─────────────────────────
        if (!epField.equals("-") && Pattern.matches("[a-h][36]", epField)) {
            final int epFile = epField.charAt(0) - 'a';
            final int epRank = epField.charAt(1) - '1'; // 0-based from rank 1
            // The pawn that just moved is on the rank opposite to the ep target
            // ep square e3 means a white pawn moved from e2 to e4; pawn is on e4 (rank index 4 = row 3 from top)
            // ep square e6 means a black pawn moved from e7 to e5; pawn is on e5 (rank index 2 from top? no)
            // Board squares: row 0=rank8, row 7=rank1.  rank1=row7, rank3=row4 (ep square row), rank6=row1
            // White ep target is rank 6 (char '6'), pawn is on rank 5 = row 3 from top = square (3*8+file)
            // Black ep target is rank 3 (char '3'), pawn is on rank 4 = row 4 from top = square (4*8+file)
            final int pawnSquare;
            if (epRank == 5) {
                // ep target is rank 6 → white pawn just moved to rank 5 (square row 3)
                pawnSquare = 3 * 8 + epFile;
            } else {
                // ep target is rank 3 → black pawn just moved to rank 4 (square row 4)
                pawnSquare = 4 * 8 + epFile;
            }
            final Piece pawnOnBoard = builder.boardConfig.get(pawnSquare);
            if (pawnOnBoard instanceof Pawn) {
                builder.setEnPassantPawn((Pawn) pawnOnBoard);
            }
        }

        return builder.build();
    }

    public String toFEN() {
        final StringBuilder sb = new StringBuilder();
        // Piece placement
        for (int rank = 0; rank < 8; rank++) {
            int empty = 0;
            for (int file = 0; file < 8; file++) {
                final Tile tile = gameBoard.get(rank * 8 + file);
                if (tile.isTileOccupied()) {
                    if (empty > 0) { sb.append(empty); empty = 0; }
                    final Piece p = tile.getPiece();
                    String s = p.getPieceType().toString();
                    sb.append(p.getPieceAlliance().isWhite() ? s.toUpperCase() : s.toLowerCase());
                } else {
                    empty++;
                }
            }
            if (empty > 0) sb.append(empty);
            if (rank < 7) sb.append('/');
        }
        sb.append(' ');
        sb.append(currentPlayer.getAlliance().isWhite() ? 'w' : 'b');
        sb.append(' ');
        // Castling rights
        String castling = "";
        if (whitePlayer.getPlayerKing().isFirstMove() && whitePlayer.getPlayerKing().getPiecePosition() == 60) {
            for (Piece p : whitePieces)
                if (p.getPieceType().isRook() && p.isFirstMove() && p.getPiecePosition() == 63) castling += "K";
            for (Piece p : whitePieces)
                if (p.getPieceType().isRook() && p.isFirstMove() && p.getPiecePosition() == 56) castling += "Q";
        }
        if (blackPlayer.getPlayerKing().isFirstMove() && blackPlayer.getPlayerKing().getPiecePosition() == 4) {
            for (Piece p : blackPieces)
                if (p.getPieceType().isRook() && p.isFirstMove() && p.getPiecePosition() == 7)  castling += "k";
            for (Piece p : blackPieces)
                if (p.getPieceType().isRook() && p.isFirstMove() && p.getPiecePosition() == 0)  castling += "q";
        }
        sb.append(castling.isEmpty() ? "-" : castling);
        sb.append(' ');
        // En passant
        if (enPassantPawn != null) {
            int pos = enPassantPawn.getPiecePosition();
            int epRank = enPassantPawn.getPieceAlliance().isWhite() ? pos - 8 : pos + 8;
            char file = (char) ('a' + (epRank % 8));
            int rank = 8 - (epRank / 8);
            sb.append(file).append(rank);
        } else {
            sb.append('-');
        }
        sb.append(" 0 1");
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
            sb.append(String.format("%3s", gameBoard.get(i)));
            if ((i + 1) % BoardUtils.NUM_TILES_PER_ROW == 0) sb.append("\n");
        }
        return sb.toString();
    }

    public Player getWhitePlayer() {
        return this.whitePlayer;
    }

    public Player getBlackPlayer() {
        return this.blackPlayer;
    }

    public Player getCurrentPlayer() {
        return this.currentPlayer;
    }

    public Collection<Piece> getBlackPieces() {
        return this.blackPieces;
    }

    public Collection<Piece> getWhitePieces() {
        return this.whitePieces;
    }

    public Pawn getEnPassantPawn() {
        return this.enPassantPawn;
    }

    public Iterable<Move> getAllLegalMoves() {
        return Iterables.unmodifiableIterable(Iterables.concat(this.whitePlayer.getLegalMoves(), this.blackPlayer.getLegalMoves()));
    }

    private Collection<Move> calculateLegalMoves(final Collection<Piece> pieces) {

        final List<Move> legalMoves = new ArrayList<>();

        for (final Piece piece : pieces) {
            legalMoves.addAll(piece.calculateLegalMoves(this));
        }

        return ImmutableList.copyOf(legalMoves);
    }

    private Collection<Piece> calculateActivePieces(final List<Tile> gameBoard, final Alliance alliance) {
        final List<Piece> activePieces = new ArrayList<>();

        for (final Tile tile : gameBoard) {
            if (tile.isTileOccupied()) {
                final Piece piece = tile.getPiece();

                if (piece.getPieceAlliance() == alliance) {
                    activePieces.add(piece);
                }
            }
        }

        return ImmutableList.copyOf(activePieces);
    }

    public Tile getTile(final int tileCoordinate) {
        return gameBoard.get(tileCoordinate);
    }

    public static class Builder {

        private final Map<Integer, Piece> boardConfig;
        Pawn enPassantPawn;
        private Alliance nextMoveMaker;

        public Builder() {
            this.boardConfig = new HashMap<>();
        }

        public Builder setPiece(final Piece piece) {
            this.boardConfig.put(piece.getPiecePosition(), piece);
            return this;
        }

        public void setMoveMaker(final Alliance nextMoveMaker) {
            this.nextMoveMaker = nextMoveMaker;
        }

        public Board build() {
            return new Board(this);
        }

        public void setEnPassantPawn(Pawn enPassantPawn) {
            this.enPassantPawn = enPassantPawn;
        }
    }
}
