# JavaChess

A fully-featured chess game written in Java with a Swing GUI and a Stockfish AI opponent.

![Java](https://img.shields.io/badge/Java-17%2B-blue) ![Gradle](https://img.shields.io/badge/Gradle-9.x-green)

## Features

### Gameplay

- **Full chess rules** — castling, en passant, pawn promotion, fifty-move rule, threefold repetition
- **Drag-and-drop or click-to-move** input
- **Pawn promotion dialog** — choose Queen, Rook, Bishop, or Knight via a piece-icon dialog
- **Draw detection** — fifty-move rule and threefold repetition handled automatically

### AI

- **Stockfish engine** — connects to a local Stockfish process via the UCI protocol
- **Difficulty presets** — Easy (100 ms), Medium (500 ms), Hard (2 s), Master (5 s), or Custom think time
- **Engine vs Engine** — both sides can be set to Computer for automated play, with pause/resume
- **AI move animation** — smooth piece glide when the engine plays

### Clock

- **Configurable chess clock** — presets: 1, 3, 5, 10, 15, 30 min, or custom
- **Decimal countdown** — displays `mm:ss` normally, switches to `ss.d` (tenths) when under one minute
- **Per-player cards** — active player highlighted with a green border

### UI

- **Modern Swing interface** — header bar with tab navigation, left sidebar, right panel with clock and history
- **Light / Dark theme** — toggle at any time; preference persisted across sessions
- **Four board themes** — Wood Modern (default), Classic, Green, Blue
- **Move arrow overlay** — gold arrow shows the last move played by either side
- **Right-click annotations** — draw arrows and circles on the board (green)
- **Red king highlight** — king tile tinted red when in check
- **Coordinate labels** — rank and file labels on the board edges
- **Legal move dots** — dots on valid destination squares when dragging or hovering
- **Flip board** — view from either side
- **Hover highlight** — show legal moves on mouse-over (optional)

### Panels & History

- **Game history panel** — scrollable move list in algebraic notation with last-move pill highlight
- **Taken pieces panel** — captured pieces per side with material advantage indicator
- **Clock cards** — per-player time display inside the right sidebar

### File Operations

- **Save / Load PGN** — export and import games in standard PGN format (Ctrl+S / Ctrl+O)
- **Load FEN** — paste any FEN string to jump to an arbitrary position
- **Export board image** — save the current board view as a PNG file
- **Board editor** — drag-and-drop position editor (File → Edit Position…)

### Misc

- **Sound effects** — move, capture, check, castle, and game-end sounds (MP3, decoded at startup)
- **Undo / Take-back** — Ctrl+Z; pops 2 plies in Human vs Computer, 1 in Human vs Human
- **Persistent preferences** — theme, board style, sound, highlights, coordinates, difficulty all saved via the JDK
  `Preferences` API

---

## Requirements

| Dependency                | Version     |
|---------------------------|-------------|
| Java                      | 17 or later |
| Gradle (wrapper included) | 9.x         |
| Stockfish binary          | Any recent  |
| Google Guava              | 33.0.0-jre  |
| imgscalr                  | 4.2         |
| JLayer (MP3)              | 1.0.1       |
| JUnit Jupiter (tests)     | 5.10.0      |

Gradle handles all Java dependencies automatically via the wrapper. Stockfish must be installed separately (see below).

---

## Installing Stockfish

Stockfish is looked up in this order:

1. System property `-Dstockfish.path=<path>` passed at startup
2. Known fixed paths: `C:\stockfish\stockfish.exe`, `/usr/bin/stockfish`, `/usr/local/bin/stockfish`,
   `/opt/homebrew/bin/stockfish`
3. `stockfish` found anywhere on `PATH`

**Windows (winget):**

```powershell
winget install Stockfish.Stockfish
```

**macOS (Homebrew):**

```bash
brew install stockfish
```

**Linux (apt):**

```bash
sudo apt install stockfish
```

---

## Building & Running

```bash
# Compile and run tests
./gradlew build

# Run the application
./gradlew run
```

On Windows use `gradlew.bat` instead of `./gradlew`.

The entry point is [`com.chess.JavaChess`](src/main/java/com/chess/JavaChess.java).

---

## How to Play

1. Launch the application — a standard board opens with **White (Human) vs Black (Computer)** as the default.
2. **Move a piece** by clicking it then clicking the destination, or by dragging it.
3. **Cancel a selection** with a right-click. Right-click drag draws annotation arrows.
4. The AI responds automatically after each human move; a gold arrow shows where it moved.
5. Open **File → Game Setup…** (or click **Settings** in the header) to change players and difficulty.

### Keyboard Shortcuts

| Shortcut | Action          |
|----------|-----------------|
| Ctrl+Z   | Undo last move  |
| Ctrl+S   | Save game (PGN) |
| Ctrl+O   | Load game (PGN) |

---

## Project Structure

```
src/main/java/com/chess/
├── JavaChess.java                   # Application entry point
└── engine/
    ├── Alliance.java                # WHITE / BLACK enum with direction helpers
    ├── PlayerType.java              # HUMAN / COMPUTER enum
    ├── board/
    │   ├── Board.java               # Immutable board state + Builder (Piece[64])
    │   ├── Move.java                # Move hierarchy (major, attack, castle, en passant, promotion)
    │   ├── MoveLog.java             # Ordered list of played moves
    │   └── Tile.java                # Empty / Occupied tile
    ├── pieces/
    │   ├── Piece.java               # Abstract base (shared sliding-ray generator)
    │   ├── Pawn.java
    │   ├── Knight.java
    │   ├── Bishop.java
    │   ├── Rook.java
    │   ├── Queen.java
    │   └── King.java
    ├── player/
    │   ├── Player.java              # Legal moves, check/mate/stale detection, attack maps
    │   ├── WhitePlayer.java
    │   ├── BlackPlayer.java
    │   ├── MoveTransition.java      # Result of attempting a move
    │   ├── MoveStatus.java          # DONE / ILLEGAL_MOVE / LEAVE_PLAYER_IN_CHECK
    │   └── ai/
    │       ├── MoveStrategy.java    # Strategy interface
    │       ├── BoardEvaluator.java  # Evaluator interface
    │       └── StockfishEngine.java # UCI bridge to local Stockfish process
    ├── gui/
    │   ├── Table.java               # Main window, event dispatch, AI worker, game state
    │   ├── TableContext.java        # Read-only rendering state interface
    │   ├── BoardPanel.java          # 8×8 tile grid, drag ghost, animations, arrows
    │   ├── BoardContainer.java      # Board wrapper with coordinate labels
    │   ├── TilePanel.java           # Single square — colors, piece icon, dots, overlays
    │   ├── GameSetup.java           # Player-type / difficulty / clock dialog
    │   ├── GameHistoryPanel.java    # Move history + playback controls + toggles
    │   ├── ClockPanel.java          # Per-player countdown clock cards (ms precision)
    │   ├── TakenPiecesPanel.java    # Captured pieces strip
    │   ├── LeftSidebar.java         # Navigation sidebar (Play / Analysis / Learn / Settings)
    │   ├── HeaderBar.java           # Top header with tab pills and theme toggle
    │   ├── BoardEditorDialog.java   # Drag-and-drop position editor
    │   ├── BoardTheme.java          # Wood / Classic / Green / Blue colour sets
    │   ├── BoardDirection.java      # NORMAL / FLIPPED board orientation
    │   ├── UITheme.java             # Light/dark palette + theme-change listener bus
    │   ├── SoundManager.java        # MP3→PCM decoder, async single-thread playback
    │   ├── PgnUtils.java            # PGN save/load helpers
    │   ├── ChessIcons.java          # SVG-style programmatic icon factory
    │   └── ToggleSwitch.java        # Animated toggle switch component
    └── util/
        └── BoardUtils.java          # 64-square constants, column/rank helpers, algebraic notation
```

---

## AI — How It Works

The computer uses a local **Stockfish** process connected over the **UCI protocol**
([`StockfishEngine.java`](src/main/java/com/chess/engine/player/ai/StockfishEngine.java)).

Each move is requested as:

```
position fen <fen>
go movetime <ms>
```

Stockfish responds with a `bestmove` line which is translated back to the game's `Move` object.

### Difficulty presets

| Preset | Think time |
|--------|------------|
| Easy   | 100 ms     |
| Medium | 500 ms     |
| Hard   | 2 000 ms   |
| Master | 5 000 ms   |
| Custom | User-set   |

The engine process is started once per game session and reused across moves, so UCI initialisation only happens once. It
is cleanly shut down (`quit`) when the window is closed or the game is reset.

---

## License

This project is provided as-is for educational purposes.
