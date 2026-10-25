# HabeshaTV plan

Design source: `HabeshaTV v3.dc.html` (kept outside this repo until the photo rights are sorted).

## Decisions
- Repo: private copy of Gathin23/cleanyourtv (MIT), not a GitHub fork.
- Third top button: **Apps** replaces **Inputs** (the Mi Box has no HDMI inputs or live TV).
- Default theme: `mesob` (basket photo wallpaper; generate our own wallpaper from the weave patterns
  before this repo is ever made public).
- Amharic strings reviewed by the owner (native reader).

## Status (tested on the Mi Box 4, Android 9)
Done and seen on the TV: ring home, app launch and return, Wi-Fi panel (name, band, address), library grid,
Tilet theme via the Menu key, coffee-ceremony saver, Ge'ez numerals, Ethiopian date and clock.
Not yet tried on the TV: hold-OK menu and move mode, the Settings and Apps buttons, Back handling, how smooth the
animations feel, the other three themes.

Known gaps: the Mi Box remote has no Menu key, so the theme switch is unreachable from it; the debug build is signed
with the debug key; the mesob theme has no photo (rights).

Undo on the stick: run `restore.sh` in the project folder; it re-enables the stock launcher and uninstalls this app.

## Steps
1. Baseline: build the unmodified launcher on the dev machine. DONE
2. Ethiopian calendar and clock (`Ethiopic.java`) with tests. DONE
3. Amharic strings and the Ethiopic font (Menbere), cut down by `tools/build_fonts.py`. DONE
4. The ring home screen, drawn natively. DONE
5. Library, move mode, wi-fi panel, coffee-ceremony saver. DONE (move mode untested on the TV)
6. Installed on the stick for testing, stock launcher kept for restore. DONE
