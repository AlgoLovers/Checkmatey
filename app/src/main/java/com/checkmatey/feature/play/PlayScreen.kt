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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.checkmatey.core.engine.BotLevel
import com.checkmatey.core.engine.KotlinMinimaxEngine
import com.checkmatey.ui.board.ChessBoard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

// Survive rotation by saving the game as a FEN string.
private val PositionSaver = Saver<Position, String>(
    save = { it.toFen() },
    restore = { Position.fromFen(it) },
)

/**
 * Play tab: the human (White) vs the on-device engine (Black). Everything is framed by
 * rating — the opponent is chosen by target Elo, and the status makes it clear whose turn
 * it is, whether the game just started, and when (and how) it ends.
 */
@Composable
fun PlayScreen(modifier: Modifier = Modifier) {
    val engine = remember { KotlinMinimaxEngine() }
    val haptic = LocalHapticFeedback.current
    val humanColor = PieceColor.WHITE

    var level by rememberSaveable { mutableStateOf(BotLevel.BEGINNER) }
    var position by rememberSaveable(stateSaver = PositionSaver) { mutableStateOf(Position.startingPosition()) }
    var selected by remember { mutableStateOf<Square?>(null) }
    var lastMove by remember { mutableStateOf<Move?>(null) }
    var thinking by remember { mutableStateOf(false) }
    var gameOverSeen by remember { mutableStateOf(false) }

    val isHumanTurn = position.sideToMove == humanColor && !position.isGameOver()
    val isStart = position.toFen() == Position.STARTING_FEN
    val targets: Set<Square> = selected?.let { from ->
        position.legalMoves().filter { it.from == from }.map { it.to }.toSet()
    } ?: emptySet()

    fun applyMove(move: Move) {
        position = position.applyMove(move)
        lastMove = move
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun newGame() {
        position = Position.startingPosition()
        selected = null
        lastMove = null
        gameOverSeen = false
    }

    // The bot moves whenever it is its turn.
    LaunchedEffect(position) {
        if (position.sideToMove != humanColor && !position.isGameOver()) {
            thinking = true
            val move = withContext(Dispatchers.Default) {
                engine.chooseMove(position, level, Random.Default)
            }
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
        val move = candidates.firstOrNull { it.promotion == null }
            ?: candidates.firstOrNull { it.promotion == PieceType.QUEEN }
        if (move != null) {
            applyMove(move)
            selected = null
        } else {
            selected = if (position.pieceAt(square)?.color == humanColor) square else null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "상대: 컴퓨터 (흑)  ·  나: 백",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        RatingSelector(selected = level, onSelect = { level = it })
        Spacer(Modifier.height(12.dp))
        StatusCard(position = position, humanColor = humanColor, thinking = thinking, isStart = isStart)
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints {
                val side = minOf(maxWidth, maxHeight).coerceAtMost(560.dp)
                ChessBoard(
                    position = position,
                    modifier = Modifier.size(side),
                    selected = selected,
                    targets = targets,
                    lastMove = lastMove,
                    onSquareClick = ::onSquareClick,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = ::newGame) { Text("새 게임") }
    }

    // Unmissable game-over dialog.
    if (position.isGameOver() && !gameOverSeen) {
        val (title, detail) = gameOverMessage(position, humanColor)
        AlertDialog(
            onDismissRequest = { gameOverSeen = true },
            title = { Text(title) },
            text = { Text(detail) },
            confirmButton = { TextButton(onClick = ::newGame) { Text("새 게임") } },
            dismissButton = { TextButton(onClick = { gameOverSeen = true }) { Text("보드 보기") } },
        )
    }
}

/** Level buttons that show both the tier name and its target rating. */
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
                Column(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(level.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text("~${level.approxElo}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/** Prominent, color-coded status: whose turn, check, start hint, or the game result. */
@Composable
private fun StatusCard(position: Position, humanColor: PieceColor, thinking: Boolean, isStart: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val loss = position.isCheckmate() && position.sideToMove == humanColor
    val win = position.isCheckmate() && position.sideToMove != humanColor
    val draw = position.isStalemate()

    val container: Color = when {
        loss -> scheme.errorContainer
        win -> scheme.tertiaryContainer
        draw || thinking -> scheme.surfaceVariant
        else -> scheme.primaryContainer
    }
    val onContainer: Color = when {
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = onContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(sub, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

private fun gameOverMessage(position: Position, humanColor: PieceColor): Pair<String, String> = when {
    position.isCheckmate() && position.sideToMove == humanColor ->
        "패배" to "체크메이트. 컴퓨터가 이겼습니다. 다시 도전해 보세요."
    position.isCheckmate() ->
        "승리 🎉" to "체크메이트! 당신이 이겼습니다."
    else ->
        "무승부" to "스테일메이트 — 둘 수 있는 합법수가 없습니다."
}
