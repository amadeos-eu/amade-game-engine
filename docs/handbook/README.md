# Amade Game Developer's Handbook

The handbook teaches game programming in Java by building the amadeOS games and their engine, one game at a time. It is published at <https://datazuul.github.io/amade-game-engine/>.

## Writing

- One Markdown file per chapter in `chapters/`, numbered: `0x` getting started, `1x` part I, `2x` part II, `3x` part III, `4x` part IV (the engine manual, by subsystem rather than by game). The order of the book is the `nav` in `mkdocs.yml`.
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

## The offline package

amadeOS installs the handbook as the Debian package `amade-game-handbook`: the pages in
`/usr/share/doc/amade-game-handbook/html` and a desktop entry of category `Documentation`, so the
Help drawer of the Front Panel lists *Game Developer's Handbook* and opens it in the web browser.

```bash
./gradlew installToAmadeOs        # builds the package and copies it to ../amadeos/config/packages.chroot
docs/handbook/package-deb.sh 0.1.0 /tmp   # the package alone
```

It is built from `mkdocs-offline.yml`, which takes everything from `mkdocs.yml` and adds what reading
from disk needs: links to `.html` files (`use_directory_urls: false`), the `offline` plugin for search
without a server, and the `privacy` plugin, which stores the fonts and scripts (Mermaid) that the
pages would load from the web. The games must be checked out next to `amade-game-engine` as for the
published book; MkDocs is installed into `.venv` if none is on the PATH. CI builds this variant too.
