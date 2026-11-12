package com.app.ringspuzzle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val S_BG = Color(0xFF0A0A0A)
private val S_INK = Color(0xFFF2F2F2)
private val S_MUTED = Color(0xFF9A9A9A)
private val S_ACCENT = Color(0xFFFFD500)

/** One dot-colour theme: a colour for each of the 6 groups (R Y B G O W). */
data class DotTheme(val name: String, val colors: List<Color>)

val GROUP_KEYS = listOf('R', 'Y', 'B', 'G', 'O', 'W')

/**
 * Every theme has 6 colours that are easy to tell apart, so the groups
 * pop out from each other on the black board.
 */
val DOT_THEMES = listOf(
    DotTheme(
        "Classic",
        listOf(
            Color(0xFFEF343B), Color(0xFFFCD415), Color(0xFF1581FD),
            Color(0xFF10A657), Color(0xFFFC700E), Color(0xFFEFEEEE)
        )
    ),
    DotTheme(
        "Pink",
        listOf(
            Color(0xFFFF4D8D), Color(0xFFFFB6D5), Color(0xFFC724B1),
            Color(0xFFFF4D6D), Color(0xFFA55CFF), Color(0xFFFFFFFF)
        )
    ),
    DotTheme(
        "Lemon",
        listOf(
            Color(0xFFFFF200), Color(0xFF7ED957), Color(0xFF00A651),
            Color(0xFF00B8A9), Color(0xFFFFB800), Color(0xFFFFFDD0)
        )
    ),
    DotTheme(
        "Earth",
        listOf(
            Color(0xFF4B2E1A), Color(0xFF8D5524), Color(0xFFB7410E),
            Color(0xFFC68A3C), Color(0xFFD2A679), Color(0xFFF5E6CC)
        )
    ),
    DotTheme(
        "Ocean",
        listOf(
            Color(0xFF283593), Color(0xFF1565FF), Color(0xFF00BCD4),
            Color(0xFF00897B), Color(0xFF4FC3F7), Color(0xFFB2EBF2)
        )
    ),
    DotTheme(
        "Sunset",
        listOf(
            Color(0xFFE53935), Color(0xFFFF6D00), Color(0xFFFFB300),
            Color(0xFFEC407A), Color(0xFF7E57C2), Color(0xFFFFAB91)
        )
    ),
    DotTheme(
        "Neon",
        listOf(
            Color(0xFFFF00AA), Color(0xFF39FF14), Color(0xFF00FFFF),
            Color(0xFFFFFF00), Color(0xFFFF6F00), Color(0xFFBF00FF)
        )
    ),
    DotTheme(
        "Pastel",
        listOf(
            Color(0xFFFFB6C1), Color(0xFFAEC6CF), Color(0xFFFDFD96),
            Color(0xFF77DD77), Color(0xFFCBC3E3), Color(0xFFFFDAB9)
        )
    )
)

fun themeColors(theme: DotTheme): Map<Char, Color> =
    GROUP_KEYS.zip(theme.colors).toMap()

val DIFFICULTIES = listOf("Beginner", "Smart", "Pro")

/** Scramble sizes per difficulty — each step up is a harder shuffle. */
fun levelsFor(difficulty: Int): List<Int> = when (difficulty) {
    1 -> listOf(6, 12, 18, 30)
    2 -> listOf(12, 18, 24, 36)
    else -> listOf(3, 6, 12, 24)
}

/** Hints allowed for a scramble of [n] turns: 3->1, 6->2, 12->5, 24->10. */
fun hintBudget(n: Int): Int = maxOf(1, (n * 0.4f).roundToInt())

