# JChess

A fully-featured chess game written in Java with a Swing GUI and a built-in AI opponent.

## Features

- **Full chess rules** — all standard moves including castling, en passant, and pawn promotion
- **Drag-and-drop or click-to-move** input on the board
- **AI opponent** powered by Minimax with alpha-beta pruning and configurable search depth (1–20)
- **Position evaluation** using material score, piece-square tables, mobility, check bonus, and castling bonus
- **Synthesised sound effects** — move, capture, check, castle, and game-end sounds generated at runtime (no audio files needed)
- **Three board themes** — Classic, Green, Blue
- **Optional visual aids** — legal-move dots on click/hover, last-move highlight, board coordinate labels
- **Flip board** — view from either side
- **Game history panel** — scrollable move list in algebraic-style notation
- **Taken pieces panel** — captured pieces displayed per side
- **Configurable player types** — each side can be Human or Computer independently

## Requirements

| Dependency | Version |
|---|---|
| Java | 17 or later |
| Gradle (wrapper included) | 9.x |
| Google Guava | 33.0.0-jre |
| imgscalr | 4.2 |
| JUnit Jupiter (tests) | 5.10.0 |

No additional downloads are needed; the Gradle wrapper (`gradlew`) handles all dependencies automatically.

## Building

```bash
./gradlew build          # compile, run tests, produce build/classes
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Running

```bash
./gradlew run
```

Or run the compiled `Main` class directly from your IDE (`src/main/java/org/example/Main.java`).

## How to Play

1. Launch the application — a standard chess board opens with **White (Human) vs Black (Computer)** as the default.
2. **Move a piece** by clicking it then clicking the destination, or by dragging it.
3. **Cancel a selection** with a right-click.
4. The AI will automatically respond after each human move.

### Game Setup

Open **File → Game Setup…** to:
- Set White and Black to **Human** or **Computer**
- Adjust the **Search Depth** (higher = stronger but slower; default is 4)

### Preferences

| Option | Description |
|---|---|
| Flip Board | Swap which side is at the bottom |
| Highlight Legal Moves | Show dots on legal squares after clicking a piece |
| Highlight on Hover | Show dots on legal squares when hovering over a piece |
| Show Coordinates | Overlay rank and file labels (a–h, 1–8) on the board |
| Board Theme | Choose Classic, Green, or Blue colour scheme |
| Sound Effects | Toggle synthesised move sounds on/off |

## Project Structure

```
src/main/java/com/chess/engine/
├── Alliance.java               # WHITE / BLACK alliance enum
├── board/
│   ├── Board.java              # Immutable board state + Builder
│   ├── Move.java               # Move hierarchy (major, attack, castle, en passant, promotion)
│   └── Tile.java               # Empty / Occupied tile
├── pieces/
│   ├── Piece.java              # Abstract base piece
│   ├── Pawn.java
│   ├── Knight.java
│   ├── Bishop.java
│   ├── Rook.java
│   ├── Queen.java
│   └── King.java
├── player/
│   ├── Player.java             # Abstract player (legal moves, check/mate/stalemate detection)
│   ├── WhitePlayer.java
│   ├── BlackPlayer.java
│   ├── MoveTransition.java     # Result of attempting a move
│   ├── MoveStatus.java         # DONE / ILLEGAL_MOVE / LEAVES_PLAYER_IN_CHECK
│   └── ai/
│       ├── MoveStrategy.java   # Strategy interface
│       ├── BoardEvaluator.java # Evaluator interface
│       ├── Minimax.java        # Minimax + alpha-beta pruning
│       └── StandardBoardEvaluator.java  # Material + PST + mobility scoring
├── gui/
│   ├── Table.java              # Main window, board rendering, mouse handling, AI dispatch
│   ├── GameSetup.java          # Player-type / search-depth dialog
│   ├── GameHistoryPanel.java   # Move history sidebar
│   ├── TakenPiecesPanel.java   # Captured pieces sidebar
│   └── SoundManager.java      # Runtime PCM sound synthesis
└── util/
    └── BoardUtils.java         # Board constants and coordinate helpers
```

## AI — How It Works

The computer uses **Minimax search with alpha-beta pruning** ([`Minimax.java`](src/main/java/com/chess/engine/player/ai/Minimax.java)).

Each board position is scored by [`StandardBoardEvaluator`](src/main/java/com/chess/engine/player/ai/StandardBoardEvaluator.java) as:

```
score = Σ White(material + PST bonus + mobility + check + checkmate + castling)
      − Σ Black(same terms)
```

| Term | Description |
|---|---|
| Material | Piece values in centipawns (Pawn=100, Knight/Bishop=300, Rook=500, Queen=800, King=10000) |
| Piece-Square Tables | Standard positional bonus tables for each piece type |
| Mobility | Number of legal moves available |
| Check bonus | +45 cp for putting the opponent in check |
| Checkmate bonus | +10 000 × depth for checkmating the opponent |
| Castling bonus | +60 cp for having castled |

## License

This project is provided as-is for educational purposes.
