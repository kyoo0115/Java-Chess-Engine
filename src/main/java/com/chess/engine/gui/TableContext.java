package com.chess.engine.gui;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.Tile;
import com.chess.engine.pieces.Piece;

import java.awt.image.BufferedImage;
import java.awt.Point;
import java.util.Map;

/**
 * Read-only view of Table's shared rendering state, passed to BoardPanel and TilePanel.
 * Avoids exposing Table as a dependency while keeping all shared fields in one place.
 */
public interface TableContext {
    Board getChessBoard();
    BoardTheme getBoardTheme();
    BoardDirection getBoardDirection();
    boolean isHighlightLegalMoves();
    boolean isHoverHighlight();
    boolean isShowCoordinates();
    Tile getSourceTile();
    Piece getHumanMovedPiece();
    BufferedImage getDragImage();
    Point getDragPoint();
    int getDragSourceTileId();
    int getHoverTileId();
    int getLastMoveSource();
    int getLastMoveDest();
    int getArrowSource();
    int getArrowDest();
    BufferedImage getAnimPiece();
    float getAnimFromX();
    float getAnimFromY();
    float getAnimToX();
    float getAnimToY();
    float getAnimProgress();
    Map<String, BufferedImage> getScaledImageCache();
    Map<String, BufferedImage> getRawImageCache();
}
