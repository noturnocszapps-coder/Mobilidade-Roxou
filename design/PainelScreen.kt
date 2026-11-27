package br.com.roxou.mobilidade.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun PainelScreen(
    ui: PainelUi,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = RoxouColors.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TopHeader(diaTexto = ui.diaTexto)

            CorridasHojeCard(
                corridasHoje = ui.corridasHoje,
                metaHoje = ui.metaHoje,
                corridasUberHoje = ui.corridasUberHoje,
                corridas99Hoje = ui.corridas99Hoje,
                faturadoHojeCentavos = ui.faturadoHojeCentavos,
            )

            MetaLocadoraCard(
                corridasSemanaUber = ui.corridasSemanaUber,
                proximaFaixaCorridas = ui.proximaFaixaCorridas,
                proximaFaixaDescontoCentavos = ui.proximaFaixaDescontoCentavos,
                descontoGarantidoCentavos = ui.descontoGarantidoCentavos,
            )

            FinanceiroRow(
                parcelaFaltaCentavos = ui.parcelaFaltaCentavos,
                dinheiroLivreCentavos = ui.dinheiroLivreCentavos,
            )
        }
    }
}

@Composable
private fun TopHeader(diaTexto: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(RoxouColors.Purple),
            )
            Text(
                text = "ROXOU",
                style = MaterialTheme.typography.labelSmall,
                color = RoxouColors.TextPrimary,
                letterSpacing = 2.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = diaTexto,
            style = MaterialTheme.typography.labelMedium,
            color = RoxouColors.TextSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CorridasHojeCard(
    corridasHoje: Int,
    metaHoje: Int,
    corridasUberHoje: Int,
    corridas99Hoje: Int,
    faturadoHojeCentavos: Long,
) {
    val temMeta = metaHoje > 0
    val metaBatida = temMeta && corridasHoje >= metaHoje
    val progresso = if (temMeta) {
        (corridasHoje.toFloat() / metaHoje.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    RoxouCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 24.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Corridas hoje",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoxouColors.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )

                if (temMeta) {
                    val percentual = ((corridasHoje.toFloat() / metaHoje.toFloat()) * 100f)
                        .toInt()
                        .coerceAtLeast(0)
                    Text(
                        text = if (metaBatida) "Meta batida" else "$percentual%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (metaBatida) RoxouColors.Green else RoxouColors.PurpleSoft,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = corridasHoje.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = RoxouColors.TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alignByBaseline(),
                )

                if (temMeta) {
                    Text(
                        text = " de $metaHoje",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        color = RoxouColors.TextSecondary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .alignByBaseline(),
                    )
                }
            }

            if (temMeta) {
                Spacer(modifier = Modifier.height(20.dp))
                BarraProgresso(
                    progress = progresso,
                    height = 10.dp,
                    fillColor = if (metaBatida) RoxouColors.Green else RoxouColors.Purple,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Uber $corridasUberHoje · 99 $corridas99Hoje · ${formatarReais(faturadoHojeCentavos)}",
                style = MaterialTheme.typography.bodyMedium,
                color = RoxouColors.TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MetaLocadoraCard(
    corridasSemanaUber: Int,
    proximaFaixaCorridas: Int?,
    proximaFaixaDescontoCentavos: Long?,
    descontoGarantidoCentavos: Long,
) {
    val bateuUltima = proximaFaixaCorridas == null || proximaFaixaDescontoCentavos == null

    RoxouCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 22.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Meta da locadora",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoxouColors.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!bateuUltima && descontoGarantidoCentavos > 0L) {
                    Text(
                        text = "Garantido ${formatarReais(descontoGarantidoCentavos)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = RoxouColors.TextMuted,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (bateuUltima) {
                Text(
                    text = "Faixa máxima: ${formatarReais(descontoGarantidoCentavos)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = RoxouColors.Green,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(14.dp))

                BarraProgresso(
                    progress = 1f,
                    height = 6.dp,
                    fillColor = RoxouColors.Green,
                )
            } else {
                val alvo = proximaFaixaCorridas!!
                val desconto = proximaFaixaDescontoCentavos!!
                val progresso = (corridasSemanaUber.toFloat() / alvo.coerceAtLeast(1).toFloat())
                    .coerceIn(0f, 1f)

                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = RoxouColors.TextPrimary)) {
                            append("$corridasSemanaUber de $alvo")
                        }
                        withStyle(SpanStyle(color = RoxouColors.TextMuted)) {
                            append(" → ")
                        }
                        withStyle(SpanStyle(color = RoxouColors.PurpleSoft)) {
                            append(formatarReais(desconto))
                        }
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(14.dp))

                BarraProgresso(
                    progress = progresso,
                    height = 6.dp,
                    fillColor = RoxouColors.Purple,
                )
            }
        }
    }
}

@Composable
private fun FinanceiroRow(
    parcelaFaltaCentavos: Long?,
    dinheiroLivreCentavos: Long,
) {
    if (parcelaFaltaCentavos == null) {
        ValorCard(
            rotulo = "Dinheiro livre",
            valor = formatarReais(dinheiroLivreCentavos),
            corValor = if (dinheiroLivreCentavos < 0L) RoxouColors.Red else RoxouColors.TextPrimary,
            destaqueMaior = true,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        val parcelaPaga = parcelaFaltaCentavos <= 0L
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ValorCard(
                rotulo = "Falta da parcela",
                valor = if (parcelaPaga) "Paga" else formatarReais(parcelaFaltaCentavos),
                corValor = if (parcelaPaga) RoxouColors.Green else RoxouColors.Red,
                destaqueMaior = false,
                modifier = Modifier.weight(1f),
            )

            ValorCard(
                rotulo = "Dinheiro livre",
                valor = formatarReais(dinheiroLivreCentavos),
                corValor = if (dinheiroLivreCentavos < 0L) RoxouColors.Red else RoxouColors.TextPrimary,
                destaqueMaior = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ValorCard(
    rotulo: String,
    valor: String,
    corValor: Color,
    destaqueMaior: Boolean,
    modifier: Modifier = Modifier,
) {
    RoxouCard(
        modifier = modifier,
        contentPadding = 20.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = rotulo,
                style = MaterialTheme.typography.labelMedium,
                color = RoxouColors.TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = valor,
                style = if (destaqueMaior) {
                    MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 34.sp)
                } else {
                    MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 28.sp)
                },
                color = corValor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RoxouCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = RoxouColors.SurfaceCard,
        border = BorderStroke(1.dp, RoxouColors.CardBorder),
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
private fun BarraProgresso(
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

internal fun formatarReais(centavos: Long): String {
    val sinal = if (centavos < 0L) "-" else ""
    val absoluto = abs(centavos)
    val reais = absoluto / 100L
    val resto = absoluto % 100L
    val reaisFormatado = formatarMilharBr(reais)
    return if (absoluto > 10_000L) {
        "${sinal}R$ $reaisFormatado"
    } else {
        val centavosStr = resto.toString().padStart(2, '0')
        "${sinal}R$ $reaisFormatado,$centavosStr"
    }
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

@Preview(
    name = "Painel - Em andamento",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 393,
    heightDp = 851,
)
@Composable
private fun PainelScreenPreview() {
    RoxouDesignTheme {
        PainelScreen(
            ui = PainelUi(
                diaTexto = "Sex 27/11",
                corridasHoje = 12,
                metaHoje = 18,
                corridasUberHoje = 12,
                corridas99Hoje = 0,
                faturadoHojeCentavos = 23000L,
                corridasSemanaUber = 54,
                proximaFaixaCorridas = 60,
                proximaFaixaDescontoCentavos = 16000L,
                descontoGarantidoCentavos = 8000L,
                parcelaFaltaCentavos = 114900L,
                dinheiroLivreCentavos = 23000L,
            ),
        )
    }
}

@Preview(
    name = "Painel - Faixa máxima e parcela paga",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 393,
    heightDp = 851,
)
@Composable
private fun PainelScreenFaixaMaximaPreview() {
    RoxouDesignTheme {
        PainelScreen(
            ui = PainelUi(
                diaTexto = "Sáb 28/11",
                corridasHoje = 20,
                metaHoje = 18,
                corridasUberHoje = 15,
                corridas99Hoje = 5,
                faturadoHojeCentavos = 41200L,
                corridasSemanaUber = 120,
                proximaFaixaCorridas = null,
                proximaFaixaDescontoCentavos = null,
                descontoGarantidoCentavos = 25000L,
                parcelaFaltaCentavos = 0L,
                dinheiroLivreCentavos = 89000L,
            ),
        )
    }
}

@Preview(
    name = "Painel - Carro próprio",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 393,
    heightDp = 851,
)
@Composable
private fun PainelScreenCarroProprioPreview() {
    RoxouDesignTheme {
        PainelScreen(
            ui = PainelUi(
                diaTexto = "Dom 29/11",
                corridasHoje = 9,
                metaHoje = 0,
                corridasUberHoje = 6,
                corridas99Hoje = 3,
                faturadoHojeCentavos = 18500L,
                corridasSemanaUber = 42,
                proximaFaixaCorridas = 60,
                proximaFaixaDescontoCentavos = 16000L,
                descontoGarantidoCentavos = 0L,
                parcelaFaltaCentavos = null,
                dinheiroLivreCentavos = 18500L,
            ),
        )
    }
}
