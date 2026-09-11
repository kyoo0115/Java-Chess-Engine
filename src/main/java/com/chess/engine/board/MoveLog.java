package com.chess.engine.board;

import java.util.ArrayList;
import java.util.List;

public class MoveLog {
    private final List<Move> moves = new ArrayList<>();

    public List<Move> getMoves() {
        return moves;
    }

    public void addMove(Move m) {
        moves.add(m);
    }

    public int size() {
        return moves.size();
    }

    public void clear() {
        moves.clear();
    }

    public void removeMove(int i) {
        moves.remove(i);
    }

    public boolean removeMove(Move m) {
        return moves.remove(m);
    }
}
