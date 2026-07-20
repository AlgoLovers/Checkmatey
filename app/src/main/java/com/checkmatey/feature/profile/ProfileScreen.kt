package com.checkmatey.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.Position
import com.checkmatey.core.study.PgnParser
import com.checkmatey.core.study.StudyGame
import com.checkmatey.core.study.StudyGames
import com.checkmatey.data.UserStore
import com.checkmatey.feature.common.ReviewScreen
import com.checkmatey.ui.Sparkline

/**
 * 분석 tab — the review hub. It lists the games you actually played (saved automatically when a
 * game ends) so one tap gives you a full coached review; pasting a PGN is the advanced extra.
 */
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { UserStore(context) }
    val games = remember { store.recentGames }
    var reviewGame by remember { mutableStateOf<StudyGame?>(null) }
    var showPgn by rememberSaveable { mutableStateOf(false) }
    var pgnText by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val open = reviewGame
    if (open != null) {
        ReviewScreen(game = open, mySide = PieceColor.WHITE, onBack = { reviewGame = null }, modifier = modifier)
        return
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        ProgressCard(store)
        Spacer(Modifier.height(14.dp))
        Text("분석 (복기)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "둔 게임을 코치가 한 수씩 채점합니다 — 어디서 실수했고 무엇이 더 좋았는지. 복기는 실력 향상의 가장 빠른 길입니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        if (games.isEmpty()) {
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    "아직 분석할 게임이 없어요.\n\n[대국] 탭에서 컴퓨터와 한 판 두면 게임이 끝나는 순간 여기에 자동으로 저장됩니다.\n그 다음 여기서 탭 한 번이면 전체 복기!",
                    Modifier.fillMaxWidth().padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Text("내 최근 게임", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(games.size) { index ->
                    val entry = games[index]
                    val result = entry.substringBefore("|")
                    val ucis = entry.substringAfter("|").split(",").filter { it.isNotBlank() }
                    val scheme = MaterialTheme.colorScheme
                    Surface(
                        onClick = { reviewGame = buildGame(ucis) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = when (result) {
                            "승리" -> scheme.tertiaryContainer
                            "패배" -> scheme.errorContainer
                            else -> scheme.surfaceVariant
                        },
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("나 vs 컴퓨터  ·  $result", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("${ucis.size}수", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("복기 →", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        TextButton(onClick = { showPgn = !showPgn }) {
            Text(if (showPgn) "기보(PGN) 붙여넣기 닫기" else "고급: 기보(PGN) 붙여넣어 분석하기")
        }
        if (showPgn) {
            OutlinedTextField(
                value = pgnText,
                onValueChange = { pgnText = it; error = null },
                label = { Text("PGN 텍스트") },
                modifier = Modifier.fillMaxWidth().height(140.dp),
            )
            error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val built = PgnParser.parse(pgnText).firstOrNull()?.let { StudyGames.build(it) }
                    if (built == null || built.moves.isEmpty()) error = "기보를 읽지 못했습니다. PGN 형식을 확인해 주세요."
                    else reviewGame = built
                },
                enabled = pgnText.isNotBlank(),
            ) { Text("분석하기") }
        }
    }
}

@Composable
private fun ProgressCard(store: UserStore) {
    val rating = store.puzzleRating
    val history = store.ratingHistory
    val lessons = store.completedLessons.size
    val drills = store.completedDrills.size
    val bestStreak = store.bestStreak
    val trend = if (history.size >= 2) history.last() - history.first() else 0

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("내 실력", style = MaterialTheme.typography.labelMedium)
                    Text("레이팅 $rating", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (history.size >= 2) {
                        val sign = if (trend >= 0) "▲ +$trend" else "▼ $trend"
                        Text("$sign  (최근 ${history.size}문제)", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Sparkline(
                    values = history,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.height(48.dp).width(120.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "레슨 $lessons/12  ·  엔드게임 실습 $drills/2  ·  최고 연속 $bestStreak",
                style = MaterialTheme.typography.bodySmall,
            )
            if (history.size < 2) {
                Spacer(Modifier.height(4.dp))
                Text("[퍼즐] 탭에서 문제를 풀면 여기에 실력 그래프가 그려집니다.", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Rebuild a saved game (UCI list) into a reviewable game. */
private fun buildGame(ucis: List<String>): StudyGame {
    var pos = Position.startingPosition()
    val moves = ucis.mapNotNull { uci ->
        val move = pos.legalMoves().firstOrNull { it.uci() == uci }
        if (move != null) pos = pos.applyMove(move)
        move
    }
    return StudyGames.fromMoves(moves)
}
