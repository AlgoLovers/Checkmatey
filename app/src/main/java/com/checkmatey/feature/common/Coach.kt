package com.checkmatey.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.Position
import com.checkmatey.core.engine.Annotator
import com.checkmatey.core.engine.KotlinMinimaxEngine
import com.checkmatey.core.engine.MoveAnnotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The coach, made concurrency-safe by construction.
 *
 * [KotlinMinimaxEngine] keeps its search state in instance fields, so a single instance must never
 * run two searches at once. Sharing one engine between the bot's move search and the coach caused an
 * intermittent mis-grading race (M43: a hinted move sometimes graded as a blunder). [Coach] removes
 * that whole class of bug: it owns a DEDICATED engine (never the bot's) and serialises every call
 * behind a mutex, hopping to a background dispatcher itself. Screens just call `coach.hint(...)` /
 * `coach.annotate(...)` — they can't wire the concurrency wrong because there's no wiring to do.
 */
class Coach(depth: Int = 4) {
    private val annotator = Annotator(KotlinMinimaxEngine(), depth)
    private val lock = Mutex()

    suspend fun hint(position: Position): MoveAnnotation? =
        lock.withLock { withContext(Dispatchers.Default) { annotator.hint(position) } }

    suspend fun annotate(before: Position, move: Move): MoveAnnotation =
        lock.withLock { withContext(Dispatchers.Default) { annotator.annotate(before, move) } }
}

/** Remembers a [Coach] for the composition — the sanctioned way to grade/hint on a board screen. */
@Composable
fun rememberCoach(depth: Int = 4): Coach = remember { Coach(depth) }
