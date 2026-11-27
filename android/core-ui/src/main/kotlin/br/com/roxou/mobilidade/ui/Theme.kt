package br.com.roxou.mobilidade.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Roxo do Roxou, tema escuro (o app é usado dirigindo, muitas vezes à noite). */
val RoxouPurple = Color(0xFF8E44FF)
val RoxouGreen = Color(0xFF2ECC71)
val RoxouYellow = Color(0xFFF1C40F)
val RoxouRed = Color(0xFFE74C3C)

private val DarkColors = darkColorScheme(
    primary = RoxouPurple,
    secondary = Color(0xFFB98CFF),
    background = Color(0xFF121016),
    surface = Color(0xFF1C1922),
    surfaceVariant = Color(0xFF2A2531),
    error = RoxouRed,
)

@Composable
fun RoxouTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
