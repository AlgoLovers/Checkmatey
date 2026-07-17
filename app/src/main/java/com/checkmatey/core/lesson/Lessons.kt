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
