package br.com.roxou.mobilidade.design

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp

data class DiagnosticoUi(
    val lidas: Int,
    val falhas: Int,
    val erros: Int,
    val ultimaOferta: String?,
    val eventos: List<String>,
)

@Composable
fun DiagnosticoModelo(
    ui: DiagnosticoUi,
    onExportar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var detalhesAbertos by remember { mutableStateOf(false) }

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
            Text(
                text = "Diagnóstico",
                style = MaterialTheme.typography.headlineLarge,
                color = RoxouColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )

            // 3 números grandes no topo: Ofertas lidas, Falhas, Erros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
            ) {
                NumeroDiagnosticoCard(
                    rotulo = "Ofertas lidas",
                    valor = ui.lidas,
                    cor = RoxouColors.PurpleSoft,
                    modifier = Modifier.weight(1f),
                )
                NumeroDiagnosticoCard(
                    rotulo = "Falhas",
                    valor = ui.falhas,
                    cor = if (ui.falhas > 0) RoxouColors.Yellow else RoxouColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                NumeroDiagnosticoCard(
                    rotulo = "Erros",
                    valor = ui.erros,
                    cor = if (ui.erros > 0) RoxouColors.Red else RoxouColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }

            // Última oferta lida: app, valor, R$/km e R$/h
            UltimaOfertaCard(ultimaOferta = ui.ultimaOferta)

            // Botão grande "Exportar registro"
            Button(
                onClick = onExportar,
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
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(RoxouSpacing.sm))
                Text(
                    text = "Exportar registro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            // Lista de eventos recolhida, com botão "Ver detalhes"
            RoxouCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                contentPadding = RoxouSpacing.md,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(RoxouRadius.sm))
                            .clickable { detalhesAbertos = !detalhesAbertos }
                            .padding(horizontal = RoxouSpacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (detalhesAbertos) "Ocultar detalhes" else "Ver detalhes",
                                style = MaterialTheme.typography.titleMedium,
                                color = RoxouColors.TextPrimary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${ui.eventos.size} eventos registrados",
                                style = MaterialTheme.typography.labelMedium,
                                color = RoxouColors.TextMuted,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = if (detalhesAbertos) {
                                Icons.Filled.KeyboardArrowUp
                            } else {
                                Icons.Filled.KeyboardArrowDown
                            },
                            contentDescription = if (detalhesAbertos) "Fechar" else "Abrir",
                            tint = RoxouColors.TextSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    if (detalhesAbertos) {
                        Spacer(modifier = Modifier.height(RoxouSpacing.sm))
                        HorizontalDivider(thickness = 1.dp, color = RoxouColors.Divider)
                        Spacer(modifier = Modifier.height(RoxouSpacing.sm))

                        if (ui.eventos.isEmpty()) {
                            Text(
                                text = "Nenhum evento registrado hoje",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RoxouColors.TextMuted,
                                modifier = Modifier.padding(
                                    horizontal = RoxouSpacing.xs,
                                    vertical = RoxouSpacing.sm,
                                ),
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RoxouSpacing.xs),
                                verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
                            ) {
                                ui.eventos.forEachIndexed { index, evento ->
                                    Text(
                                        text = evento,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = RoxouColors.TextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (index < ui.eventos.lastIndex) {
                                        HorizontalDivider(
                                            thickness = 1.dp,
                                            color = RoxouColors.Divider.copy(alpha = 0.6f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumeroDiagnosticoCard(
    rotulo: String,
    valor: Int,
    cor: Color,
    modifier: Modifier = Modifier,
) {
    RoxouCard(
        modifier = modifier,
        contentPadding = RoxouSpacing.md,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RoxouSpacing.xs),
        ) {
            Text(
                text = valor.toString(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                ),
                color = cor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = rotulo,
                style = MaterialTheme.typography.labelMedium,
                color = RoxouColors.TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun UltimaOfertaCard(ultimaOferta: String?) {
    RoxouCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = RoxouSpacing.lg,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
        ) {
            Text(
                text = "Última oferta lida",
                style = MaterialTheme.typography.labelMedium,
                color = RoxouColors.TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )

            if (ultimaOferta.isNullOrBlank()) {
                Text(
                    text = "Nenhuma oferta lida ainda",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoxouColors.TextMuted,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                val partes = ultimaOferta
                    .split("·", "|")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                val temApp = partes.firstOrNull()?.let {
                    it.equals("Uber", ignoreCase = true) || it == "99"
                } == true

                if (temApp && partes.size >= 2) {
                    val app = partes[0]
                    val valor = partes[1]
                    val metricas = partes.drop(2).joinToString(" · ")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
                        ) {
                            AppMarcaSelo(app = app)
                            Text(
                                text = valor,
                                style = MaterialTheme.typography.headlineLarge,
                                color = RoxouColors.TextPrimary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (metricas.isNotEmpty()) {
                        Text(
                            text = metricas,
                            style = MaterialTheme.typography.bodyMedium,
                            color = RoxouColors.PurpleSoft,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        text = ultimaOferta,
                        style = MaterialTheme.typography.titleMedium,
                        color = RoxouColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Diagnóstico - Com dados",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 760,
)
@Composable
private fun DiagnosticoModeloPreview() {
    RoxouDesignTheme {
        DiagnosticoModelo(
            ui = DiagnosticoUi(
                lidas = 48,
                falhas = 2,
                erros = 0,
                ultimaOferta = "Uber · R$ 18,50 · R$ 2,15/km · R$ 58/h",
                eventos = listOf(
                    "14:32 · Uber · R$ 18,50 lida com sucesso",
                    "14:27 · 99 · R$ 12,40 lida com sucesso",
                    "14:15 · Uber · Tempo não encontrado (falha parcial)",
                    "13:58 · Uber · R$ 26,00 lida com sucesso",
                ),
            ),
            onExportar = {},
        )
    }
}

@Preview(
    name = "Diagnóstico - Sem ofertas",
    showBackground = true,
    backgroundColor = 0xFF121016,
    widthDp = 360,
    heightDp = 760,
)
@Composable
private fun DiagnosticoModeloVazioPreview() {
    RoxouDesignTheme {
        DiagnosticoModelo(
            ui = DiagnosticoUi(
                lidas = 0,
                falhas = 0,
                erros = 1,
                ultimaOferta = null,
                eventos = emptyList(),
            ),
            onExportar = {},
        )
    }
}
