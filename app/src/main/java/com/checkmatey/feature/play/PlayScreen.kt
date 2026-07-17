package com.checkmatey.feature.play

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.PieceType
import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import com.checkmatey.core.engine.Annotator
import com.checkmatey.core.engine.BotLevel
import com.checkmatey.core.engine.KotlinMinimaxEngine
import com.checkmatey.core.engine.MoveAnnotation
import com.checkmatey.core.engine.MoveQuality
import com.checkmatey.core.study.StudyGames
import com.checkmatey.feature.review.ReviewScreen
import com.checkmatey.ui.board.ChessBoard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

private val PositionSaver = Saver<Position, String>(save = { it.toFen() }, restore = { Position.fromFen(it) })

/**
 * Play tab: the human (White) vs the on-device engine (Black), with a coach. Tap a piece to move;
 * the "힌트" button shows the best move and why, and (when the coach is on) each of your moves is
 * graded — turning a game into a lesson.
 */
@Composable
fun PlayScreen(modifier: Modifier = Modifier) {
    val engine = remember { KotlinMinimaxEngine() }
    val annotator = remember { Annotator(engine) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val humanColor = PieceColor.WHITE

    var level by rememberSaveable { mutableStateOf(BotLevel.BEGINNER) }
    var position by rememberSaveable(stateSaver = PositionSaver) { mutableStateOf(Position.startingPosition()) }
    var selected by remember { mutableStateOf<Square?>(null) }
    var lastMove by remember { mutableStateOf<Move?>(null) }
    var thinking by remember { mutableStateOf(false) }
    var gameOverSeen by remember { mutableStateOf(false) }
    var coachOn by rememberSaveable { mutableStateOf(true) }
    var hint by remember { mutableStateOf<MoveAnnotation?>(null) }
    var feedback by remember { mutableStateOf<MoveAnnotation?>(null) }
    val moves = remember { mutableStateListOf<Move>() }
    var reviewing by remember { mutableStateOf(false) }

    if (reviewing) {
        val reviewGame = remember(reviewing) { StudyGames.fromMoves(moves.toList()) }
        ReviewScreen(game = reviewGame, mySide = humanColor, onBack = { reviewing = false }, modifier = modifier)
        return
    }

    val isHumanTurn = position.sideToMove == humanColor && !position.isGameOver()
    val isStart = position.toFen() == Position.STARTING_FEN
    val targets: Set<Square> = selected?.let { from ->
        position.legalMoves().filter { it.from == from }.map { it.to }.toSet()
    } ?: emptySet()
    val hintSquares: Set<Square> = hint?.bestMove?.let { setOf(it.from, it.to) } ?: emptySet()

    fun applyMove(move: Move) {
        position = position.applyMove(move)
        moves.add(move)
        lastMove = move
        hint = null
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun newGame() {
        position = Position.startingPosition()
        moves.clear()
        selected = null
        lastMove = null
        hint = null
        feedback = null
        gameOverSeen = false
    }

    LaunchedEffect(position) {
        if (position.sideToMove != humanColor && !position.isGameOver()) {
            thinking = true
            val move = withContext(Dispatchers.Default) { engine.chooseMove(position, level, Random.Default) }
            delay(200)
            if (move != null) applyMove(move)
            thinking = false
        }
    }

    fun onSquareClick(square: Square) {
        if (!isHumanTurn) return
        val current = selected
        if (current == null) {
            if (position.pieceAt(square)?.color == humanColor) selected = square
            return
        }
        if (square == current) {
            selected = null
            return
        }
        val candidates = position.legalMoves().filter { it.from == current && it.to == square }
        val move = candidates.firstOrNull { it.promotion == null } ?: candidates.firstOrNull { it.promotion == PieceType.QUEEN }
        if (move != null) {
            val before = position
            applyMove(move)
            selected = null
            feedback = null
            if (coachOn) scope.launch { feedback = withContext(Dispatchers.Default) { annotator.annotate(before, move) } }
        } else {
            selected = if (position.pieceAt(square)?.color == humanColor) square else null
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RatingSelector(selected = level, onSelect = { level = it })
        Spacer(Modifier.height(10.dp))
        StatusCard(position = position, humanColor = humanColor, thinking = thinking, isStart = isStart)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = { if (isHumanTurn) scope.launch { hint = withContext(Dispatchers.Default) { annotator.hint(position) }; feedback = null } },
                enabled = isHumanTurn,
            ) { Text("💡 힌트") }
            Toggle(label = "코치", on = coachOn, onChange = { coachOn = it; if (!it) feedback = null })
        }
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            BoxWithConstraints {
                val side = minOf(maxWidth, maxHeight).coerceAtMost(520.dp)
                ChessBoard(
                    position = position,
                    modifier = Modifier.size(side),
                    selected = selected,
                    targets = targets,
                    lastMove = lastMove,
                    hintSquares = hintSquares,
                    onSquareClick = ::onSquareClick,
                )
            }
        }

        CoachCard(hint = hint, feedback = feedback.takeIf { coachOn })
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { reviewing = true }, enabled = moves.isNotEmpty()) { Text("📊 분석") }
            OutlinedButton(onClick = ::newGame) { Text("새 게임") }
        }
    }

    if (position.isGameOver() && !gameOverSeen) {
        val (title, detail) = gameOverMessage(position, humanColor)
        AlertDialog(
            onDismissRequest = { gameOverSeen = true },
            title = { Text(title) },
            text = { Text(detail) },
            confirmButton = { TextButton(onClick = ::newGame) { Text("새 게임") } },
            dismissButton = {
                TextButton(onClick = { gameOverSeen = true; reviewing = true }, enabled = moves.isNotEmpty()) {
                    Text("📊 분석하기")
                }
            },
        )
    }
}

