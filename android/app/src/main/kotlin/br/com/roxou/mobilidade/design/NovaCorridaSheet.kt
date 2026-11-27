package br.com.roxou.mobilidade.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class NovaCorrida(
    val app: String,
    val valorCentavos: Long,
    val km: Double?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaCorridaSheet(
    onSalvar: (NovaCorrida) -> Unit,
    onFechar: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = sheetState,
        containerColor = RoxouColors.SurfaceCard,
        contentColor = RoxouColors.TextPrimary,
        dragHandle = null,
    ) {
        NovaCorridaSheetConteudo(
            onSalvar = onSalvar,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
private fun NovaCorridaSheetConteudo(
    onSalvar: (NovaCorrida) -> Unit,
    modifier: Modifier = Modifier,
) {
    var appSelecionado by remember { mutableStateOf("Uber") }
    var valorTexto by remember { mutableStateOf("") }
    var kmTexto by remember { mutableStateOf("") }

    val valorCentavos = parseCentavos(valorTexto)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RoxouSpacing.xl, vertical = RoxouSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(RoxouSpacing.lg),
    ) {
        Text(
            text = "Nova corrida",
            style = MaterialTheme.typography.titleLarge,
            color = RoxouColors.TextPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )

        // 1. Escolher Uber ou 99
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
        ) {
            AppEscolhaBotao(
                nome = "Uber",
                selecionado = appSelecionado == "Uber",
                corFundoAtivo = RoxouColors.BadgeUberBlack,
                corTextoAtivo = Color.White,
                onClick = { appSelecionado = "Uber" },
                modifier = Modifier.weight(1f),
            )
            AppEscolhaBotao(
                nome = "99",
                selecionado = appSelecionado == "99",
                corFundoAtivo = RoxouColors.Badge99Yellow,
                corTextoAtivo = RoxouColors.Background,
                onClick = { appSelecionado = "99" },
                modifier = Modifier.weight(1f),
            )
        }

        // 2. Valor (teclado numérico)
        OutlinedTextField(
            value = valorTexto,
            onValueChange = { novo ->
                val filtrado = novo.filter { it.isDigit() || it == ',' || it == '.' }
                valorTexto = filtrado
            },
            label = {
                Text(
                    text = "Valor",
                    maxLines = 1,
                    softWrap = false,
                )
            },
            prefix = {
                Text(
                    text = "R$ ",
                    color = RoxouColors.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                )
            },
            placeholder = {
                Text(
                    text = "18,50",
                    color = RoxouColors.TextMuted,
                    maxLines = 1,
                    softWrap = false,
                )
            },
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

        // 3. Km (opcional)
        OutlinedTextField(
            value = kmTexto,
            onValueChange = { novo ->
                val filtrado = novo.filter { it.isDigit() || it == ',' || it == '.' }
                kmTexto = filtrado
            },
            label = {
                Text(
                    text = "Distância (opcional)",
                    maxLines = 1,
                    softWrap = false,
                )
            },
            suffix = {
                Text(
                    text = "km",
                    color = RoxouColors.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                )
            },
            placeholder = {
                Text(
                    text = "6,5",
                    color = RoxouColors.TextMuted,
                    maxLines = 1,
                    softWrap = false,
                )
            },
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

        Spacer(modifier = Modifier.height(RoxouSpacing.xs))

        Button(
            onClick = {
                if (valorCentavos > 0L) {
                    onSalvar(
                        NovaCorrida(
                            app = appSelecionado,
                            valorCentavos = valorCentavos,
                            km = parseKm(kmTexto),
                        ),
                    )
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
private fun AppEscolhaBotao(
    nome: String,
    selecionado: Boolean,
    corFundoAtivo: Color,
    corTextoAtivo: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selecionado) corFundoAtivo else RoxouColors.Background,
        label = "app_btn_bg",
    )
    val fg by animateColorAsState(
        targetValue = if (selecionado) corTextoAtivo else RoxouColors.TextSecondary,
        label = "app_btn_fg",
    )
    val border by animateColorAsState(
        targetValue = if (selecionado) RoxouColors.Purple else RoxouColors.CardBorder,
        label = "app_btn_border",
    )

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = BorderStroke(if (selecionado) 2.dp else 1.dp, border),
        modifier = modifier.height(52.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = nome,
                style = MaterialTheme.typography.titleLarge,
                color = fg,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Preview(
    name = "NovaCorridaSheet",
    showBackground = true,
    backgroundColor = 0xFF1E1A26,
    widthDp = 360,
)
@Composable
private fun NovaCorridaSheetPreview() {
    RoxouDesignTheme {
        Surface(color = RoxouColors.SurfaceCard) {
            NovaCorridaSheetConteudo(onSalvar = {})
        }
    }
}
