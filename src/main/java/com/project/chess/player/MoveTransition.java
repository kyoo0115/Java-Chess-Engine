package com.project.chess.player;

import com.project.chess.board.Board;
import com.project.chess.moves.Move;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MoveTransition {

  private final Board transitionBoard;
  private final Move move;
  private final MoveStatus moveStatus;

}