/** Shows the active hint, else the coach's grade of your last move. */
@Composable
private fun CoachCard(hint: MoveAnnotation?, feedback: MoveAnnotation?) {
    val scheme = MaterialTheme.colorScheme
    val text: String
    val container: Color
    val onContainer: Color
    when {
        hint != null -> {
            text = "💡 추천: ${hint.bestSan}  —  ${hint.reason}"
            container = scheme.secondaryContainer
            onContainer = scheme.onSecondaryContainer
        }
        feedback != null -> {
            val f = feedback
            val head = "직전 수 ${f.san}: ${f.quality.label} ${f.quality.symbol}"
            text = if (f.quality.ordinal >= MoveQuality.INACCURACY.ordinal) "$head\n${f.reason}" else head
            val bad = f.quality == MoveQuality.MISTAKE || f.quality == MoveQuality.BLUNDER
            container = if (bad) scheme.errorContainer else scheme.tertiaryContainer
            onContainer = if (bad) scheme.onErrorContainer else scheme.onTertiaryContainer
        }
        else -> return
    }
    Spacer(Modifier.height(8.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = onContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        onClick = { onChange(!on) },
        shape = RoundedCornerShape(8.dp),
        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (on) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            "$label ${if (on) "✓" else ""}".trim(),
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun RatingSelector(selected: BotLevel, onSelect: (BotLevel) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (level in BotLevel.entries) {
            val isSel = level == selected
            Surface(
                onClick = { onSelect(level) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = if (isSel) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(level.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text("~${level.approxElo}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(position: Position, humanColor: PieceColor, thinking: Boolean, isStart: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val loss = position.isCheckmate() && position.sideToMove == humanColor
    val win = position.isCheckmate() && position.sideToMove != humanColor
    val draw = position.isStalemate()

    val container = when {
        loss -> scheme.errorContainer
        win -> scheme.tertiaryContainer
        draw || thinking -> scheme.surfaceVariant
        else -> scheme.primaryContainer
    }
    val onContainer = when {
        loss -> scheme.onErrorContainer
        win -> scheme.onTertiaryContainer
        draw || thinking -> scheme.onSurfaceVariant
        else -> scheme.onPrimaryContainer
    }
    val headline = when {
        loss -> "체크메이트 — 패배"
        win -> "체크메이트 — 승리 🎉"
        draw -> "스테일메이트 — 무승부"
        thinking -> "컴퓨터가 생각 중…"
        position.isInCheck() -> "체크! — 당신(백) 차례"
        position.sideToMove == humanColor -> "당신(백) 차례"
        else -> "컴퓨터(흑) 차례"
    }
    val sub = when {
        position.isGameOver() -> "'새 게임'으로 다시 시작하세요"
        isStart -> "새 게임 · 흰 기물을 탭해 첫 수를 두세요"
        else -> "${position.fullmoveNumber}수째"
    }
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = container, contentColor = onContainer) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(sub, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

private fun gameOverMessage(position: Position, humanColor: PieceColor): Pair<String, String> = when {
    position.isCheckmate() && position.sideToMove == humanColor -> "패배" to "체크메이트. 컴퓨터가 이겼습니다. 다시 도전해 보세요."
    position.isCheckmate() -> "승리 🎉" to "체크메이트! 당신이 이겼습니다."
    else -> "무승부" to "스테일메이트 — 둘 수 있는 합법수가 없습니다."
}
