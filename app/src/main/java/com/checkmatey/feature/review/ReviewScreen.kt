package com.checkmatey.feature.review

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.engine.Annotator
import com.checkmatey.core.engine.GameReviewer
import com.checkmatey.core.engine.KotlinMinimaxEngine
import com.checkmatey.core.engine.MoveAnnotation
import com.checkmatey.core.engine.MoveQuality
import com.checkmatey.core.study.StudyGame
import com.checkmatey.ui.board.ChessBoard
import com.checkmatey.ui.board.BoardArrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reviews a whole game — your game vs the computer, or an imported PGN. Runs the coach over every
 * move, shows an accuracy summary, then lets you step through with each move graded and explained.
 */
@Composable
fun ReviewScreen(game: StudyGame, mySide: PieceColor, onBack: () -> Unit, modifier: Modifier = Modifier) {
    // Depth 3 + the engine's quiescence search makes the per-move verdicts noticeably more accurate.
    val annotator = remember { Annotator(KotlinMinimaxEngine(), depth = 3) }
    val reviewer = remember { GameReviewer(annotator) }

    var annotations by remember { mutableStateOf<List<MoveAnnotation>?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var ply by remember { mutableIntStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { com.checkmatey.data.UserStore(context) }
    LaunchedEffect(game) { store.reviewedLatestGame = true }
    LaunchedEffect(game) {
        val list = ArrayList<MoveAnnotation>(game.plyCount)
        for (i in game.moves.indices) {
            val annotation = withContext(Dispatchers.Default) { annotator.annotate(game.positions[i], game.moves[i]) }
            list.add(annotation)
            progress = i + 1
        }
        annotations = list.toList()
        // Feed my weak themes (from this game's mistakes on my side) into the puzzle trainer.
        val mine = list.filterIndexed { i, _ -> game.positions[i].sideToMove == mySide }
        val themes = reviewer.weakThemes(mine)
        if (themes.isNotEmpty()) store.recommendedThemes = themes
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBack) { Text("← 뒤로") }
        }

        val done = annotations
        if (done == null) {
            Spacer(Modifier.height(24.dp))
            Text("분석 중…  $progress / ${game.plyCount}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (game.plyCount == 0) 1f else progress.toFloat() / game.plyCount },
                modifier = Modifier.fillMaxWidth(0.8f),
            )
            return@Column
        }

        val summary = reviewer.summarize(game, done, mySide)
        SummaryCard(summary)
        Spacer(Modifier.height(10.dp))

        val annotation = if (ply > 0) done.getOrNull(ply - 1) else null
        MoveCard(game = game, ply = ply, annotation = annotation)
        Spacer(Modifier.height(10.dp))

        // Board arrows: your move (colour by quality) + the better move in green when they differ.
        val arrows = remember(ply) {
            val list = ArrayList<BoardArrow>(2)
            if (annotation != null && ply > 0) {
                val played = game.moveAt(ply - 1)
                val best = annotation.bestMove
                val playedColor = when {
                    annotation.quality == MoveQuality.BLUNDER || annotation.quality == MoveQuality.MISTAKE -> Color(0xE0E53935)
                    annotation.quality == MoveQuality.INACCURACY -> Color(0xE0FB8C00)
                    else -> Color(0xE043A047)
                }
                if (played != null) list.add(BoardArrow(played.from, played.to, playedColor))
                // Only show the "better move" arrow when it actually differs and the move wasn't best.
                if (best != null && annotation.quality.ordinal >= MoveQuality.INACCURACY.ordinal &&
                    (best.from != played?.from || best.to != played.to)
                ) {
                    list.add(BoardArrow(best.from, best.to, Color(0xF01E88E5))) // blue = recommended
                }
            }
            list
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            BoxWithConstraints {
                val side = minOf(maxWidth, maxHeight).coerceAtMost(480.dp)
                ChessBoard(
                    position = game.positionAt(ply),
                    modifier = Modifier.size(side),
                    lastMove = if (ply > 0) game.moveAt(ply - 1) else null,
                    arrows = arrows,
                )
            }
        }

        // Legend so the colours are self-explanatory.
        Spacer(Modifier.height(6.dp))
        Text(
            "화살표 — 초록/노랑/빨강: 내가 둔 수(좋음/부정확/실수)   ·   파랑: 더 좋았던 수",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))
        var autoplay by remember { mutableStateOf(false) }
        LaunchedEffect(autoplay, ply) {
            if (autoplay) {
                if (ply < game.plyCount) {
                    kotlinx.coroutines.delay(1100)
                    ply++
                } else {
                    autoplay = false
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { ply = 0; autoplay = false }, enabled = ply > 0) { Text("⏮") }
            OutlinedButton(onClick = { ply--; autoplay = false }, enabled = ply > 0) { Text("◀") }
            OutlinedButton(onClick = { autoplay = !autoplay }) { Text(if (autoplay) "⏸ 정지" else "▶ 자동재생") }
            OutlinedButton(onClick = { ply++; autoplay = false }, enabled = ply < game.plyCount) { Text("▶") }
            OutlinedButton(
                onClick = { ply = nextMistake(game, done, mySide, ply); autoplay = false },
                enabled = hasNextMistake(game, done, mySide, ply),
            ) { Text("다음 실수") }
        }
    }
}

@Composable
private fun SummaryCard(summary: com.checkmatey.core.engine.ReviewSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("내 정확도  ${summary.accuracy}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "최선 ${summary.count(MoveQuality.BEST)} · 좋은수 ${summary.count(MoveQuality.GOOD)} · " +
                    "부정확 ${summary.count(MoveQuality.INACCURACY)} · 실수 ${summary.count(MoveQuality.MISTAKE)} · " +
                    "블런더 ${summary.count(MoveQuality.BLUNDER)}",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MoveCard(game: StudyGame, ply: Int, annotation: MoveAnnotation?) {
    val scheme = MaterialTheme.colorScheme
    if (ply == 0 || annotation == null) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = scheme.surfaceVariant) {
            Text(
                "시작 위치 — ▶ 로 수를 넘기며 코치의 평가를 보세요",
                Modifier.fillMaxWidth().padding(14.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    val bad = annotation.quality == MoveQuality.MISTAKE || annotation.quality == MoveQuality.BLUNDER
    val inacc = annotation.quality == MoveQuality.INACCURACY
    val container = when {
        bad -> scheme.errorContainer
        inacc -> scheme.surfaceVariant
        else -> scheme.tertiaryContainer
    }
    val onContainer = when {
        bad -> scheme.onErrorContainer
        inacc -> scheme.onSurfaceVariant
        else -> scheme.onTertiaryContainer
    }
    val number = (ply + 1) / 2
    val dots = if (ply % 2 == 1) "." else "..."
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = container, contentColor = onContainer) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$number$dots ${annotation.san}   ${annotation.quality.label} ${annotation.quality.symbol}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(annotation.reason, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

private fun isMine(game: StudyGame, ply: Int, side: PieceColor): Boolean =
    ply in 1..game.plyCount && game.positions[ply - 1].sideToMove == side

private fun nextMistake(game: StudyGame, annotations: List<MoveAnnotation>, side: PieceColor, from: Int): Int {
    for (p in (from + 1)..game.plyCount) {
        val a = annotations.getOrNull(p - 1) ?: continue
        if (isMine(game, p, side) && a.quality.ordinal >= MoveQuality.MISTAKE.ordinal) return p
    }
    return from
}

private fun hasNextMistake(game: StudyGame, annotations: List<MoveAnnotation>, side: PieceColor, from: Int): Boolean =
    nextMistake(game, annotations, side, from) != from
