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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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

private val BG = Color(0xFF07080A)
private val SURFACE = Color(0xFF13151A)
private val BORDER_COL = Color(0xFF21262D)
private val INK = Color(0xFFF8F9FA)
private val MUTED = Color(0xFF8B949E)
private val RING_GREY = Color(0xFF30363D)
private val RING_ACTIVE = Color(0xFF58A6FF)
private val ACCENT = Color(0xFFFFB703)
private val ARROW = Color(0xFF8B949E)

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
    var stack by rememberSaveable(stateSaver = ListSaver) { mutableStateOf(listOf<String>()) }
    var levelIdx by rememberSaveable { mutableStateOf(3) }
    var activeN by rememberSaveable { mutableStateOf(24) }
    var hints by rememberSaveable { mutableStateOf(0) }
    var hintsLeft by rememberSaveable { mutableStateOf(hintBudget(24)) }
    var hintMove by rememberSaveable { mutableStateOf("") }

    var showSettings by remember { mutableStateOf(false) }
    var showHowTo by remember { mutableStateOf(false) }
    var themeIdx by rememberSaveable { mutableStateOf(prefs.getInt("theme_idx", 0)) }
    var glowDots by rememberSaveable { mutableStateOf(prefs.getBoolean("glow_dots", true)) }
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

    var animCircle by remember { mutableStateOf<Circle?>(null) }
    var animFrom by remember { mutableStateOf(DotsGame.solved) }
    var animStep by remember { mutableStateOf(0) }
    val animProgress = remember { Animatable(0f) }

    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val boardScale = remember(boardSize) {
        val w = boardSize.width.toFloat()
        val h = boardSize.height.toFloat()
        if (w <= 0f || h <= 0f) 1f else min(w / 2.2f, h / 1.8f)
    }
    val boardCx = boardSize.width / 2f
    val boardCy = boardSize.height / 2f
    fun toPx(nx: Float, ny: Float): Offset =
        Offset(boardCx + nx * boardScale, boardCy + ny * boardScale)

    LaunchedEffect(started, solved, showTimer) {
        while (started && !solved && showTimer) {
            delay(1000)
            seconds++
        }
    }

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

    fun stepFor(circle: Circle, isPrime: Boolean): Int =
        if (oneDirection) {
            if (isPrime) -DotsGame.STEP else DotsGame.STEP
        } else {
            DotsGame.tapStep(circle, isPrime)
        }

    fun nameForStep(circle: Circle, step: Int): String =
        circle.name + if (step == DotsGame.STEP * circle.dir) "" else "'"

    fun playMove(circle: Circle, isPrime: Boolean) {
        if (scrambling || solved || animCircle != null) return
        val step = stepFor(circle, isPrime)
        val name = nameForStep(circle, step)
        hintMove = ""
        turnJob[0] = scope.launch {
            animateTurn(circle, step, 350)
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
            animateTurn(circle, DotsGame.tapStep(circle, isPrime), 220)
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
            delay(120)
            for ((circle, isPrime) in DotsGame.scrambleMoves(n)) {
                animateTurn(circle, DotsGame.tapStep(circle, isPrime), if (n <= 6) 200 else 80)
                stack = DotsGame.pushMove(stack, DotsGame.moveName(circle, isPrime))
                delay(25)
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RINGS",
                        color = INK,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "The 2D Rubik's Cube Puzzle",
                        color = MUTED,
                        fontSize = 12.sp
                    )
                }
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier
                        .background(SURFACE, RoundedCornerShape(12.dp))
                        .border(1.dp, BORDER_COL, RoundedCornerShape(12.dp))
                        .size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = ACCENT
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Stats Card Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SURFACE, RoundedCornerShape(16.dp))
                    .border(1.dp, BORDER_COL, RoundedCornerShape(16.dp))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
            Spacer(modifier = Modifier.height(10.dp))

            // Game Canvas Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SURFACE, RoundedCornerShape(20.dp))
                    .border(1.dp, BORDER_COL, RoundedCornerShape(20.dp))
                    .padding(8.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
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
                                if (best != null && bestD < 0.12f) tapCircle(best)
                            }
                        }
                ) {
                    val live = animCircle
                    val progress = animProgress.value

                    // Draw 9 circles
                    for (c in DotsData.circles) {
                        val centre = toPx(c.cx, c.cy)
                        val radius = c.r * boardScale
                        if (live == c) {
                            drawCircle(
                                color = RING_ACTIVE,
                                radius = radius,
                                center = centre,
                                style = Stroke(width = 6f)
                            )
                        } else if (hintMove.isNotEmpty() && hintMove.removeSuffix("'") == c.name) {
                            drawCircle(
                                color = ACCENT,
                                radius = radius,
                                center = centre,
                                style = Stroke(width = 6f)
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

                    // Draw direction arrows
                    val arrowSize = boardScale * 0.05f
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
                        if (bestGap < 40f) continue
                        val mid = Math.toRadians(bestMid.toDouble())
                        val centre = toPx(c.cx, c.cy)
                        val radius = c.r * boardScale
                        val px = centre.x + radius * cos(mid).toFloat()
                        val py = centre.y + radius * sin(mid).toFloat()
                        val step = stepFor(c, prime)
                        val sgn = if (step > 0) 1f else -1f
                        val tx = -sin(mid).toFloat() * sgn
                        val ty = cos(mid).toFloat() * sgn
                        val tip = Offset(px + tx * arrowSize, py + ty * arrowSize)
                        val base = Offset(px - tx * arrowSize * 0.7f, py - ty * arrowSize * 0.7f)
                        val perp = Offset(-ty, tx)
                        val b1 = Offset(base.x + perp.x * arrowSize * 0.6f, base.y + perp.y * arrowSize * 0.6f)
                        val b2 = Offset(base.x - perp.x * arrowSize * 0.6f, base.y - perp.y * arrowSize * 0.6f)
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

                    // Draw 54 dots
                    val dotR = boardScale * 0.048f
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
                            drawCircle(color = colour.copy(alpha = 0.22f), radius = dotR * 1.85f, center = pos)
                            drawCircle(color = colour.copy(alpha = 0.35f), radius = dotR * 1.35f, center = pos)
                        }
                        drawCircle(color = colour, radius = dotR, center = pos)
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.45f),
                            radius = dotR,
                            center = pos,
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Move History Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SURFACE, RoundedCornerShape(12.dp))
                    .border(1.dp, BORDER_COL, RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        hintMove.isNotEmpty() -> "Hint: Tap ${hintMove.replace("'", "′")} (highlighted)"
                        history.isEmpty() -> "Tap any circle to begin solving"
                        else -> history.takeLast(12).joinToString("  ")
                    },
                    color = if (hintMove.isNotEmpty()) ACCENT else if (history.isEmpty()) MUTED else INK,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Utility Row (Undo / Hint / Level)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { undo() },
                    enabled = !scrambling && !solved && history.isNotEmpty() && animCircle == null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = INK),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BORDER_COL)
                ) { Text(text = "Undo", fontSize = 13.sp, maxLines = 1) }

                OutlinedButton(
                    onClick = { hint() },
                    enabled = !scrambling && !solved && stack.isNotEmpty() && hintsLeft > 0 && animCircle == null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = INK),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BORDER_COL)
                ) { Text(text = "Hint ($hintsLeft)", fontSize = 13.sp, maxLines = 1) }

                OutlinedButton(
                    onClick = { levelIdx = (levelIdx + 1) % levels.size },
                    enabled = !scrambling,
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = INK),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BORDER_COL)
                ) { Text(text = "Lvl: ${levels[levelIdx]}", fontSize = 13.sp, maxLines = 1) }
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons Row (Forward / Prime / Scramble / Reset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { prime = false },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (!prime) ACCENT.copy(alpha = 0.15f) else SURFACE,
                        contentColor = if (!prime) ACCENT else INK
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (!prime) ACCENT else BORDER_COL
                    )
                ) {
                    Text(text = "Fwd", fontSize = 13.sp, fontWeight = if (!prime) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { prime = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (prime) ACCENT.copy(alpha = 0.15f) else SURFACE,
                        contentColor = if (prime) ACCENT else INK
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (prime) ACCENT else BORDER_COL
                    )
                ) {
                    Text(text = "Prime ′", fontSize = 13.sp, fontWeight = if (prime) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                }
                Button(
                    onClick = { scramble() },
                    enabled = !scrambling,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
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
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = INK),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BORDER_COL)
                ) { Text(text = "Reset", fontSize = 13.sp, maxLines = 1) }
            }

            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/iopXtiop_bot"))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Connect with Creator", color = MUTED, fontSize = 12.sp)
            }
        }
    }

    if (showHowTo) {
        HowToPlayDialog(onClose = { showHowTo = false })
    }

    if (showWin && !showSettings) {
        AlertDialog(
            onDismissRequest = { showWin = false },
            containerColor = SURFACE,
            titleContentColor = ACCENT,
            textContentColor = INK,
            title = { Text(text = "🎉 Puzzle Solved!", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
            text = {
                Text(
                    text = "Magnificent! The entire 2D cube is back in order.\n\n" +
                        (if (showTimer) "⏱ Time: ${fmtTime(seconds)}\n" else "") +
                        "🔄 Moves: $moves (Par: $activeN)\n" +
                        (if (hints > 0) "💡 Hints used: $hints\n" else "") +
                        (if (showTimer) "🏆 Best Time: ${if (bestTime < 0) "—" else fmtTime(bestTime)}\n" else "") +
                        "🌟 Best Moves: ${if (bestMoves < 0) "—" else bestMoves.toString()}",
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showWin = false; scramble() },
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Play Again", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWin = false }) {
                    Text(text = "View Board", color = MUTED)
                }
            }
        )
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = MUTED, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = INK, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
