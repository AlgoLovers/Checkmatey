#!/usr/bin/env python3
"""Curate the Lichess CC0 puzzle database (https://database.lichess.org/#puzzles) into a small,
beginner-focused, on-device asset: app/src/main/assets/puzzles.csv

Lichess row: PuzzleId,FEN,Moves,Rating,RatingDeviation,Popularity,NbPlays,Themes,GameUrl,OpeningTags
- FEN is the position BEFORE the setup move; Moves[0] is that setup move (auto-played), the rest is
  the solution the solver must find (solver, forced reply, solver, ...).
- We pre-apply the setup move here (python-chess) so the app just loads the position the solver
  faces + the solution line, and we validate the whole line is legal before writing it.

Curation for the "beginner ~1400 tutor" wedge:
- rating 400..1500, decent quality (Popularity, NbPlays), primary beginner theme
- bucket by (theme, 100-pt rating band) and cap per bucket so difficulty & motif coverage is even
- target a few thousand puzzles total (small enough to bundle, big enough to never repeat)

Usage: zstdcat lichess_db_puzzle.csv.zst | python3 tools/puzzles/build_puzzles.py
"""
import csv
import os
import sys
import chess

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "app/src/main/assets/puzzles.csv")

MIN_RATING, MAX_RATING = 400, 1500
MIN_POPULARITY = 90        # -100..100; higher = more upvoted
MIN_PLAYS = 500
PER_BUCKET = 90            # cap per (theme, rating-band)
BAND = 100

# Lichess theme -> Korean label. Order = priority for picking a row's primary theme.
THEME_MAP = [
    ("mateIn1", "메이트 1수"),
    ("mateIn2", "메이트 2수"),
    ("mateIn3", "메이트 3수"),
    ("backRankMate", "백랭크 메이트"),
    ("smotheredMate", "질식 메이트"),
    ("hangingPiece", "매달린 기물"),
    ("fork", "포크"),
    ("pin", "핀"),
    ("skewer", "스큐어"),
    ("discoveredAttack", "디스커버드 어택"),
    ("doubleCheck", "더블 체크"),
    ("sacrifice", "희생"),
    ("deflection", "디플렉션"),
    ("attraction", "어트랙션"),
    ("trappedPiece", "갇힌 기물"),
    ("promotion", "승격"),
    ("advancedPawn", "전진한 폰"),
    ("crushing", "결정적 우위"),
    ("advantage", "우위 잡기"),
]
LABEL = dict(THEME_MAP)
PRIORITY = [k for k, _ in THEME_MAP]


def primary_theme(themes):
    parts = set(themes.split())
    for key in PRIORITY:
        if key in parts:
            return key
    return None


def main():
    buckets = {}
    seen = read = kept = 0
    reader = csv.reader(sys.stdin)
    header = next(reader, None)  # skip header row
    for row in reader:
        read += 1
        if len(row) < 8:
            continue
        pid, fen, moves, rating, rdev, pop, plays, themes = row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7]
        try:
            rating = int(rating); pop = int(pop); plays = int(plays)
        except ValueError:
            continue
        if not (MIN_RATING <= rating <= MAX_RATING):
            continue
        if pop < MIN_POPULARITY or plays < MIN_PLAYS:
            continue
        theme = primary_theme(themes)
        if theme is None:
            continue
        band = (rating // BAND) * BAND
        key = (theme, band)
        if len(buckets.get(key, ())) >= PER_BUCKET:
            continue
        # Pre-apply the setup move; validate the whole line.
        mv = moves.split()
        if len(mv) < 2:
            continue
        try:
            board = chess.Board(fen)
            board.push_uci(mv[0])            # setup move -> the position the solver faces
            solver_fen = board.fen()
            for u in mv[1:]:                 # ensure the solution line is fully legal
                board.push_uci(u)
        except (ValueError, AssertionError):
            continue
        seen += 1
        buckets.setdefault(key, []).append((pid, solver_fen, " ".join(mv[1:]), rating, theme))

    rows = []
    for key in sorted(buckets):
        rows.extend(buckets[key])
    rows.sort(key=lambda r: r[3])  # by rating, easiest first
    with open(OUT, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id", "fen", "solution", "rating", "theme"])  # theme = Korean label
        for pid, solver_fen, solution, rating, theme in rows:
            w.writerow([pid, solver_fen, solution, rating, LABEL[theme]])
    kept = len(rows)
    size_kb = os.path.getsize(OUT) // 1024
    print(f"read {read} rows, validated {seen}, wrote {kept} puzzles -> {OUT} ({size_kb} KB)")
    # Coverage report
    by_theme = {}
    for _, _, _, _, theme in rows:
        by_theme[theme] = by_theme.get(theme, 0) + 1
    for t, n in sorted(by_theme.items(), key=lambda x: -x[1]):
        print(f"  {LABEL[t]:16} {n}")


if __name__ == "__main__":
    main()
