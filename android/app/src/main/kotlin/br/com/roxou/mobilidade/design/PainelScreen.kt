package br.com.roxou.mobilidade.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    radarEstado: String = "Lendo ofertas",
    onArrumarRadar: () -> Unit = {},
    onAbrirDiagnostico: () -> Unit = {},
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RoxouSpacing.lg, vertical = RoxouSpacing.xl)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(RoxouSpacing.lg),
        ) {
            TopHeader(diaTexto = ui.diaTexto)

            // Topo: Cartão do Radar com estado e link discreto "Diagnóstico"
            RadarCard(
                estado = radarEstado,
                onArrumar = onArrumarRadar,
                onAbrirDiagnostico = onAbrirDiagnostico,
            )

            // Meta do dia em número grande: "7 de 18 corridas" + "Uber 5 · 99 2"
            CorridasHojeCard(
                corridasHoje = ui.corridasHoje,
                metaHoje = if (ui.recursos.metaDiaria) ui.metaHoje else 0,
                corridasUberHoje = ui.corridasUberHoje,
                corridas99Hoje = ui.corridas99Hoje,
            )

            // Meta da locadora (só Uber) separada e menor
            if (ui.recursos.metaLocadora) {
                MetaLocadoraCard(
                    corridasSemanaUber = ui.corridasSemanaUber,
                    proximaFaixaCorridas = ui.proximaFaixaCorridas,
                    proximaFaixaDescontoCentavos = ui.proximaFaixaDescontoCentavos,
                    descontoGarantidoCentavos = ui.descontoGarantidoCentavos,
                )
            }

            // Parcela e/ou Dinheiro livre
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
private fun RadarCard(
    estado: String,
    onArrumar: () -> Unit,
    onAbrirDiagnostico: () -> Unit,
) {
    val norm = estado.trim().lowercase()
    val faltaPermissao = norm.contains("permiss") || norm.contains("falta")
    val desligado = norm.contains("deslig")
    val lendo = !faltaPermissao && !desligado

    val corDestaque = when {
        faltaPermissao -> RoxouColors.Red
        desligado -> RoxouColors.TextMuted
        else -> RoxouColors.Purple
    }
    val corBorda = when {
        faltaPermissao -> RoxouColors.Red.copy(alpha = 0.55f)
        desligado -> RoxouColors.CardBorder
        else -> RoxouColors.Purple.copy(alpha = 0.55f)
    }
    val corTextoEstado = when {
        faltaPermissao -> RoxouColors.Red
        desligado -> RoxouColors.TextSecondary
        else -> RoxouColors.TextPrimary
    }

    val pulsoTransicao = rememberInfiniteTransition(label = "radar_pulso")
    val alfaPulso by pulsoTransicao.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "radar_alfa",
    )

    RoxouCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = RoxouSpacing.lg,
        borderColor = corBorda,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
                    modifier = Modifier.weight(1f),
                ) {
                    // Indicador de estado (pulsa em roxo quando "Lendo ofertas")
                    Box(
                        modifier = Modifier.size(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (lendo) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .alpha(alfaPulso * 0.4f)
                                    .clip(CircleShape)
                                    .background(RoxouColors.Purple),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .alpha(if (lendo) (0.65f + alfaPulso * 0.35f) else 1f)
                                .clip(CircleShape)
                                .background(corDestaque),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Radar",
                            style = MaterialTheme.typography.labelMedium,
                            color = RoxouColors.TextSecondary,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            text = estado,
                            style = MaterialTheme.typography.titleLarge,
                            color = corTextoEstado,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (faltaPermissao) {
                    Button(
                        onClick = onArrumar,
                        shape = RoundedCornerShape(RoxouRadius.sm),
                        contentPadding = PaddingValues(horizontal = RoxouSpacing.md),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoxouColors.Red,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text(
                            text = "Arrumar",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RoxouSpacing.md))
            HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)

            // Rodapé discreto com link "Diagnóstico" (área de toque mínima de 48 dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(RoxouRadius.sm))
                    .clickable(onClick = onAbrirDiagnostico)
                    .padding(horizontal = RoxouSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Diagnóstico",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = RoxouColors.TextMuted,
                    maxLines = 1,
                    softWrap = false,
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Abrir diagnóstico",
                    tint = RoxouColors.TextMuted,
                    modifier = Modifier.size(18.dp),
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
                    text = "Meta do dia",
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

            Spacer(modifier = Modifier.height(RoxouSpacing.sm))

            // Número grande: "7 de 18 corridas"
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = RoxouColors.TextPrimary,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append(corridasHoje.toString())
                    }
                    withStyle(
                        SpanStyle(
                            color = RoxouColors.TextSecondary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    ) {
                        if (temMeta) {
                            append(" de $metaHoje corridas")
                        } else {
                            append(if (corridasHoje == 1) " corrida" else " corridas")
                        }
                    }
                },
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )

            if (temMeta) {
                Spacer(modifier = Modifier.height(RoxouSpacing.md))
                RoxouBarraProgresso(
                    progress = progresso,
                    height = 8.dp,
                    fillColor = corProgresso,
                )
            }

            Spacer(modifier = Modifier.height(RoxouSpacing.md))

            Text(
                text = "Uber $corridasUberHoje · 99 $corridas99Hoje",
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
        contentPadding = RoxouSpacing.md,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Meta da locadora · só Uber",
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

            Spacer(modifier = Modifier.height(RoxouSpacing.sm))

            if (bateuUltima) {
                Text(
                    text = "Faixa máxima: ${formatarReais(descontoGarantidoCentavos)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RoxouColors.Green,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(RoxouSpacing.sm))

                RoxouBarraProgresso(
                    progress = 1f,
                    height = 5.dp,
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(RoxouSpacing.sm))

                RoxouBarraProgresso(
                    progress = progresso,
                    height = 5.dp,
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
    name = "Painel - Radar lendo ofertas",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 780,
)
@Composable
private fun PainelScreenPreview() {
    RoxouDesignTheme {
        PainelScreen(
            ui = PainelUi(
                diaTexto = "Sex 27/11",
                corridasHoje = 7,
                metaHoje = 18,
                corridasUberHoje = 5,
                corridas99Hoje = 2,
                faturadoHojeCentavos = 14800L,
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
            radarEstado = "Lendo ofertas",
        )
    }
}

@Preview(
    name = "Painel - Radar falta permissão",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 780,
)
@Composable
private fun PainelScreenPermissaoPreview() {
    RoxouDesignTheme {
        PainelScreen(
            ui = PainelUi(
                diaTexto = "Sex 27/11",
                corridasHoje = 7,
                metaHoje = 18,
                corridasUberHoje = 5,
                corridas99Hoje = 2,
                faturadoHojeCentavos = 14800L,
                corridasSemanaUber = 54,
                proximaFaixaCorridas = 60,
                proximaFaixaDescontoCentavos = 16000L,
                descontoGarantidoCentavos = 8000L,
                parcelaFaltaCentavos = null,
                dinheiroLivreCentavos = 23000L,
                recursos = Recursos(
                    metaLocadora = true,
                    locacao = false,
                    financas = true,
                    metaDiaria = true,
                    janelaFlutuante = true,
                ),
            ),
            radarEstado = "Falta permissão",
        )
    }
}

@Preview(
    name = "Painel - Radar desligado",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 780,
)
@Composable
private fun PainelScreenDesligadoPreview() {
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
                    metaDiaria = true,
                    janelaFlutuante = true,
                ),
            ),
            radarEstado = "Desligado",
        )
    }
}
