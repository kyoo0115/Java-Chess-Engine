package com.project.chess.moves;

import com.project.chess.board.Board;
import lombok.Getter;

@Getter
public final class NullMove extends Move {

  public NullMove() {
    super(null, null, -1);
  }

  @Override
  public Board execute() {
    throw new RuntimeException("Cannot execute the null move");
  }
}
