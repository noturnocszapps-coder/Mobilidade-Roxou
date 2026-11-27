package br.com.roxou.mobilidade.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JanelaAjustesSheet(
    posicao: Int,
    segundos: Int,
    notaMinima: Double,
    wazeDepoisDoAceite: Boolean,
    onPosicao: (Int) -> Unit,
    onSegundos: (Int) -> Unit,
    onNotaMinima: (Double) -> Unit,
    onWaze: (Boolean) -> Unit,
    onFechar: () -> Unit,
    /** 0 = Waze, 1 = Google Maps. */
    mapa: Int = 0,
    onMapa: (Int) -> Unit = {},
    notificacao: Boolean = true,
    onNotificacao: (Boolean) -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = RoxouColors.SurfaceCard,
        contentColor = RoxouColors.TextPrimary,
        dragHandle = null,
    ) {
        JanelaAjustesConteudo(
            posicao = posicao,
            segundos = segundos,
            notaMinima = notaMinima,
            wazeDepoisDoAceite = wazeDepoisDoAceite,
            onPosicao = onPosicao,
            onSegundos = onSegundos,
            onNotaMinima = onNotaMinima,
            onWaze = onWaze,
            onFechar = onFechar,
            mapa = mapa,
            onMapa = onMapa,
            notificacao = notificacao,
            onNotificacao = onNotificacao,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JanelaAjustesConteudo(
    posicao: Int,
    segundos: Int,
    notaMinima: Double,
    wazeDepoisDoAceite: Boolean,
    onPosicao: (Int) -> Unit,
    onSegundos: (Int) -> Unit,
    onNotaMinima: (Double) -> Unit,
    onWaze: (Boolean) -> Unit,
    onFechar: () -> Unit,
    /** 0 = Waze, 1 = Google Maps. */
    mapa: Int = 0,
    onMapa: (Int) -> Unit = {},
    notificacao: Boolean = true,
    onNotificacao: (Boolean) -> Unit = {},
) {
    val tempos = listOf(2, 3, 5, 10)
    val notas = listOf(
        0.0 to "Não avisar",
        4.70 to "4,70",
        4.80 to "4,80",
        4.85 to "4,85",
        4.90 to "4,90",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(RoxouSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(RoxouSpacing.lg),
    ) {
        // Cabeçalho
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Janela flutuante",
                style = MaterialTheme.typography.titleLarge,
                color = RoxouColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
            )
            IconButton(
                onClick = onFechar,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Fechar",
                    tint = RoxouColors.TextSecondary,
                )
            }
        }

        // 1. Posição (Esquerda, Centro, Direita) com desenho do celular
        Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm)) {
            SecaoCabecalho(
                titulo = "Posição na tela",
                explicacao = "Escolha o lado onde o aviso aparece",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
            ) {
                PosicaoCelularOpcao(
                    rotulo = "Esquerda",
                    alinhamento = Alignment.CenterStart,
                    selecionado = posicao == 0,
                    onClick = { onPosicao(0) },
                    modifier = Modifier.weight(1f),
                )
                PosicaoCelularOpcao(
                    rotulo = "Centro",
                    alinhamento = Alignment.Center,
                    selecionado = posicao == 1,
                    onClick = { onPosicao(1) },
                    modifier = Modifier.weight(1f),
                )
                PosicaoCelularOpcao(
                    rotulo = "Direita",
                    alinhamento = Alignment.CenterEnd,
                    selecionado = posicao == 2,
                    onClick = { onPosicao(2) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)

        // 2. Tempo na tela (2 s, 3 s, 5 s, 10 s)
        Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm)) {
            SecaoCabecalho(
                titulo = "Tempo na tela",
                explicacao = "Quanto tempo a janela fica visível",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
            ) {
                tempos.forEach { valor ->
                    OpcaoBotao(
                        texto = "$valor s",
                        selecionado = segundos == valor,
                        onClick = { onSegundos(valor) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)

        // 3. Nota mínima do passageiro
        Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm)) {
            SecaoCabecalho(
                titulo = "Nota mínima do passageiro",
                explicacao = "Avisa quando a nota ficar abaixo",
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
            ) {
                notas.forEach { (valor, rotulo) ->
                    OpcaoBotao(
                        texto = rotulo,
                        selecionado = abs(notaMinima - valor) < 0.005,
                        onClick = { onNotaMinima(valor) },
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)

        // Mapa padrão
        Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm)) {
            SecaoCabecalho(
                titulo = "Mapa",
                explicacao = "Qual app abre no embarque e no destino",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
            ) {
                listOf("Waze", "Google Maps").forEachIndexed { i, nome ->
                    OpcaoBotao(texto = nome, selecionado = mapa == i, onClick = { onMapa(i) }, modifier = Modifier.weight(1f))
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)

        // 4. Waze depois do aceite
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecaoCabecalho(
                titulo = "Botão do mapa na corrida",
                explicacao = "Ir ao embarque e, com o passageiro a bordo, ir ao destino",
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(RoxouSpacing.sm))
            Switch(
                checked = wazeDepoisDoAceite,
                onCheckedChange = onWaze,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = RoxouColors.Purple,
                    uncheckedThumbColor = RoxouColors.TextSecondary,
                    uncheckedTrackColor = RoxouColors.Track,
                    uncheckedBorderColor = RoxouColors.CardBorder,
                ),
            )
        }

        // Notificação da corrida (reserva)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecaoCabecalho(
                titulo = "Notificação da corrida",
                explicacao = "Embarque e Destino na barra de cima, a qualquer hora",
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(RoxouSpacing.sm))
            Switch(
                checked = notificacao,
                onCheckedChange = onNotificacao,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = RoxouColors.Purple,
                    uncheckedThumbColor = RoxouColors.TextSecondary,
                    uncheckedTrackColor = RoxouColors.Track,
                    uncheckedBorderColor = RoxouColors.CardBorder,
                ),
            )
        }

        // Botão concluir
        Button(
            onClick = onFechar,
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
                text = "Pronto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun SecaoCabecalho(
    titulo: String,
    explicacao: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = RoxouColors.TextPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = explicacao,
            style = MaterialTheme.typography.labelMedium,
            color = RoxouColors.TextSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PosicaoCelularOpcao(
    rotulo: String,
    alinhamento: Alignment,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selecionado) RoxouColors.PurpleSurface else RoxouColors.Background,
        label = "pos_bg",
    )
    val borda by animateColorAsState(
        targetValue = if (selecionado) RoxouColors.Purple else RoxouColors.CardBorder,
        label = "pos_borda",
    )
    val corTexto by animateColorAsState(
        targetValue = if (selecionado) RoxouColors.PurpleSoft else RoxouColors.TextSecondary,
        label = "pos_texto",
    )

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = BorderStroke(if (selecionado) 1.5.dp else 1.dp, borda),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RoxouSpacing.md, horizontal = RoxouSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
        ) {
            // Desenho pequeno do celular mostrando onde a janela flutuante fica
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(RoxouColors.SurfaceCard)
                    .padding(3.dp),
            ) {
                // Topo do celular (alto-falante)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                        .width(12.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(RoxouColors.Track),
                )
                // Janelinha flutuante posicionada
                Box(
                    modifier = Modifier
                        .align(alinhamento)
                        .padding(horizontal = 2.dp)
                        .width(18.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (selecionado) RoxouColors.Green else RoxouColors.PurpleSoft),
                )
            }

            Text(
                text = rotulo,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Medium,
                color = corTexto,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun OpcaoBotao(
    texto: String,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selecionado) RoxouColors.Purple else RoxouColors.Background,
        label = "opcao_bg",
    )
    val borda by animateColorAsState(
        targetValue = if (selecionado) RoxouColors.Purple else RoxouColors.CardBorder,
        label = "opcao_borda",
    )
    val fg by animateColorAsState(
        targetValue = if (selecionado) Color.White else RoxouColors.TextSecondary,
        label = "opcao_fg",
    )

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = bg,
        border = BorderStroke(1.dp, borda),
        modifier = modifier.height(48.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = RoxouSpacing.md),
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Medium,
                color = fg,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Preview(
    name = "Ajustes da janela flutuante",
    showBackground = true,
    backgroundColor = 0xFF1E1A26,
    widthDp = 360,
    heightDp = 640,
)
@Composable
private fun JanelaAjustesSheetPreview() {
    RoxouDesignTheme {
        Surface(color = RoxouColors.SurfaceCard) {
            JanelaAjustesConteudo(
                posicao = 2,
                segundos = 5,
                notaMinima = 4.80,
                wazeDepoisDoAceite = true,
                onPosicao = {},
                onSegundos = {},
                onNotaMinima = {},
                onWaze = {},
                onFechar = {},
            )
        }
    }
}
