package br.com.roxou.mobilidade.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object RoxouColors {
    val Purple = Color(0xFF8E44FF)
    val PurpleSoft = Color(0xFFB380FF)
    val Background = Color(0xFF121016)
    val SurfaceCard = Color(0xFF1E1A26)
    val CardBorder = Color(0xFF2B2538)
    val Track = Color(0xFF2D273B)
    val Green = Color(0xFF2ECC71)
    val Yellow = Color(0xFFF1C40F)
    val Red = Color(0xFFE74C3C)
    val TextPrimary = Color(0xFFF7F5FA)
    val TextSecondary = Color(0xFFA39BB0)
    val TextMuted = Color(0xFF7B728A)
}

private val RoxouDarkColorScheme = darkColorScheme(
    primary = RoxouColors.Purple,
    onPrimary = Color.White,
    secondary = RoxouColors.PurpleSoft,
    onSecondary = RoxouColors.Background,
    tertiary = RoxouColors.Green,
    onTertiary = RoxouColors.Background,
    background = RoxouColors.Background,
    onBackground = RoxouColors.TextPrimary,
    surface = RoxouColors.SurfaceCard,
    onSurface = RoxouColors.TextPrimary,
    surfaceVariant = RoxouColors.Track,
    onSurfaceVariant = RoxouColors.TextSecondary,
    outline = RoxouColors.CardBorder,
    error = RoxouColors.Red,
    onError = Color.White,
)

private val RoxouTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 72.sp,
        lineHeight = 76.sp,
        letterSpacing = (-2).sp,
        fontFeatureSettings = "tnum",
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
        fontFeatureSettings = "tnum",
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = "tnum",
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp,
    ),
)

private val RoxouShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun RoxouDesignTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RoxouDarkColorScheme,
        typography = RoxouTypography,
        shapes = RoxouShapes,
        content = content,
    )
}
