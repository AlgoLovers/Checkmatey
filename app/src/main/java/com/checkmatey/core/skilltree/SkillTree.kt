package com.checkmatey.core.skilltree

/** Whether a skill is reachable yet, and whether it's been demonstrated. */
enum class NodeStatus { LOCKED, AVAILABLE, DONE }

/** The measurable progress a skill node is judged against — all sourced from UserStore. */
data class SkillProgress(
    val completedLessons: Set<String>,
    val puzzlesSolved: Int,
    val gamesPlayed: Int,
    val completedDrills: Set<String>,
)

/**
 * One node on the learning map: a milestone, its prerequisites (by id), and the concrete condition
 * that marks it done. Kept as data + a predicate so the whole tree is pure, testable Kotlin.
 */
data class SkillNode(
    val id: String,
    val title: String,
    val subtitle: String,
    val prereqs: List<String>,
    val done: (SkillProgress) -> Boolean,
)

/**
 * The beginner learning map — the visible "where am I on the path" that turns a pile of tabs into a
 * journey. It mirrors the curriculum (rules → special rules → tactics/mates/openings → endgame →
 * real games) and grades each node from real progress, so the map can never drift from what the
 * user has actually done. Nodes are listed in dependency order so [statuses] resolves in one pass.
 */
object SkillTree {

    val ALL: List<SkillNode> = listOf(
        SkillNode(
            "basics", "기물과 규칙", "폰·나이트·비숍·룩·퀸 다루기", emptyList(),
        ) { it.completedLessons.containsAll(listOf("coords", "pawn", "knight", "brq")) },
        SkillNode(
            "special", "특수 규칙", "체크 대응·캐슬링·앙파상", listOf("basics"),
        ) { it.completedLessons.containsAll(listOf("check_escape", "castle", "enpassant")) },
        SkillNode(
            "tactics", "기초 전술", "핀·스큐어 + 퍼즐 20문제", listOf("basics"),
        ) { it.completedLessons.contains("pin_skewer") && it.puzzlesSolved >= 20 },
        SkillNode(
            "mates", "메이트 패턴", "백랭크·킹+룩·킹+퀸 메이트", listOf("special"),
        ) { it.completedLessons.containsAll(listOf("backrank", "kr_mate", "kq_mate")) },
        SkillNode(
            "openings", "오프닝 원칙", "중앙·전개·킹 안전", listOf("special"),
        ) { it.completedLessons.contains("opening") },
        SkillNode(
            "endgame", "엔드게임 마무리", "K+Q·K+R 직접 메이트 완성", listOf("mates"),
        ) { it.completedDrills.isNotEmpty() },
        SkillNode(
            "play", "실전과 복기", "컴퓨터와 3판 이상 두기", listOf("tactics", "openings"),
        ) { it.gamesPlayed >= 3 },
    )

    private val byId = ALL.associateBy { it.id }

    /**
     * Status of every node for the given [progress]. A node is DONE when its condition holds; else
     * AVAILABLE if every prerequisite is DONE; else LOCKED. (DONE ignores prerequisites — a skill
     * you've demonstrated counts even if you skipped the lead-up.)
     */
    fun statuses(progress: SkillProgress): Map<String, NodeStatus> {
        val out = LinkedHashMap<String, NodeStatus>()
        for (node in ALL) {
            out[node.id] = when {
                node.done(progress) -> NodeStatus.DONE
                node.prereqs.all { out[it] == NodeStatus.DONE } -> NodeStatus.AVAILABLE
                else -> NodeStatus.LOCKED
            }
        }
        return out
    }

    fun byId(id: String): SkillNode? = byId[id]

    /** Overall completion, 0–100, for the header progress bar. */
    fun progressPercent(progress: SkillProgress): Int {
        val done = statuses(progress).values.count { it == NodeStatus.DONE }
        return done * 100 / ALL.size
    }
}
