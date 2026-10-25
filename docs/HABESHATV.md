# HabeshaTV plan

Design source: `HabeshaTV v3.dc.html` (kept outside this repo until the photo rights are sorted).

## Decisions
- Repo: private copy of Gathin23/cleanyourtv (MIT), not a GitHub fork.
- Third top button: **Apps** replaces **Inputs** (the Mi Box has no HDMI inputs or live TV).
- Default theme: `mesob` (basket photo wallpaper; generate our own wallpaper from the weave patterns
  before this repo is ever made public).
- Amharic strings reviewed by the owner (native reader).

## Steps
1. Baseline: build the unmodified launcher on the dev machine (no install).
2. Ethiopian calendar and clock (`Ethiopic.java`, next to `Lunar.java` and `Shamsi.java`) with tests.
3. Amharic strings and the Ethiopic font (Menbere, Noto Sans Ethiopic), subset to keep the APK small.
4. The ring home screen, drawn natively (no 2040 px bitmaps; the Mi Box has 2 GB of RAM).
5. Library, move mode, Wi-Fi and Apps panels, coffee-ceremony screensaver.
6. Install on the stick only with the owner's approval, with the stock launcher kept for restore.
