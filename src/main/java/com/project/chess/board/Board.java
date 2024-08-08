package com.project.chess.board;

import static com.project.chess.Alliance.BLACK;
import static com.project.chess.Alliance.WHITE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.project.chess.Alliance;
import com.project.chess.moves.Move;
import com.project.chess.pieces.Bishop;
import com.project.chess.pieces.King;
import com.project.chess.pieces.Knight;
import com.project.chess.pieces.Pawn;
import com.project.chess.pieces.Piece;
import com.project.chess.pieces.Queen;
import com.project.chess.pieces.Rook;
import com.project.chess.player.BlackPlayer;
import com.project.chess.player.Player;
import com.project.chess.player.WhitePlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Getter;

/**
 * 체스 보드를 나타내는 클래스입니다.
 */
@Getter
public class Board {

  private final List<Tile> gameBoard;
  private final Collection<Piece> whitePieces;
  private final Collection<Piece> blackPieces;

  private final WhitePlayer whitePlayer;
  private final BlackPlayer blackPlayer;
  private final Player currentPlayer;

  /**
   * 생성자: 빌더를 사용하여 보드를 초기화합니다.
   *
   * @param builder 보드를 구성하는 빌더 객체
   */
  Board(final Builder builder) {
    this.gameBoard = createGameBoard(builder);
    this.whitePieces = calculateActivePieces(this.gameBoard, WHITE);
    this.blackPieces = calculateActivePieces(this.gameBoard, BLACK);

    final Collection<Move> whiteStandardLegalMoves = calculateLegalMoves(this.whitePieces);
    final Collection<Move> blackStandardLegalMoves = calculateLegalMoves(this.blackPieces);

    this.whitePlayer = new WhitePlayer(this, whiteStandardLegalMoves, blackStandardLegalMoves);
    this.blackPlayer = new BlackPlayer(this, whiteStandardLegalMoves, blackStandardLegalMoves);
    this.currentPlayer = builder.getNextMoveMaker()
        .choosePlayer(this.whitePlayer, this.blackPlayer);
  }

  /**
   * 주어진 조각들의 합법적인 이동을 계산합니다.
   *
   * @param pieces 조각 컬렉션
   * @return 합법적인 이동 목록
   */
  private Collection<Move> calculateLegalMoves(final Collection<Piece> pieces) {
    final List<Move> legalMoves = new ArrayList<>();

    for (final Piece piece : pieces) {
      legalMoves.addAll(piece.calculateLegalMoves(this));
    }

    return ImmutableList.copyOf(legalMoves);
  }

  /**
   * 활성화된 조각들을 계산합니다.
   *
   * @param gameBoard 게임 보드
   * @param alliance  연합(색깔)
   * @return 활성화된 조각 컬렉션
   */
  private static Collection<Piece> calculateActivePieces(final List<Tile> gameBoard,
      final Alliance alliance) {
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

  /**
   * 주어진 좌표의 타일을 반환합니다.
   *
   * @param tileCoordinate 타일의 좌표
   * @return 해당 좌표의 타일
   */
  public Tile getTile(final int tileCoordinate) {
    return gameBoard.get(tileCoordinate);
  }

  /**
   * 빌더 객체를 사용하여 게임 보드를 생성합니다.
   *
   * @param builder 보드 빌더 객체
   * @return 생성된 게임 보드 리스트
   */
  private static List<Tile> createGameBoard(final Builder builder) {
    final Tile[] tiles = new Tile[BoardUtil.NUM_TILES];

    // 각 타일을 생성하여 배열에 할당합니다.
    for (int i = 0; i < BoardUtil.NUM_TILES; i++) {
      tiles[i] = Tile.createTile(i, builder.getBoardConfig().get(i));
    }

    return ImmutableList.copyOf(tiles);
  }

  /**
   * 표준 체스 보드를 생성합니다.
   *
   * @return 생성된 체스 보드
   */
  public static Board createStandardBoard() {
    final Builder builder = new Builder();

    // 블랙 기물 배치
    builder.setPiece(new Rook(0, BLACK))
        .setPiece(new Knight(1, BLACK))
        .setPiece(new Bishop(2, BLACK))
        .setPiece(new Queen(3, BLACK))
        .setPiece(new King(4, BLACK))
        .setPiece(new Bishop(5, BLACK))
        .setPiece(new Knight(6, BLACK))
        .setPiece(new Rook(7, BLACK));

    for (int i = 8; i < 16; i++) {
      builder.setPiece(new Pawn(i, BLACK));
    }

    // 화이트 기물 배치
    for (int i = 48; i < 56; i++) {
      builder.setPiece(new Pawn(i, WHITE));
    }

    builder.setPiece(new Rook(56, WHITE))
        .setPiece(new Knight(57, WHITE))
        .setPiece(new Bishop(58, WHITE))
        .setPiece(new Queen(59, WHITE))
        .setPiece(new King(60, WHITE))
        .setPiece(new Bishop(61, WHITE))
        .setPiece(new Knight(62, WHITE))
        .setPiece(new Rook(63, WHITE));

    builder.setMoveMaker(WHITE);

    return builder.build();
  }

  public Iterable<Move> getAllLegalMoves() {
    return Iterables.unmodifiableIterable(Iterables.concat(
        this.whitePlayer.getLegalMoves(),
        this.blackPlayer.getLegalMoves()));
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < BoardUtil.NUM_TILES; i++) {
      final String tileText = this.gameBoard.get(i).toString();
      builder.append(String.format("%3s", tileText));
      if ((i + 1) % BoardUtil.NUM_TILES_PER_ROW == 0) {
        builder.append("\n");
      }
    }
    return builder.toString();
  }
}