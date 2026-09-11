package com.chess.engine.gui;

import com.google.common.collect.Lists;

import java.util.List;

public enum BoardDirection {
    NORMAL {
        @Override
        public List<TilePanel> traverse(List<TilePanel> t) {
            return t;
        }

        @Override
        public BoardDirection opposite() {
            return FLIPPED;
        }
    },
    FLIPPED {
        @Override
        public List<TilePanel> traverse(List<TilePanel> t) {
            return Lists.reverse(t);
        }

        @Override
        public BoardDirection opposite() {
            return NORMAL;
        }
    };

    public abstract List<TilePanel> traverse(List<TilePanel> boardTiles);

    public abstract BoardDirection opposite();
}
