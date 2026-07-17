package com.checkmatey.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.study.PgnParser
import com.checkmatey.core.study.StudyGame
import com.checkmatey.core.study.StudyGames
import com.checkmatey.feature.review.ReviewScreen

/** Profile tab, for now a game-review hub: paste a PGN and the coach analyses the whole game. */
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    var pgnText by rememberSaveable { mutableStateOf("") }
    var reviewGame by remember { mutableStateOf<StudyGame?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val game = reviewGame
    if (game != null) {
        ReviewScreen(game = game, mySide = PieceColor.WHITE, onBack = { reviewGame = null }, modifier = modifier)
        return
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("기보 분석", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "받은 PGN 기보를 붙여넣으면 코치가 한 수씩 채점하고 실수를 짚어줍니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pgnText,
            onValueChange = { pgnText = it; error = null },
            label = { Text("PGN 붙여넣기") },
            modifier = Modifier.fillMaxWidth().height(220.dp),
        )
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val parsed = PgnParser.parse(pgnText).firstOrNull()
                val built = parsed?.let { StudyGames.build(it) }
                if (built == null || built.moves.isEmpty()) {
                    error = "기보를 읽지 못했습니다. PGN 형식을 확인해 주세요."
                } else {
                    reviewGame = built
                }
            },
            enabled = pgnText.isNotBlank(),
        ) {
            Text("분석하기")
        }
    }
}
