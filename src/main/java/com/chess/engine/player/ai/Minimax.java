package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.MoveTransition;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Minimax implements MoveStrategy {

    // ── Killer moves ─────────────────────────────────────────────────
    // Two killer slots per ply depth (killers[depth][0..1]).
    private static final int MAX_DEPTH = 32;
    private static final int QUIESCENCE_DEPTH_LIMIT = 4;
    // ── Opening book ─────────────────────────────────────────────────
    // Maps "FEN-field1 FEN-field2" → coordinate move string (e.g. "e2e4")
    private static final Map<String, List<String>> OPENING_BOOK = loadOpeningBook();
    private final BoardEvaluator boardEvaluator;
    private final int searchDepth;
    // ── Transposition table ───────────────────────────────────────────
    // Maps Zobrist hash → cached (score, depth, flag).
    // Flag: EXACT=0, LOWER_BOUND=1 (beta cutoff), UPPER_BOUND=2 (no cutoff).
    private final Map<Long, TtEntry> transpositionTable = new HashMap<>(1 << 20);
    private final Move[][] killers = new Move[MAX_DEPTH][2];
    // ── History heuristic ─────────────────────────────────────────────
    // historyTable[from][to] accumulates scores for quiet moves that caused beta cutoffs.
    private final int[][] historyTable = new int[64][64];

    // ─────────────────────────────────────────────────────────────────
    //  Public interface
    // ─────────────────────────────────────────────────────────────────

    public Minimax(final int searchDepth) {
        this.boardEvaluator = new StandardBoardEvaluator();
        this.searchDepth = searchDepth;
    }

    private static Map<String, List<String>> loadOpeningBook() {
        final Map<String, List<String>> book = new HashMap<>();
        try (final InputStream is = Minimax.class.getClassLoader()
                .getResourceAsStream("opening_book.txt")) {
            if (is == null) return book;
            try (final BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    final String[] parts = line.split("\\s+");
                    if (parts.length < 3) continue;
                    // key = "field1 field2", value = move (field3)
                    final String key = parts[0] + " " + parts[1];
                    book.computeIfAbsent(key, k -> new ArrayList<>()).add(parts[2]);
                }
            }
        } catch (Exception e) {
            System.err.println("Opening book load error: " + e.getMessage());
        }
        System.out.println("Opening book loaded: " + book.size() + " positions");
        return Collections.unmodifiableMap(book);
    }

    /**
     * Captures and quiet promotions, sorted by MVV-LVA — used in quiescence search
     * when the side to move is NOT in check. Quiet promotions are included because
     * a pawn reaching the back rank is a huge material swing that must not be invisible
     * at the search horizon.
     */
    private static List<Move> capturesAndPromotions(final Board board) {
        final List<Move> tactical = new ArrayList<>();
        for (final Move m : board.getCurrentPlayer().getLegalMoves()) {
            if (m.isAttack() || m instanceof Move.PawnPromotion) tactical.add(m);
        }
        tactical.sort(Comparator.comparingInt(Minimax::tacticalOrderingScore).reversed());
        return tactical;
    }

    private static int tacticalOrderingScore(final Move m) {
        if (m.isAttack()) {
            return m.getAttackedPiece().getPieceValue() * 10 - m.getMovedPiece().getPieceValue();
        }
        // Quiet promotion: order after captures
        return 0;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Alpha-Beta with Transposition Table + Killer Moves
    // ─────────────────────────────────────────────────────────────────

    private static boolean isEndGameScenario(final Board board) {
        return board.getCurrentPlayer().isCheckMate() ||
                board.getCurrentPlayer().isStaleMate();
    }

    /**
     * Zobrist-style hash: XOR together piece/square contributions for all pieces,
     * side-to-move, castling rights (via king/corner-rook first-move flags), and
     * en passant target square.
     *
     * The original hash omitted castling rights and en passant state, so two positions
     * with identical piece placement but different castling legality could collide in the
     * transposition table and return a stale cached score — a real correctness bug.
     */
    private static long zobrist(final Board board) {
        long hash = 0L;
        for (final Piece p : board.getWhitePieces()) {
            hash ^= pieceHash(p, true);
        }
        for (final Piece p : board.getBlackPieces()) {
            hash ^= pieceHash(p, false);
        }
        if (board.getCurrentPlayer().getAlliance().isWhite()) hash ^= 0x9E3779B97F4A7C15L;

        // ── Castling rights ────────────────────────────────────────
        // Hash the first-move status of the king and corner rooks — those are exactly
        // the pieces castling rights depend on.
        for (final Piece p : board.getWhitePieces()) hash ^= castlingBit(p);
        for (final Piece p : board.getBlackPieces()) hash ^= castlingBit(p);

        // ── En passant target square ──────────────────────────────
        final Piece enPassantPawn = board.getEnPassantPawn();
        if (enPassantPawn != null) {
            hash ^= (enPassantPawn.getPiecePosition() + 1) * 0x27220A5F4A1A7C51L;
        }

        return hash;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Quiescence Search — extend depth on captures to avoid horizon effect
    // ─────────────────────────────────────────────────────────────────

    /**
     * Contributes a bit to the Zobrist hash for a king or corner rook that has NOT yet
     * moved (i.e. still a potential castling participant). Once a king or corner rook
     * has moved, it contributes nothing — its castling right is gone.
     */
    private static long castlingBit(final Piece p) {
        final boolean isCorner = p.getPiecePosition() == 0 || p.getPiecePosition() == 7
                || p.getPiecePosition() == 56 || p.getPiecePosition() == 63;
        final boolean relevant = p.getPieceType() == Piece.PieceType.KING
                || (p.getPieceType() == Piece.PieceType.ROOK && isCorner);
        if (!relevant || !p.isFirstMove()) return 0L;

        final long slot = p.getPieceType() == Piece.PieceType.KING
                ? (p.getPieceAlliance().isWhite() ? 1L : 2L)
                : (p.getPieceAlliance().isWhite() ? 10L : 20L) + p.getPiecePosition();
        return slot * 0x9E3779B97F4A7C15L + 0xBF58476D1CE4E5B9L;
    }

    private static long pieceHash(final Piece p, final boolean white) {
        // Spread bits: piece ordinal (0-5) * 128 + position (0-63) + white offset
        final int typeOrd = p.getPieceType().ordinal();
        final int base = (white ? 0 : 384) + typeOrd * 64 + p.getPiecePosition();
        // Mix with a large prime to avoid clustering
        return base * 6364136223846793005L + 1442695040888963407L;
    }

    /**
     * Returns true if the current player has at least one major or minor piece
     * (queen, rook, bishop, knight) — i.e. not a zugzwang-prone K+pawns-only position.
     */
    private static boolean hasNonPawnMaterial(final Board board) {
        for (final Piece p : board.getCurrentPlayer().getActivePieces()) {
            final Piece.PieceType t = p.getPieceType();
            if (t == Piece.PieceType.QUEEN || t == Piece.PieceType.ROOK
                    || t == Piece.PieceType.BISHOP || t == Piece.PieceType.KNIGHT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Constructs a "null move" board: same position, but with the turn passed to the opponent.
     * En-passant is cleared (it would be stale after passing a turn).
     */
    private static Board makeNullMoveBoard(final Board board) {
        final Board.Builder builder = new Board.Builder();
        for (final Piece p : board.getWhitePieces()) builder.setPiece(p);
        for (final Piece p : board.getBlackPieces()) builder.setPiece(p);
        builder.setMoveMaker(board.getCurrentPlayer().getOpponent().getAlliance());
        return builder.build();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Move ordering  (MVV-LVA captures → killers → quiet moves)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Serialises a board position to the two-field FEN prefix used as the book key:
     * "piece-placement side-to-move"  e.g. "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b"
     */
    private static String boardToFenKey(final Board board) {
        final StringBuilder sb = new StringBuilder();
        for (int rank = 0; rank < 8; rank++) {
            int empty = 0;
            for (int file = 0; file < 8; file++) {
                final com.chess.engine.board.Tile tile = board.getTile(rank * 8 + file);
                if (!tile.isTileOccupied()) {
                    empty++;
                } else {
                    if (empty > 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    final Piece p = tile.getPiece();
                    final char c = fenChar(p);
                    sb.append(c);
                }
            }
            if (empty > 0) sb.append(empty);
            if (rank < 7) sb.append('/');
        }
        sb.append(' ');
        sb.append(board.getCurrentPlayer().getAlliance().isWhite() ? 'w' : 'b');
        return sb.toString();
    }

    private static char fenChar(final Piece p) {
        final char c = switch (p.getPieceType()) {
            case KING -> 'k';
            case QUEEN -> 'q';
            case ROOK -> 'r';
            case BISHOP -> 'b';
            case KNIGHT -> 'n';
            case PAWN -> 'p';
        };
        return p.getPieceAlliance().isWhite() ? Character.toUpperCase(c) : c;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Killer move storage
    // ─────────────────────────────────────────────────────────────────

    /**
     * Finds a legal move matching a coordinate string like "e2e4" or "e7e8q".
     */
    private static Move findMoveByCoord(final Board board, final String coord) {
        if (coord.length() < 4) return null;
        final int fromFile = coord.charAt(0) - 'a';
        final int fromRank = '8' - coord.charAt(1);
        final int toFile = coord.charAt(2) - 'a';
        final int toRank = '8' - coord.charAt(3);
        final int fromId = fromRank * 8 + fromFile;
        final int toId = toRank * 8 + toFile;
        for (final Move m : board.getCurrentPlayer().getLegalMoves()) {
            if (m.getCurrentCoordinate() == fromId && m.getDestinationCoordinate() == toId)
                return m;
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Minimax (Alpha-Beta + TT + Killers + Quiescence)";
    }

    @Override
    public Move execute(final Board board) {
        transpositionTable.clear();
        for (int i = 0; i < MAX_DEPTH; i++) {
            killers[i][0] = null;
            killers[i][1] = null;
        }

        final long startTime = System.currentTimeMillis();
        final boolean isWhite = board.getCurrentPlayer().getAlliance().isWhite();

        // ── Opening book lookup ───────────────────────────────────────
        final String bookKey = boardToFenKey(board);
        final List<String> bookMoves = OPENING_BOOK.get(bookKey);
        if (bookMoves != null && !bookMoves.isEmpty()) {
            final String chosen = bookMoves.get(new Random().nextInt(bookMoves.size()));
            final Move bookMove = findMoveByCoord(board, chosen);
            if (bookMove != null) {
                System.out.println("Book move: " + chosen);
                return bookMove;
            }
        }

        Move bestMove = null;

        // FIX (bug #4): fall back to the first legal move up front so execute() can never
        // return null even if depth-1 search somehow fails to update bestMove (e.g. every
        // move transition comes back not-done, which shouldn't normally happen for legal
        // moves, but defensively guarding against it avoids an NPE for the caller).
        final List<Move> rootLegalMoves = new ArrayList<>(board.getCurrentPlayer().getLegalMoves());
        if (!rootLegalMoves.isEmpty()) {
            bestMove = rootLegalMoves.getFirst();
        }

        // Iterative deepening: search depth 1..searchDepth, carrying TT across iterations.
        // The best move from iteration d is placed first in the move list for iteration d+1.
        for (int currentDepth = 1; currentDepth <= this.searchDepth; currentDepth++) {
            // Reset killers each iteration so they are relevant to the current depth
            for (int i = 0; i < MAX_DEPTH; i++) {
                killers[i][0] = null;
                killers[i][1] = null;
            }

            Move iterationBest = null;
            int highestSeenValue = Integer.MIN_VALUE;
            int lowestSeenValue = Integer.MAX_VALUE;
            int alpha = Integer.MIN_VALUE;
            int beta = Integer.MAX_VALUE;

            // Build move list with previous iteration's best move at the front
            final List<Move> moves = orderedMoves(board, 0);
            if (bestMove != null && moves.remove(bestMove)) moves.addFirst(bestMove);

            for (final Move move : moves) {
                final MoveTransition moveTransition = board.getCurrentPlayer().makeMove(move);
                if (!moveTransition.getMoveStatus().isDone()) continue;

                if (isWhite) {
                    final int currentValue = min(
                            moveTransition.getTransitionBoard(),
                            currentDepth - 1, alpha, beta, 1);
                    if (currentValue > highestSeenValue) {
                        highestSeenValue = currentValue;
                        iterationBest = move;
                        alpha = highestSeenValue;
                    }
                } else {
                    final int currentValue = max(
                            moveTransition.getTransitionBoard(),
                            currentDepth - 1, alpha, beta, 1);
                    if (currentValue < lowestSeenValue) {
                        lowestSeenValue = currentValue;
                        iterationBest = move;
                        beta = lowestSeenValue;
                    }
                }
            }

            if (iterationBest != null) bestMove = iterationBest;

            // Early exit if checkmate found — no point searching deeper
            final int bestScore = isWhite ? highestSeenValue : lowestSeenValue;
            if (Math.abs(bestScore) >= 10000 * 100) break;
        }

        System.out.printf("Move chosen: %s  (%.2f s)  TT size: %d%n",
                bestMove, (System.currentTimeMillis() - startTime) / 1000.0, transpositionTable.size());
        return bestMove;
    }

    /**
     * Minimising node (Black's turn).
     *
     * @param ply distance from root (used for killer slot indexing)
     */
    public int min(final Board board, final int depth, int alpha, int beta, final int ply) {
        if (isEndGameScenario(board)) return this.boardEvaluator.evaluate(board, depth);
        if (depth == 0) return quiescence(board, alpha, beta, false);

        // ── Transposition table lookup ────────────────────────────────
        final long hash = zobrist(board);
        final TtEntry cached = transpositionTable.get(hash);
        if (cached != null && cached.depth >= depth) {
            if (cached.flag == TtEntry.EXACT) return cached.score;
            if (cached.flag == TtEntry.LOWER_BOUND) alpha = Math.max(alpha, cached.score);
            if (cached.flag == TtEntry.UPPER_BOUND) beta = Math.min(beta, cached.score);
            if (alpha >= beta) return cached.score;
        }

        // ── Null move pruning ─────────────────────────────────────────
        // Skip if: in check, shallow depth, or zugzwang-prone (only K+pawns)
        if (depth >= 3 && !board.getCurrentPlayer().isInCheck() && hasNonPawnMaterial(board)) {
            final Board nullBoard = makeNullMoveBoard(board);
            final int nullScore = max(nullBoard, depth - 3, alpha, beta, ply + 1);
            if (nullScore <= alpha) return alpha;
        }

        int lowestSeenValue = beta;
        Move bestLocal = null;
        int moveCount = 0;

        for (final Move move : orderedMoves(board, ply)) {
            final MoveTransition t = board.getCurrentPlayer().makeMove(move);
            if (!t.getMoveStatus().isDone()) continue;
            moveCount++;

            int value;
            // ── Late Move Reductions ──────────────────────────────────
            // Quiet moves beyond the 4th at depth >= 3: try a reduced search first.
            final boolean isQuiet = !move.isAttack() && !(move instanceof Move.PawnPromotion);
            if (isQuiet && moveCount > 4 && depth >= 3
                    && !board.getCurrentPlayer().isInCheck()
                    && !t.getTransitionBoard().getCurrentPlayer().isInCheck()) {
                value = max(t.getTransitionBoard(), depth - 2, alpha, lowestSeenValue, ply + 1);
                // If the reduced search beats alpha, re-search at full depth
                if (value < lowestSeenValue) {
                    value = max(t.getTransitionBoard(), depth - 1, alpha, lowestSeenValue, ply + 1);
                }
            } else {
                value = max(t.getTransitionBoard(), depth - 1, alpha, lowestSeenValue, ply + 1);
            }
            if (value < lowestSeenValue) {
                lowestSeenValue = value;
                bestLocal = move;
            }
            if (lowestSeenValue <= alpha) {
                // Alpha cut-off in a min node — score is an upper bound
                storeKiller(ply, move);
                transpositionTable.put(hash, new TtEntry(lowestSeenValue, depth, TtEntry.UPPER_BOUND));
                return lowestSeenValue;
            }
        }

        final int flag = bestLocal != null ? TtEntry.EXACT : TtEntry.LOWER_BOUND;
        transpositionTable.put(hash, new TtEntry(lowestSeenValue, depth, flag));
        return lowestSeenValue;
    }

    /**
     * Maximising node (White's turn).
     */
    public int max(final Board board, final int depth, int alpha, int beta, final int ply) {
        if (isEndGameScenario(board)) return this.boardEvaluator.evaluate(board, depth);
        if (depth == 0) return quiescence(board, alpha, beta, true);

        // ── Transposition table lookup ────────────────────────────────
        final long hash = zobrist(board);
        final TtEntry cached = transpositionTable.get(hash);
        if (cached != null && cached.depth >= depth) {
            if (cached.flag == TtEntry.EXACT) return cached.score;
            if (cached.flag == TtEntry.LOWER_BOUND) alpha = Math.max(alpha, cached.score);
            if (cached.flag == TtEntry.UPPER_BOUND) beta = Math.min(beta, cached.score);
            if (alpha >= beta) return cached.score;
        }

        // ── Null move pruning ─────────────────────────────────────────
        if (depth >= 3 && !board.getCurrentPlayer().isInCheck() && hasNonPawnMaterial(board)) {
            final Board nullBoard = makeNullMoveBoard(board);
            final int nullScore = min(nullBoard, depth - 3, alpha, beta, ply + 1);
            if (nullScore >= beta) return beta;
        }

        int highestSeenValue = alpha;
        Move bestLocal = null;
        int moveCount = 0;

        for (final Move move : orderedMoves(board, ply)) {
            final MoveTransition t = board.getCurrentPlayer().makeMove(move);
            if (!t.getMoveStatus().isDone()) continue;
            moveCount++;

            int value;
            // ── Late Move Reductions ──────────────────────────────────
            final boolean isQuiet = !move.isAttack() && !(move instanceof Move.PawnPromotion);
            if (isQuiet && moveCount > 4 && depth >= 3
                    && !board.getCurrentPlayer().isInCheck()
                    && !t.getTransitionBoard().getCurrentPlayer().isInCheck()) {
                value = min(t.getTransitionBoard(), depth - 2, highestSeenValue, beta, ply + 1);
                // If the reduced search improves alpha, re-search at full depth
                if (value > highestSeenValue) {
                    value = min(t.getTransitionBoard(), depth - 1, highestSeenValue, beta, ply + 1);
                }
            } else {
                value = min(t.getTransitionBoard(), depth - 1, highestSeenValue, beta, ply + 1);
            }
            if (value > highestSeenValue) {
                highestSeenValue = value;
                bestLocal = move;
            }
            if (highestSeenValue >= beta) {
                storeKiller(ply, move);
                transpositionTable.put(hash, new TtEntry(highestSeenValue, depth, TtEntry.LOWER_BOUND));
                return highestSeenValue;
            }
        }

        final int flag = bestLocal != null ? TtEntry.EXACT : TtEntry.UPPER_BOUND;
        transpositionTable.put(hash, new TtEntry(highestSeenValue, depth, flag));
        return highestSeenValue;
    }

    /**
     * At depth=0, keep searching captures and promotions until the position is "quiet".
     * Prevents the engine from mis-evaluating positions mid-exchange.
     * Limited to QUIESCENCE_DEPTH_LIMIT extra plies to bound the tree.
     *
     * If the side to move is in check, stand-pat is skipped and all legal evasions are
     * searched (not just captures), since a player in check has no "do nothing" option.
     *
     * @param maximising true if it is the maximising player's turn
     */
    private int quiescence(final Board board, int alpha, int beta, final boolean maximising) {
        return quiescence(board, alpha, beta, maximising, 0);
    }

    private int quiescence(final Board board, int alpha, int beta,
                           final boolean maximising, final int qDepth) {
        final boolean inCheck = board.getCurrentPlayer().isInCheck();
        final int standPat = this.boardEvaluator.evaluate(board, 0);

        if (isEndGameScenario(board)) return standPat;
        // Even at the qDepth limit, don't stand pat while in check — cap the check
        // evasion search at the limit too, but don't return a static eval for a side
        // that has no legal "do nothing" option.
        if (qDepth >= QUIESCENCE_DEPTH_LIMIT && !inCheck) return standPat;

        final Collection<Move> movesToSearch = inCheck
                ? board.getCurrentPlayer().getLegalMoves()
                : capturesAndPromotions(board);

        if (maximising) {
            if (!inCheck) {
                if (standPat >= beta) return beta;   // beta cut-off
                alpha = Math.max(alpha, standPat);
            }
            if (inCheck && movesToSearch.isEmpty()) {
                // No legal evasions while in check == checkmate; let the evaluator's
                // mate scoring handle it via isEndGameScenario on the next ply, but as
                // a safety net return the static eval here too.
                return standPat;
            }
            for (final Move move : movesToSearch) {
                final MoveTransition t = board.getCurrentPlayer().makeMove(move);
                if (!t.getMoveStatus().isDone()) continue;
                final int value = quiescence(t.getTransitionBoard(), alpha, beta, false, qDepth + 1);
                alpha = Math.max(alpha, value);
                if (alpha >= beta) return beta;
            }
            return alpha;
        } else {
            if (!inCheck) {
                if (standPat <= alpha) return alpha; // alpha cut-off
                beta = Math.min(beta, standPat);
            }
            if (inCheck && movesToSearch.isEmpty()) {
                return standPat;
            }
            for (final Move move : movesToSearch) {
                final MoveTransition t = board.getCurrentPlayer().makeMove(move);
                if (!t.getMoveStatus().isDone()) continue;
                final int value = quiescence(t.getTransitionBoard(), alpha, beta, true, qDepth + 1);
                beta = Math.min(beta, value);
                if (beta <= alpha) return alpha;
            }
            return beta;
        }
    }

    /**
     * Full ordering for main search: captures (MVV-LVA), killer moves, then quiet.
     */
    private List<Move> orderedMoves(final Board board, final int ply) {
        final Collection<Move> legal = board.getCurrentPlayer().getLegalMoves();
        final List<Move> captures = new ArrayList<>();
        final List<Move> killerList = new ArrayList<>();
        final List<Move> quiet = new ArrayList<>();

        final Move k0 = (ply < MAX_DEPTH) ? killers[ply][0] : null;
        final Move k1 = (ply < MAX_DEPTH) ? killers[ply][1] : null;

        for (final Move m : legal) {
            if (m.isAttack()) {
                captures.add(m);
            } else if (m.equals(k0) || m.equals(k1)) {
                killerList.add(m);
            } else {
                quiet.add(m);
            }
        }
        // Sort captures by MVV-LVA: highest (victim value - attacker value) first
        captures.sort(Comparator.comparingInt(m ->
                -(m.getAttackedPiece().getPieceValue() * 10 - m.getMovedPiece().getPieceValue())));

        final List<Move> ordered = new ArrayList<>(captures.size() + killerList.size() + quiet.size());
        ordered.addAll(captures);
        ordered.addAll(killerList);
        ordered.addAll(quiet);
        return ordered;
    }

    private void storeKiller(final int ply, final Move move) {
        if (move.isAttack() || ply >= MAX_DEPTH) return; // killers are quiet moves only
        if (!move.equals(killers[ply][0])) {
            killers[ply][1] = killers[ply][0];
            killers[ply][0] = move;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Transposition table entry
    // ─────────────────────────────────────────────────────────────────

    private record TtEntry(int score, int depth, int flag) {
        static final int EXACT = 0;
        static final int LOWER_BOUND = 1; // score is a lower bound (beta cutoff stored)
        static final int UPPER_BOUND = 2; // score is an upper bound (alpha never raised)

    }
}