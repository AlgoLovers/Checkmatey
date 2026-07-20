package com.checkmatey.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkmatey.core.chess.Material
import com.checkmatey.core.chess.PieceType
import com.checkmatey.core.chess.koreanName
import kotlinx.coroutines.delay

/**
 * A just-captured piece, for the on-screen callout. [byMe] distinguishes "I won it" (celebrate) from
 * "I lost it" (a gentle nudge). [counter] lets the same piece re-trigger the callout on a later
 * capture (state-equality would otherwise swallow it).
 */
data class CaptureNote(val piece: PieceType, val byMe: Boolean, val counter: Int)

/**
 * A small floating chip that names the piece just captured — "♞ 상대 나이트 획득! +3" or "내 비숍을
 *내줬어요 −3" — so the player always knows *what* was taken, not just that something flashed. It
 * shows briefly then fades on its own; overlay it on the board (e.g. Box + Modifier.align(TopCenter)).
 */
@Composable
fun CaptureCallout(note: CaptureNote?, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf<CaptureNote?>(null) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(note?.counter) {
        val n = note ?: return@LaunchedEffect
        shown = n
        alpha.snapTo(1f)
        delay(1400)
        alpha.animateTo(0f, tween(400))
        shown = null
    }
    val n = shown ?: return
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = if (n.byMe) scheme.tertiaryContainer to scheme.onTertiaryContainer
    else scheme.errorContainer to scheme.onErrorContainer
    Surface(
        modifier = modifier.alpha(alpha.value),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        contentColor = fg,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Text(
            calloutText(n),
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun calloutText(n: CaptureNote): String {
    val glyph = Material.glyph(n.piece)
    val name = n.piece.koreanName()
    val v = Material.pawnValue(n.piece)
    val badge = if (v > 0) if (n.byMe) "  +$v" else "  −$v" else ""
    return if (n.byMe) "$glyph 상대 ${name} 획득!$badge" else "$glyph 내 ${name}을(를) 내줬어요$badge"
}
