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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val S_BG = Color(0xFF07080A)
private val S_SURFACE = Color(0xFF13151A)
private val S_INK = Color(0xFFF8F9FA)
private val S_MUTED = Color(0xFF8B949E)
private val S_ACCENT = Color(0xFFFFB703)
private val S_BORDER = Color(0xFF21262D)

data class DotTheme(val name: String, val colors: List<Color>)

val GROUP_KEYS = listOf('R', 'Y', 'B', 'G', 'O', 'W')

val DOT_THEMES = listOf(
    DotTheme(
        "Classic Amber",
        listOf(
            Color(0xFFFF3366), Color(0xFFFFD700), Color(0xFF00B4D8),
            Color(0xFF2EC4B6), Color(0xFFFF9F1C), Color(0xFFFFFFFF)
        )
    ),
    DotTheme(
        "Cyberpunk Neon",
        listOf(
            Color(0xFFFF007F), Color(0xFF39FF14), Color(0xFF00FFFF),
            Color(0xFFFFFF00), Color(0xFFFF5E00), Color(0xFFBD00FF)
        )
    ),
    DotTheme(
        "Sunset Glow",
        listOf(
            Color(0xFFE63946), Color(0xFFF4A261), Color(0xFFE9C46A),
            Color(0xFF2A9D8F), Color(0xFF264653), Color(0xFFFFF1E6)
        )
    ),
    DotTheme(
        "Emerald Deep",
        listOf(
            Color(0xFF2D6A4F), Color(0xFF52B788), Color(0xFF74C69D),
            Color(0xFFB7E4C7), Color(0xFFD8F3DC), Color(0xFF1B4332)
        )
    ),
    DotTheme(
        "Royal Amethyst",
        listOf(
            Color(0xFF7B2CBF), Color(0xFF9D4EDD), Color(0xFFC77DFF),
            Color(0xFFE0AAFF), Color(0xFF5A189A), Color(0xFFF8F9FA)
        )
    ),
    DotTheme(
        "Arctic Frost",
        listOf(
            Color(0xFF03045E), Color(0xFF0077B6), Color(0xFF00B4D8),
            Color(0xFF90E0EF), Color(0xFFADE8F4), Color(0xFFFFFFFF)
        )
    ),
    DotTheme(
        "Ember Dark",
        listOf(
            Color(0xFF370617), Color(0xFF6A040F), Color(0xFF9D0208),
            Color(0xFFD00000), Color(0xFFDC2F02), Color(0xFFFFF3B0)
        )
    ),
    DotTheme(
        "Pastel Dream",
        listOf(
            Color(0xFFFFB5A7), Color(0xFFFCD5CE), Color(0xFFF8EDEB),
            Color(0xFFF9DCC4), Color(0xFFFEC89A), Color(0xFFE8D1FC)
        )
    )
)

fun themeColors(theme: DotTheme): Map<Char, Color> =
    GROUP_KEYS.zip(theme.colors).toMap()

val DIFFICULTIES = listOf("Beginner", "Smart", "Pro")

fun levelsFor(difficulty: Int): List<Int> = when (difficulty) {
    1 -> listOf(6, 12, 18, 30)
    2 -> listOf(12, 18, 24, 36)
    else -> listOf(3, 6, 12, 24)
}

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
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Settings",
                    color = S_INK,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Customize your puzzle experience",
                    color = S_MUTED,
                    fontSize = 13.sp
                )
            }
            TextButton(
                onClick = onDone,
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(listOf(Color(0xFFFFB703), Color(0xFFFF9F1C))),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(text = "Done", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle("Color Theme")
        Spacer(modifier = Modifier.height(6.dp))
        for ((idx, theme) in DOT_THEMES.withIndex()) {
            val selected = idx == themeIdx
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .background(S_SURFACE, RoundedCornerShape(14.dp))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) S_ACCENT else S_BORDER,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onTheme(idx) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = theme.name,
                    color = S_INK,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in theme.colors) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        )
                    }
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
        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle("Difficulty Level")
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for ((idx, name) in DIFFICULTIES.withIndex()) {
                val selected = idx == difficulty
                OutlinedButton(
                    onClick = { onDifficulty(idx) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) S_ACCENT.copy(alpha = 0.15f) else S_SURFACE,
                        contentColor = if (selected) S_ACCENT else S_INK
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) S_ACCENT else S_BORDER
                    )
                ) {
                    Text(
                        text = name,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
        Text(
            text = "Beginner 3·6·12·24 turns · Smart 6·12·18·30 · Pro 12·18·24·36",
            color = S_MUTED,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle("Gameplay Preferences")
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(S_SURFACE, RoundedCornerShape(16.dp))
                .border(1.dp, S_BORDER, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Column {
                ToggleRow(
                    title = "Neon Glow Dots",
                    subtitle = "Enhances dots with an atmospheric aura",
                    checked = glowDots,
                    onChecked = onGlowDots
                )
                ToggleRow(
                    title = "Unified Ring Direction",
                    subtitle = "Every tap rotates in the same default direction",
                    checked = oneDirection,
                    onChecked = onOneDirection
                )
                ToggleRow(
                    title = "Show Game Timer",
                    subtitle = "Display elapsed time during puzzles",
                    checked = showTimer,
                    onChecked = onShowTimer
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle("Help & Info")
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(S_SURFACE, RoundedCornerShape(14.dp))
                .border(1.dp, S_BORDER, RoundedCornerShape(14.dp))
                .clickable { onHowToPlay() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "How to Play & Guide", color = S_INK, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Rules, notation, and strategy tips",
                    color = S_MUTED,
                    fontSize = 12.sp
                )
            }
            Text(text = "›", color = S_ACCENT, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Hints per game: 3 turns → 1 hint · 6 → 2 · 12 → 5 · 24 → 10",
            color = S_MUTED,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 20.dp)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = S_ACCENT,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
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
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = S_INK, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = S_MUTED, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = S_ACCENT,
                uncheckedThumbColor = S_INK,
                uncheckedTrackColor = Color(0xFF2A2E39)
            )
        )
    }
}

@Composable
fun HowToPlayDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Color(0xFF13151A),
        titleContentColor = Color(0xFFFFB703),
        textContentColor = Color(0xFFF8F9FA),
        title = { Text(text = "How to Play Rings", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                HowToText("Goal: Sort all 54 dots back into their original 6 color clusters to solve the 2D Rubik's Cube.")
                HowToText("Tap any ring to rotate its 12 dots by 3 slots (a quarter turn). Direction arrows on each ring show the rotation path.")
                HowToText("Forward & Prime (′): Toggle between clockwise and counter-clockwise rotation directions.")
                HowToText("Undo & Hint: Undo takes back your last move. Hint highlights the optimal next move toward the solved state in glowing gold.")
                HowToText("Scramble & Levels: Choose difficulty and turn count (Par) to test your puzzle-solving mastery.")
                HowToText("Cube Notation: U (Up), D (Down), F (Front), B (Back), L (Left), R (Right), M (Middle), E (Equator), S (Standing).")
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(text = "Got it", color = Color(0xFFFFB703), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun HowToText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color(0xFFC9D1D9),
        modifier = Modifier.padding(bottom = 10.dp)
    )
}
