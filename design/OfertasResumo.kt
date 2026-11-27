package br.com.roxou.mobilidade.design

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

data class OfertasResumoUi(
    val hojeTotal: Int,
    val hojeAceitas: Int,
    val mediaPorDia: Double,
    val taxaAceite: Double,
    val melhorDiaTexto: String?,
    val melhorHorarioTexto: String?,
    val porDiaDaSemana: List<Pair<String, Double>>,
    val uber: Int,
    val noventaENove: Int,
)

@Composable
fun OfertasResumoScreen(
    ui: OfertasResumoUi,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val taxaPercentual = if (ui.taxaAceite in 0.0..1.0 && (ui.taxaAceite < 1.0 || (ui.hojeTotal > 0 && ui.hojeAceitas == ui.hojeTotal))) {
        (ui.taxaAceite * 100.0).roundToInt()
    } else {
        ui.taxaAceite.roundToInt()
    }.coerceIn(0, 100)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = RoxouColors.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RoxouSpacing.lg, vertical = RoxouSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RoxouSpacing.lg),
        ) {
            // Topo com voltar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
            ) {
                IconButton(
                    onClick = onVoltar,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = RoxouColors.TextPrimary,
                    )
                }
                Text(
                    text = "Resumo das ofertas",
                    style = MaterialTheme.typography.headlineLarge,
                    color = RoxouColors.TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Topo: Hoje: X ofertas · Y aceitas
            RoxouCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = RoxouSpacing.xl,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Hoje: ${ui.hojeTotal} ofertas · ${ui.hojeAceitas} aceitas",
                        style = MaterialTheme.typography.titleLarge,
                        color = RoxouColors.TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(RoxouSpacing.lg))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
                    ) {
                        MetricaResumo(
                            rotulo = "Média por dia",
                            valor = formatarDecimalCurto(ui.mediaPorDia),
                            corValor = RoxouColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        MetricaResumo(
                            rotulo = "Aceite",
                            valor = "$taxaPercentual%",
                            corValor = RoxouColors.PurpleSoft,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Melhor dia e melhor horário
            if (!ui.melhorDiaTexto.isNullOrBlank() || !ui.melhorHorarioTexto.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
                ) {
                    if (!ui.melhorDiaTexto.isNullOrBlank()) {
                        RoxouCard(
                            modifier = Modifier.weight(1f),
                            contentPadding = RoxouSpacing.lg,
                        ) {
                            MetricaResumo(
                                rotulo = "Dia mais forte",
                                valor = ui.melhorDiaTexto,
                                corValor = RoxouColors.Green,
                            )
                        }
                    }
                    if (!ui.melhorHorarioTexto.isNullOrBlank()) {
                        RoxouCard(
                            modifier = Modifier.weight(1f),
                            contentPadding = RoxouSpacing.lg,
                        ) {
                            MetricaResumo(
                                rotulo = "Mais movimento",
                                valor = ui.melhorHorarioTexto,
                                corValor = RoxouColors.TextPrimary,
                            )
                        }
                    }
                }
            }

            // Barrinhas por dia da semana (Seg a Dom)
            if (ui.porDiaDaSemana.isNotEmpty()) {
                val maxValor = (ui.porDiaDaSemana.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
                RoxouCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = RoxouSpacing.lg,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Por dia da semana",
                            style = MaterialTheme.typography.titleMedium,
                            color = RoxouColors.TextPrimary,
                            maxLines = 1,
                            softWrap = false,
                        )

                        Spacer(modifier = Modifier.height(RoxouSpacing.lg))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp),
                            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            ui.porDiaDaSemana.forEach { (dia, valor) ->
                                val frac = (valor / maxValor).toFloat().coerceIn(0f, 1f)
                                val destaque = valor >= maxValor && valor > 0.0
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                ) {
                                    Text(
                                        text = valor.roundToInt().toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (destaque) RoxouColors.PurpleSoft else RoxouColors.TextMuted,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                    Spacer(modifier = Modifier.height(RoxouSpacing.xs))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.65f)
                                            .weight(1f),
                                        contentAlignment = Alignment.BottomCenter,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(frac.coerceAtLeast(0.08f))
                                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                                .background(
                                                    if (destaque) RoxouColors.Purple else RoxouColors.Track,
                                                ),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(RoxouSpacing.sm))
                                    Text(
                                        text = dia,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (destaque) RoxouColors.TextPrimary else RoxouColors.TextSecondary,
                                        fontWeight = if (destaque) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Uber x 99
            val totalApps = (ui.uber + ui.noventaENove).coerceAtLeast(0)
            val fracUber = if (totalApps > 0) ui.uber.toFloat() / totalApps.toFloat() else 0.5f
            val frac99 = if (totalApps > 0) ui.noventaENove.toFloat() / totalApps.toFloat() else 0.5f

            RoxouCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = RoxouSpacing.lg,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Uber x 99",
                        style = MaterialTheme.typography.titleMedium,
                        color = RoxouColors.TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                    )

                    Spacer(modifier = Modifier.height(RoxouSpacing.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                        ) {
                            AppMarcaSelo(app = "Uber")
                            Text(
                                text = ui.uber.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = RoxouColors.TextPrimary,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                        ) {
                            Text(
                                text = ui.noventaENove.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = RoxouColors.TextPrimary,
                                maxLines = 1,
                                softWrap = false,
                            )
                            AppMarcaSelo(app = "99")
                        }
                    }

                    Spacer(modifier = Modifier.height(RoxouSpacing.md))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(RoxouColors.Track),
                    ) {
                        if (totalApps > 0) {
                            if (fracUber > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(fracUber)
                                        .fillMaxHeight()
                                        .background(RoxouColors.Purple),
                                )
                            }
                            if (frac99 > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(frac99)
                                        .fillMaxHeight()
                                        .background(RoxouColors.Badge99Yellow),
                                )
                            }
                        }
                    }
                }
            }

            // Aviso discreto no fim
            Text(
                text = "Esses dados ficam só no seu celular e vão ajudar a IA do Roxou a te dar dicas.",
                style = MaterialTheme.typography.labelMedium,
                color = RoxouColors.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RoxouSpacing.sm, vertical = RoxouSpacing.xs),
            )
        }
    }
}

@Composable
private fun MetricaResumo(
    rotulo: String,
    valor: String,
    corValor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelMedium,
            color = RoxouColors.TextSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(RoxouSpacing.xs))
        Text(
            text = valor,
            style = MaterialTheme.typography.headlineLarge,
            color = corValor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatarDecimalCurto(valor: Double): String {
    val decimos = (valor * 10.0).roundToInt()
    val inteiro = decimos / 10
    val frac = abs(decimos % 10)
    return if (frac == 0) "$inteiro" else "$inteiro,$frac"
}

@Preview(
    name = "Resumo das ofertas",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 820,
)
@Composable
private fun OfertasResumoScreenPreview() {
    RoxouDesignTheme {
        OfertasResumoScreen(
            ui = OfertasResumoUi(
                hojeTotal = 12,
                hojeAceitas = 3,
                mediaPorDia = 24.5,
                taxaAceite = 0.25,
                melhorDiaTexto = "Sexta",
                melhorHorarioTexto = "18h – 21h",
                porDiaDaSemana = listOf(
                    "Seg" to 18.0,
                    "Ter" to 20.0,
                    "Qua" to 22.0,
                    "Qui" to 26.0,
                    "Sex" to 34.0,
                    "Sáb" to 31.0,
                    "Dom" to 15.0,
                ),
                uber = 114,
                noventaENove = 52,
            ),
            onVoltar = {},
        )
    }
}
