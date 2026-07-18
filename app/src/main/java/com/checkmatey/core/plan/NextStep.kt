package com.checkmatey.core.plan

/** Where a recommended next step sends the user. */
enum class StepTarget { LESSON, PUZZLE, PLAY, REVIEW }

data class NextStep(val title: String, val subtitle: String, val cta: String, val target: StepTarget)

/** What the planner needs to know about the user's progress. */
data class Progress(
    val lessonsDone: Int,
    val lessonsTotal: Int,
    val puzzlesSolved: Int,
    val gamesPlayed: Int,
    val hasWeakThemeToDrill: Boolean,
    val unreviewedGame: Boolean,
    val dueReviews: Int = 0,
)

/**
 * Picks the single most useful next action, so the home screen can say "do this now" instead of
 * dropping the user in front of five tabs. The order encodes a beginner-friendly learning path:
 * clear time-sensitive spaced reviews first, then learn the rules, reinforce with tactics, play,
 * review games, and drill weaknesses.
 */
object Planner {
    fun next(p: Progress): NextStep = when {
        p.dueReviews > 0 -> NextStep(
            "복습할 시간 (${p.dueReviews})",
            "예전에 틀린 전술을 다시 풀어 기억에 새깁니다 — 간격 반복이 실력을 굳혀요",
            "복습 시작",
            StepTarget.PUZZLE,
        )
        p.lessonsDone < p.lessonsTotal -> NextStep(
            "레슨 이어서 배우기",
            "기초부터 순서대로 — ${p.lessonsDone}/${p.lessonsTotal} 완료",
            "레슨 계속하기",
            StepTarget.LESSON,
        )
        p.unreviewedGame -> NextStep(
            "방금 둔 게임 복기하기",
            "어디서 실수했는지 코치가 짚어줍니다 — 실력이 가장 빨리 느는 단계",
            "복기하기",
            StepTarget.REVIEW,
        )
        p.hasWeakThemeToDrill -> NextStep(
            "내 약점 집중 훈련",
            "복기에서 찾은 약한 전술을 퍼즐로 보강하세요",
            "약점 퍼즐 풀기",
            StepTarget.PUZZLE,
        )
        p.puzzlesSolved < 3 -> NextStep(
            "전술 퍼즐 풀기",
            "매일 몇 문제가 실력 향상의 지름길입니다",
            "퍼즐 풀기",
            StepTarget.PUZZLE,
        )
        p.gamesPlayed < 1 -> NextStep(
            "컴퓨터와 첫 대국",
            "배운 걸 실전에서 — 코치가 옆에서 도와줍니다",
            "대국 시작",
            StepTarget.PLAY,
        )
        else -> NextStep(
            "오늘의 훈련",
            "퍼즐로 몸을 풀고 한 판 두고 복기하기 — 이 루틴이 실력을 만듭니다",
            "퍼즐 풀기",
            StepTarget.PUZZLE,
        )
    }
}
