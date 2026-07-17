package com.checkmatey.feature.study

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import com.checkmatey.core.study.StudyGame
import com.checkmatey.core.study.StudyGames
import com.checkmatey.ui.board.ChessBoard

/** Learn tab: pick a master game, then replay it or test yourself with "guess the move". */
@Composable
fun StudyScreen(modifier: Modifier = Modifier) {
    val games = remember { StudyGames.all() }
    var selected by rememberSaveable { mutableIntStateOf(-1) }

    if (selected in games.indices) {
        GameDetail(game = games[selected], onBack = { selected = -1 }, modifier = modifier)
    } else {
        GameList(games = games, onSelect = { selected = it }, modifier = modifier)
    }
}

@Composable
private fun GameList(games: List<StudyGame>, onSelect: (Int) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("명국 공부", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "고수의 명국을 따라 두고, 다음 수를 직접 맞혀보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(games) { index, game ->
                Surface(
                    onClick = { onSelect(index) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(game.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(game.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${game.plyCount}수 · 체크메이트로 종료", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun GameDetail(game: StudyGame, onBack: () -> Unit, modifier: Modifier) {
    val haptic = LocalHapticFeedback.current
    var ply by rememberSaveable(game.title) { mutableIntStateOf(0) }
    var guessMode by rememberSaveable(game.title) { mutableStateOf(false) }
    var correct by rememberSaveable(game.title) { mutableIntStateOf(0) }
    var attempts by rememberSaveable(game.title) { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Square?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }

    val position = game.positionAt(ply)
    val lastMove = if (ply > 0) game.moveAt(ply - 1) else null
    val actualNext = game.moveAt(ply)
    val targets: Set<Square> = if (guessMode) {
        selected?.let { from -> position.legalMoves().filter { it.from == from }.map { it.to }.toSet() } ?: emptySet()
    } else {
        emptySet()
    }

    fun goTo(target: Int) {
        ply = target.coerceIn(0, game.plyCount)
        selected = null
        feedback = null
    }

    fun onGuess(square: Square) {
        val next = actualNext ?: return
        val current = selected
        if (current == null) {
            if (position.pieceAt(square)?.color == position.sideToMove) selected = square
            return
        }
        if (square == current) {
            selected = null
            return
        }
        val legal = position.legalMoves().firstOrNull { it.from == current && it.to == square }
        if (legal == null) {
            selected = if (position.pieceAt(square)?.color == position.sideToMove) square else null
            return
        }
        selected = null
        attempts++
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val guessedRight = legal.from == next.from && legal.to == next.to
        val playedSan = game.sans.getOrNull(ply) ?: ""
        feedback = if (guessedRight) {
            correct++
            "정답! ✓  ($playedSan)"
        } else {
            "아쉬워요 — 실제 수는 $playedSan"
        }
        ply += 1 // reveal the real move (board animates it)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("← 목록") }
            ModeToggle(guessMode = guessMode, onChange = { guessMode = it; selected = null; feedback = null })
        }
        Text(game.title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        StatusCard(game = game, ply = ply, guessMode = guessMode, position = position, feedback = feedback)
        Spacer(Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            BoxWithConstraints {
                val side = minOf(maxWidth, maxHeight).coerceAtMost(480.dp)
                ChessBoard(
                    position = position,
                    modifier = Modifier.size(side),
                    selected = if (guessMode) selected else null,
                    targets = targets,
                    lastMove = lastMove,
                    onSquareClick = if (guessMode && actualNext != null) ::onGuess else null,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepButton("⏮", enabled = ply > 0) { goTo(0) }
            StepButton("◀", enabled = ply > 0) { goTo(ply - 1) }
            StepButton("▶", enabled = ply < game.plyCount) { goTo(ply + 1) }
            StepButton("⏭", enabled = ply < game.plyCount) { goTo(game.plyCount) }
        }
        if (guessMode) {
            Spacer(Modifier.height(6.dp))
            Text("맞힌 수  $correct / $attempts", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ModeToggle(guessMode: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (guess in listOf(false, true)) {
            val label = if (guess) "맞히기" else "감상"
            val isSel = guess == guessMode
            Surface(
                onClick = { onChange(guess) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            ) {
                Text(label, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled) { Text(label) }
}

@Composable
private fun StatusCard(game: StudyGame, ply: Int, guessMode: Boolean, position: Position, feedback: String?) {
    val ended = ply >= game.plyCount
    val toMove = if (position.sideToMove == PieceColor.WHITE) "백" else "흑"
    val headline = when {
        ended -> "명국 종료 · 결과 ${game.meta.result}"
        guessMode -> "다음 수를 맞혀보세요 — $toMove 차례"
        ply == 0 -> "시작 위치 · ▶ 로 진행"
        else -> moveLabel(game, ply)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = feedback ?: "$ply / ${game.plyCount} 수",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** e.g. "12. Rxd7" for white's move, "12... Rxd7" for black's. */
private fun moveLabel(game: StudyGame, ply: Int): String {
    val san = game.sans.getOrNull(ply - 1) ?: return "$ply / ${game.plyCount}"
    val moveNumber = (ply + 1) / 2
    val dots = if (ply % 2 == 1) "." else "..."
    return "$moveNumber$dots $san"
}
