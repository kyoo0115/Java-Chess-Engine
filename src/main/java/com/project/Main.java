package com.project;


import com.project.chess.board.Board;
import com.project.gui.Table;

public class Main {

  public static void main(String[] args) {

    Board board = Board.createStandardBoard();

    System.out.println(board);

    Table table = new Table();
  }
}