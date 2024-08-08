package com.project.gui;

import com.project.chess.board.Board;
import com.project.chess.board.BoardUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import org.imgscalr.Scalr;
import com.google.common.collect.ImmutableMap;
import org.imgscalr.Scalr.Method;

public class Table {

  private final JFrame gameFrame;
  private final BoardPanel boardPanel;
  private final Board chessBoard;
  private final ImmutableMap<String, ImageIcon> pieceIconCache;

  private static final Dimension OUTER_PANEL_DIMENSION = new Dimension(600, 600);
  private static final Dimension BOARD_PANEL_DIMENSION = new Dimension(400, 350);
  private static final Dimension TILE_PANEL_DIMENSION = new Dimension(10, 10);

  private static final Color LIGHT_TILE_COLOR = new Color(232, 232, 232);
  private static final Color DARK_TILE_COLOR = new Color(118, 150, 86);

  private static final String PIECE_ICON_PATH = "/images/";
  private static final int CHESS_PIECE_WIDTH = 60;
  private static final int CHESS_PIECE_HEIGHT = 60;

  /**
   * 테이블 생성자. 게임 프레임, 체스 보드 및 기물 아이콘 캐시를 초기화합니다.
   * 기물 이미지를 로드하고 보드 패널을 설정합니다.
   */
  public Table() {
    this.gameFrame = createGameFrame();
    this.chessBoard = Board.createStandardBoard();
    this.pieceIconCache = loadAndCachePieceImages();

    this.boardPanel = new BoardPanel();
    this.gameFrame.add(this.boardPanel, BorderLayout.CENTER);
    this.gameFrame.setVisible(true);
  }

  /**
   * 게임 프레임을 생성합니다.
   *
   * @return 생성된 게임 프레임
   */
  private JFrame createGameFrame() {
    JFrame frame = new JFrame("Java Chess");
    frame.setLayout(new BorderLayout());
    frame.setJMenuBar(createTableMenuBar());
    frame.setSize(OUTER_PANEL_DIMENSION);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    return frame;
  }

  /**
   * 테이블 메뉴 바를 생성합니다.
   *
   * @return 생성된 메뉴 바
   */
  private JMenuBar createTableMenuBar() {
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(createFileMenu());
    return menuBar;
  }

  /**
   * 파일 메뉴를 생성합니다.
   *
   * @return 생성된 파일 메뉴
   */
  private JMenu createFileMenu() {
    JMenu fileMenu = new JMenu("File");

    JMenuItem openPGN = new JMenuItem("Load PGN File");
    openPGN.addActionListener(e -> System.out.println("open PGN File"));
    fileMenu.add(openPGN);

    JMenuItem exit = new JMenuItem("Exit");
    exit.addActionListener(e -> System.exit(0));
    fileMenu.add(exit);

    return fileMenu;
  }

  /**
   * 기물 이미지를 로드하고 캐시합니다.
   *
   * @return 불변 맵으로 캐시된 기물 이미지
   */
  private ImmutableMap<String, ImageIcon> loadAndCachePieceImages() {
    ImmutableMap.Builder<String, ImageIcon> pieceIconCacheBuilder = ImmutableMap.builder();
    String[] pieceNames = {"WP", "WR", "WN", "WB", "WQ", "WK", "BP", "BR", "BN", "BB", "BQ", "BK"};
    for (String pieceName : pieceNames) {
      try (InputStream is = getClass().getResourceAsStream(PIECE_ICON_PATH + pieceName + ".png")) {
        if (is == null) {
          throw new IOException("해당 이미지 파일이 없습니다: " + pieceName);
        }
        BufferedImage image = ImageIO.read(is);
        BufferedImage resizedImage = Scalr.resize(image, Method.ULTRA_QUALITY, CHESS_PIECE_WIDTH, CHESS_PIECE_HEIGHT);
        pieceIconCacheBuilder.put(pieceName, new ImageIcon(resizedImage));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return pieceIconCacheBuilder.build();
  }

  /**
   * 보드 패널을 나타내는 클래스입니다.
   */
  private class BoardPanel extends JPanel {
    final List<TilePanel> boardTiles;

    BoardPanel() {
      super(new GridLayout(8, 8));
      this.boardTiles = new ArrayList<>();
      initializeBoardTiles();
      setPreferredSize(BOARD_PANEL_DIMENSION);
      validate();
    }

    /**
     * 보드 타일을 초기화합니다.
     */
    private void initializeBoardTiles() {
      for (int i = 0; i < BoardUtil.NUM_TILES; i++) {
        final TilePanel tilePanel = new TilePanel(this, i);
        this.boardTiles.add(tilePanel);
        add(tilePanel);
      }
    }
  }

  /**
   * 타일 패널을 나타내는 클래스입니다.
   */
  private class TilePanel extends JPanel {

    private final int tileId;

    TilePanel(final BoardPanel boardPanel, final int tileId) {
      super(new GridBagLayout());
      this.tileId = tileId;
      setPreferredSize(TILE_PANEL_DIMENSION);
      assignTileColor();
      assignTilePieceIcon(chessBoard);
      validate();
    }

    /**
     * 타일에 기물 아이콘을 할당합니다.
     *
     * @param board 현재 체스 보드
     */
    private void assignTilePieceIcon(final Board board) {
      this.removeAll();

      if (board.getTile(this.tileId).isTileOccupied()) {
        String pieceName = board.getTile(this.tileId).getPiece().getPieceAlliance().toString().charAt(0) +
            board.getTile(this.tileId).getPiece().toString();
        add(new JLabel(pieceIconCache.get(pieceName)));
      }
    }

    /**
     * 타일 색상을 할당합니다.
     */
    private void assignTileColor() {
      if (BoardUtil.EIGHTH_RANK[this.tileId] ||
          BoardUtil.SIXTH_RANK[this.tileId] ||
          BoardUtil.FOURTH_RANK[this.tileId] ||
          BoardUtil.SECOND_RANK[this.tileId]) {
        setBackground(this.tileId % 2 == 0 ? LIGHT_TILE_COLOR : DARK_TILE_COLOR);
      } else if (BoardUtil.SEVENTH_RANK[this.tileId] ||
          BoardUtil.FIFTH_RANK[this.tileId] ||
          BoardUtil.THIRD_RANK[this.tileId] ||
          BoardUtil.FIRST_RANK[this.tileId]) {
        setBackground(this.tileId % 2 != 0 ? LIGHT_TILE_COLOR : DARK_TILE_COLOR);
      }
    }
  }
}