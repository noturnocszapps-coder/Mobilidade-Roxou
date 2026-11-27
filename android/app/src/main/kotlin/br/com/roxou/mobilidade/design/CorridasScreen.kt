package br.com.roxou.mobilidade.design

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

data class CorridaItem(
    val id: String,
    val app: String,
    val hora: String,
    val valorCentavos: Long,
    val km: Double?,
    val confirmada: Boolean,
    val origem: String,
)

@Composable
fun CorridasScreen(
    itens: List<CorridaItem>,
    onAdicionar: () -> Unit,
    onConfirmar: (String) -> Unit,
    onApagar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var itemParaApagar by remember { mutableStateOf<CorridaItem?>(null) }

    val qtdUber = itens.count { it.app.equals("Uber", ignoreCase = true) && it.confirmada }
    val qtd99 = itens.count { it.app.trim() == "99" && it.confirmada }
    val qtdPendentes = itens.count { !it.confirmada }

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

                Spacer(modifier = Modifier.height(RoxouSpacing.lg))

                if (itens.isEmpty()) {
                    CorridasEstadoVazio(
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
                        items(itens, key = { it.id }) { item ->
                            CorridaCard(
                                item = item,
                                onConfirmar = { onConfirmar(item.id) },
                                onLongPress = { itemParaApagar = item },
                            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CorridaCard(
    item: CorridaItem,
    onConfirmar: () -> Unit,
    onLongPress: () -> Unit,
) {
    val shape = RoundedCornerShape(RoxouRadius.lg)
    val dashColor = RoxouColors.Yellow.copy(alpha = 0.65f)
    val cardModifier = if (item.confirmada) {
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RoxouColors.SurfaceCard)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
    } else {
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RoxouColors.SurfaceCard.copy(alpha = 0.72f))
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
                    .alpha(if (item.confirmada) 1f else 0.85f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
                    modifier = Modifier.weight(1f),
                ) {
                    AppMarcaSelo(app = item.app)

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                        ) {
                            Text(
                                text = item.hora,
                                style = MaterialTheme.typography.titleMedium,
                                color = RoxouColors.TextPrimary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!item.confirmada) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(RoxouColors.Yellow.copy(alpha = 0.16f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "a confirmar",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = RoxouColors.Yellow,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = item.origem,
                            style = MaterialTheme.typography.labelMedium,
                            color = RoxouColors.TextMuted,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(RoxouSpacing.sm))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatarReais(item.valorCentavos),
                        style = MaterialTheme.typography.titleLarge,
                        color = RoxouColors.TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.km != null) {
                        Text(
                            text = formatarKm(item.km),
                            style = MaterialTheme.typography.labelMedium,
                            color = RoxouColors.TextSecondary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (!item.confirmada) {
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
private fun CorridasEstadoVazio(modifier: Modifier = Modifier) {
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
            text = "Nenhuma corrida hoje",
            style = MaterialTheme.typography.titleLarge,
            color = RoxouColors.TextPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(RoxouSpacing.xs))

        Text(
            text = "Toque em + para registrar",
            style = MaterialTheme.typography.bodyMedium,
            color = RoxouColors.TextSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(
    name = "Corridas - Com lista",
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
                ),
                CorridaItem(
                    id = "2",
                    app = "99",
                    hora = "14:05",
                    valorCentavos = 1420L,
                    km = 5.1,
                    confirmada = false,
                    origem = "Botão Aceitei",
                ),
                CorridaItem(
                    id = "3",
                    app = "Uber",
                    hora = "13:40",
                    valorCentavos = 2600L,
                    km = 11.2,
                    confirmada = true,
                    origem = "Lida pela tela",
                ),
                CorridaItem(
                    id = "4",
                    app = "99",
                    hora = "13:12",
                    valorCentavos = 1200L,
                    km = null,
                    confirmada = true,
                    origem = "Manual",
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
