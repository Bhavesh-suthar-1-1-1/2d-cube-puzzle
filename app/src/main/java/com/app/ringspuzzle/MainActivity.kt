package com.app.ringspuzzle

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private val BG = Color(0xFF0A0A0A)
private val INK = Color(0xFFF2F2F2)
private val MUTED = Color(0xFF9A9A9A)
private val RING_GREY = Color(0xFF8A8A8A)
private val ACCENT = Color(0xFFFFD500)
private val ARROW = Color(0xFFBFBFBF)
private fun timeKey(n: Int) = if (n == 24) "best_time" else "best_time_$n"
private fun movesKey(n: Int) = if (n == 24) "best_moves" else "best_moves_$n"

private val CharArraySaver = Saver<CharArray, String>(
    save = { String(it) },
    restore = { it.toCharArray() }
)
private val ListSaver = Saver<List<String>, String>(
    save = { it.joinToString(" ") },
    restore = { if (it.isEmpty()) emptyList() else it.split(" ") }
)

private fun fmtTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RingsApp() }
    }
}

@Composable
fun RingsApp() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("rings_prefs", Context.MODE_PRIVATE)
    }
    val scope = rememberCoroutineScope()

    var balls by rememberSaveable(stateSaver = CharArraySaver) { mutableStateOf(DotsGame.solved) }
    var moves by rememberSaveable { mutableStateOf(0) }
    var seconds by rememberSaveable { mutableStateOf(0) }
    var started by rememberSaveable { mutableStateOf(false) }
    var solved by rememberSaveable { mutableStateOf(false) }
    var scrambling by remember { mutableStateOf(false) }
    var showWin by remember { mutableStateOf(false) }
    var prime by rememberSaveable { mutableStateOf(false) }
    var history by rememberSaveable(stateSaver = ListSaver) { mutableStateOf(listOf<String>()) }
    // Word from solved to the current board (cancelling pairs removed). Used by Hint.
    var stack by rememberSaveable(stateSaver = ListSaver) { mutableStateOf(listOf<String>()) }
    var levelIdx by rememberSaveable { mutableStateOf(3) }
    var activeN by rememberSaveable { mutableStateOf(24) }
    var hints by rememberSaveable { mutableStateOf(0) }
    var hintsLeft by rememberSaveable { mutableStateOf(hintBudget(24)) }
    var hintMove by rememberSaveable { mutableStateOf("") }
    // Settings (saved on the phone).
    var showSettings by remember { mutableStateOf(false) }
    var showHowTo by remember { mutableStateOf(false) }
    var themeIdx by rememberSaveable { mutableStateOf(prefs.getInt("theme_idx", 0)) }
    var glowDots by rememberSaveable { mutableStateOf(prefs.getBoolean("glow_dots", false)) }
    var oneDirection by rememberSaveable { mutableStateOf(prefs.getBoolean("one_direction", false)) }
    var showTimer by rememberSaveable { mutableStateOf(prefs.getBoolean("show_timer", true)) }
    var difficulty by rememberSaveable { mutableStateOf(prefs.getInt("difficulty", 0)) }
    val levels = remember(difficulty) { levelsFor(difficulty) }
    val dotColors = remember(themeIdx) {
        themeColors(DOT_THEMES[themeIdx.coerceIn(DOT_THEMES.indices)])
    }
    var bestTime by remember { mutableStateOf(prefs.getInt(timeKey(activeN), -1)) }
    var bestMoves by remember { mutableStateOf(prefs.getInt(movesKey(activeN), -1)) }
    val turnJob = remember { arrayOfNulls<Job>(1) }

    // Turn animation state: which circle is turning, from which board, by how much.
    var animCircle by remember { mutableStateOf<Circle?>(null) }
    var animFrom by remember { mutableStateOf(DotsGame.solved) }
    var animStep by remember { mutableStateOf(0) }
    val animProgress = remember { Animatable(0f) }

    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    // Board mapping: normalised (x, y) -> pixels. The diagram spans
    // x in [-1, 1], y in [-0.78, 0.78]; scale from the real board size so
    // it grows to fill the screen.
    val boardScale = remember(boardSize) {
        val w = boardSize.width.toFloat()
        val h = boardSize.height.toFloat()
        if (w <= 0f || h <= 0f) 1f else min(w / 2.2f, h / 1.8f)
    }
    val boardCx = boardSize.width / 2f
    val boardCy = boardSize.height / 2f
    fun toPx(nx: Float, ny: Float): Offset =
        Offset(boardCx + nx * boardScale, boardCy + ny * boardScale)

    // Timer: runs from the first move until solved (off when hidden).
    LaunchedEffect(started, solved, showTimer) {
        while (started && !solved && showTimer) {
            delay(1000)
            seconds++
        }
    }

    /** Animate one turn of [circle] by [step] slots, then commit it. */
    suspend fun animateTurn(circle: Circle, step: Int, durationMs: Int) {
        animFrom = balls
        animCircle = circle
        animStep = step
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(durationMs))
        balls = DotsGame.rotate(animFrom, circle, step)
        animCircle = null
    }

    fun afterCountedMove() {
        moves++
        if (!started) started = true
        if (DotsGame.isSolved(balls)) {
            solved = true
            // Runs that used hints don't count for the records.
            if (hints == 0) {
                if (showTimer && (bestTime < 0 || seconds < bestTime)) {
                    bestTime = seconds
                    prefs.edit().putInt(timeKey(activeN), seconds).apply()
                }
                if (bestMoves < 0 || moves < bestMoves) {
                    bestMoves = moves
                    prefs.edit().putInt(movesKey(activeN), moves).apply()
                }
            }
            showWin = true
        }
    }

    /** Tap step with the "one direction" setting: every tap turns the same way. */
    fun stepFor(circle: Circle, isPrime: Boolean): Int =
        if (oneDirection) {
            if (isPrime) -DotsGame.STEP else DotsGame.STEP
        } else {
            DotsGame.tapStep(circle, isPrime)
        }

    /** Move name for an actual step: "U" when the step is the circle's own way, "U'" otherwise. */
    fun nameForStep(circle: Circle, step: Int): String =
        circle.name + if (step == DotsGame.STEP * circle.dir) "" else "'"

    fun playMove(circle: Circle, isPrime: Boolean) {
        if (scrambling || solved || animCircle != null) return
        val step = stepFor(circle, isPrime)
        val name = nameForStep(circle, step)
        hintMove = ""
        turnJob[0] = scope.launch {
            animateTurn(circle, step, 380)
            history = history + name
            stack = DotsGame.pushMove(stack, name)
            afterCountedMove()
        }
    }

    fun tapCircle(circle: Circle) = playMove(circle, prime)

    fun undo() {
        if (scrambling || solved || animCircle != null || history.isEmpty()) return
        val inv = DotsGame.inverse(history.last())
        val (circle, isPrime) = DotsGame.parseMove(inv)
        hintMove = ""
        turnJob[0] = scope.launch {
            animateTurn(circle, DotsGame.tapStep(circle, isPrime), 250)
            history = history.dropLast(1)
            stack = DotsGame.pushMove(stack, inv)
            moves = maxOf(0, moves - 1)
        }
    }

    fun hint() {
        if (scrambling || solved || animCircle != null || stack.isEmpty() || hintsLeft <= 0) return
        val h = DotsGame.inverse(stack.last())
        hintMove = h
        val (hc, hp) = DotsGame.parseMove(h)
        // With "one direction" on, the toggle must point the way the hint needs.
        prime = if (oneDirection) DotsGame.tapStep(hc, hp) < 0 else hp
        hints++
        hintsLeft--
    }

    fun scramble() {
        if (scrambling) return
        scrambling = true
        val n = levels[levelIdx]
        turnJob[0]?.cancel()
        turnJob[0] = scope.launch {
            animCircle = null
            solved = false
            showWin = false
            moves = 0
            seconds = 0
            started = false
            history = emptyList()
            stack = emptyList()
            hintMove = ""
            hints = 0
            hintsLeft = hintBudget(n)
            activeN = n
            bestTime = prefs.getInt(timeKey(n), -1)
            bestMoves = prefs.getInt(movesKey(n), -1)
            balls = DotsGame.solved
            delay(150)
            for ((circle, isPrime) in DotsGame.scrambleMoves(n)) {
                animateTurn(circle, DotsGame.tapStep(circle, isPrime), if (n <= 6) 220 else 90)
                stack = DotsGame.pushMove(stack, DotsGame.moveName(circle, isPrime))
                delay(30)
            }
            scrambling = false
        }
    }

    fun reset() {
        if (scrambling) return
        turnJob[0]?.cancel()
        scope.launch { animProgress.snapTo(0f) }
        animCircle = null
        balls = DotsGame.solved
        moves = 0
        seconds = 0
        started = false
        solved = false
        showWin = false
        history = emptyList()
        stack = emptyList()
        hintMove = ""
        hints = 0
        hintsLeft = hintBudget(activeN)
    }

    if (showSettings) {
        SettingsScreen(
            themeIdx = themeIdx,
            onTheme = { themeIdx = it; prefs.edit().putInt("theme_idx", it).apply() },
            difficulty = difficulty,
            onDifficulty = {
                difficulty = it
                levelIdx = 0
                prefs.edit().putInt("difficulty", it).apply()
            },
            glowDots = glowDots,
            onGlowDots = { glowDots = it; prefs.edit().putBoolean("glow_dots", it).apply() },
            oneDirection = oneDirection,
            onOneDirection = { oneDirection = it; prefs.edit().putBoolean("one_direction", it).apply() },
            showTimer = showTimer,
            onShowTimer = { showTimer = it; prefs.edit().putBoolean("show_timer", it).apply() },
            onHowToPlay = { showHowTo = true },
            onDone = { showSettings = false }
        )
    } else {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rings",
                color = INK,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = INK
                )
            }
        }
        Text(
            text = "The whole Rubik's cube in 2D — tap a circle to turn it.",
            color = MUTED,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat("Moves", moves.toString())
            if (showTimer) Stat("Time", fmtTime(seconds))
            Stat(
                "Best",
                if (showTimer) {
                    if (bestTime < 0) "—" else fmtTime(bestTime)
                } else {
                    if (bestMoves < 0) "—" else bestMoves.toString()
                }
            )
            Stat("Par", activeN.toString())
        }
        Spacer(modifier = Modifier.height(4.dp))

        // The board takes all the space in the middle — the biggest thing here.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { boardSize = it }
                .pointerInput(scrambling, solved, prime, boardSize) {
                    detectTapGestures { tap ->
                        if (scrambling || solved || animCircle != null) return@detectTapGestures
                        if (boardSize.width <= 0) return@detectTapGestures
                        val nx = (tap.x - boardCx) / boardScale
                        val ny = (tap.y - boardCy) / boardScale
                        var best: Circle? = null
                        var bestD = Float.MAX_VALUE
                        for (c in DotsData.circles) {
                            val dx = nx - c.cx
                            val dy = ny - c.cy
                            val d = abs(sqrt(dx * dx + dy * dy) - c.r)
                            if (d < bestD) {
                                bestD = d
                                best = c
                            }
                        }
                        if (best != null && bestD < 0.11f) tapCircle(best)
                    }
                }
        ) {
            val live = animCircle
            val progress = animProgress.value

            // The 9 circles, grey like the video; the turning one goes white.
            for (c in DotsData.circles) {
                val centre = toPx(c.cx, c.cy)
                val radius = c.r * boardScale
                if (live == c) {
                    drawCircle(
                        color = INK,
                        radius = radius,
                        center = centre,
                        style = Stroke(width = 5f)
                    )
                } else if (hintMove.isNotEmpty() && hintMove.removeSuffix("'") == c.name) {
                    drawCircle(
                        color = ACCENT,
                        radius = radius,
                        center = centre,
                        style = Stroke(width = 5f)
                    )
                } else {
                    drawCircle(
                        color = RING_GREY,
                        radius = radius,
                        center = centre,
                        style = Stroke(width = 2.5f)
                    )
                }
            }

            // Small arrows in each circle's empty arc, showing which way
            // its dots will move when tapped. They flip with the Prime toggle.
            val arrowSize = boardScale * 0.055f
            for (c in DotsData.circles) {
                val angles = c.slots.map { DotsGame.angleOn(c, it) }.sorted()
                var bestGap = -1f
                var bestMid = 0f
                for (i in angles.indices) {
                    val a0 = angles[i]
                    val a1 = if (i == angles.lastIndex) angles[0] + 360f else angles[i + 1]
                    val gap = a1 - a0
                    if (gap > bestGap) {
                        bestGap = gap
                        bestMid = a0 + gap / 2f
                    }
                }
                // Only draw where there is real empty space.
                if (bestGap < 40f) continue
                val mid = Math.toRadians(bestMid.toDouble())
                val centre = toPx(c.cx, c.cy)
                val radius = c.r * boardScale
                val px = centre.x + radius * cos(mid).toFloat()
                val py = centre.y + radius * sin(mid).toFloat()
                // Travel direction: a forward tap moves dots toward
                // increasing slot index (increasing angle); prime reverses it.
                val step = stepFor(c, prime)
                val sgn = if (step > 0) 1f else -1f
                // Tangent at this point, in the travel direction.
                val tx = -sin(mid).toFloat() * sgn
                val ty = cos(mid).toFloat() * sgn
                val tip = Offset(px + tx * arrowSize, py + ty * arrowSize)
                val base = Offset(px - tx * arrowSize * 0.7f, py - ty * arrowSize * 0.7f)
                val perp = Offset(-ty, tx)
                val b1 = Offset(
                    base.x + perp.x * arrowSize * 0.6f,
                    base.y + perp.y * arrowSize * 0.6f
                )
                val b2 = Offset(
                    base.x - perp.x * arrowSize * 0.6f,
                    base.y - perp.y * arrowSize * 0.6f
                )
                drawPath(
                    path = Path().apply {
                        moveTo(tip.x, tip.y)
                        lineTo(b1.x, b1.y)
                        lineTo(b2.x, b2.y)
                        close()
                    },
                    color = ARROW
                )
            }

            // Dots.
            val dotR = boardScale * 0.05f
            for (slot in 0 until DotsGame.SLOTS) {
                val pos: Offset
                val colour: Color
                if (live != null && live.slots.contains(slot)) {
                    val centre = toPx(live.cx, live.cy)
                    val radius = live.r * boardScale
                    val a0 = Math.toRadians(DotsGame.angleOn(live, slot).toDouble())
                    val delta = Math.toRadians(DotsGame.arcDelta(live, slot, animStep).toDouble())
                    val a = a0 + progress * delta
                    pos = Offset(
                        centre.x + radius * cos(a).toFloat(),
                        centre.y + radius * sin(a).toFloat()
                    )
                    colour = dotColors.getValue(animFrom[slot])
                } else {
                    val (nx, ny) = DotsData.slots[slot]
                    pos = toPx(nx, ny)
                    colour = dotColors.getValue(balls[slot])
                }
                if (glowDots) {
                    drawCircle(color = colour.copy(alpha = 0.18f), radius = dotR * 1.9f, center = pos)
                    drawCircle(color = colour.copy(alpha = 0.30f), radius = dotR * 1.45f, center = pos)
                }
                drawCircle(color = colour, radius = dotR, center = pos)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = dotR,
                    center = pos,
                    style = Stroke(width = 2f)
                )
            }
        }

        // Move history, like the video's strip at the bottom.
        Text(
            text = when {
                hintMove.isNotEmpty() -> "Hint: tap ${hintMove.replace("'", "′")} (highlighted)"
                history.isEmpty() -> "Tap a circle — its name joins the line below."
                else -> history.takeLast(14).joinToString("  ")
            },
            color = if (hintMove.isNotEmpty()) ACCENT else if (history.isEmpty()) MUTED else INK,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Undo / Hint / Level.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { undo() },
                enabled = !scrambling && !solved && history.isNotEmpty() && animCircle == null,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) { Text(text = "Undo", color = INK, fontSize = 13.sp, maxLines = 1) }
            OutlinedButton(
                onClick = { hint() },
                enabled = !scrambling && !solved && stack.isNotEmpty() && hintsLeft > 0 && animCircle == null,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) { Text(text = "Hint ($hintsLeft)", color = INK, fontSize = 13.sp, maxLines = 1) }
            OutlinedButton(
                onClick = { levelIdx = (levelIdx + 1) % levels.size },
                enabled = !scrambling,
                modifier = Modifier.weight(1.4f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) { Text(text = "Level: ${levels[levelIdx]} turns", color = INK, fontSize = 13.sp, maxLines = 1) }
        }
        Spacer(modifier = Modifier.height(6.dp))

        // All controls in one line at the bottom.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { prime = false },
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 4.dp, vertical = 8.dp
                )
            ) {
                Text(
                    text = "Forward",
                    color = if (!prime) ACCENT else INK,
                    fontSize = 13.sp,
                    fontWeight = if (!prime) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
            OutlinedButton(
                onClick = { prime = true },
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 4.dp, vertical = 8.dp
                )
            ) {
                Text(
                    text = "Prime ′",
                    color = if (prime) ACCENT else INK,
                    fontSize = 13.sp,
                    fontWeight = if (prime) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
            Button(
                onClick = { scramble() },
                enabled = !scrambling,
                modifier = Modifier.weight(1.2f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 4.dp, vertical = 8.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ACCENT,
                    contentColor = Color.Black,
                    disabledContainerColor = ACCENT.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = if (scrambling) "Shuffling…" else "Scramble",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            OutlinedButton(
                onClick = { reset() },
                enabled = !scrambling,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 4.dp, vertical = 8.dp
                )
            ) {
                Text(text = "Reset", color = INK, fontSize = 13.sp, maxLines = 1)
            }
        }
        TextButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/iopXtiop_bot"))
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Connect with", color = MUTED, fontSize = 13.sp)
        }
    }
    }

    if (showHowTo) {
        HowToPlayDialog(onClose = { showHowTo = false })
    }

    if (showWin && !showSettings) {
        AlertDialog(
            onDismissRequest = { showWin = false },
            title = { Text(text = "Solved!") },
            text = {
                Text(
                    text = "The whole cube, back in order.\n" +
                        (if (showTimer) "Time ${fmtTime(seconds)}  ·  " else "") +
                        "$moves moves  ·  par $activeN\n" +
                        (if (hints > 0) "Hints used: $hints (not counted for best)\n" else "") +
                        (if (showTimer) "Best ${if (bestTime < 0) "—" else fmtTime(bestTime)}  ·  " else "") +
                        "${if (bestMoves < 0) "—" else "$bestMoves moves"}"
                )
            },
            confirmButton = {
                TextButton(onClick = { showWin = false; scramble() }) {
                    Text(text = "Play again")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWin = false }) {
                    Text(text = "Keep looking")
                }
            }
        )
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = MUTED, fontSize = 12.sp)
        Text(text = value, color = INK, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
