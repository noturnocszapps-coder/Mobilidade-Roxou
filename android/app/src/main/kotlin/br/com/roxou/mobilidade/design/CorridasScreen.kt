package br.com.roxou.mobilidade.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToLong

data class CorridaItem(
    val id: String,
    val app: String,
    val hora: String,
    val valorCentavos: Long,
    val km: Double?,
    val confirmada: Boolean,
    val origem: String,
    val status: String = "Aceita",
    val valorPorKmCentavos: Long? = null,
    val valorPorHoraCentavos: Long? = null,
)

private enum class FiltroCorridas(val rotulo: String) {
    TODAS("Todas"),
    ACEITAS("Aceitas"),
    RECUSADAS("Recusadas"),
}

private fun statusEfetivo(item: CorridaItem): String {
    val norm = item.status.trim().lowercase()
    return when {
        norm.startsWith("recus") -> "Recusada"
        norm.contains("confirm") || !item.confirmada -> "A confirmar"
        else -> "Aceita"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorridasScreen(
    itens: List<CorridaItem>,
    onAdicionar: () -> Unit,
    onConfirmar: (String) -> Unit,
    onApagar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var itemParaApagar by remember { mutableStateOf<CorridaItem?>(null) }
    var filtroAtual by remember { mutableStateOf(FiltroCorridas.TODAS) }

    val itensFiltrados = remember(itens, filtroAtual) {
        when (filtroAtual) {
            FiltroCorridas.TODAS -> itens
            FiltroCorridas.ACEITAS -> itens.filter { statusEfetivo(it) == "Aceita" }
            FiltroCorridas.RECUSADAS -> itens.filter { statusEfetivo(it) == "Recusada" }
        }
    }

    val qtdUber = itens.count { it.app.equals("Uber", ignoreCase = true) && statusEfetivo(it) == "Aceita" }
    val qtd99 = itens.count { it.app.trim() == "99" && statusEfetivo(it) == "Aceita" }
    val qtdPendentes = itens.count { statusEfetivo(it) == "A confirmar" }

    val resumoTexto = buildString {
        append("Uber $qtdUber · 99 $qtd99")
        if (qtdPendentes > 0) {
            append(" · $qtdPendentes a confirmar")
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = RoxouColors.Background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = RoxouSpacing.lg),
            ) {
                Spacer(modifier = Modifier.height(RoxouSpacing.xl))

                Text(
                    text = "Corridas de hoje",
                    style = MaterialTheme.typography.headlineLarge,
                    color = RoxouColors.TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(RoxouSpacing.xs))

                Text(
                    text = resumoTexto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (qtdPendentes > 0) RoxouColors.Yellow else RoxouColors.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(RoxouSpacing.md))

                // Filtros em pílulas: Todas, Aceitas, Recusadas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                ) {
                    FiltroCorridas.entries.forEach { filtro ->
                        FiltroPilula(
                            rotulo = filtro.rotulo,
                            selecionado = filtro == filtroAtual,
                            onClick = { filtroAtual = filtro },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(RoxouSpacing.lg))

                if (itensFiltrados.isEmpty()) {
                    CorridasEstadoVazio(
                        titulo = if (itens.isEmpty()) "Nenhuma corrida hoje" else "Nenhuma corrida neste filtro",
                        subtitulo = if (itens.isEmpty()) "Toque em + para registrar" else "Mude o filtro acima para ver outras",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
                        contentPadding = PaddingValues(bottom = 88.dp),
                    ) {
                        items(itensFiltrados, key = { it.id }) { item ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { valor ->
                                    if (valor == SwipeToDismissBoxValue.EndToStart) {
                                        itemParaApagar = item
                                    }
                                    false
                                },
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(RoxouRadius.lg))
                                            .background(RoxouColors.Red)
                                            .padding(horizontal = RoxouSpacing.xl),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Apagar",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp),
                                            )
                                            Text(
                                                text = "Apagar",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        }
                                    }
                                },
                            ) {
                                CorridaCard(
                                    item = item,
                                    onConfirmar = { onConfirmar(item.id) },
                                    onLongPress = { itemParaApagar = item },
                                )
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onAdicionar,
                shape = CircleShape,
                containerColor = RoxouColors.Purple,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(RoxouSpacing.lg)
                    .size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Nova corrida",
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }

    itemParaApagar?.let { alvo ->
        AlertDialog(
            onDismissRequest = { itemParaApagar = null },
            containerColor = RoxouColors.SurfaceCard,
            titleContentColor = RoxouColors.TextPrimary,
            textContentColor = RoxouColors.TextSecondary,
            title = {
                Text(
                    text = "Apagar corrida?",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    softWrap = false,
                )
            },
            text = {
                Text(
                    text = "${alvo.app} · ${alvo.hora} · ${formatarReais(alvo.valorCentavos)}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onApagar(alvo.id)
                        itemParaApagar = null
                    },
                ) {
                    Text(
                        text = "Apagar",
                        color = RoxouColors.Red,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { itemParaApagar = null }) {
                    Text(
                        text = "Cancelar",
                        color = RoxouColors.TextSecondary,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            },
        )
    }
}

@Composable
private fun FiltroPilula(
    rotulo: String,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selecionado) RoxouColors.Purple else RoxouColors.SurfaceCard,
        label = "filtro_bg",
    )
    val fg by animateColorAsState(
        targetValue = if (selecionado) Color.White else RoxouColors.TextSecondary,
        label = "filtro_fg",
    )
    val borda by animateColorAsState(
        targetValue = if (selecionado) RoxouColors.Purple else RoxouColors.CardBorder,
        label = "filtro_borda",
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.dp, borda),
        modifier = modifier.height(48.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = RoxouSpacing.sm),
        ) {
            Text(
                text = rotulo,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Medium,
                color = fg,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CorridaCard(
    item: CorridaItem,
    onConfirmar: () -> Unit,
    onLongPress: () -> Unit,
) {
    val status = statusEfetivo(item)
    val pendente = status == "A confirmar"
    val recusada = status == "Recusada"

    val porKmCentavos = item.valorPorKmCentavos ?: if (item.km != null && item.km > 0.0) {
        (item.valorCentavos.toDouble() / item.km).roundToLong()
    } else {
        null
    }
    val metricasTexto = buildList {
        if (porKmCentavos != null) add("${formatarReaisPreciso(porKmCentavos)}/km")
        if (item.valorPorHoraCentavos != null) add("${formatarReaisInteiro(item.valorPorHoraCentavos)}/h")
    }.joinToString(" · ")

    val (corStatusTexto, corStatusFundo) = when (status) {
        "Aceita" -> RoxouColors.Green to RoxouColors.Green.copy(alpha = 0.16f)
        "Recusada" -> RoxouColors.TextSecondary to RoxouColors.Track
        else -> RoxouColors.Yellow to RoxouColors.Yellow.copy(alpha = 0.16f)
    }

    val shape = RoundedCornerShape(RoxouRadius.lg)
    val dashColor = RoxouColors.Yellow.copy(alpha = 0.65f)
    val cardModifier = if (!pendente) {
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RoxouColors.SurfaceCard)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
    } else {
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RoxouColors.SurfaceCard.copy(alpha = 0.85f))
            .drawBehind {
                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                )
                drawRoundRect(
                    color = dashColor,
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                    size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                    cornerRadius = CornerRadius(RoxouRadius.lg.toPx(), RoxouRadius.lg.toPx()),
                    style = stroke,
                )
            }
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
    }

    Box(modifier = cardModifier.animateContentSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RoxouSpacing.lg),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (recusada) 0.72f else 1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
                    modifier = Modifier.weight(1f),
                ) {
                    AppMarcaSelo(app = item.app)

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(RoxouSpacing.xs),
                    ) {
                        Text(
                            text = formatarReais(item.valorCentavos),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 24.sp,
                                lineHeight = 28.sp,
                            ),
                            color = RoxouColors.TextPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = metricasTexto.ifEmpty { item.hora },
                            style = MaterialTheme.typography.labelMedium,
                            color = RoxouColors.TextSecondary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(RoxouSpacing.sm))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(RoxouSpacing.xs),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(corStatusFundo)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = corStatusTexto,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }

                    Text(
                        text = item.hora,
                        style = MaterialTheme.typography.labelMedium,
                        color = RoxouColors.TextMuted,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            if (pendente) {
                Spacer(modifier = Modifier.height(RoxouSpacing.md))
                Button(
                    onClick = onConfirmar,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoxouColors.Green,
                        contentColor = RoxouColors.Background,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(RoxouSpacing.sm))
                    Text(
                        text = "Confirmar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun CorridasEstadoVazio(
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(modifier = Modifier.size(88.dp)) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = RoxouColors.SurfaceCard,
                radius = w * 0.48f,
                center = Offset(w / 2f, h / 2f),
            )
            drawRoundRect(
                color = RoxouColors.Track,
                topLeft = Offset(w * 0.22f, h * 0.36f),
                size = Size(w * 0.56f, h * 0.28f),
                cornerRadius = CornerRadius(18f, 18f),
            )
            drawLine(
                color = RoxouColors.PurpleSoft,
                start = Offset(w * 0.32f, h * 0.50f),
                end = Offset(w * 0.68f, h * 0.50f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
            )
            drawCircle(
                color = RoxouColors.Purple,
                radius = 5.dp.toPx(),
                center = Offset(w * 0.30f, h * 0.50f),
            )
            drawCircle(
                color = RoxouColors.Green,
                radius = 5.dp.toPx(),
                center = Offset(w * 0.70f, h * 0.50f),
            )
        }

        Spacer(modifier = Modifier.height(RoxouSpacing.lg))

        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            color = RoxouColors.TextPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(RoxouSpacing.xs))

        Text(
            text = subtitulo,
            style = MaterialTheme.typography.bodyMedium,
            color = RoxouColors.TextSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(
    name = "Corridas - Com filtros e status",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 760,
)
@Composable
private fun CorridasScreenPreview() {
    RoxouDesignTheme {
        CorridasScreen(
            itens = listOf(
                CorridaItem(
                    id = "1",
                    app = "Uber",
                    hora = "14:32",
                    valorCentavos = 1850L,
                    km = 7.4,
                    confirmada = false,
                    origem = "Lida pela tela",
                    status = "A confirmar",
                    valorPorKmCentavos = 250L,
                    valorPorHoraCentavos = 5400L,
                ),
                CorridaItem(
                    id = "2",
                    app = "99",
                    hora = "14:05",
                    valorCentavos = 1420L,
                    km = 5.1,
                    confirmada = true,
                    origem = "Botão Aceitei",
                    status = "Aceita",
                    valorPorKmCentavos = 278L,
                    valorPorHoraCentavos = 4800L,
                ),
                CorridaItem(
                    id = "3",
                    app = "Uber",
                    hora = "13:40",
                    valorCentavos = 1150L,
                    km = 9.8,
                    confirmada = true,
                    origem = "Lida pela tela",
                    status = "Recusada",
                    valorPorKmCentavos = 117L,
                    valorPorHoraCentavos = 2300L,
                ),
                CorridaItem(
                    id = "4",
                    app = "99",
                    hora = "13:12",
                    valorCentavos = 2600L,
                    km = 11.2,
                    confirmada = true,
                    origem = "Manual",
                    status = "Aceita",
                    valorPorKmCentavos = 232L,
                    valorPorHoraCentavos = 5200L,
                ),
            ),
            onAdicionar = {},
            onConfirmar = {},
            onApagar = {},
        )
    }
}

@Preview(
    name = "Corridas - Vazio",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 760,
)
@Composable
private fun CorridasScreenVazioPreview() {
    RoxouDesignTheme {
        CorridasScreen(
            itens = emptyList(),
            onAdicionar = {},
            onConfirmar = {},
            onApagar = {},
        )
    }
}
