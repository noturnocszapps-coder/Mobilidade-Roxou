package br.com.roxou.mobilidade.design

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class Aba { INICIO, CORRIDAS, COMPENSA, AJUSTES }

@Composable
fun AppShell(
    abaAtual: Aba,
    mostrarCompensa: Boolean,
    onAba: (Aba) -> Unit,
    conteudo: @Composable () -> Unit,
) {
    val abasVisiveis = buildList {
        add(Aba.INICIO)
        add(Aba.CORRIDAS)
        if (mostrarCompensa) add(Aba.COMPENSA)
        add(Aba.AJUSTES)
    }

    Scaffold(
        containerColor = RoxouColors.Background,
        bottomBar = {
            Surface(color = RoxouColors.SurfaceCard) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RoxouSpacing.sm, vertical = RoxouSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        abasVisiveis.forEach { aba ->
                            AbaItem(
                                aba = aba,
                                selecionada = aba == abaAtual,
                                onClick = { onAba(aba) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RoxouColors.Background)
                .padding(innerPadding),
        ) {
            conteudo()
        }
    }
}

@Composable
private fun AbaItem(
    aba: Aba,
    selecionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val corConteudo by animateColorAsState(
        targetValue = if (selecionada) RoxouColors.PurpleSoft else RoxouColors.TextMuted,
        label = "aba_cor",
    )
    val corFundo by animateColorAsState(
        targetValue = if (selecionada) RoxouColors.PurpleSurface else RoxouColors.SurfaceCard,
        label = "aba_fundo",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(RoxouRadius.md))
            .background(corFundo)
            .clickable(onClick = onClick)
            .padding(vertical = RoxouSpacing.sm, horizontal = RoxouSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = iconeDaAba(aba),
            contentDescription = rotuloDaAba(aba),
            tint = corConteudo,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(RoxouSpacing.xs))
        Text(
            text = rotuloDaAba(aba),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selecionada) FontWeight.SemiBold else FontWeight.Medium,
            color = corConteudo,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun rotuloDaAba(aba: Aba): String = when (aba) {
    Aba.INICIO -> "Início"
    Aba.CORRIDAS -> "Corridas"
    Aba.COMPENSA -> "Compensa"
    Aba.AJUSTES -> "Ajustes"
}

private fun iconeDaAba(aba: Aba): ImageVector = when (aba) {
    Aba.INICIO -> Icons.Filled.Home
    Aba.CORRIDAS -> Icons.Filled.DateRange
    Aba.COMPENSA -> Icons.Filled.Star
    Aba.AJUSTES -> Icons.Filled.Settings
}

@Preview(
    name = "AppShell - 4 abas",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 760,
)
@Composable
private fun AppShellPreview() {
    RoxouDesignTheme {
        AppShell(
            abaAtual = Aba.INICIO,
            mostrarCompensa = true,
            onAba = {},
        ) {
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
}

@Preview(
    name = "AppShell - 3 abas (sem Compensa)",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 760,
)
@Composable
private fun AppShellSemCompensaPreview() {
    RoxouDesignTheme {
        AppShell(
            abaAtual = Aba.INICIO,
            mostrarCompensa = false,
            onAba = {},
        ) {
            PainelScreen(
                ui = PainelUi(
                    diaTexto = "Sex 27/11",
                    corridasHoje = 8,
                    metaHoje = 0,
                    corridasUberHoje = 5,
                    corridas99Hoje = 3,
                    faturadoHojeCentavos = 16500L,
                    corridasSemanaUber = 0,
                    proximaFaixaCorridas = null,
                    proximaFaixaDescontoCentavos = null,
                    descontoGarantidoCentavos = 0L,
                    parcelaFaltaCentavos = null,
                    dinheiroLivreCentavos = 16500L,
                    recursos = Recursos(
                        metaLocadora = false,
                        locacao = false,
                        financas = true,
                        metaDiaria = false,
                        janelaFlutuante = false,
                    ),
                ),
            )
        }
    }
}
