package com.checkmatey.feature.puzzles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.Square
import com.checkmatey.core.chess.toSan
import com.checkmatey.core.puzzle.Puzzle
import com.checkmatey.core.puzzle.PuzzleRepository
import com.checkmatey.core.puzzle.Rating
import com.checkmatey.core.srs.Grade
import com.checkmatey.core.srs.Srs
import com.checkmatey.data.UserStore
import com.checkmatey.ui.board.ChessBoard
import kotlinx.coroutines.delay

private enum class PuzzleState { SOLVING, SOLVED, FAILED }

/** New puzzles near your rating, or spaced-repetition review of ones you've missed. */
private enum class Mode { NEW, REVIEW }

/**
 * Puzzles tab: a rating-based tactics trainer over the Lichess CC0 set, now backed by **spaced
 * repetition** (core/srs). Solving a new puzzle raises your rating and picks a harder one; missing
 * it files the puzzle into an SM-2 review deck that resurfaces it — 1 day, then 6, then further out
 * — right as you're about to forget the pattern. When cards are due the tab opens straight into
 * Review mode; you can switch to fresh puzzles anytime. Progress persists via [UserStore].
 */
@Composable
fun PuzzlesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { UserStore(context) }
    val repo = remember { PuzzleRepository(context) }
    val haptic = LocalHapticFeedback.current
    val today = remember { System.currentTimeMillis() / 86_400_000L }

    var rating by remember { mutableIntStateOf(store.puzzleRating) }
    var streak by remember { mutableIntStateOf(store.streak) }
    var bestStreak by remember { mutableIntStateOf(store.bestStreak) }
    var solvedCount by remember { mutableIntStateOf(store.solvedCount) }
    var solvedIds by remember { mutableStateOf(emptySet<String>()) }
    var srsCards by remember { mutableStateOf(store.srsCards) }
    var weakest by remember { mutableStateOf(store.weakestTheme()) }
    var focusWeakness by rememberSaveable { mutableStateOf(false) }
    // Themes the last game review flagged (review -> targeted practice).
    val themes = remember { repo.themes() }
    var recommended by remember { mutableStateOf(store.recommendedThemes.firstOrNull { it in themes }) }

    // The review deck: due card ids present in the asset, taken as a snapshot for this session.
    fun freshQueue(): List<String> = Srs.due(srsCards, today).map { it.id }.filter { repo.byId(it) != null }
    var reviewQueue by remember { mutableStateOf(freshQueue()) }
    var reviewPos by remember { mutableIntStateOf(0) }
    var mode by rememberSaveable { mutableStateOf(if (reviewQueue.isNotEmpty()) Mode.REVIEW else Mode.NEW) }
    val dueNow = Srs.dueCount(srsCards, today)

    fun pickNew(): Puzzle = repo.next(
        rating = rating,
        solved = solvedIds,
        themeFilter = when {
            recommended != null -> recommended
            focusWeakness -> weakest
            else -> null
        },
    )

    var puzzle by remember {
        mutableStateOf(reviewQueue.getOrNull(0)?.let { repo.byId(it) } ?: pickNew())
    }
    var selected by remember { mutableStateOf<Square?>(null) }
    var state by remember { mutableStateOf(PuzzleState.SOLVING) }
    // Multi-move solving: [step] is the index of the next expected solver move in solution (even
    // indices are the player's moves; odd indices are the opponent's forced replies).
    var step by remember(puzzle) { mutableIntStateOf(0) }
    var displayPos by remember(puzzle) { mutableStateOf(puzzle.position) }
    var lastMove by remember(puzzle) { mutableStateOf<Move?>(null) }
    var awaitingReply by remember(puzzle) { mutableStateOf(false) }
    var failedSan by remember(puzzle) { mutableStateOf<String?>(null) }

    val solverColor = puzzle.position.sideToMove
    val canMove = state == PuzzleState.SOLVING && !awaitingReply && displayPos.sideToMove == solverColor
    val targets: Set<Square> = if (canMove) {
        selected?.let { from -> displayPos.legalMoves().filter { it.from == from }.map { it.to }.toSet() } ?: emptySet()
    } else {
        emptySet()
    }

    fun serve(p: Puzzle) {
        puzzle = p
        selected = null
        state = PuzzleState.SOLVING
    }

    // The opponent's forced reply plays itself after a short beat, then it's the player's turn again.
    LaunchedEffect(awaitingReply, puzzle) {
        if (awaitingReply) {
            delay(450)
            val reply = displayPos.findMove(puzzle.solution[step])
            if (reply != null) {
                displayPos = displayPos.applyMove(reply)
                lastMove = reply
                step += 1
            }
            awaitingReply = false
        }
    }

    fun finish(solved: Boolean) {
        // Every review counts toward the schedule; a missed *new* puzzle enters the deck too.
        if (mode == Mode.REVIEW || !solved) {
            store.scheduleReview(puzzle.id, if (solved) Grade.GOOD else Grade.AGAIN, today)
            srsCards = store.srsCards
        }
        store.recordTheme(puzzle.theme, solved)
        rating = Rating.update(rating, puzzle.rating, solved)
        store.puzzleRating = rating
        store.pushRating(rating)
        if (solved) {
            solvedCount += 1; store.solvedCount = solvedCount
            store.recordSolvedToday(today) // daily goal + day streak
            streak += 1; store.streak = streak
            if (streak > bestStreak) { bestStreak = streak; store.bestStreak = bestStreak }
            state = PuzzleState.SOLVED
        } else {
            streak = 0; store.streak = 0
            state = PuzzleState.FAILED
        }
        weakest = store.weakestTheme()
    }

    fun loadNext() {
        if (mode == Mode.REVIEW) {
            reviewPos += 1
            val nextId = reviewQueue.getOrNull(reviewPos)
            if (nextId != null) {
                serve(repo.byId(nextId)!!)
            } else {
                mode = Mode.NEW // deck cleared for today — back to fresh puzzles
                serve(pickNew())
            }
        } else {
            solvedIds = solvedIds + puzzle.id
            serve(pickNew())
        }
    }

    fun enterReview() {
        reviewQueue = freshQueue()
        reviewPos = 0
        if (reviewQueue.isEmpty()) return // nothing due — stay put
        mode = Mode.REVIEW
        serve(repo.byId(reviewQueue[0])!!)
    }

    fun enterNew() {
        mode = Mode.NEW
        serve(pickNew())
    }

    fun onSquareClick(square: Square) {
        if (!canMove) return
        val current = selected
        if (current == null) {
            if (displayPos.pieceAt(square)?.color == solverColor) selected = square
            return
        }
        if (square == current) { selected = null; return }
        val move = displayPos.legalMoves().firstOrNull { it.from == current && it.to == square }
        if (move == null) {
            selected = if (displayPos.pieceAt(square)?.color == solverColor) square else null
            return
        }
        selected = null
        val expected = displayPos.findMove(puzzle.solution[step]) ?: return
        val correct = move.from == expected.from && move.to == expected.to
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (correct) {
            displayPos = displayPos.applyMove(expected)
            lastMove = expected
            step += 1
            if (step >= puzzle.solution.size) finish(solved = true) else awaitingReply = true
        } else {
            failedSan = displayPos.toSan(expected)
            displayPos = displayPos.applyMove(expected)
            lastMove = expected
            finish(solved = false)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StatBar(rating = rating, streak = streak, bestStreak = bestStreak, solved = solvedCount)
        Spacer(Modifier.height(8.dp))
        ModeRow(
            mode = mode,
            dueCount = dueNow,
            onNew = { if (mode != Mode.NEW) enterNew() },
            onReview = { if (mode != Mode.REVIEW) enterReview() },
        )
        if (mode == Mode.NEW) {
            recommended?.let { theme ->
                Spacer(Modifier.height(6.dp))
                Surface(
                    onClick = { store.recommendedThemes = emptyList(); recommended = null; serve(pickNew()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        "🎯 복기에서 찾은 약점: \"$theme\" — 지금 이 테마를 훈련 중입니다 (탭하면 일반 퍼즐로)",
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            WeaknessRow(
                focus = focusWeakness,
                onToggle = { focusWeakness = it },
                weakest = weakest,
                weakestRate = weakest?.let { store.successRate(it) },
            )
        }
        Spacer(Modifier.height(10.dp))
        StatusCard(
            state = state,
            reviewMode = mode == Mode.REVIEW,
            theme = puzzle.theme,
            puzzleRating = puzzle.rating,
            sideToMove = solverColor,
            movesLeft = (puzzle.solution.size - step + 1) / 2,
            failedSan = failedSan,
        )
        Spacer(Modifier.height(10.dp))

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            BoxWithConstraints {
                val side = minOf(maxWidth, maxHeight).coerceAtMost(520.dp)
                ChessBoard(
                    position = displayPos,
                    modifier = Modifier.size(side),
                    selected = selected,
                    targets = targets,
                    lastMove = lastMove,
                    onSquareClick = if (canMove) ::onSquareClick else null,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        if (state != PuzzleState.SOLVING) {
            Button(onClick = ::loadNext) {
                Text(if (mode == Mode.REVIEW) "다음 복습 →" else "다음 문제 →")
            }
        } else {
            Text(
                if (step == 0) "기물을 탭해 최선의 수를 두세요" else "좋아요 — 수순을 계속 이어가세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModeRow(mode: Mode, dueCount: Int, onNew: () -> Unit, onReview: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ModeChip(
            label = "새 문제",
            selected = mode == Mode.NEW,
            enabled = true,
            onClick = onNew,
            modifier = Modifier.weight(1f),
        )
        ModeChip(
            label = if (dueCount > 0) "복습 $dueCount" else "복습",
            selected = mode == Mode.REVIEW,
            enabled = dueCount > 0 || mode == Mode.REVIEW,
            onClick = onReview,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) scheme.primary else scheme.surface,
        contentColor = if (selected) scheme.onPrimary else if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Text(
            label,
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun WeaknessRow(
    focus: Boolean,
    onToggle: (Boolean) -> Unit,
    weakest: String?,
    weakestRate: Int?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = { onToggle(!focus) },
            shape = RoundedCornerShape(8.dp),
            color = if (focus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (focus) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            border = if (focus) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text("약점 집중 ${if (focus) "✓" else ""}".trim(), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
        }
        val label = buildString {
            if (weakest != null) append("약점: $weakest ${weakestRate ?: 0}%")
            if (isEmpty()) append("풀수록 약점을 찾아 드립니다")
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatBar(rating: Int, streak: Int, bestStreak: Int, solved: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("내 퍼즐 레이팅  $rating", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "연속 $streak  ·  최고 연속 $bestStreak  ·  푼 문제 $solved",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StatusCard(
    state: PuzzleState,
    reviewMode: Boolean,
    theme: String,
    puzzleRating: Int,
    sideToMove: PieceColor,
    movesLeft: Int,
    failedSan: String?,
) {
    val scheme = MaterialTheme.colorScheme
    val side = if (sideToMove == PieceColor.WHITE) "백" else "흑"
    val moveHint = if (movesLeft > 1) " · ${movesLeft}수 수순" else ""
    val tag = if (reviewMode) "🔁 복습 · " else ""
    val (container, onContainer, text) = when (state) {
        PuzzleState.SOLVING -> Triple(
            scheme.surfaceVariant,
            scheme.onSurfaceVariant,
            "$tag$theme · $puzzleRating$moveHint\n$side 차례 — 최선의 수를 찾으세요",
        )
        PuzzleState.SOLVED -> Triple(scheme.tertiaryContainer, scheme.onTertiaryContainer, "정답입니다! ✓")
        PuzzleState.FAILED -> Triple(scheme.errorContainer, scheme.onErrorContainer, "아쉬워요. 정답은 ${failedSan ?: "?"} 였습니다")
    }
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = container, contentColor = onContainer) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}
