package com.checkmatey.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.Piece
import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.PieceType
import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import com.checkmatey.ui.theme.BoardDark
import com.checkmatey.ui.theme.BoardHighlight
import com.checkmatey.ui.theme.BoardLight

/**
 * Renders an 8x8 board. Pieces are drawn as solid, side-colored glyphs (white bodies with a
 * dark edge, black bodies with a light edge) so both are easy to read on any square. The most
 * recent move's squares are tinted, and the moved piece slides from origin to destination.
 *
 * Files (a–h) and ranks (1–8) are labelled on the board edges, so instructions like
 * "e2 폰을 e4로" can actually be followed by someone who has never read chess notation.
 *
 * @param lastMove the move to highlight and animate; null on a fresh board.
 * @param onSquareClick invoked with the tapped square; null makes the board display-only.
 */
@Composable
fun ChessBoard(
    position: Position,
    modifier: Modifier = Modifier,
    selected: Square? = null,
    targets: Set<Square> = emptySet(),
    lastMove: Move? = null,
    hintSquares: Set<Square> = emptySet(),
    arrows: List<BoardArrow> = emptyList(),
    // A capture "pop" plays at the square whenever the value changes; scale grows with the piece.
    captureEffect: CaptureFx? = null,
    onSquareClick: ((Square) -> Unit)? = null,
) {
    BoxWithConstraints(modifier.aspectRatio(1f)) {
        val cell = maxWidth / 8
        val glyphSize = with(LocalDensity.current) { (cell * 0.72f).toSp() }
        val coordSize = with(LocalDensity.current) { (cell * 0.20f).toSp() }

        // Slide the last-moved piece from its origin to its destination.
        // Key the Animatable to lastMove so it resets to 0 *in the same composition* the new
        // position arrives — otherwise the piece flashes at the destination for a frame, jumps
        // back to the origin, then slides (the reported flicker).
        val progress = remember(lastMove) { Animatable(if (lastMove == null) 1f else 0f) }
        LaunchedEffect(lastMove) {
            if (lastMove != null) progress.animateTo(1f, animationSpec = tween(durationMillis = 180))
        }
        val animating = lastMove != null && progress.value < 1f

        Column(Modifier.fillMaxSize()) {
            for (rank in 7 downTo 0) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (file in 0..7) {
                        val square = Square(file, rank)
                        val isLight = (file + rank) % 2 == 1
                        val piece = position.pieceAt(square)
                        val isLastMoveSquare = lastMove != null && (square == lastMove.from || square == lastMove.to)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (isLight) BoardLight else BoardDark)
                                .then(
                                    if (onSquareClick != null) Modifier.clickable { onSquareClick(square) } else Modifier,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            // Coordinate labels on the edges: files along the bottom, ranks on the left.
                            val labelColor = if (isLight) BoardDark else BoardLight
                            if (rank == 0) {
                                Text(
                                    text = ('a' + file).toString(),
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 2.dp),
                                    style = TextStyle(fontSize = coordSize, color = labelColor, fontWeight = FontWeight.Bold),
                                )
                            }
                            if (file == 0) {
                                Text(
                                    text = (rank + 1).toString(),
                                    modifier = Modifier.align(Alignment.TopStart).padding(horizontal = 2.dp),
                                    style = TextStyle(fontSize = coordSize, color = labelColor, fontWeight = FontWeight.Bold),
                                )
                            }
                            when {
                                square == selected ->
                                    Box(Modifier.matchParentSize().background(BoardHighlight.copy(alpha = 0.55f)))
                                isLastMoveSquare ->
                                    Box(Modifier.matchParentSize().background(BoardHighlight.copy(alpha = 0.32f)))
                            }
                            // Hide the moved piece in its cell while it's sliding (drawn in the overlay).
                            if (piece != null && !(animating && square == lastMove?.to)) {
                                PieceGlyph(piece, glyphSize)
                            }
                            if (square in hintSquares) {
                                Box(Modifier.matchParentSize().border(3.dp, HintColor, RectangleShape))
                            }
                            if (square in targets) {
                                if (piece != null) {
                                    Box(Modifier.fillMaxSize(0.92f).border(3.dp, Color.Black.copy(alpha = 0.30f), CircleShape))
                                } else {
                                    Box(Modifier.fillMaxSize(0.30f).background(Color.Black.copy(alpha = 0.25f), CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sliding piece overlay.
        if (animating && lastMove != null) {
            position.pieceAt(lastMove.to)?.let { moving ->
                val x = lerp(cell * lastMove.from.file, cell * lastMove.to.file, progress.value)
                val y = lerp(cell * (7 - lastMove.from.rank), cell * (7 - lastMove.to.rank), progress.value)
                Box(
                    modifier = Modifier.offset(x = x, y = y).size(cell),
                    contentAlignment = Alignment.Center,
                ) {
                    PieceGlyph(moving, glyphSize)
                }
            }
        }

        // Capture pop, scaled to what was taken: a pawn gives a small tick, a minor piece the classic
        // ring, a rook a double ripple, a queen a full golden burst with rays. The prize should FEEL
        // like the prize.
        if (captureEffect != null) {
            val tier = captureTier(captureEffect.piece)
            val pop = remember(captureEffect) { Animatable(0f) }
            LaunchedEffect(captureEffect) { pop.animateTo(1f, tween(durationMillis = tier.durationMs)) }
            val p = pop.value
            if (p < 1f) {
                val cellPx = with(LocalDensity.current) { cell.toPx() }
                Canvas(Modifier.matchParentSize()) {
                    val sq = captureEffect.square
                    val center = Offset(
                        x = (sq.file + 0.5f) * cellPx,
                        y = (7 - sq.rank + 0.5f) * cellPx,
                    )
                    // Stay fully visible for the first 60% of the animation, then dissolve —
                    // the point is that the player NOTICES the capture, not just a flicker.
                    val alpha = ((1f - p) / 0.4f).coerceAtMost(1f)
                    // Impact flash: a bright filled disc that expands and fades in the first half, so
                    // the capture LANDS with a punch instead of only outlining a ring.
                    val flash = ((0.5f - p) / 0.5f).coerceAtLeast(0f)
                    if (flash > 0f) {
                        drawCircle(
                            color = Color(0xFFFFE2A6).copy(alpha = flash * 0.5f),
                            radius = cellPx * (0.22f + tier.ringScale * 0.9f * p),
                            center = center,
                        )
                    }
                    // Main ring — thick and bright so it reads as an explosion, not a hairline.
                    drawCircle(
                        color = Color(0xFFFFC46A).copy(alpha = alpha * 0.9f),
                        radius = cellPx * (0.15f + tier.ringScale * p),
                        center = center,
                        style = Stroke(width = cellPx * 0.15f * (1f - 0.55f * p)),
                    )
                    // Bright core flash.
                    drawCircle(
                        color = Color(0xFFFFF3DC).copy(alpha = alpha * tier.coreAlpha),
                        radius = cellPx * 0.30f * (1f - p),
                        center = center,
                    )
                    // Second ripple for rook and queen — a delayed echo of the main ring.
                    if (tier.doubleRing && p > 0.25f) {
                        val p2 = (p - 0.25f) / 0.75f
                        drawCircle(
                            color = Color(0xFFFFD98E).copy(alpha = (1f - p2) * 0.6f),
                            radius = cellPx * (0.10f + tier.ringScale * 1.25f * p2),
                            center = center,
                            style = Stroke(width = cellPx * 0.06f),
                        )
                    }
                    // Queen only: eight golden rays bursting outward.
                    if (tier.rays) {
                        val rayLen = cellPx * (0.35f + 0.75f * p)
                        val inner = cellPx * 0.25f * (1f + p)
                        for (k in 0 until 8) {
                            val angle = Math.PI / 4 * k
                            val dx = kotlin.math.cos(angle).toFloat()
                            val dy = kotlin.math.sin(angle).toFloat()
                            drawLine(
                                color = Color(0xFFFFE2A6).copy(alpha = alpha * 0.9f),
                                start = Offset(center.x + dx * inner, center.y + dy * inner),
                                end = Offset(center.x + dx * rayLen, center.y + dy * rayLen),
                                strokeWidth = cellPx * 0.05f * (1f - 0.5f * p),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            )
                        }
                    }
                    // Confetti shards bursting out and arcing down — the playful bit. Angles/speeds
                    // are derived from the particle index so they're stable across recompositions.
                    for (k in 0 until tier.particles) {
                        val ang = (k * 2.399963f) // golden-angle spread, no clustering
                        val speed = 0.55f + 0.45f * ((k * 7) % 10) / 10f
                        val dist = cellPx * (tier.ringScale + 0.5f) * speed * p
                        val gravity = cellPx * 0.9f * p * p // arc downward like real confetti
                        val px = center.x + kotlin.math.cos(ang) * dist
                        val py = center.y + kotlin.math.sin(ang) * dist + gravity
                        val size = cellPx * (0.05f + 0.03f * ((k * 3) % 5) / 5f) * (1f - 0.5f * p)
                        drawCircle(
                            color = CaptureConfetti[k % CaptureConfetti.size].copy(alpha = alpha),
                            radius = size,
                            center = Offset(px, py),
                        )
                    }
                }
            }
        }

        // Arrows drawn over the board (e.g. your move vs. the better move in review).
        if (arrows.isNotEmpty()) {
            val cellPx = with(LocalDensity.current) { cell.toPx() }
            Canvas(Modifier.matchParentSize()) {
                for (arrow in arrows) {
                    fun center(sq: Square) = Offset(
                        x = (sq.file + 0.5f) * cellPx,
                        y = (7 - sq.rank + 0.5f) * cellPx,
                    )
                    drawArrow(center(arrow.from), center(arrow.to), arrow.color, cellPx)
                }
            }
        }
    }
}

/** An arrow to draw on the board, from [from] to [to]. */
data class BoardArrow(val from: Square, val to: Square, val color: Color)

/**
 * A capture to celebrate: where, what was taken (drives the effect size), and a counter so
 * consecutive captures on the same square still re-fire the animation.
 */
data class CaptureFx(val square: Square, val piece: PieceType, val counter: Int)

/** Effect parameters per captured-piece tier — the prize should feel like the prize. */
private data class CaptureTier(
    val durationMs: Int,
    val ringScale: Float,
    val coreAlpha: Float,
    val doubleRing: Boolean,
    val rays: Boolean,
    val particles: Int, // little shards that burst outward — the "fun"
)

private fun captureTier(piece: PieceType): CaptureTier = when (piece) {
    PieceType.PAWN -> CaptureTier(600, ringScale = 0.55f, coreAlpha = 0.55f, doubleRing = false, rays = false, particles = 9)
    PieceType.KNIGHT, PieceType.BISHOP -> CaptureTier(780, ringScale = 0.90f, coreAlpha = 0.70f, doubleRing = true, rays = false, particles = 16)
    PieceType.ROOK -> CaptureTier(900, ringScale = 1.15f, coreAlpha = 0.80f, doubleRing = true, rays = false, particles = 22)
    PieceType.QUEEN -> CaptureTier(1100, ringScale = 1.40f, coreAlpha = 0.95f, doubleRing = true, rays = true, particles = 30)
    PieceType.KING -> CaptureTier(780, ringScale = 0.90f, coreAlpha = 0.70f, doubleRing = true, rays = false, particles = 16)
}

// Warm confetti palette for the shards — reads as celebratory, not clinical.
private val CaptureConfetti = listOf(
    Color(0xFFFFC46A), Color(0xFFFF8A5B), Color(0xFFFFE08A), Color(0xFF6FE0B0), Color(0xFFFFF3DC),
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color,
    cellPx: Float,
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = kotlin.math.hypot(dx, dy)
    if (len < 1f) return
    val ux = dx / len
    val uy = dy / len
    val head = cellPx * 0.42f
    val width = cellPx * 0.16f
    // Shaft stops short so the arrowhead sits cleanly on the target square.
    val shaftEnd = Offset(end.x - ux * head, end.y - uy * head)
    drawLine(color = color, start = start, end = shaftEnd, strokeWidth = width, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    // Arrowhead triangle.
    val perpX = -uy
    val perpY = ux
    val tip = end
    val baseL = Offset(shaftEnd.x + perpX * head * 0.6f, shaftEnd.y + perpY * head * 0.6f)
    val baseR = Offset(shaftEnd.x - perpX * head * 0.6f, shaftEnd.y - perpY * head * 0.6f)
    val path = Path().apply {
        moveTo(tip.x, tip.y); lineTo(baseL.x, baseL.y); lineTo(baseR.x, baseR.y); close()
    }
    drawPath(path, color)
}

// Border color marking the hint (best-move) squares.
private val HintColor = Color(0xFF1E88E5)

@Composable
private fun PieceGlyph(piece: Piece, size: TextUnit) {
    val isWhite = piece.color == PieceColor.WHITE
    Text(
        text = filledGlyph(piece.type).toString(),
        style = TextStyle(
            fontSize = size,
            color = if (isWhite) Color(0xFFF7F7F7) else Color(0xFF1A1A1A),
            // Symmetric shadow acts as an outline: dark edge on white pieces, light edge on black.
            shadow = Shadow(
                color = if (isWhite) Color(0xDD000000) else Color(0x88FFFFFF),
                offset = Offset(0f, 0f),
                blurRadius = if (isWhite) 6f else 3f,
            ),
        ),
    )
}

/** The filled (black-series) chess glyph for a type, so both colors render as solid shapes. */
private fun filledGlyph(type: PieceType): Char {
    val offset = when (type) {
        PieceType.KING -> 0
        PieceType.QUEEN -> 1
        PieceType.ROOK -> 2
        PieceType.BISHOP -> 3
        PieceType.KNIGHT -> 4
        PieceType.PAWN -> 5
    }
    return (0x265A + offset).toChar()
}
