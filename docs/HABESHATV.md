# HabeshaTV plan

Design source: `HabeshaTV v3.dc.html` (kept outside this repo until the photo rights are sorted).

## Decisions
- Repo: private copy of Gathin23/cleanyourtv (MIT), not a GitHub fork.
- Third top button: **Apps** replaces **Inputs** (the Mi Box has no HDMI inputs or live TV).
- Default theme: `mesob`, with the basket photo from the design as its wallpaper
  (`launcher/app/src/main/res/drawable-nodpi/wallpaper_mesob.jpg`). The photo's licence is unverified: keep this
  repo private, or swap in a wallpaper made from the weave patterns, before it is ever made public.
- The big clock shows Ethiopian hours (10:24 reads 4:24); the line beside it spells the hour in Amharic.
- Amharic strings reviewed by the owner (native reader).

## Status (installed on the Mi Box 4, Android 9)
Done and seen on the TV: ring home, app launch and return, Wi-Fi panel (name, band, address), library grid,
the Theme row in the hold-OK menu (steps mesob, tilet, lalibela, buna), a still clock screensaver, Ge'ez numerals,
Ethiopian date and clock, and the basket wallpaper.
Not yet tried on the TV: move mode, the Remove row, the Settings and Apps buttons, how smooth the animations feel.

Known gaps: the screensaver no longer drifts,
so a bright still image stays on screen (burn-in is a risk on OLED sets); the debug build is signed with the debug key; the wallpaper photo's licence is unverified.

Undo on the stick: run `restore.sh` in the project folder; it re-enables the stock launcher and uninstalls this app.

## Steps
1. Baseline: build the unmodified launcher on the dev machine. DONE
2. Ethiopian calendar and clock (`Ethiopic.java`) with tests. DONE
3. Amharic strings and the Ethiopic font (Menbere), cut down by `tools/build_fonts.py`. DONE
4. The ring home screen, drawn natively. DONE
5. Library, move mode, wi-fi panel, still clock saver (the coffee-ceremony version was removed on request). DONE
6. Installed on the stick as its permanent home screen (release build, stock launcher disabled, survives a reboot);
   the stock launcher stays installed so the change can be undone. DONE

## Keeping it permanent
- The stock launcher is switched off with `pm disable-user`, which survives reboots. A system update can switch it
  back on; if Home ever shows the stock screen again, run `disable_batch.sh` for `com.google.android.tvlauncher`.
- To reinstall after a factory reset: install the release APK, grant `ACCESS_FINE_LOCATION` (it lets the Wi-Fi
  button show the network name), then disable the stock launcher.
- Build it with `./gradlew assembleRelease` in `launcher/`. Release builds are signed with the debug key, which is
  fine for a sideloaded app that is never published.
