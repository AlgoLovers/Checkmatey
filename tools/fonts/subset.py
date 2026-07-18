#!/usr/bin/env python3
"""Subset PretendardVariable.ttf down to the glyphs Checkmatey actually renders.

The full Hangul font is 6.7MB; keeping only characters that appear in our own
source/resources drops it to ~0.4MB. All Korean text is authored by us (no user
input), so nothing is missing. Re-run whenever UI copy changes.

Usage: python3 tools/fonts/subset.py <PretendardVariable.ttf>
"""
import glob
import os
import subprocess
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "app/src/main/res/font/pretendard.ttf")


def collect_charset():
    chars = set()
    # All authored strings live in Kotlin source + res + assets.
    patterns = [
        "app/src/main/java/**/*.kt",
        "app/src/main/res/values*/*.xml",
        "app/src/main/assets/**/*.json",
    ]
    for pat in patterns:
        for f in glob.glob(os.path.join(ROOT, pat), recursive=True):
            try:
                chars |= set(open(f, encoding="utf-8").read())
            except (UnicodeDecodeError, IsADirectoryError):
                pass
    chars |= set("".join(chr(c) for c in range(0x20, 0x7F)))  # ASCII
    chars |= set("♔♕♖♗♘♙♚♛♜♝♞♟△▲▽▼◀▶●○◆■□★☆✓✗✕→←↑↓↔·…—–％＋－＝°±×÷≤≥≠")
    chars |= set("0123456789")
    return "".join(sorted(chars))


def main():
    if len(sys.argv) < 2:
        sys.exit("Usage: python3 tools/fonts/subset.py <PretendardVariable.ttf>")
    src = sys.argv[1]
    charset_file = os.path.join(os.path.dirname(__file__), "_charset.txt")
    open(charset_file, "w", encoding="utf-8").write(collect_charset())
    subprocess.run(
        [
            sys.executable, "-m", "fontTools.subset", src,
            f"--text-file={charset_file}", f"--output-file={OUT}",
            "--layout-features=*", "--notdef-glyph", "--notdef-outline", "--recommended-glyphs",
        ],
        check=True,
    )
    print(f"-> {OUT}  ({os.path.getsize(OUT) // 1024} KB)")


if __name__ == "__main__":
    main()
