# JChess

A fully-featured chess game written in Java with a Swing GUI and a built-in AI opponent.

## Features

- **Full chess rules** — all standard moves including castling, en passant, and pawn promotion
- **Drag-and-drop or click-to-move** input on the board
- **Pawn promotion dialog** — choose Queen, Rook, Bishop, or Knight via a piece-icon dialog
- **AI opponent** powered by Minimax with alpha-beta pruning, transposition table, killer moves, quiescence search, and
  MVV-LVA move ordering
- **Difficulty presets** — Easy (depth 2), Medium (depth 4), Hard (depth 6), Master (depth 8), or Custom depth via Game
  Setup
- **Endgame-aware evaluation** — switches to a centralisation king PST when material drops below ~1300 cp
- **Move arrow overlay** — a chess.com-style orange arrow shows the last move played (for both human and AI moves)
- **Red king highlight** — the king's tile is tinted red when in check
- **Undo / Take-back** — File → Undo (or Ctrl+Z) takes back the last move (2 plies in Human vs Computer, 1 in Human vs
  Human)
- **Sound effects** — move, capture, check, castle, and game-end sounds via MP3 files (lichess open-source audio)
- **Three board themes** — Classic, Green, Blue
- **Optional visual aids** — legal-move dots on click/hover, last-move highlight, selected-tile highlight, board
  coordinate labels
- **Flip board** — view from either side
- **Game history panel** — scrollable move list in algebraic-style notation
- **Taken pieces panel** — captured pieces displayed per side with material advantage
- **Configurable player types** — each side can be Human or Computer independently
- **Persistent preferences** — board theme, sound, highlights, coordinates, and difficulty are saved across sessions (no
  config file needed — uses the JDK `Preferences` API)

## Requirements

| Dependency                | Version     |
|---------------------------|-------------|
| Java                      | 17 or later |
| Gradle (wrapper included) | 9.x         |
| Google Guava              | 33.0.0-jre  |
| imgscalr                  | 4.2         |
| JUnit Jupiter (tests)     | 5.10.0      |

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
4. The AI will automatically respond after each human move; an orange arrow shows where it moved.

### Game Setup

Open **File → Game Setup…** to:

- Set White and Black to **Human** or **Computer**
- Choose a **Difficulty** preset (Easy / Medium / Hard / Master / Custom)

### Preferences

| Option                | Description                                           |
|-----------------------|-------------------------------------------------------|
| Flip Board            | Swap which side is at the bottom                      |
| Highlight Legal Moves | Show dots on legal squares after clicking a piece     |
| Highlight on Hover    | Show dots on legal squares when hovering over a piece |
| Show Coordinates      | Overlay rank and file labels (a–h, 1–8) on the board  |
| Board Theme           | Choose Classic, Green, or Blue colour scheme          |
| Sound Effects         | Toggle move sounds on/off                             |

All preferences are saved automatically and restored on next launch.

### Keyboard Shortcuts

| Shortcut | Action         |
|----------|----------------|
| Ctrl+Z   | Undo last move |

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
│       ├── Minimax.java        # Minimax + alpha-beta + transposition table + killer moves + quiescence
│       └── StandardBoardEvaluator.java  # Material + PST (with endgame king table) + mobility scoring
├── gui/
│   ├── Table.java              # Main window, board rendering, mouse handling, AI dispatch
│   ├── GameSetup.java          # Player-type / difficulty dialog
│   ├── GameHistoryPanel.java   # Move history sidebar
│   ├── TakenPiecesPanel.java   # Captured pieces sidebar
│   └── SoundManager.java      # Runtime PCM sound synthesis
└── util/
    └── BoardUtils.java         # Board constants and coordinate helpers
```

## AI — How It Works

The computer uses **Minimax search with alpha-beta pruning** ([
`Minimax.java`](src/main/java/com/chess/engine/player/ai/Minimax.java)).

Additional optimisations:

- **Transposition table** — Zobrist-hashed cache with EXACT / LOWER_BOUND / UPPER_BOUND flags
- **Killer move heuristic** — 2 killer slots per ply
- **Quiescence search** — captures-only extension up to 4 plies with stand-pat pruning
- **MVV-LVA move ordering** — most-valuable-victim / least-valuable-attacker capture ordering

Each board position is scored by [
`StandardBoardEvaluator`](src/main/java/com/chess/engine/player/ai/StandardBoardEvaluator.java) as:

```
score = Σ White(material + PST bonus + mobility + check + checkmate + castling)
      − Σ Black(same terms)
```

| Term                | Description                                                                                            |
|---------------------|--------------------------------------------------------------------------------------------------------|
| Material            | Piece values in centipawns (Pawn=100, Knight/Bishop=300, Rook=500, Queen=800, King=10000)              |
| Piece-Square Tables | Standard positional bonus tables; king uses endgame centralisation table when total material < 1300 cp |
| Mobility            | Number of legal moves available                                                                        |
| Check bonus         | +45 cp for putting the opponent in check                                                               |
| Checkmate bonus     | +10 000 × depth for checkmating the opponent                                                           |
| Castling bonus      | +60 cp for having castled                                                                              |

## License

This project is provided as-is for educational purposes.
