package br.com.roxou.mobilidade.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompensaScreen(
    inicial: CompensaUi,
    onSalvar: (CompensaUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var minimoPorKm by remember(inicial) {
        mutableLongStateOf(inicial.minimoPorKmCentavos.coerceIn(80L, 400L))
    }
    var bomPorKm by remember(inicial) {
        mutableLongStateOf(inicial.bomPorKmCentavos.coerceIn(minimoPorKm, 400L))
    }
    var minimoPorHora by remember(inicial) {
        mutableLongStateOf(inicial.minimoPorHoraCentavos.coerceIn(1000L, 10000L))
    }
    var bomPorHora by remember(inicial) {
        mutableLongStateOf(inicial.bomPorHoraCentavos.coerceIn(minimoPorHora, 10000L))
    }
    var buscaMaximaKm by remember(inicial) {
        mutableDoubleStateOf(inicial.buscaMaximaKm.coerceIn(0.0, 15.0))
    }
    var custoPorKm by remember(inicial) {
        mutableLongStateOf(inicial.custoPorKmCentavos.coerceIn(10L, 300L))
    }
    var bairrosEvitados by remember(inicial) {
        mutableStateOf(inicial.bairrosEvitados)
    }
    var novoBairro by remember { mutableStateOf("") }
    var maisOpcoesAberto by remember { mutableStateOf(false) }

    fun adicionarBairro() {
        val limpo = novoBairro.trim()
        if (limpo.isNotEmpty() && bairrosEvitados.none { it.equals(limpo, ignoreCase = true) }) {
            bairrosEvitados = bairrosEvitados + limpo
        }
        novoBairro = ""
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = RoxouColors.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RoxouSpacing.lg, vertical = RoxouSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RoxouSpacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.xs)) {
                Text(
                    text = "O que compensa",
                    style = MaterialTheme.typography.headlineLarge,
                    color = RoxouColors.TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = RoxouColors.Red, fontWeight = FontWeight.SemiBold)) {
                            append("Mínimo")
                        }
                        append(" · ")
                        withStyle(SpanStyle(color = RoxouColors.Green, fontWeight = FontWeight.SemiBold)) {
                            append("Bom")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoxouColors.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            // Cartão único: R$ por km + R$ por hora com RangeSlider
            RoxouCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = RoxouSpacing.lg,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.md)) {
                    FaixaControle(
                        titulo = "R$ por km",
                        minimoTexto = formatarReaisPreciso(minimoPorKm),
                        bomTexto = formatarReaisPreciso(bomPorKm),
                        value = minimoPorKm.toFloat()..bomPorKm.toFloat(),
                        valueRange = 80f..400f,
                        steps = 63,
                        onValueChange = { range ->
                            val novoMin = ((range.start / 5f).roundToInt() * 5L).coerceIn(80L, 400L)
                            val novoBom = ((range.endInclusive / 5f).roundToInt() * 5L).coerceIn(novoMin, 400L)
                            minimoPorKm = novoMin
                            bomPorKm = novoBom
                        },
                    )

                    HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)

                    FaixaControle(
                        titulo = "R$ por hora",
                        minimoTexto = formatarReaisInteiro(minimoPorHora),
                        bomTexto = formatarReaisInteiro(bomPorHora),
                        value = minimoPorHora.toFloat()..bomPorHora.toFloat(),
                        valueRange = 1000f..10000f,
                        steps = 89,
                        onValueChange = { range ->
                            val novoMin = ((range.start / 100f).roundToInt() * 100L).coerceIn(1000L, 10000L)
                            val novoBom = ((range.endInclusive / 100f).roundToInt() * 100L).coerceIn(novoMin, 10000L)
                            minimoPorHora = novoMin
                            bomPorHora = novoBom
                        },
                    )
                }
            }

            // Cartão "Mais opções" (começa fechado)
            RoxouCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                contentPadding = RoxouSpacing.lg,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RoxouRadius.sm))
                            .clickable { maisOpcoesAberto = !maisOpcoesAberto }
                            .padding(vertical = RoxouSpacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mais opções",
                                style = MaterialTheme.typography.titleMedium,
                                color = RoxouColors.TextPrimary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!maisOpcoesAberto) {
                                Text(
                                    text = "${formatarBuscaKm(buscaMaximaKm)} · ${formatarReaisPreciso(custoPorKm)}/km",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RoxouColors.TextMuted,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Icon(
                            imageVector = if (maisOpcoesAberto) {
                                Icons.Filled.KeyboardArrowUp
                            } else {
                                Icons.Filled.KeyboardArrowDown
                            },
                            contentDescription = if (maisOpcoesAberto) "Fechar" else "Abrir",
                            tint = RoxouColors.TextSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    if (maisOpcoesAberto) {
                        Spacer(modifier = Modifier.height(RoxouSpacing.md))
                        HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)
                        Spacer(modifier = Modifier.height(RoxouSpacing.md))

                        SimplesSlider(
                            rotulo = "Busca máxima",
                            valorTexto = formatarBuscaKm(buscaMaximaKm),
                            value = buscaMaximaKm.toFloat(),
                            valueRange = 0f..15f,
                            steps = 29,
                            onValueChange = { raw ->
                                buscaMaximaKm = ((raw * 2f).roundToInt() / 2.0).coerceIn(0.0, 15.0)
                            },
                        )

                        Spacer(modifier = Modifier.height(RoxouSpacing.sm))

                        SimplesSlider(
                            rotulo = "Custo por km",
                            valorTexto = formatarReaisPreciso(custoPorKm),
                            value = custoPorKm.toFloat(),
                            valueRange = 10f..300f,
                            steps = 57,
                            onValueChange = { raw ->
                                custoPorKm = ((raw / 5f).roundToInt() * 5L).coerceIn(10L, 300L)
                            },
                        )
                    }
                }
            }

            // Bairros evitados em chips
            RoxouCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = RoxouSpacing.lg,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Bairros evitados",
                        style = MaterialTheme.typography.titleMedium,
                        color = RoxouColors.TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(RoxouSpacing.md))

                    if (bairrosEvitados.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                        ) {
                            bairrosEvitados.forEach { bairro ->
                                BairroChip(
                                    texto = bairro,
                                    onRemover = {
                                        bairrosEvitados = bairrosEvitados.filterNot { it == bairro }
                                    },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(RoxouSpacing.md))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = novoBairro,
                            onValueChange = { novoBairro = it },
                            placeholder = {
                                Text(
                                    text = "Bairro ou código",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = RoxouColors.TextMuted,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { adicionarBairro() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoxouColors.Purple,
                                unfocusedBorderColor = RoxouColors.CardBorder,
                                focusedContainerColor = RoxouColors.Background,
                                unfocusedContainerColor = RoxouColors.Background,
                                focusedTextColor = RoxouColors.TextPrimary,
                                unfocusedTextColor = RoxouColors.TextPrimary,
                                cursorColor = RoxouColors.Purple,
                            ),
                            modifier = Modifier.weight(1f),
                        )

                        Button(
                            onClick = { adicionarBairro() },
                            enabled = novoBairro.trim().isNotEmpty(),
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = RoxouSpacing.md),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoxouColors.Purple,
                                contentColor = Color.White,
                                disabledContainerColor = RoxouColors.Track,
                                disabledContentColor = RoxouColors.TextMuted,
                            ),
                            modifier = Modifier.height(52.dp),
                        ) {
                            Text(
                                text = "Adicionar",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }

            // Exemplo ao vivo pequeno
            ExemploAoVivoCompacto(
                minimoPorKmCentavos = minimoPorKm,
                bomPorKmCentavos = bomPorKm,
                minimoPorHoraCentavos = minimoPorHora,
                bomPorHoraCentavos = bomPorHora,
            )

            // Botão Salvar
            Button(
                onClick = {
                    onSalvar(
                        CompensaUi(
                            minimoPorKmCentavos = minimoPorKm,
                            bomPorKmCentavos = bomPorKm,
                            minimoPorHoraCentavos = minimoPorHora,
                            bomPorHoraCentavos = bomPorHora,
                            custoPorKmCentavos = custoPorKm,
                            buscaMaximaKm = buscaMaximaKm,
                            bairrosEvitados = bairrosEvitados,
                        ),
                    )
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoxouColors.Purple,
                    contentColor = Color.White,
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
}

@Composable
private fun FaixaControle(
    titulo: String,
    minimoTexto: String,
    bomTexto: String,
    value: ClosedFloatingPointRange<Float>,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = RoxouColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = RoxouColors.Red, fontWeight = FontWeight.Bold)) {
                        append(minimoTexto)
                    }
                    withStyle(SpanStyle(color = RoxouColors.TextMuted)) {
                        append(" – ")
                    }
                    withStyle(SpanStyle(color = RoxouColors.Green, fontWeight = FontWeight.Bold)) {
                        append(bomTexto)
                    }
                },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }

        RangeSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = RoxouColors.Purple,
                activeTrackColor = RoxouColors.Purple,
                inactiveTrackColor = RoxouColors.Track,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SimplesSlider(
    rotulo: String,
    valorTexto: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = rotulo,
                style = MaterialTheme.typography.bodyMedium,
                color = RoxouColors.TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = valorTexto,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RoxouColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = RoxouColors.Purple,
                activeTrackColor = RoxouColors.Purple,
                inactiveTrackColor = RoxouColors.Track,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BairroChip(
    texto: String,
    onRemover: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = RoxouColors.Track,
        border = BorderStroke(1.dp, RoxouColors.CardBorder),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onRemover)
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                color = RoxouColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remover",
                tint = RoxouColors.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ExemploAoVivoCompacto(
    minimoPorKmCentavos: Long,
    bomPorKmCentavos: Long,
    minimoPorHoraCentavos: Long,
    bomPorHoraCentavos: Long,
) {
    val porKmCentavos = (1500.0 / 8.0).roundToLong() // 188 -> R$ 1,88/km
    val porHoraCentavos = (1500.0 * 60.0 / 20.0).roundToLong() // 4500 -> R$ 45/h

    val (statusTexto, alvoCor) = when {
        porKmCentavos < minimoPorKmCentavos || porHoraCentavos < minimoPorHoraCentavos ->
            "VERMELHO" to RoxouColors.Red
        porKmCentavos >= bomPorKmCentavos && porHoraCentavos >= bomPorHoraCentavos ->
            "VERDE" to RoxouColors.Green
        else ->
            "AMARELO" to RoxouColors.Yellow
    }

    val statusCor by animateColorAsState(targetValue = alvoCor, label = "exemplo_cor")

    RoxouCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = RoxouSpacing.md,
        borderColor = statusCor.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "R$ 15 · 8 km · 20 min",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoxouColors.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatarReaisPreciso(porKmCentavos)}/km · ${formatarReaisInteiro(porHoraCentavos)}/h",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = RoxouColors.TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(RoxouSpacing.sm))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusCor.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = statusTexto,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusCor,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Preview(
    name = "Compensa - Leve com RangeSlider",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 780,
)
@Composable
private fun CompensaScreenPreview() {
    RoxouDesignTheme {
        CompensaScreen(
            inicial = CompensaUi(
                minimoPorKmCentavos = 150L,
                bomPorKmCentavos = 200L,
                minimoPorHoraCentavos = 3000L,
                bomPorHoraCentavos = 4000L,
                custoPorKmCentavos = 45L,
                buscaMaximaKm = 5.0,
                bairrosEvitados = listOf("Jardim Leonor", "UP5S2"),
            ),
            onSalvar = {},
        )
    }
}