@Composable
fun SettingsScreen(
    themeIdx: Int,
    onTheme: (Int) -> Unit,
    difficulty: Int,
    onDifficulty: (Int) -> Unit,
    glowDots: Boolean,
    onGlowDots: (Boolean) -> Unit,
    oneDirection: Boolean,
    onOneDirection: (Boolean) -> Unit,
    showTimer: Boolean,
    onShowTimer: (Boolean) -> Unit,
    onHowToPlay: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(S_BG)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Settings",
                color = S_INK,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDone) {
                Text(text = "Done", color = S_ACCENT, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        SectionTitle("Dot colours")
        Text(
            text = "Each theme gives the 6 dot groups colours that pop out from each other.",
            color = S_MUTED,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        for ((idx, theme) in DOT_THEMES.withIndex()) {
            val selected = idx == themeIdx
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) S_ACCENT else Color(0xFF2A2A2A),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onTheme(idx) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = theme.name,
                    color = S_INK,
                    fontSize = 16.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                for (c in theme.colors) {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    )
                }
                if (selected) {
                    Text(
                        text = "  ✓",
                        color = S_ACCENT,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        SectionTitle("Difficulty")
        Text(
            text = "Each step up shuffles harder.",
            color = S_MUTED,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for ((idx, name) in DIFFICULTIES.withIndex()) {
                val selected = idx == difficulty
                OutlinedButton(
                    onClick = { onDifficulty(idx) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = name,
                        color = if (selected) S_ACCENT else S_INK,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
        Text(
            text = "Beginner 3 · 6 · 12 · 24 turns — Smart 6 · 12 · 18 · 30 — Pro 12 · 18 · 24 · 36",
            color = S_MUTED,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        SectionTitle("Game")
        ToggleRow(
            title = "Glow dots",
            subtitle = "Every dot glows",
            checked = glowDots,
            onChecked = onGlowDots
        )
        ToggleRow(
            title = "All rings turn one way",
            subtitle = "Every tap turns the same way — Forward / Prime still flips it",
            checked = oneDirection,
            onChecked = onOneDirection
        )
        ToggleRow(
            title = "Show timer",
            subtitle = "Turn off to play without the clock",
            checked = showTimer,
            onChecked = onShowTimer
        )
        Spacer(modifier = Modifier.height(16.dp))

        SectionTitle("Learn")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onHowToPlay() }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "How to play", color = S_INK, fontSize = 16.sp)
                Text(
                    text = "The rules, and what every move letter means",
                    color = S_MUTED,
                    fontSize = 13.sp
                )
            }
            Text(text = "›", color = S_MUTED, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Hints per game: 3 turns → 1 hint · 6 → 2 · 12 → 5 · 24 → 10",
            color = S_MUTED,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = S_ACCENT,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = S_INK, fontSize = 16.sp)
            Text(text = subtitle, color = S_MUTED, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = S_ACCENT,
                uncheckedThumbColor = S_INK,
                uncheckedTrackColor = Color(0xFF3A3A3A)
            )
        )
    }
}

@Composable
fun HowToPlayDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(text = "How to play") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                HowToText("Goal: sort all 54 dots back into their colour groups. When every group sits together, you win.")
                HowToText("Tap a circle to turn it. Its 12 dots move 3 slots along the ring. Forward turns it one way, Prime ′ turns it the other way. The small arrows on each circle show which way its dots will go.")
                HowToText("Scramble shuffles the dots. Level picks how many turns the shuffle is. Undo takes back your last move. Hint shows the next move back toward solved — its circle lights up yellow.")
                HowToText("Hints are limited: 1 hint on a 3-turn game, 2 on 6, 5 on 12, 10 on 24. A run that uses a hint doesn't count for best time or moves.")
                HowToText("The move names — each circle is one move of the cube:")
                HowToText("U — Up, the top face")
                HowToText("D — Down, the bottom face")
                HowToText("F — Front, the front face")
                HowToText("B — Back, the back face")
                HowToText("L — Left, the left face")
                HowToText("R — Right, the right face")
                HowToText("M — Middle, the slice between Left and Right")
                HowToText("E — Equator, the slice between Up and Down")
                HowToText("S — Standing, the slice between Front and Back")
                HowToText("′ — Prime: the same circle turns the opposite way.")
                HowToText("Example scramble: L D′ F′ S′ B′ R′ M′ L′ U′ E′ D′ M′ M′ B′ — read left to right, one turn per letter.")
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text(text = "Got it") }
        }
    )
}

@Composable
private fun HowToText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
