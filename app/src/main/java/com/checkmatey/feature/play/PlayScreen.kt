package com.checkmatey.feature.play

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.Material
import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.PieceType
import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import com.checkmatey.core.engine.Annotator
import com.checkmatey.core.engine.BotLevel
import com.checkmatey.core.engine.KotlinMinimaxEngine
import com.checkmatey.core.engine.MoveAnnotation
import com.checkmatey.core.engine.MoveQuality
import com.checkmatey.core.engine.Tutor
import com.checkmatey.core.engine.Threat
import com.checkmatey.core.study.StudyGames
import com.checkmatey.data.UserStore
import com.checkmatey.feature.review.ReviewScreen
import com.checkmatey.sound.Sfx
import com.checkmatey.sound.SoundFx
import com.checkmatey.ui.board.ChessBoard
import com.checkmatey.ui.components.EvalBar
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
    val context = LocalContext.current
    val store = remember { UserStore(context) }
    val userRating = remember { store.puzzleRating }
    val soundFx = remember { SoundFx() }
    val humanColor = PieceColor.WHITE

    var level by rememberSaveable { mutableStateOf(BotLevel.BEGINNER) }
    // Adaptive on by default so the bot always matches the player without them knowing to toggle it.
    var adaptive by rememberSaveable { mutableStateOf(true) }
    var soundOn by remember { mutableStateOf(store.soundOn) }
    var position by rememberSaveable(stateSaver = PositionSaver) { mutableStateOf(Position.startingPosition()) }
    var selected by remember { mutableStateOf<Square?>(null) }
    var lastMove by remember { mutableStateOf<Move?>(null) }
    var thinking by remember { mutableStateOf(false) }
    var gameOverSeen by remember { mutableStateOf(false) }
    var coachOn by rememberSaveable { mutableStateOf(true) }
    var hint by remember { mutableStateOf<MoveAnnotation?>(null) }
    var feedback by remember { mutableStateOf<MoveAnnotation?>(null) }
    // Tutor layer: staged Socratic hint (question → piece → answer), live threat warnings,
    // and the "this keeps happening" recall line tied to the player's weakness history.
    var hintStage by remember { mutableStateOf(0) }
    var hintLadder by remember { mutableStateOf<List<String>>(emptyList()) }
    var threats by remember { mutableStateOf<List<Threat>>(emptyList()) }
    var recall by remember { mutableStateOf<String?>(null) }
    var promotionChoice by remember { mutableStateOf<List<Move>>(emptyList()) }
    // Capture pop effect target: (square, counter) so consecutive captures on one square re-fire.
    var captureFx by remember { mutableStateOf<Pair<Square, Int>?>(null) }
    var showCaptured by remember { mutableStateOf(false) }
    val moves = remember { mutableStateListOf<Move>() }
    // Position after every ply (starts with the current position) — powers undo and
    // threefold-repetition detection.
    val history = remember { mutableStateListOf(position) }
    var reviewing by remember { mutableStateOf(false) }

    if (reviewing) {
        val reviewGame = remember(reviewing) { StudyGames.fromMoves(moves.toList()) }
        ReviewScreen(game = reviewGame, mySide = humanColor, onBack = { reviewing = false }, modifier = modifier)
        return
    }

    // Threefold repetition needs game history, so the screen judges it (core handles the rest).
    fun repetitionKey(p: Position): String = p.toFen().split(" ").take(4).joinToString(" ")
    val repetitionDraw = history.count { repetitionKey(it) == repetitionKey(position) } >= 3
    val gameEnded = position.isGameOver() || repetitionDraw
    val drawReason: String? = when {
        position.isCheckmate() -> null
        repetitionDraw -> "3회 반복"
        position.isFiftyMoveDraw() -> "50수 규칙"
        position.hasInsufficientMaterial() -> "기물 부족"
        position.isStalemate() -> "스테일메이트"
        else -> null
    }
    val isHumanTurn = position.sideToMove == humanColor && !gameEnded
    val isStart = position.toFen() == Position.STARTING_FEN
    val targets: Set<Square> = selected?.let { from ->
        position.legalMoves().filter { it.from == from }.map { it.to }.toSet()
    } ?: emptySet()
    // Hints reveal the board gradually: nothing at the question stage, the piece at stage 2, all at 3.
    val hintSquares: Set<Square> = when {
        hintStage >= 3 -> hint?.bestMove?.let { setOf(it.from, it.to) } ?: emptySet()
        hintStage == 2 -> hint?.bestMove?.let { setOf(it.from) } ?: emptySet()
        else -> emptySet()
    }
    val effectiveLevel = if (adaptive) BotLevel.forRating(userRating) else level

    fun applyMove(move: Move) {
        val capture = position.pieceAt(move.to) != null || move.isEnPassant
        position = position.applyMove(move)
        moves.add(move)
        history.add(position)
        lastMove = move
        if (capture) captureFx = move.to to ((captureFx?.second ?: 0) + 1)
        hint = null
        hintStage = 0
        hintLadder = emptyList()
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (soundOn) {
            soundFx.play(
                when {
                    position.isCheckmate() -> if (position.sideToMove == humanColor) Sfx.LOSE else Sfx.WIN
                    position.isInCheck() -> Sfx.CHECK
                    capture -> Sfx.CAPTURE
                    else -> Sfx.MOVE
                },
            )
        }
    }

    fun newGame() {
        position = Position.startingPosition()
        moves.clear()
        history.clear()
        history.add(position)
        selected = null
        lastMove = null
        hint = null
        hintStage = 0
        hintLadder = emptyList()
        feedback = null
        recall = null
        gameOverSeen = false
    }

    /** Take back your last move (and the bot's reply if it already came). */
    fun undo() {
        if (moves.isEmpty()) return
        val plies = if (position.sideToMove == humanColor && moves.size >= 2) 2 else 1
        repeat(plies) {
            if (moves.isNotEmpty()) {
                moves.removeAt(moves.lastIndex)
                history.removeAt(history.lastIndex)
            }
        }
        position = history.last()
        lastMove = moves.lastOrNull()
        selected = null
        hint = null
        hintStage = 0
        hintLadder = emptyList()
        feedback = null
        recall = null
        gameOverSeen = false
    }

    // Threat radar: while it's the student's turn, watch the board like a tutor would.
    LaunchedEffect(position, coachOn) {
        threats = if (coachOn && isHumanTurn) {
            withContext(Dispatchers.Default) { Tutor.threats(position, humanColor) }
        } else {
            emptyList()
        }
    }

    LaunchedEffect(position) {
        if (position.sideToMove != humanColor && !gameEnded) {
            thinking = true
            val move = withContext(Dispatchers.Default) { engine.chooseMove(position, effectiveLevel, Random.Default) }
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
        // A promotion offers four pieces — ask instead of silently queening.
        if (candidates.size > 1 && candidates.all { it.promotion != null }) {
            promotionChoice = candidates
            selected = null
            return
        }
        val move = candidates.firstOrNull { it.promotion == null } ?: candidates.firstOrNull { it.promotion == PieceType.QUEEN }
        if (move != null) {
            val before = position
            applyMove(move)
            selected = null
            feedback = null
            recall = null
            if (coachOn) {
                scope.launch {
                    val fb = withContext(Dispatchers.Default) { annotator.annotate(before, move) }
                    feedback = fb
                    // "This keeps happening": connect the mistake to the player's weakness history.
                    recall = if (fb.quality.ordinal >= MoveQuality.MISTAKE.ordinal) {
                        Tutor.themeOf(fb.reason)?.let { theme ->
                            store.themeStats()[theme]?.let { (attempts, _) ->
                                Tutor.recallLine(theme, attempts, store.successRate(theme))
                            }
                        }
                    } else {
                        null
                    }
                }
            }
        } else {
            selected = if (position.pieceAt(square)?.color == humanColor) square else null
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RatingSelector(selected = if (adaptive) effectiveLevel else level, onSelect = { level = it; adaptive = false })
        if (adaptive) {
            Spacer(Modifier.height(4.dp))
            Text(
                "적응 난이도 · 내 레이팅 $userRating → ${effectiveLevel.displayName} ~${effectiveLevel.approxElo}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(10.dp))
        StatusCard(position = position, humanColor = humanColor, thinking = thinking, isStart = isStart, drawReason = drawReason)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = {
                    if (!isHumanTurn) return@FilledTonalButton
                    if (hintStage == 0) {
                        scope.launch {
                            val h = withContext(Dispatchers.Default) { annotator.hint(position) }
                            hint = h
                            hintLadder = h?.let { Tutor.hintLadder(position, it) } ?: emptyList()
                            hintStage = if (h != null) 1 else 0
                            feedback = null
                        }
                    } else if (hintStage < 3) {
                        hintStage++
                    }
                },
                enabled = isHumanTurn && hintStage < 3,
            ) { Text(if (hintStage == 0) "💡 힌트" else "💡 더 보기 $hintStage/3") }
            Toggle(label = "코치", on = coachOn, onChange = { coachOn = it; if (!it) feedback = null })
            Toggle(label = "적응", on = adaptive, onChange = { adaptive = it })
            Toggle(label = "소리", on = soundOn, onChange = { soundOn = it; store.soundOn = it })
        }
        Spacer(Modifier.height(8.dp))

        // Advantage gauge (full evaluation: material + positioning) + captured pieces, tap for detail.
        val evalCp = remember(position) { engine.evaluate(position) }
        val captured = remember(position) { Material.captured(position) }
        EvalBar(evalCp = evalCp)
        if (captured.byWhite.isNotEmpty() || captured.byBlack.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Surface(
                onClick = { showCaptured = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "나: ${captured.byWhite.joinToString("") { Material.glyph(it) }.ifEmpty { "—" }}",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                    Text(
                        when {
                            captured.diffPawns > 0 -> "+${captured.diffPawns}"
                            captured.diffPawns < 0 -> "${captured.diffPawns}"
                            else -> "="
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "봇: ${captured.byBlack.joinToString("") { Material.glyph(it) }.ifEmpty { "—" }}",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))

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
                    captureEffect = captureFx,
                )
            }
        }

        // Tutor voice, in priority order: threat warning (before you move) → hint ladder → coach verdict.
        if (threats.isNotEmpty() && isHumanTurn && hintStage == 0 && feedback == null) {
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Text(
                    threats.joinToString("\n") { it.text },
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(6.dp))
        }
        if (hintStage in 1..hintLadder.size) {
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    hintLadder[hintStage - 1],
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(6.dp))
        }
        CoachCard(hint = null, feedback = feedback.takeIf { coachOn })
        recall?.takeIf { coachOn }?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = ::undo, enabled = moves.isNotEmpty() && !thinking) { Text("↩ 무르기") }
            OutlinedButton(onClick = { reviewing = true }, enabled = moves.isNotEmpty()) { Text("📊 분석") }
            OutlinedButton(onClick = ::newGame) { Text("새 게임") }
        }
    }

    // Save every finished game so the 분석 tab can offer it for review.
    LaunchedEffect(gameEnded, moves.size) {
        if (gameEnded && moves.isNotEmpty()) {
            val result = when {
                position.isCheckmate() && position.sideToMove == humanColor -> "패배"
                position.isCheckmate() -> "승리"
                else -> "무승부"
            }
            store.saveGame(result, moves.map { it.uci() })
        }
    }

    if (showCaptured) {
        val captured = Material.captured(position)
        AlertDialog(
            onDismissRequest = { showCaptured = false },
            title = { Text("잡은 기물") },
            text = {
                Column {
                    Text("내가 잡은 기물", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        captured.byWhite.joinToString(" ") { Material.glyph(it) }.ifEmpty { "아직 없음" },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("봇이 잡은 기물", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        captured.byBlack.joinToString(" ") { Material.glyph(it) }.ifEmpty { "아직 없음" },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when {
                            captured.diffPawns > 0 -> "기물 점수로 내가 폰 ${captured.diffPawns}점만큼 앞서 있어요."
                            captured.diffPawns < 0 -> "기물 점수로 봇이 폰 ${-captured.diffPawns}점만큼 앞서 있어요."
                            else -> "기물 점수는 동등해요."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showCaptured = false }) { Text("닫기") } },
        )
    }

    if (promotionChoice.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { promotionChoice = emptyList() },
            title = { Text("승격할 기물을 고르세요") },
            text = {
                Column {
                    Text("보통은 가장 강한 퀸을 고릅니다. 드물게 룩/비숍/나이트가 더 좋을 때도 있어요(스테일메이트 회피 등).")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (option in promotionChoice) {
                            val type = option.promotion ?: continue
                            FilledTonalButton(onClick = {
                                val before = position
                                applyMove(option)
                                promotionChoice = emptyList()
                                feedback = null
                                if (coachOn) scope.launch { feedback = withContext(Dispatchers.Default) { annotator.annotate(before, option) } }
                            }) { Text(promotionName(type)) }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { promotionChoice = emptyList() }) { Text("취소") } },
        )
    }

    if (gameEnded && !gameOverSeen) {
        val (title, detail) = gameOverMessage(position, humanColor, drawReason)
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
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (level in BotLevel.entries) {
            val isSel = level == selected
            Surface(
                onClick = { onSelect(level) },
                modifier = Modifier.width(64.dp),
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
private fun StatusCard(position: Position, humanColor: PieceColor, thinking: Boolean, isStart: Boolean, drawReason: String?) {
    val scheme = MaterialTheme.colorScheme
    val loss = position.isCheckmate() && position.sideToMove == humanColor
    val win = position.isCheckmate() && position.sideToMove != humanColor
    val draw = drawReason != null

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
        draw -> "무승부 — $drawReason"
        thinking -> "컴퓨터가 생각 중…"
        position.isInCheck() -> "체크! — 당신(백) 차례"
        position.sideToMove == humanColor -> "당신(백) 차례"
        else -> "컴퓨터(흑) 차례"
    }
    val sub = when {
        loss || win || draw -> "'새 게임'으로 다시 시작하세요"
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

private fun promotionName(type: PieceType): String = when (type) {
    PieceType.QUEEN -> "퀸 ♛"
    PieceType.ROOK -> "룩 ♜"
    PieceType.BISHOP -> "비숍 ♝"
    PieceType.KNIGHT -> "나이트 ♞"
    else -> type.name
}

private fun gameOverMessage(position: Position, humanColor: PieceColor, drawReason: String?): Pair<String, String> = when {
    position.isCheckmate() && position.sideToMove == humanColor -> "패배" to "체크메이트. 컴퓨터가 이겼습니다. 다시 도전해 보세요."
    position.isCheckmate() -> "승리 🎉" to "체크메이트! 당신이 이겼습니다."
    else -> "무승부" to "무승부 — ${drawReason ?: "스테일메이트"}."
}
