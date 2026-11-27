package br.com.roxou.mobilidade.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

object RoxouColors {
    val Purple = Color(0xFF8E44FF)
    val PurpleSoft = Color(0xFFB380FF)
    val PurpleSurface = Color(0xFF261C3B)
    val Background = Color(0xFF121016)
    val SurfaceCard = Color(0xFF1E1A26)
    val SurfaceElevated = Color(0xFF262131)
    val CardBorder = Color(0xFF2C263A)
    val Divider = Color(0xFF2A2436)
    val Track = Color(0xFF2E283D)
    val Green = Color(0xFF2ECC71)
    val Yellow = Color(0xFFF1C40F)
    val Red = Color(0xFFE74C3C)
    val TextPrimary = Color(0xFFF7F5FA)
    val TextSecondary = Color(0xFFA39BB0)
    val TextMuted = Color(0xFF7B728A)
    val Badge99Yellow = Color(0xFFFFC800)
    val BadgeUberBlack = Color(0xFF0A090D)
}

object RoxouSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object RoxouRadius {
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 28.dp
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

// Escala padronizada: 3 tamanhos de título (headlineLarge, titleLarge, titleMedium) e 2 de texto (bodyMedium, labelMedium)
private val RoxouTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 68.sp,
        lineHeight = 72.sp,
        letterSpacing = (-2).sp,
        fontFeatureSettings = "tnum",
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
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
        fontSize = 12.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.2.sp,
        fontFeatureSettings = "tnum",
    ),
)

private val RoxouShapes = Shapes(
    small = RoundedCornerShape(RoxouRadius.sm),
    medium = RoundedCornerShape(RoxouRadius.md),
    large = RoundedCornerShape(RoxouRadius.lg),
    extraLarge = RoundedCornerShape(RoxouRadius.xl),
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

@Composable
internal fun RoxouCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = RoxouSpacing.lg,
    borderColor: Color = RoxouColors.CardBorder,
    containerColor: Color = RoxouColors.SurfaceCard,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
internal fun RoxouBarraProgresso(
    progress: Float,
    height: Dp,
    fillColor: Color,
    modifier: Modifier = Modifier,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(RoxouColors.Track),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(fillColor),
            )
        }
    }
}

@Composable
internal fun AppMarcaSelo(
    app: String,
    modifier: Modifier = Modifier,
) {
    val is99 = app.trim() == "99"
    val bgColor = if (is99) RoxouColors.Badge99Yellow else RoxouColors.BadgeUberBlack
    val txtColor = if (is99) Color(0xFF121016) else Color.White
    val border = if (is99) null else BorderStroke(1.dp, RoxouColors.CardBorder)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = border,
    ) {
        Text(
            text = if (is99) "99" else "Uber",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = txtColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
internal fun RoxouEstadoCarregando(
    mensagem: String = "Carregando...",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(RoxouSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = RoxouColors.Purple,
            trackColor = RoxouColors.Track,
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp),
        )
        Spacer(modifier = Modifier.height(RoxouSpacing.md))
        Text(
            text = mensagem,
            style = MaterialTheme.typography.bodyMedium,
            color = RoxouColors.TextSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun RoxouEstadoErro(
    mensagem: String,
    onTentarNovamente: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(RoxouSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = RoxouColors.Red,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.height(RoxouSpacing.sm))
        Text(
            text = mensagem,
            style = MaterialTheme.typography.bodyMedium,
            color = RoxouColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        if (onTentarNovamente != null) {
            Spacer(modifier = Modifier.height(RoxouSpacing.md))
            Button(
                onClick = onTentarNovamente,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoxouColors.Purple,
                    contentColor = Color.White,
                ),
                modifier = Modifier.height(48.dp),
            ) {
                Text(
                    text = "Tentar de novo",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

// Helpers únicos de formatação para todo o pacote br.com.roxou.mobilidade.design
internal fun formatarReais(centavos: Long): String {
    if (centavos == 0L) return "R$ 0"
    val sinal = if (centavos < 0L) "-" else ""
    val absoluto = abs(centavos)
    val reais = absoluto / 100L
    val resto = absoluto % 100L
    val reaisFormatado = formatarMilharBr(reais)
    return if (absoluto > 10_000L || resto == 0L) {
        "${sinal}R$ $reaisFormatado"
    } else {
        "${sinal}R$ $reaisFormatado,${resto.toString().padStart(2, '0')}"
    }
}

internal fun formatarReaisPreciso(centavos: Long): String {
    val sinal = if (centavos < 0L) "-" else ""
    val absoluto = abs(centavos)
    val reais = absoluto / 100L
    val resto = absoluto % 100L
    val reaisFormatado = formatarMilharBr(reais)
    return "${sinal}R$ $reaisFormatado,${resto.toString().padStart(2, '0')}"
}

internal fun formatarReaisInteiro(centavos: Long): String {
    val sinal = if (centavos < 0L) "-" else ""
    val reais = abs(centavos) / 100L
    return "${sinal}R$ ${formatarMilharBr(reais)}"
}

internal fun formatarBuscaKm(km: Double): String {
    if (km <= 0.0) return "Sem limite"
    val inteiro = km.toInt()
    return if (km == inteiro.toDouble()) {
        "$inteiro km"
    } else {
        "${inteiro},5 km"
    }
}

internal fun formatarKm(km: Double): String {
    val decimos = (km * 10.0).toInt()
    val inteiro = decimos / 10
    val frac = abs(decimos % 10)
    return if (frac == 0) "$inteiro km" else "$inteiro,$frac km"
}

internal fun parseCentavos(texto: String): Long {
    val limpo = texto.trim().replace("R$", "").replace(" ", "")
    if (limpo.isEmpty()) return 0L
    val normalizado = if (limpo.contains(",")) {
        limpo.replace(".", "").replace(",", ".")
    } else {
        limpo
    }
    val valor = normalizado.toDoubleOrNull() ?: return 0L
    return (valor * 100.0).toLong().coerceAtLeast(0L)
}

internal fun parseKm(texto: String): Double? {
    val limpo = texto.trim().replace(",", ".")
    if (limpo.isEmpty()) return null
    val valor = limpo.toDoubleOrNull() ?: return null
    return if (valor > 0.0) valor else null
}

private fun formatarMilharBr(valor: Long): String {
    val digitos = valor.toString()
    if (digitos.length <= 3) return digitos
    val sb = StringBuilder(digitos.length + digitos.length / 3)
    var contador = 0
    for (i in digitos.lastIndex downTo 0) {
        if (contador > 0 && contador % 3 == 0) {
            sb.append('.')
        }
        sb.append(digitos[i])
        contador++
    }
    return sb.reverse().toString()
}
