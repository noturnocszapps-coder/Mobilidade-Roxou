package br.com.roxou.mobilidade.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PainelScreen(
    ui: PainelUi,
    modifier: Modifier = Modifier,
    onAbrirFinancas: () -> Unit = {},
) {
    val mostrarParcela = ui.recursos.locacao && ui.parcelaFaltaCentavos != null
    val mostrarLivre = ui.recursos.financas

    Surface(
        modifier = modifier.fillMaxSize(),
        color = RoxouColors.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = RoxouSpacing.lg, vertical = RoxouSpacing.xl)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(RoxouSpacing.lg),
        ) {
            TopHeader(diaTexto = ui.diaTexto)

            // Bloco 1: Corridas de hoje (sempre visível)
            CorridasHojeCard(
                corridasHoje = ui.corridasHoje,
                metaHoje = if (ui.recursos.metaDiaria) ui.metaHoje else 0,
                corridasUberHoje = ui.corridasUberHoje,
                corridas99Hoje = ui.corridas99Hoje,
                faturadoHojeCentavos = ui.faturadoHojeCentavos,
            )

            // Bloco 2: Meta da locadora (só se recursos.metaLocadora)
            if (ui.recursos.metaLocadora) {
                MetaLocadoraCard(
                    corridasSemanaUber = ui.corridasSemanaUber,
                    proximaFaixaCorridas = ui.proximaFaixaCorridas,
                    proximaFaixaDescontoCentavos = ui.proximaFaixaDescontoCentavos,
                    descontoGarantidoCentavos = ui.descontoGarantidoCentavos,
                )
            }

            // Bloco 3: Parcela e/ou Dinheiro livre
            if (mostrarParcela || mostrarLivre) {
                FinanceiroBloco(
                    mostrarParcela = mostrarParcela,
                    parcelaFaltaCentavos = ui.parcelaFaltaCentavos,
                    mostrarLivre = mostrarLivre,
                    dinheiroLivreCentavos = ui.dinheiroLivreCentavos,
                    onAbrirFinancas = onAbrirFinancas,
                )
            }
        }
    }
}

@Composable
private fun TopHeader(diaTexto: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RoxouSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
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
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
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
    val corProgresso by animateColorAsState(
        targetValue = if (metaBatida) RoxouColors.Green else RoxouColors.Purple,
        label = "progresso_hoje_cor",
    )

    RoxouCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = RoxouSpacing.xl,
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
                        fontWeight = FontWeight.SemiBold,
                        color = if (metaBatida) RoxouColors.Green else RoxouColors.PurpleSoft,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(RoxouSpacing.md))

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
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Medium,
                        color = RoxouColors.TextSecondary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = RoxouSpacing.xs)
                            .alignByBaseline(),
                    )
                }
            }

            if (temMeta) {
                Spacer(modifier = Modifier.height(RoxouSpacing.lg))
                RoxouBarraProgresso(
                    progress = progresso,
                    height = 8.dp,
                    fillColor = corProgresso,
                )
            }

            Spacer(modifier = Modifier.height(RoxouSpacing.lg))

            Text(
                text = if (corridasHoje == 0 && faturadoHojeCentavos == 0L) {
                    "Uber 0 · 99 0 · R$ 0"
                } else {
                    "Uber $corridasUberHoje · 99 $corridas99Hoje · ${formatarReais(faturadoHojeCentavos)}"
                },
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
        contentPadding = RoxouSpacing.lg,
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

            Spacer(modifier = Modifier.height(RoxouSpacing.md))

            if (bateuUltima) {
                Text(
                    text = "Faixa máxima: ${formatarReais(descontoGarantidoCentavos)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = RoxouColors.Green,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(RoxouSpacing.md))

                RoxouBarraProgresso(
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
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(RoxouSpacing.md))

                RoxouBarraProgresso(
                    progress = progresso,
                    height = 6.dp,
                    fillColor = RoxouColors.Purple,
                )
            }
        }
    }
}

@Composable
private fun FinanceiroBloco(
    mostrarParcela: Boolean,
    parcelaFaltaCentavos: Long?,
    mostrarLivre: Boolean,
    dinheiroLivreCentavos: Long,
    onAbrirFinancas: () -> Unit,
) {
    if (mostrarParcela && mostrarLivre) {
        val falta = parcelaFaltaCentavos ?: 0L
        val paga = falta <= 0L
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
        ) {
            ValorCard(
                rotulo = "Falta da parcela",
                valor = if (paga) "Paga" else formatarReais(falta),
                corValor = if (paga) RoxouColors.Green else RoxouColors.Red,
                larguraTotal = false,
                onClick = onAbrirFinancas,
                modifier = Modifier.weight(1f),
            )
            ValorCard(
                rotulo = "Dinheiro livre",
                valor = formatarReais(dinheiroLivreCentavos),
                corValor = if (dinheiroLivreCentavos < 0L) RoxouColors.Red else RoxouColors.TextPrimary,
                larguraTotal = false,
                onClick = onAbrirFinancas,
                modifier = Modifier.weight(1f),
            )
        }
    } else if (mostrarParcela) {
        val falta = parcelaFaltaCentavos ?: 0L
        val paga = falta <= 0L
        ValorCard(
            rotulo = "Falta da parcela",
            valor = if (paga) "Paga" else formatarReais(falta),
            corValor = if (paga) RoxouColors.Green else RoxouColors.Red,
            larguraTotal = true,
            onClick = onAbrirFinancas,
            modifier = Modifier.fillMaxWidth(),
        )
    } else if (mostrarLivre) {
        ValorCard(
            rotulo = "Dinheiro livre",
            valor = formatarReais(dinheiroLivreCentavos),
            corValor = if (dinheiroLivreCentavos < 0L) RoxouColors.Red else RoxouColors.TextPrimary,
            larguraTotal = true,
            onClick = onAbrirFinancas,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ValorCard(
    rotulo: String,
    valor: String,
    corValor: Color,
    larguraTotal: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RoxouCard(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        contentPadding = RoxouSpacing.lg,
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
            Spacer(modifier = Modifier.height(RoxouSpacing.sm))
            Text(
                text = valor,
                style = if (larguraTotal) {
                    MaterialTheme.typography.headlineLarge
                } else {
                    MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 26.sp)
                },
                color = corValor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(
    name = "Painel - Completo",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 760,
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
                recursos = Recursos(
                    metaLocadora = true,
                    locacao = true,
                    financas = true,
                    metaDiaria = true,
                    janelaFlutuante = true,
                ),
            ),
        )
    }
}

@Preview(
    name = "Painel - Dia zerado / carro próprio",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 760,
)
@Composable
private fun PainelScreenVazioPreview() {
    RoxouDesignTheme {
        PainelScreen(
            ui = PainelUi(
                diaTexto = "Seg 30/11",
                corridasHoje = 0,
                metaHoje = 18,
                corridasUberHoje = 0,
                corridas99Hoje = 0,
                faturadoHojeCentavos = 0L,
                corridasSemanaUber = 0,
                proximaFaixaCorridas = 40,
                proximaFaixaDescontoCentavos = 8000L,
                descontoGarantidoCentavos = 0L,
                parcelaFaltaCentavos = null,
                dinheiroLivreCentavos = 0L,
                recursos = Recursos(
                    metaLocadora = false,
                    locacao = false,
                    financas = true,
                    metaDiaria = false,
                    janelaFlutuante = true,
                ),
            ),
        )
    }
}
