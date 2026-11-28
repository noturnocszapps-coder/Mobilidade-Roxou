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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private data class AjudaJanela(
    val titulo: String,
    val explicacao: String,
)

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
    perguntarAceite: Boolean = true,
    onPerguntarAceite: (Boolean) -> Unit = {},
    mapaPadrao: String = "Waze",
    onMapaPadrao: (String) -> Unit = {},
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
            perguntarAceite = perguntarAceite,
            mapaPadrao = mapaPadrao,
            onPosicao = onPosicao,
            onSegundos = onSegundos,
            onNotaMinima = onNotaMinima,
            onWaze = onWaze,
            onPerguntarAceite = onPerguntarAceite,
            onMapaPadrao = onMapaPadrao,
            onFechar = onFechar,
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
    perguntarAceite: Boolean,
    mapaPadrao: String,
    onPosicao: (Int) -> Unit,
    onSegundos: (Int) -> Unit,
    onNotaMinima: (Double) -> Unit,
    onWaze: (Boolean) -> Unit,
    onPerguntarAceite: (Boolean) -> Unit,
    onMapaPadrao: (String) -> Unit,
    onFechar: () -> Unit,
) {
    var ajudaAberta by remember { mutableStateOf<AjudaJanela?>(null) }

    val tempos = listOf(2, 3, 5, 10)
    val notas = listOf(
        0.0 to "Não avisar",
        4.70 to "4,70",
        4.80 to "4,80",
        4.85 to "4,85",
        4.90 to "4,90",
    )
    val mapas = listOf("Waze", "Google Maps")

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

        // BLOCO 1: Quando aparece
        BlocoAjustes(titulo = "Quando aparece") {
            // Posição na tela
            Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm)) {
                OpcaoTituloComInfo(
                    titulo = "Posição na tela",
                    onInfo = {
                        ajudaAberta = AjudaJanela(
                            titulo = "Posição na tela",
                            explicacao = "Escolhe em qual lado da tela a janela flutuante aparece quando chega uma oferta.",
                        )
                    },
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

            // Tempo na tela
            Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm)) {
                OpcaoTituloComInfo(
                    titulo = "Tempo na tela",
                    onInfo = {
                        ajudaAberta = AjudaJanela(
                            titulo = "Tempo na tela",
                            explicacao = "Define quantos segundos o aviso fica visível antes de sumir sozinho.",
                        )
                    },
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

            // Nota mínima do passageiro
            Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm)) {
                OpcaoTituloComInfo(
                    titulo = "Nota mínima do passageiro",
                    onInfo = {
                        ajudaAberta = AjudaJanela(
                            titulo = "Nota mínima do passageiro",
                            explicacao = "Mostra uma faixa vermelha de aviso quando a nota do passageiro ficar abaixo desse limite.",
                        )
                    },
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
        }

        // BLOCO 2: Depois do aceite
        BlocoAjustes(titulo = "Depois do aceite") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OpcaoTituloComInfo(
                    titulo = "Perguntar se aceitou",
                    onInfo = {
                        ajudaAberta = AjudaJanela(
                            titulo = "Perguntar se aceitou",
                            explicacao = "Exibe uma confirmação rápida na tela para registrar a corrida nas suas metas.",
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(RoxouSpacing.sm))
                Switch(
                    checked = perguntarAceite,
                    onCheckedChange = onPerguntarAceite,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RoxouColors.Purple,
                        uncheckedThumbColor = RoxouColors.TextSecondary,
                        uncheckedTrackColor = RoxouColors.Track,
                        uncheckedBorderColor = RoxouColors.CardBorder,
                    ),
                )
            }

            HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OpcaoTituloComInfo(
                    titulo = "Abrir rota ao aceitar",
                    onInfo = {
                        ajudaAberta = AjudaJanela(
                            titulo = "Abrir rota ao aceitar",
                            explicacao = "Mostra o atalho direto para abrir o embarque e depois o destino no mapa.",
                        )
                    },
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
        }

        // BLOCO 3: Mapa
        BlocoAjustes(titulo = "Mapa") {
            Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm)) {
                OpcaoTituloComInfo(
                    titulo = "App de navegação",
                    onInfo = {
                        ajudaAberta = AjudaJanela(
                            titulo = "App de navegação",
                            explicacao = "Define qual mapa abre quando você toca em Embarque ou Destino na janela flutuante.",
                        )
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                ) {
                    mapas.forEach { nome ->
                        OpcaoBotao(
                            texto = nome,
                            selecionado = mapaPadrao.equals(nome, ignoreCase = true),
                            onClick = { onMapaPadrao(nome) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
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

    ajudaAberta?.let { ajuda ->
        AlertDialog(
            onDismissRequest = { ajudaAberta = null },
            containerColor = RoxouColors.SurfaceElevated,
            titleContentColor = RoxouColors.TextPrimary,
            textContentColor = RoxouColors.TextSecondary,
            title = {
                Text(
                    text = ajuda.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            text = {
                Text(
                    text = ajuda.explicacao,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoxouColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { ajudaAberta = null }) {
                    Text(
                        text = "Entendi",
                        color = RoxouColors.PurpleSoft,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            },
        )
    }
}

@Composable
private fun BlocoAjustes(
    titulo: String,
    conteudo: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = RoxouColors.PurpleSoft,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = RoxouSpacing.xs),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = RoxouColors.Background,
            border = BorderStroke(1.dp, RoxouColors.CardBorder),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RoxouSpacing.md),
                verticalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
            ) {
                conteudo()
            }
        }
    }
}

@Composable
private fun OpcaoTituloComInfo(
    titulo: String,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = RoxouColors.TextPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        IconButton(
            onClick = onInfo,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Explicação de $titulo",
                tint = RoxouColors.TextMuted,
                modifier = Modifier.size(18.dp),
            )
        }
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
        targetValue = if (selecionado) RoxouColors.PurpleSurface else RoxouColors.SurfaceCard,
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
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(RoxouColors.Background)
                    .padding(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                        .width(12.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(RoxouColors.Track),
                )
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
        targetValue = if (selecionado) RoxouColors.Purple else RoxouColors.SurfaceCard,
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
    name = "Ajustes da janela flutuante - 3 blocos",
    showBackground = true,
    backgroundColor = 0xFF1E1A26,
    widthDp = 360,
    heightDp = 820,
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
                perguntarAceite = true,
                mapaPadrao = "Waze",
                onPosicao = {},
                onSegundos = {},
                onNotaMinima = {},
                onWaze = {},
                onPerguntarAceite = {},
                onMapaPadrao = {},
                onFechar = {},
            )
        }
    }
}
