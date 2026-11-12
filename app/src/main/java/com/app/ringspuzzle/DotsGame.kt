package com.app.ringspuzzle

import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.random.Random

/**
 * Dots diagram — the whole Rubik's cube in 2D, as in the
 * "Rubik's Cube as Permutations" video.
 *
 * 54 dots on 9 circles (3 centres x 3 radii). Every dot sits on exactly
 * 2 circles. A move taps one circle and rotates its 12 dots by 3 slots
 * (a quarter turn), the way the video's highlighted circle does.
 * Solved = every dot back in its home colour cluster, exactly the
 * video's final frame.
 */
object DotsGame {

    const val SLOTS = 54
    const val STEP = 3 // slots per quarter turn

    val COLORS: Map<Char, Color> = mapOf(
        'R' to Color(0xFFEF343B),
        'Y' to Color(0xFFFCD415),
        'B' to Color(0xFF1581FD),
        'G' to Color(0xFF10A657),
        'O' to Color(0xFFFC700E),
        'W' to Color(0xFFEFEEEE)
    )

    val solved: CharArray get() = DotsData.solved.toCharArray()

    /** Angle (degrees) of a slot as seen from a circle's centre. */
    fun angleOn(circle: Circle, slot: Int): Float {
        val (x, y) = DotsData.slots[slot]
        return Math.toDegrees(atan2((y - circle.cy).toDouble(), (x - circle.cx).toDouble())).toFloat()
    }

    /** Signed arc (degrees) a dot travels when its circle turns by [step] slots. */
    fun arcDelta(circle: Circle, fromSlot: Int, step: Int): Float {
        val order = circle.slots
        val fromIdx = order.indexOf(fromSlot)
        val toIdx = (fromIdx + step).mod(12)
        val toSlot = order[toIdx]
        var d = angleOn(circle, toSlot) - angleOn(circle, fromSlot)
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        // Travel in the direction of the turn.
        if (step > 0 && d < 0) d += 360f
        if (step < 0 && d > 0) d -= 360f
        return d
    }

    /** Rotate a circle's dots by [step] slots (+3 forward, -3 prime). */
    fun rotate(state: CharArray, circle: Circle, step: Int): CharArray {
        val next = state.copyOf()
        val order = circle.slots
        for (k in order.indices) {
            next[order[(k + step).mod(12)]] = state[order[k]]
        }
        return next
    }

    /** Forward tap = the move as written in the video; prime = the opposite. */
    fun tapStep(circle: Circle, prime: Boolean): Int {
        val s = STEP * circle.dir
        return if (prime) -s else s
    }

    fun moveName(circle: Circle, prime: Boolean): String =
        circle.name + if (prime) "'" else ""

    fun isSolved(state: CharArray): Boolean = state.contentEquals(solved)

    /** The opposite move: U <-> U'. */
    fun inverse(name: String): String =
        if (name.endsWith("'")) name.dropLast(1) else "$name'"

    /** "U'" -> (circle U, prime = true). */
    fun parseMove(name: String): Pair<Circle, Boolean> {
        val prime = name.endsWith("'")
        val base = name.removeSuffix("'")
        return DotsData.circles.first { it.name == base } to prime
    }

    /**
     * Keeps a running "how to get from solved to here" word.
     * A move that cancels the previous one removes it instead of piling up.
     */
    fun pushMove(stack: List<String>, name: String): List<String> =
        if (stack.isNotEmpty() && stack.last() == inverse(name)) stack.dropLast(1)
        else stack + name

    /**
     * Random turns for scrambling. Never turns the same circle twice in a row
     * (so moves can't cancel or stack up) and never ends on a solved board.
     */
    fun scrambleMoves(n: Int): List<Pair<Circle, Boolean>> {
        val rnd = Random.Default
        while (true) {
            val out = ArrayList<Pair<Circle, Boolean>>(n)
            var prevCircle: Circle? = null
            while (out.size < n) {
                val c = DotsData.circles[rnd.nextInt(DotsData.circles.size)]
                if (c === prevCircle) continue
                out += c to rnd.nextBoolean()
                prevCircle = c
            }
            var s = solved
            for ((c, p) in out) s = rotate(s, c, tapStep(c, p))
            if (!isSolved(s)) return out
        }
    }
}
