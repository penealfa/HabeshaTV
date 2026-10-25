#!/usr/bin/env python3
"""Cut the HabeshaTV fonts down to the characters the launcher uses.

Usage: python3 -I tools/build_fonts.py <folder with the three variable .ttf files> [<repo root>]

Inputs (OFL, from github.com/google/fonts, ofl/<family>/):
  Menbere[wght].ttf, Figtree[wght].ttf, BigShouldersDisplay[wght].ttf
Outputs: launcher/app/src/main/res/font/*.ttf, one static file per weight we draw with.

The Ge'ez characters come from the launcher's own source (HabeshaStrings.java and Ethiopic.java) plus every Ge'ez
numeral, so after adding a word there, run this again. Needs fontTools (pip install fonttools).
"""
import os
import sys

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

# output name -> (source file, weight)
INSTANCES = {
    "menbere_light": ("Menbere[wght].ttf", 300),
    "menbere_medium": ("Menbere[wght].ttf", 500),
    "menbere_semibold": ("Menbere[wght].ttf", 600),
    "figtree_regular": ("Figtree[wght].ttf", 400),
    "figtree_medium": ("Figtree[wght].ttf", 500),
    "figtree_semibold": ("Figtree[wght].ttf", 600),
    "figtree_bold": ("Figtree[wght].ttf", 700),
    "bigshoulders_extralight": ("BigShouldersDisplay[wght].ttf", 200),
    "bigshoulders_extrabold": ("BigShouldersDisplay[wght].ttf", 800),
}

LATIN = set(range(0x20, 0x7F)) | {0xA0, 0xB7, 0x2013, 0x2022}
GEEZ_NUMERALS = set(range(0x1369, 0x137D))  # ፩ to ፼
GEEZ_PUNCTUATION = set(range(0x1360, 0x1369))


def geez_in(path):
    with open(path, encoding="utf-8") as f:
        return {ord(c) for c in f.read() if 0x1200 <= ord(c) <= 0x139F}


def main():
    src = sys.argv[1]
    root = sys.argv[2] if len(sys.argv) > 2 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
    java = os.path.join(root, "launcher", "app", "src", "main", "java", "com", "gathin", "tvhome")
    out = os.path.join(root, "launcher", "app", "src", "main", "res", "font")
    geez = geez_in(os.path.join(java, "HabeshaStrings.java")) | geez_in(os.path.join(java, "Ethiopic.java"))
    geez |= GEEZ_NUMERALS | GEEZ_PUNCTUATION
    print(f"{len(geez)} Ge'ez characters, {len(LATIN)} Latin")
    os.makedirs(out, exist_ok=True)
    for name, (file, weight) in INSTANCES.items():
        font = TTFont(os.path.join(src, file))
        static = instancer.instantiateVariableFont(font, {"wght": weight}, inplace=False)
        unicodes = set(LATIN)
        if file.startswith("Menbere"):
            unicodes |= geez
        opts = subset.Options()
        opts.hinting = False
        opts.desubroutinize = True
        opts.notdef_outline = True
        opts.name_IDs = [0, 1, 2, 3, 4, 6, 13, 14]
        opts.layout_features = ["kern", "liga", "ccmp", "locl", "mark", "mkmk"]
        sub = subset.Subsetter(opts)
        sub.populate(unicodes=unicodes)
        sub.subset(static)
        dest = os.path.join(out, name + ".ttf")
        static.save(dest)
        print(f"{name}.ttf  {os.path.getsize(dest) // 1024} KB")


if __name__ == "__main__":
    main()
