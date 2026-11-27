package br.com.roxou.mobilidade.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class GastoItem(
    val id: String,
    val descricao: String,
    val valorCentavos: Long,
    val dataTexto: String,
    val combustivel: Boolean,
)

data class FinancasUi(
    val ganhosSemanaCentavos: Long,
    val gastosSemanaCentavos: Long,
    val combustivelSemanaCentavos: Long,
    val outrosGastosSemanaCentavos: Long,
    val dinheiroLivreCentavos: Long,
    val parcelaFaltaCentavos: Long?,
    val gastos: List<GastoItem>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancasScreen(
    ui: FinancasUi,
    onLancarGasto: (descricao: String, valorCentavos: Long, combustivel: Boolean) -> Unit,
    onApagarGasto: (id: String) -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostrarFolhaGasto by remember { mutableStateOf(false) }
    var gastoParaApagar by remember { mutableStateOf<GastoItem?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = RoxouColors.Background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = RoxouSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RoxouSpacing.lg),
            contentPadding = PaddingValues(top = RoxouSpacing.lg, bottom = RoxouSpacing.xxl),
        ) {
            // Topo com voltar
            item {
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
                        text = "Finanças",
                        style = MaterialTheme.typography.headlineLarge,
                        color = RoxouColors.TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Cartão principal: Dinheiro livre da semana + resumo
            item {
                RoxouCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = RoxouSpacing.xl,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Dinheiro livre da semana",
                            style = MaterialTheme.typography.labelMedium,
                            color = RoxouColors.TextSecondary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(RoxouSpacing.sm))

                        Text(
                            text = formatarReais(ui.dinheiroLivreCentavos),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp, lineHeight = 48.sp),
                            color = if (ui.dinheiroLivreCentavos < 0L) RoxouColors.Red else RoxouColors.TextPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(RoxouSpacing.lg))
                        HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)
                        Spacer(modifier = Modifier.height(RoxouSpacing.md))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                        ) {
                            ResumoNumeroMini(
                                rotulo = "Ganhos",
                                valor = formatarReais(ui.ganhosSemanaCentavos),
                                corValor = RoxouColors.Green,
                                modifier = Modifier.weight(1f),
                            )
                            ResumoNumeroMini(
                                rotulo = "Gastos",
                                valor = formatarReais(ui.gastosSemanaCentavos),
                                corValor = RoxouColors.TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            if (ui.parcelaFaltaCentavos != null) {
                                val paga = ui.parcelaFaltaCentavos <= 0L
                                ResumoNumeroMini(
                                    rotulo = "Parcela",
                                    valor = if (paga) "Paga" else formatarReais(ui.parcelaFaltaCentavos),
                                    corValor = if (paga) RoxouColors.Green else RoxouColors.Red,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        if (ui.combustivelSemanaCentavos > 0L || ui.outrosGastosSemanaCentavos > 0L) {
                            Spacer(modifier = Modifier.height(RoxouSpacing.md))
                            Text(
                                text = "Combustível ${formatarReais(ui.combustivelSemanaCentavos)} · Outros ${formatarReais(ui.outrosGastosSemanaCentavos)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = RoxouColors.TextMuted,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Botão grande "+ Lançar gasto"
            item {
                Button(
                    onClick = { mostrarFolhaGasto = true },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoxouColors.Purple,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(RoxouSpacing.sm))
                    Text(
                        text = "Lançar gasto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            // Título da lista
            item {
                Text(
                    text = "Gastos da semana",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoxouColors.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = RoxouSpacing.xs),
                )
            }

            if (ui.gastos.isEmpty()) {
                item {
                    RoxouCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = RoxouSpacing.xl,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(RoxouSpacing.xs),
                        ) {
                            Text(
                                text = "Nenhum gasto lançado",
                                style = MaterialTheme.typography.titleMedium,
                                color = RoxouColors.TextPrimary,
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                text = "Combustível e outros aparecem aqui",
                                style = MaterialTheme.typography.labelMedium,
                                color = RoxouColors.TextMuted,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            } else {
                items(ui.gastos, key = { it.id }) { gasto ->
                    GastoCard(
                        item = gasto,
                        onApagar = { gastoParaApagar = gasto },
                    )
                }
            }
        }
    }

    if (mostrarFolhaGasto) {
        ModalBottomSheet(
            onDismissRequest = { mostrarFolhaGasto = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = RoxouColors.SurfaceCard,
            dragHandle = null,
        ) {
            NovoGastoFolhaConteudo(
                onSalvar = { descricao, valorCentavos, combustivel ->
                    onLancarGasto(descricao, valorCentavos, combustivel)
                    mostrarFolhaGasto = false
                },
            )
        }
    }

    gastoParaApagar?.let { alvo ->
        AlertDialog(
            onDismissRequest = { gastoParaApagar = null },
            containerColor = RoxouColors.SurfaceCard,
            titleContentColor = RoxouColors.TextPrimary,
            textContentColor = RoxouColors.TextSecondary,
            title = {
                Text(
                    text = "Apagar gasto?",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    softWrap = false,
                )
            },
            text = {
                Text(
                    text = "${alvo.descricao} · ${formatarReais(alvo.valorCentavos)}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onApagarGasto(alvo.id)
                        gastoParaApagar = null
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
                TextButton(onClick = { gastoParaApagar = null }) {
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
private fun ResumoNumeroMini(
    rotulo: String,
    valor: String,
    corValor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelMedium,
            color = RoxouColors.TextMuted,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(RoxouSpacing.xs))
        Text(
            text = valor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = corValor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GastoCard(
    item: GastoItem,
    onApagar: () -> Unit,
) {
    RoxouCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = RoxouSpacing.md,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (item.combustivel) {
                                    RoxouColors.PurpleSurface
                                } else {
                                    RoxouColors.Track
                                },
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = if (item.combustivel) "Combustível" else "Outro",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (item.combustivel) RoxouColors.PurpleSoft else RoxouColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    Text(
                        text = item.dataTexto,
                        style = MaterialTheme.typography.labelMedium,
                        color = RoxouColors.TextMuted,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(RoxouSpacing.xs))

                Text(
                    text = item.descricao,
                    style = MaterialTheme.typography.titleMedium,
                    color = RoxouColors.TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = formatarReais(item.valorCentavos),
                style = MaterialTheme.typography.titleLarge,
                color = RoxouColors.Red,
                maxLines = 1,
                softWrap = false,
            )

            IconButton(
                onClick = onApagar,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Apagar gasto",
                    tint = RoxouColors.TextMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun NovoGastoFolhaConteudo(
    onSalvar: (descricao: String, valorCentavos: Long, combustivel: Boolean) -> Unit,
) {
    var valorTexto by remember { mutableStateOf("") }
    var combustivel by remember { mutableStateOf(true) }
    var descricao by remember { mutableStateOf("") }

    val valorCentavos = parseCentavos(valorTexto)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(RoxouSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(RoxouSpacing.lg),
    ) {
        Text(
            text = "Lançar gasto",
            style = MaterialTheme.typography.titleLarge,
            color = RoxouColors.TextPrimary,
            maxLines = 1,
            softWrap = false,
        )

        OutlinedTextField(
            value = valorTexto,
            onValueChange = { valorTexto = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' } },
            label = { Text("Valor", maxLines = 1, softWrap = false) },
            prefix = { Text("R$ ", color = RoxouColors.TextSecondary, maxLines = 1, softWrap = false) },
            placeholder = { Text("120,00", color = RoxouColors.TextMuted, maxLines = 1, softWrap = false) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RoxouColors.Purple,
                unfocusedBorderColor = RoxouColors.CardBorder,
                focusedContainerColor = RoxouColors.Background,
                unfocusedContainerColor = RoxouColors.Background,
                focusedTextColor = RoxouColors.TextPrimary,
                unfocusedTextColor = RoxouColors.TextPrimary,
                focusedLabelColor = RoxouColors.PurpleSoft,
                unfocusedLabelColor = RoxouColors.TextSecondary,
                cursorColor = RoxouColors.Purple,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
        ) {
            TipoGastoBotao(
                rotulo = "Combustível",
                selecionado = combustivel,
                onClick = { combustivel = true },
                modifier = Modifier.weight(1f),
            )
            TipoGastoBotao(
                rotulo = "Outro",
                selecionado = !combustivel,
                onClick = { combustivel = false },
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição (opcional)", maxLines = 1, softWrap = false) },
            placeholder = { Text("Ex.: Posto Shell", color = RoxouColors.TextMuted, maxLines = 1, softWrap = false) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RoxouColors.Purple,
                unfocusedBorderColor = RoxouColors.CardBorder,
                focusedContainerColor = RoxouColors.Background,
                unfocusedContainerColor = RoxouColors.Background,
                focusedTextColor = RoxouColors.TextPrimary,
                unfocusedTextColor = RoxouColors.TextPrimary,
                focusedLabelColor = RoxouColors.PurpleSoft,
                unfocusedLabelColor = RoxouColors.TextSecondary,
                cursorColor = RoxouColors.Purple,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                if (valorCentavos > 0L) {
                    val descFinal = descricao.trim().ifEmpty {
                        if (combustivel) "Combustível" else "Outro"
                    }
                    onSalvar(descFinal, valorCentavos, combustivel)
                }
            },
            enabled = valorCentavos > 0L,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = RoxouColors.Purple,
                contentColor = Color.White,
                disabledContainerColor = RoxouColors.Track,
                disabledContentColor = RoxouColors.TextMuted,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                text = "Salvar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun TipoGastoBotao(
    rotulo: String,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selecionado) RoxouColors.PurpleSurface else RoxouColors.Background,
        label = "tipo_gasto_bg",
    )
    val border by animateColorAsState(
        targetValue = if (selecionado) RoxouColors.Purple else RoxouColors.CardBorder,
        label = "tipo_gasto_border",
    )
    val fg by animateColorAsState(
        targetValue = if (selecionado) RoxouColors.PurpleSoft else RoxouColors.TextSecondary,
        label = "tipo_gasto_fg",
    )

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = BorderStroke(if (selecionado) 1.5.dp else 1.dp, border),
        modifier = modifier.height(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = rotulo,
                style = MaterialTheme.typography.titleMedium,
                color = fg,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Preview(
    name = "Finanças - Com gastos",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 780,
)
@Composable
private fun FinancasScreenPreview() {
    RoxouDesignTheme {
        FinancasScreen(
            ui = FinancasUi(
                ganhosSemanaCentavos = 185000L,
                gastosSemanaCentavos = 47100L,
                combustivelSemanaCentavos = 39000L,
                outrosGastosSemanaCentavos = 8100L,
                dinheiroLivreCentavos = 23000L,
                parcelaFaltaCentavos = 114900L,
                gastos = listOf(
                    GastoItem("1", "Posto Ipiranga", 15000L, "Hoje · 11:20", true),
                    GastoItem("2", "Lava rápido", 4500L, "Ontem · 17:40", false),
                    GastoItem("3", "Etanol", 12000L, "Qua · 08:15", true),
                ),
            ),
            onLancarGasto = { _, _, _ -> },
            onApagarGasto = {},
            onVoltar = {},
        )
    }
}

@Preview(
    name = "Finanças - Novo gasto folha",
    showBackground = true,
    backgroundColor = 0xFF1E1A26,
    widthDp = 360,
)
@Composable
private fun NovoGastoFolhaPreview() {
    RoxouDesignTheme {
        Surface(color = RoxouColors.SurfaceCard) {
            NovoGastoFolhaConteudo(onSalvar = { _, _, _ -> })
        }
    }
}
