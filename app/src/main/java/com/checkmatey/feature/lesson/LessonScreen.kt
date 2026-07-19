package com.checkmatey.feature.lesson

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import com.checkmatey.core.lesson.Lesson
import com.checkmatey.core.lesson.Lessons
import com.checkmatey.data.UserStore
import com.checkmatey.ui.board.ChessBoard
import com.checkmatey.ui.components.ResponsiveBoardLayout

/** Endgame drills played against the engine (it defends for real). */
private data class PracticeDrill(val id: String, val title: String, val subtitle: String, val fen: String)

private val PRACTICE_DRILLS = listOf(
    PracticeDrill("kq", "킹+퀸 메이트 실습", "엔진을 상대로 직접 몰아넣기", "8/8/8/4k3/8/8/8/2QK4 w - - 0 1"),
    PracticeDrill("kr", "킹+룩 메이트 실습", "상자 기술을 실전으로", "8/8/8/4k3/8/8/8/2RK4 w - - 0 1"),
)

/** Lessons tab: a beginner curriculum of guided, hands-on lessons plus engine practice. */
@Composable
fun LessonScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { UserStore(context) }
    var completed by remember { mutableStateOf(store.completedLessons) }
    val completedDrills = store.completedDrills
    var openIndex by rememberSaveable { mutableIntStateOf(-1) }
    var practiceIndex by rememberSaveable { mutableIntStateOf(-1) }

    if (practiceIndex in PRACTICE_DRILLS.indices) {
        val drill = PRACTICE_DRILLS[practiceIndex]
        PracticeScreen(drillId = drill.id, title = drill.title, startFen = drill.fen, onBack = { practiceIndex = -1 }, modifier = modifier)
        return
    }
    if (openIndex in Lessons.ALL.indices) {
        LessonDetail(
            lesson = Lessons.ALL[openIndex],
            onDone = { id ->
                completed = (completed + id).also { store.completedLessons = it }
                openIndex = -1
            },
            onBack = { openIndex = -1 },
            modifier = modifier,
        )
    } else {
        LessonList(
            completed = completed,
            completedDrills = completedDrills,
            onOpen = { openIndex = it },
            onOpenPractice = { practiceIndex = it },
            modifier = modifier,
        )
    }
}

@Composable
private fun LessonList(
    completed: Set<String>,
    completedDrills: Set<String>,
    onOpen: (Int) -> Unit,
    onOpenPractice: (Int) -> Unit,
    modifier: Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("레슨", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "기초부터 순서대로 — 직접 두면서 배웁니다. (${completed.size}/${Lessons.ALL.size} 완료)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(Lessons.ALL.size) { index ->
                val lesson = Lessons.ALL[index]
                val done = lesson.id in completed
                Surface(
                    onClick = { onOpen(index) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (done) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${index + 1}. ${lesson.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(lesson.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(if (done) "✓ 완료" else "${lesson.steps.size}단계", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            item {
                Spacer(Modifier.height(6.dp))
                Text("엔드게임 실습 — 엔진 상대", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            items(PRACTICE_DRILLS.size) { index ->
                val drill = PRACTICE_DRILLS[index]
                val done = drill.id in completedDrills
                Surface(
                    onClick = { onOpenPractice(index) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (done) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(drill.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(drill.subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                        if (done) Text("✓ 성공", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

private enum class StepPhase { TRYING, CORRECT, FINISHED }

@Composable
private fun LessonDetail(lesson: Lesson, onDone: (String) -> Unit, onBack: () -> Unit, modifier: Modifier) {
    val haptic = LocalHapticFeedback.current
    var stepIndex by rememberSaveable(lesson.id) { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(StepPhase.TRYING) }
    var selected by remember { mutableStateOf<Square?>(null) }
    var wrongMsg by remember { mutableStateOf<String?>(null) }
    var lastMove by remember { mutableStateOf<Move?>(null) }

    val step = lesson.steps[stepIndex.coerceIn(lesson.steps.indices)]
    val basePos = remember(step) { Position.fromFen(step.fen) }
    var displayPos by remember(step) { mutableStateOf(basePos) }

    val targets: Set<Square> = if (phase == StepPhase.TRYING) {
        selected?.let { from -> basePos.legalMoves().filter { it.from == from }.map { it.to }.toSet() } ?: emptySet()
    } else {
        emptySet()
    }

    fun onSquareClick(square: Square) {
        if (phase != StepPhase.TRYING) return
        val current = selected
        if (current == null) {
            if (basePos.pieceAt(square)?.color == basePos.sideToMove) selected = square
            return
        }
        if (square == current) {
            selected = null
            return
        }
        val candidates = basePos.legalMoves().filter { it.from == current && it.to == square }
        val move = candidates.firstOrNull { it.uci() in step.acceptUci }
            ?: candidates.firstOrNull()
        if (move == null) {
            selected = if (basePos.pieceAt(square)?.color == basePos.sideToMove) square else null
            return
        }
        selected = null
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (move.uci() in step.acceptUci) {
            displayPos = basePos.applyMove(move)
            lastMove = move
            wrongMsg = null
            phase = if (stepIndex == lesson.steps.lastIndex) StepPhase.FINISHED else StepPhase.CORRECT
        } else {
            wrongMsg = "그 수가 아니에요 — 지시를 다시 읽고 시도해 보세요!"
        }
    }

    val (text, container, onContainer) = when {
        phase != StepPhase.TRYING -> Triple(step.explain, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        wrongMsg != null -> Triple(wrongMsg!!, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        else -> Triple(step.instruction, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    }

    ResponsiveBoardLayout(
        modifier = modifier,
        top = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← 목록") }
                Text("${stepIndex + 1} / ${lesson.steps.size}", style = MaterialTheme.typography.labelLarge)
            }
            Text(lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = container, contentColor = onContainer) {
                Text(
                    text,
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        },
        board = { m ->
            BoxWithConstraints(m, contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight).coerceAtMost(900.dp)
                ChessBoard(
                    position = displayPos,
                    modifier = Modifier.size(side),
                    selected = selected,
                    targets = targets,
                    lastMove = lastMove,
                    onSquareClick = if (phase == StepPhase.TRYING) ::onSquareClick else null,
                )
            }
        },
        bottom = {
            when (phase) {
                StepPhase.CORRECT -> Button(onClick = {
                    stepIndex += 1
                    phase = StepPhase.TRYING
                    lastMove = null
                    wrongMsg = null
                }) { Text("다음 단계 →") }
                StepPhase.FINISHED -> Button(onClick = { onDone(lesson.id) }) { Text("레슨 완료! ✓") }
                StepPhase.TRYING -> Text(
                    "기물을 탭해서 지시대로 움직여 보세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
