package com.checkmatey.feature.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
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
 * Play tab: the human plays White against the on-device [KotlinMinimaxEngine] (Black).
 * Tap a piece to see its legal moves, tap a highlighted square to move. The bot replies
 * on a background thread. Promotions auto-queen for now.
 */
@Composable
fun PlayScreen(modifier: Modifier = Modifier) {
    val engine = remember { KotlinMinimaxEngine() }
    val humanColor = PieceColor.WHITE

    var level by rememberSaveable { mutableStateOf(BotLevel.BEGINNER) }
    var position by rememberSaveable(stateSaver = PositionSaver) { mutableStateOf(Position.startingPosition()) }
    var selected by remember { mutableStateOf<Square?>(null) }
    var thinking by remember { mutableStateOf(false) }

    val isHumanTurn = position.sideToMove == humanColor && !position.isGameOver()
    val targets: Set<Square> = selected?.let { from ->
        position.legalMoves().filter { it.from == from }.map { it.to }.toSet()
    } ?: emptySet()

    // The bot moves whenever it is its turn.
    LaunchedEffect(position) {
        if (position.sideToMove != humanColor && !position.isGameOver()) {
            thinking = true
            val move = withContext(Dispatchers.Default) {
                engine.chooseMove(position, level, Random.Default)
            }
            delay(200) // brief pause so the move doesn't feel instant
            if (move != null) position = position.applyMove(move)
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
            position = position.applyMove(move)
            selected = null
        } else {
            // Tapped elsewhere: re-select own piece or clear.
            selected = if (position.pieceAt(square)?.color == humanColor) square else null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = statusText(position, humanColor, thinking), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        LevelSelector(selected = level, onSelect = { level = it })
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // Fit a square board within the available space, capped for large screens.
            BoxWithConstraints {
                val side = minOf(maxWidth, maxHeight).coerceAtMost(560.dp)
                ChessBoard(
                    position = position,
                    modifier = Modifier.size(side),
                    selected = selected,
                    targets = targets,
                    onSquareClick = ::onSquareClick,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {
            position = Position.startingPosition()
            selected = null
        }) {
            Text("새 게임")
        }
    }
}

private fun statusText(position: Position, humanColor: PieceColor, thinking: Boolean): String = when {
    position.isCheckmate() ->
        if (position.sideToMove == humanColor) "체크메이트 — 컴퓨터 승" else "체크메이트 — 당신 승! 🎉"
    position.isStalemate() -> "스테일메이트 — 무승부"
    thinking -> "컴퓨터가 생각 중…"
    position.sideToMove == humanColor && position.isInCheck() -> "체크! 당신(백) 차례"
    position.sideToMove == humanColor -> "당신(백) 차례"
    else -> "컴퓨터(흑) 차례"
}

@Composable
private fun LevelSelector(selected: BotLevel, onSelect: (BotLevel) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (level in BotLevel.entries) {
            val padding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            if (level == selected) {
                Button(onClick = { onSelect(level) }, contentPadding = padding) {
                    Text(level.displayName)
                }
            } else {
                OutlinedButton(onClick = { onSelect(level) }, contentPadding = padding) {
                    Text(level.displayName)
                }
            }
        }
    }
}
