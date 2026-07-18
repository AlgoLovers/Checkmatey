package com.checkmatey.feature.diagnostic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.checkmatey.core.chess.Square
import com.checkmatey.core.chess.toSan
import com.checkmatey.core.diagnostic.Placement
import com.checkmatey.core.puzzle.Puzzle
import com.checkmatey.core.puzzle.PuzzleRepository
import com.checkmatey.data.UserStore
import com.checkmatey.ui.board.ChessBoard
import com.checkmatey.ui.components.GradientPrimaryButton

private enum class Phase { SOLVING, ANSWERED, RESULT }

/**
 * Diagnostic placement quiz — a short adaptive test that finds the player's starting rating so the
 * app can begin at the right level (the diagnostic pillar of the adaptive-learning moat). Each of
 * [Placement.LENGTH] questions is one tactic served near the running estimate; the player just finds
 * the best first move, and an Elo update (core/diagnostic) converges the estimate. Finishing sets
 * the puzzle rating; the whole quiz can be skipped for absolute beginners.
 */
@Composable
fun PlacementScreen(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { UserStore(context) }
    val repo = remember { PuzzleRepository(context) }
    val haptic = LocalHapticFeedback.current

    var estimate by remember { mutableIntStateOf(Placement.START) }
    var index by remember { mutableIntStateOf(0) } // 0-based question number
    var asked by remember { mutableStateOf(emptySet<String>()) }
    var phase by remember { mutableStateOf(Phase.SOLVING) }
    var lastCorrect by remember { mutableStateOf(false) }
    var answerSan by remember { mutableStateOf<String?>(null) }

    var puzzle by remember { mutableStateOf(repo.next(Placement.target(estimate), emptySet())) }
    var displayPos by remember(puzzle) { mutableStateOf(puzzle.position) }
    var selected by remember(puzzle) { mutableStateOf<Square?>(null) }

    val solverColor = puzzle.position.sideToMove
    val canMove = phase == Phase.SOLVING
    val targets: Set<Square> = if (canMove) {
        selected?.let { from -> displayPos.legalMoves().filter { it.from == from }.map { it.to }.toSet() } ?: emptySet()
    } else {
        emptySet()
    }

    fun grade(correct: Boolean) {
        lastCorrect = correct
        estimate = Placement.update(estimate, puzzle.rating, correct, index)
        phase = Phase.ANSWERED
    }

    fun next() {
        if (index + 1 >= Placement.LENGTH) {
            phase = Phase.RESULT
            return
        }
        index += 1
        asked = asked + puzzle.id
        puzzle = repo.next(Placement.target(estimate), asked)
        phase = Phase.SOLVING
    }

    fun finish(apply: Boolean) {
        if (apply) {
            store.puzzleRating = estimate
            store.pushRating(estimate)
        }
        store.placementDone = true
        onDone()
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
        val expected = displayPos.findMove(puzzle.solution[0]) ?: return
        val correct = move.from == expected.from && move.to == expected.to
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        answerSan = displayPos.toSan(expected)
        displayPos = displayPos.applyMove(expected) // show the key move either way
        grade(correct)
    }

    Column(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (phase == Phase.RESULT) {
            ResultView(estimate = estimate, onStart = { finish(apply = true) })
            return@Column
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("실력 진단", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("문항 ${index + 1} / ${Placement.LENGTH}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { finish(apply = false) }) { Text("건너뛰기") }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (index + if (phase == Phase.ANSWERED) 1 else 0).toFloat() / Placement.LENGTH },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        FeedbackCard(phase = phase, correct = lastCorrect, answerSan = answerSan, sideToMove = solverColor)
        Spacer(Modifier.height(12.dp))

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            BoxWithConstraints {
                val side = minOf(maxWidth, maxHeight).coerceAtMost(520.dp)
                ChessBoard(
                    position = displayPos,
                    modifier = Modifier.size(side),
                    selected = selected,
                    targets = targets,
                    onSquareClick = if (canMove) ::onSquareClick else null,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        if (phase == Phase.ANSWERED) {
            GradientPrimaryButton(onClick = ::next, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (index + 1 >= Placement.LENGTH) "결과 보기" else "다음 문항 →",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            Text(
                "가장 좋은 첫 수를 찾아 기물을 옮겨 보세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FeedbackCard(
    phase: Phase,
    correct: Boolean,
    answerSan: String?,
    sideToMove: com.checkmatey.core.chess.PieceColor,
) {
    val scheme = MaterialTheme.colorScheme
    val side = if (sideToMove == com.checkmatey.core.chess.PieceColor.WHITE) "백" else "흑"
    val (container, onContainer, text) = when {
        phase == Phase.SOLVING -> Triple(scheme.surfaceVariant, scheme.onSurfaceVariant, "$side 차례 — 최선의 수는?")
        correct -> Triple(scheme.tertiaryContainer, scheme.onTertiaryContainer, "정확해요! ✓")
        else -> Triple(scheme.errorContainer, scheme.onErrorContainer, "최선의 수는 ${answerSan ?: "?"} 였어요")
    }
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = container, contentColor = onContainer) {
        Text(
            text,
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ColumnScope.ResultView(estimate: Int, onStart: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Text("진단 완료 🎉", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(20.dp))
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("나의 시작 레이팅", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Text("$estimate", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "이제 퍼즐과 대국이 이 레이팅에 맞춰 나옵니다. 풀수록 자동으로 조정돼요.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    GradientPrimaryButton(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
        Text("이 레이팅으로 시작", style = MaterialTheme.typography.labelLarge)
    }
    Spacer(Modifier.weight(1f))
}
