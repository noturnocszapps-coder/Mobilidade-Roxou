package br.com.roxou.mobilidade.design

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.sp
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
        mutableLongStateOf(inicial.custoPorKmCentavos.coerceIn(10L, 150L))
    }
    var bairrosEvitados by remember(inicial) {
        mutableStateOf(inicial.bairrosEvitados)
    }
    var novoBairro by remember { mutableStateOf("") }

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
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "O que compensa pra mim",
                    style = MaterialTheme.typography.headlineSmall,
                    color = RoxouColors.TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = RoxouColors.Green, fontWeight = FontWeight.SemiBold)) {
                            append("Verde")
                        }
                        append(" quando passa do BOM. ")
                        withStyle(SpanStyle(color = RoxouColors.Red, fontWeight = FontWeight.SemiBold)) {
                            append("Vermelho")
                        }
                        append(" quando fica abaixo do MÍNIMO.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoxouColors.TextSecondary,
                )
            }

            // Cartão 1: Por km rodado (busca + viagem)
            CompensaCard(titulo = "Por km rodado (busca + viagem)") {
                ControleDeslizanteValor(
                    rotulo = "Mínimo",
                    valorTexto = formatarReaisComCentavos(minimoPorKm),
                    valorCor = RoxouColors.Red,
                    sliderValue = minimoPorKm.toFloat(),
                    valueRange = 80f..400f,
                    steps = 63,
                    accentColor = RoxouColors.Red,
                    onValueChange = { raw ->
                        val snapped = ((raw / 5f).roundToInt() * 5L).coerceIn(80L, 400L)
                        minimoPorKm = snapped
                        if (bomPorKm < snapped) {
                            bomPorKm = snapped
                        }
                    },
                )

                Spacer(modifier = Modifier.height(10.dp))

                ControleDeslizanteValor(
                    rotulo = "Bom",
                    valorTexto = formatarReaisComCentavos(bomPorKm),
                    valorCor = RoxouColors.Green,
                    sliderValue = bomPorKm.toFloat(),
                    valueRange = 80f..400f,
                    steps = 63,
                    accentColor = RoxouColors.Green,
                    onValueChange = { raw ->
                        val snapped = ((raw / 5f).roundToInt() * 5L).coerceIn(80L, 400L)
                        bomPorKm = snapped
                        if (minimoPorKm > snapped) {
                            minimoPorKm = snapped
                        }
                    },
                )
            }

            // Cartão 2: Por hora
            CompensaCard(titulo = "Por hora") {
                ControleDeslizanteValor(
                    rotulo = "Mínimo",
                    valorTexto = formatarReaisInteiro(minimoPorHora),
                    valorCor = RoxouColors.Red,
                    sliderValue = minimoPorHora.toFloat(),
                    valueRange = 1000f..10000f,
                    steps = 89,
                    accentColor = RoxouColors.Red,
                    onValueChange = { raw ->
                        val snapped = ((raw / 100f).roundToInt() * 100L).coerceIn(1000L, 10000L)
                        minimoPorHora = snapped
                        if (bomPorHora < snapped) {
                            bomPorHora = snapped
                        }
                    },
                )

                Spacer(modifier = Modifier.height(10.dp))

                ControleDeslizanteValor(
                    rotulo = "Bom",
                    valorTexto = formatarReaisInteiro(bomPorHora),
                    valorCor = RoxouColors.Green,
                    sliderValue = bomPorHora.toFloat(),
                    valueRange = 1000f..10000f,
                    steps = 89,
                    accentColor = RoxouColors.Green,
                    onValueChange = { raw ->
                        val snapped = ((raw / 100f).roundToInt() * 100L).coerceIn(1000L, 10000L)
                        bomPorHora = snapped
                        if (minimoPorHora > snapped) {
                            minimoPorHora = snapped
                        }
                    },
                )
            }

            // Cartão 3: Busca máxima e Custo por km
            CompensaCard(titulo = "Busca máxima e custo") {
                ControleDeslizanteValor(
                    rotulo = "Busca máxima",
                    valorTexto = formatarBuscaKm(buscaMaximaKm),
                    valorCor = RoxouColors.TextPrimary,
                    sliderValue = buscaMaximaKm.toFloat(),
                    valueRange = 0f..15f,
                    steps = 29,
                    accentColor = RoxouColors.Purple,
                    onValueChange = { raw ->
                        buscaMaximaKm = ((raw * 2f).roundToInt() / 2.0).coerceIn(0.0, 15.0)
                    },
                )

                Spacer(modifier = Modifier.height(10.dp))

                ControleDeslizanteValor(
                    rotulo = "Custo por km",
                    valorTexto = formatarReaisComCentavos(custoPorKm),
                    valorCor = RoxouColors.TextPrimary,
                    sliderValue = custoPorKm.toFloat(),
                    valueRange = 10f..150f,
                    steps = 27,
                    accentColor = RoxouColors.Purple,
                    onValueChange = { raw ->
                        custoPorKm = ((raw / 5f).roundToInt() * 5L).coerceIn(10L, 150L)
                    },
                )
            }

            // Cartão 4: Bairros que eu evito
            CompensaCard(titulo = "Bairros que eu evito") {
                if (bairrosEvitados.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = novoBairro,
                        onValueChange = { novoBairro = it },
                        placeholder = {
                            Text(
                                text = "Nome ou código (ex.: UP5S2)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RoxouColors.TextMuted,
                                maxLines = 1,
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoxouColors.Purple,
                            contentColor = Color.White,
                            disabledContainerColor = RoxouColors.Track,
                            disabledContentColor = RoxouColors.TextMuted,
                        ),
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

            // Exemplo ao vivo
            ExemploAoVivoCard(
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
                    .height(56.dp),
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
private fun CompensaCard(
    titulo: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = RoxouColors.SurfaceCard,
        border = BorderStroke(1.dp, RoxouColors.CardBorder),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = RoxouColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ControleDeslizanteValor(
    rotulo: String,
    valorTexto: String,
    valorCor: Color,
    sliderValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    accentColor: Color,
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
                style = MaterialTheme.typography.labelMedium,
                color = RoxouColors.TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = valorTexto,
                style = MaterialTheme.typography.headlineSmall,
                color = valorCor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Slider(
            value = sliderValue,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
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
                .padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
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
                contentDescription = "Remover $texto",
                tint = RoxouColors.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ExemploAoVivoCard(
    minimoPorKmCentavos: Long,
    bomPorKmCentavos: Long,
    minimoPorHoraCentavos: Long,
    bomPorHoraCentavos: Long,
) {
    val valorCorridaCentavos = 1500L
    val distanciaKm = 8.0
    val duracaoMin = 20.0

    val porKmCentavos = (valorCorridaCentavos / distanciaKm).roundToLong() // 188 (R$ 1,88/km)
    val porHoraCentavos = (valorCorridaCentavos * 60.0 / duracaoMin).roundToLong() // 4500 (R$ 45/h)

    val (statusTexto, statusCor) = when {
        porKmCentavos < minimoPorKmCentavos || porHoraCentavos < minimoPorHoraCentavos ->
            "VERMELHO" to RoxouColors.Red
        porKmCentavos >= bomPorKmCentavos && porHoraCentavos >= bomPorHoraCentavos ->
            "VERDE" to RoxouColors.Green
        else ->
            "AMARELO" to RoxouColors.Yellow
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = RoxouColors.SurfaceCard,
        border = BorderStroke(1.5.dp, statusCor.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Exemplo ao vivo",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoxouColors.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusCor.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = statusTexto,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusCor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            Text(
                text = buildAnnotatedString {
                    append("Corrida de R$ 15 com 8 km e 20 min → ")
                    withStyle(SpanStyle(color = RoxouColors.TextPrimary, fontWeight = FontWeight.SemiBold)) {
                        append("${formatarReaisComCentavos(porKmCentavos)}/km")
                    }
                    append(" e ")
                    withStyle(SpanStyle(color = RoxouColors.TextPrimary, fontWeight = FontWeight.SemiBold)) {
                        append("${formatarReaisInteiro(porHoraCentavos)}/h")
                    }
                    append(" → ")
                    withStyle(SpanStyle(color = statusCor, fontWeight = FontWeight.Bold)) {
                        append(statusTexto)
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = RoxouColors.TextSecondary,
            )
        }
    }
}

private fun formatarReaisComCentavos(centavos: Long): String {
    val reais = centavos / 100L
    val resto = (centavos % 100L).toString().padStart(2, '0')
    return "R$ $reais,$resto"
}

private fun formatarReaisInteiro(centavos: Long): String {
    val reais = centavos / 100L
    return "R$ $reais"
}

private fun formatarBuscaKm(km: Double): String {
    if (km <= 0.0) return "Sem limite"
    val inteiro = km.toInt()
    return if (km == inteiro.toDouble()) {
        "$inteiro km"
    } else {
        "${inteiro},5 km"
    }
}

@Preview(
    name = "O que compensa pra mim",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 393,
    heightDp = 1050,
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
