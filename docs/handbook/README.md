# Amade Game Developer's Handbook

The handbook teaches game programming in Java by building the amadeOS games and their engine, one game at a time. It is published at <https://datazuul.github.io/amade-game-engine/>.

## Writing

- One Markdown file per chapter in `chapters/`, numbered: `0x` getting started, `1x` part I, `2x` part II, `3x` part III. The order of the book is the `nav` in `mkdocs.yml`.
- Pictures go to `chapters/images/<game>/`.
- **Code is never copied into a chapter.** It is included from the game's repository, from a section marked in the source:

  ```java
  // --8<-- [start:flood]
  private void flood(int start) { … }
  // --8<-- [end:flood]
  ```

  and in the chapter, inside a code block:

  ```text
  --8<-- "amade-mines-game/src/main/java/eu/amadeos/games/mines/domain/Board.java:flood"
  ```

  Paths start at the directory that holds the repositories, so the games must be checked out next to `amade-game-engine`. The book then always shows code that compiles and is tested.
- The workflow `.github/workflows/handbook.yml` checks out each game at a fixed commit (`MINES_COMMIT`, `SOLITAIRE_COMMIT`, `PADDLE_COMMIT`, …). Raise it when a chapter is updated for a newer state of the game.

## Building locally

```bash
cd docs/handbook
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
.venv/bin/mkdocs serve          # http://127.0.0.1:8000, reloads on every save
.venv/bin/mkdocs build --strict # what CI runs: fails on broken links and missing excerpts
```
