package com.checkmatey.feature.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.MoveSelection
import com.checkmatey.core.chess.TapResult
import com.checkmatey.core.chess.PieceType
import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import com.checkmatey.core.engine.KotlinMinimaxEngine
import com.checkmatey.ui.board.ChessBoard
import com.checkmatey.ui.board.SquareBoardBox
import com.checkmatey.ui.components.ResponsiveBoardLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Endgame practice vs the engine: play from [startFen] until you deliver mate. The engine
 * defends with its best moves (no blunders), so the mating *technique* must be real.
 * Stalemate counts as a miss — that's the classic trap this practice teaches.
 */
@Composable
fun PracticeScreen(drillId: String, title: String, startFen: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val engine = remember { KotlinMinimaxEngine() }
    val haptic = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { com.checkmatey.data.UserStore(context) }
    val humanColor = remember(startFen) { Position.fromFen(startFen).sideToMove }

    var position by remember(startFen) { mutableStateOf(Position.fromFen(startFen)) }
    var selected by remember { mutableStateOf<Square?>(null) }
    var lastMove by remember { mutableStateOf<Move?>(null) }
    var thinking by remember { mutableStateOf(false) }
    var plies by remember { mutableStateOf(0) }

    val isHumanTurn = position.sideToMove == humanColor && !position.isGameOver()
    val targets: Set<Square> = if (isHumanTurn) {
        selected?.let { from -> position.legalMoves().filter { it.from == from }.map { it.to }.toSet() } ?: emptySet()
    } else {
        emptySet()
    }

    fun applyMove(move: Move) {
        position = position.applyMove(move)
        lastMove = move
        plies += 1
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun restart() {
        position = Position.fromFen(startFen)
        selected = null
        lastMove = null
        plies = 0
    }

    // The engine defends whenever it's its turn.
    LaunchedEffect(position) {
        if (position.sideToMove != humanColor && !position.isGameOver()) {
            thinking = true
            val move = withContext(Dispatchers.Default) { engine.bestMove(position, depth = 3) }
            delay(180)
            if (move != null) applyMove(move)
            thinking = false
        }
    }

    fun onSquareClick(square: Square) {
        if (!isHumanTurn) return
        when (val r = MoveSelection.onTap(position, selected, square, humanColor)) {
            is TapResult.Select -> selected = r.square
            TapResult.Clear -> selected = null
            TapResult.Ignore -> {}
            is TapResult.Moves -> {
                selected = null
                val move = r.candidates.firstOrNull { it.promotion == null } ?: r.candidates.first { it.promotion == PieceType.QUEEN }
                applyMove(move)
            }
        }
    }

    val scheme = MaterialTheme.colorScheme
    val won = position.isCheckmate() && position.sideToMove != humanColor
    if (won) LaunchedEffect(drillId) { store.completedDrills = store.completedDrills + drillId }
    val drawn = !won && position.isGameOver() // stalemate, insufficient material, fifty-move
    val (text, container, onContainer) = when {
        won -> Triple("성공! 체크메이트 ✓  ($plies 수)", scheme.tertiaryContainer, scheme.onTertiaryContainer)
        drawn -> Triple("무승부 — 실패! 스테일메이트/기물 상실을 조심하고 다시 시도하세요.", scheme.errorContainer, scheme.onErrorContainer)
        thinking -> Triple("상대가 최선의 수비 중…", scheme.surfaceVariant, scheme.onSurfaceVariant)
        else -> Triple("체크메이트를 완성하세요 — 상대는 최선을 다해 도망칩니다", scheme.primaryContainer, scheme.onPrimaryContainer)
    }

    ResponsiveBoardLayout(
        modifier = modifier,
        top = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← 목록") }
                Text("$plies 수", style = MaterialTheme.typography.labelLarge)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
            SquareBoardBox(m) { side ->
                ChessBoard(
                    position = position,
                    modifier = Modifier.size(side),
                    selected = selected,
                    targets = targets,
                    lastMove = lastMove,
                    onSquareClick = if (isHumanTurn) ::onSquareClick else null,
                )
            }
        },
        bottom = {
            OutlinedButton(onClick = ::restart) { Text("다시 시작") }
        },
    )
}
