package com.checkmatey.core.lesson

/**
 * One guided step: a position, an instruction, and the accepted move(s) in UCI.
 * [expectMate] steps must end in checkmate (verified by tests, so lesson data can't rot).
 */
data class LessonStep(
    val fen: String,
    val instruction: String,
    val acceptUci: Set<String>,
    val explain: String,
    val expectMate: Boolean = false,
)

data class Lesson(
    val id: String,
    val title: String,
    val subtitle: String,
    val steps: List<LessonStep>,
)

/**
 * The beginner curriculum: piece skills -> castling -> mate patterns -> basic mating technique.
 * Follows the standard fast-improvement path (rules -> patterns -> technique); tactics volume and
 * game review live in the Puzzles / 분석 tabs.
 */
object Lessons {

    val ALL: List<Lesson> = listOf(
        Lesson(
            id = "pawn",
            title = "폰 다루기",
            subtitle = "전진 · 잡기 · 승격",
            steps = listOf(
                LessonStep(
                    fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    instruction = "폰은 앞으로 한 칸(첫 이동은 두 칸) 전진합니다.\ne2 폰을 e4로 두 칸 전진시켜 보세요.",
                    acceptUci = setOf("e2e4"),
                    explain = "잘했어요! 폰은 첫 이동에만 두 칸 갈 수 있습니다.",
                ),
                LessonStep(
                    fen = "4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1",
                    instruction = "폰은 대각선 앞의 기물만 잡을 수 있습니다.\ne4 폰으로 d5 폰을 잡아보세요.",
                    acceptUci = setOf("e4d5"),
                    explain = "정확해요! 폰은 전진은 직진, 잡기는 대각선입니다.",
                ),
                LessonStep(
                    fen = "k7/4P3/8/8/8/8/8/4K3 w - - 0 1",
                    instruction = "폰이 끝줄에 도착하면 퀸으로 승격할 수 있습니다.\ne7 폰을 e8로 전진시켜 보세요.",
                    acceptUci = setOf("e7e8q"),
                    explain = "승격! 폰이 퀸이 되었습니다. 대부분 퀸으로 승격합니다.",
                ),
            ),
        ),
        Lesson(
            id = "knight",
            title = "나이트 다루기",
            subtitle = "L자 점프 · 포크",
            steps = listOf(
                LessonStep(
                    fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    instruction = "나이트는 L자로 움직이고, 유일하게 다른 기물을 뛰어넘습니다.\ng1 나이트를 f3로 점프시켜 보세요.",
                    acceptUci = setOf("g1f3"),
                    explain = "좋아요! 나이트는 시작 국면에서 폰을 넘어 바로 나올 수 있습니다.",
                ),
                LessonStep(
                    fen = "k3q3/8/4N3/8/8/8/8/6K1 w - - 0 1",
                    instruction = "나이트의 최강 무기는 포크(양수겸장)!\n한 수로 킹과 퀸을 동시에 공격해 보세요.",
                    acceptUci = setOf("e6c7"),
                    explain = "완벽한 포크! 킹이 피하면 퀸을 잡습니다.",
                ),
            ),
        ),
        Lesson(
            id = "castle",
            title = "캐슬링",
            subtitle = "킹을 안전하게",
            steps = listOf(
                LessonStep(
                    fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                    instruction = "캐슬링은 킹을 안전하게 하고 룩을 전개하는 특별한 수입니다.\n킹을 g1으로 두 칸 옮겨 킹사이드 캐슬링(O-O)을 해보세요.",
                    acceptUci = setOf("e1g1"),
                    explain = "캐슬링 성공! 킹과 룩이 한 수에 함께 움직였습니다.",
                ),
            ),
        ),
        Lesson(
            id = "backrank",
            title = "백랭크 메이트",
            subtitle = "가장 흔한 메이트 패턴",
            steps = listOf(
                LessonStep(
                    fen = "6k1/5ppp/8/8/8/8/8/R6K w - - 0 1",
                    instruction = "폰 뒤에 갇힌 킹은 뒷줄(백랭크)에서 메이트됩니다.\n룩으로 메이트를 완성해 보세요.",
                    acceptUci = setOf("a1a8"),
                    explain = "체크메이트! 킹이 자기 폰에 막혀 도망갈 곳이 없습니다.",
                    expectMate = true,
                ),
                LessonStep(
                    fen = "6k1/5ppp/8/8/8/8/5PPP/1Q4K1 w - - 0 1",
                    instruction = "퀸도 같은 패턴을 만듭니다.\n퀸으로 백랭크 메이트를 해보세요.",
                    acceptUci = setOf("b1b8"),
                    explain = "체크메이트! 이 패턴을 기억하면 이기고, 조심하면 지지 않습니다.",
                    expectMate = true,
                ),
            ),
        ),
        Lesson(
            id = "pin_skewer",
            title = "핀과 스큐어",
            subtitle = "직선 기물의 필살기",
            steps = listOf(
                LessonStep(
                    fen = "4k3/8/2n5/8/8/8/8/4KB2 w - - 0 1",
                    instruction = "핀: 기물을 킹과 같은 선에 묶으면 움직일 수 없습니다.\n비숍을 b5로 옮겨 나이트를 핀해 보세요.",
                    acceptUci = setOf("f1b5"),
                    explain = "핀! 나이트가 움직이면 킹이 노출되므로 꼼짝 못 합니다.",
                ),
                LessonStep(
                    fen = "3q4/8/8/3k4/8/8/8/K6R w - - 0 1",
                    instruction = "스큐어: 킹을 먼저 공격해 비키게 한 뒤, 뒤의 기물을 잡습니다.\n룩을 d1으로 옮겨 킹을 체크해 보세요.",
                    acceptUci = setOf("h1d1"),
                    explain = "스큐어! 킹이 피하면 뒤에 있던 퀸을 잡습니다.",
                ),
            ),
        ),
        Lesson(
            id = "kr_mate",
            title = "킹+룩 메이트",
            subtitle = "상자 좁히기 기술",
            steps = listOf(
                LessonStep(
                    fen = "4k3/8/4K3/8/8/8/8/R7 w - - 0 1",
                    instruction = "킹+룩도 반드시 이깁니다. 내 킹을 마주 세우고 룩으로 뒷줄을 치면 끝!\n룩을 a8로 옮겨 메이트해 보세요.",
                    acceptUci = setOf("a1a8"),
                    explain = "체크메이트! 두 킹이 마주 본 순간 룩이 뒷줄을 지배합니다.",
                    expectMate = true,
                ),
                LessonStep(
                    fen = "7k/8/6K1/8/8/8/8/1R6 w - - 0 1",
                    instruction = "구석에서도 같은 기술입니다.\n룩으로 뒷줄 메이트를 완성하세요.",
                    acceptUci = setOf("b1b8"),
                    explain = "완벽! 킹으로 도주로를 막고 룩으로 마무리 — 이것이 상자 기술입니다.",
                    expectMate = true,
                ),
            ),
        ),
        Lesson(
            id = "opening",
            title = "오프닝 3원칙",
            subtitle = "중앙 · 전개 · 킹 안전",
            steps = listOf(
                LessonStep(
                    fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    instruction = "원칙 1 — 중앙 장악.\n중앙 폰(e 또는 d)을 두 칸 전진시켜 보세요.",
                    acceptUci = setOf("e2e4", "d2d4"),
                    explain = "좋아요! 중앙을 차지하면 기물들이 움직일 공간이 생깁니다.",
                ),
                LessonStep(
                    fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
                    instruction = "원칙 2 — 기물 전개.\n나이트를 먼저 전개하세요 (f3 또는 c3).",
                    acceptUci = setOf("g1f3", "b1c3"),
                    explain = "정석! 나이트 먼저, 비숍 다음 — 같은 기물을 두 번 움직이지 마세요.",
                ),
                LessonStep(
                    fen = "r1bqk1nr/pppp1ppp/2n5/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                    instruction = "원칙 3 — 킹 안전.\n전개가 끝나면 캐슬링으로 오프닝을 마무리하세요 (O-O).",
                    acceptUci = setOf("e1g1"),
                    explain = "완벽한 오프닝! 중앙 장악 → 전개 → 캐슬링. 이 순서만 지켜도 대등하게 시작합니다.",
                ),
            ),
        ),
        Lesson(
            id = "kq_mate",
            title = "킹+퀸 메이트",
            subtitle = "반드시 이기는 기술",
            steps = listOf(
                LessonStep(
                    fen = "7k/8/5K2/8/8/8/8/6Q1 w - - 0 1",
                    instruction = "킹+퀸 대 킹은 반드시 이깁니다. 핵심: 내 킹으로 지원하며 퀸으로 마무리.\n퀸을 g7로 옮겨 메이트해 보세요.",
                    acceptUci = setOf("g1g7"),
                    explain = "체크메이트! 퀸이 킹의 보호를 받아 잡히지 않습니다.",
                    expectMate = true,
                ),
                LessonStep(
                    fen = "k7/8/1K6/8/8/8/6Q1/8 w - - 0 1",
                    instruction = "구석에 몰린 킹은 뒷줄 전체를 노리면 됩니다.\n퀸을 g8로 옮겨 메이트해 보세요.",
                    acceptUci = setOf("g2g8"),
                    explain = "완벽합니다! 내 킹이 도주로를 막고 퀸이 뒷줄을 지배했습니다.",
                    expectMate = true,
                ),
            ),
        ),
    )
}
