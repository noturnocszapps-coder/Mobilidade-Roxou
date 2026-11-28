package br.com.roxou.mobilidade.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class JanelaModeloUi(
    val veredito: String,
    val valorKm: String,
    val linhaDetalhe: String,
    val aviso: String?,
    val modo: String,
)

@Composable
fun JanelaFlutuanteModelo(
    ui: JanelaModeloUi,
    modifier: Modifier = Modifier,
) {
    val modoNormalizado = ui.modo.trim().uppercase()

    Surface(
        modifier = modifier.widthIn(max = 300.dp).fillMaxWidth(),
        shape = RoundedCornerShape(RoxouRadius.lg),
        color = Color(0xFF0E0C12).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, RoxouColors.Purple.copy(alpha = 0.75f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RoxouSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
        ) {
            // Faixa curta de aviso vermelho (quando houver)
            if (!ui.aviso.isNullOrBlank()) {
                AvisoFaixa(
                    texto = ui.aviso,
                    mostrarFechar = modoNormalizado == "AVISO",
                )
            }

            when (modoNormalizado) {
                "PERGUNTA" -> ConteudoPergunta(ui = ui)
                "ACEITA", "ACEITA_EMBARQUE" -> ConteudoAceita(
                    textoBotao = "➜ Ir ao embarque · Waze",
                )
                "ACEITA_DESTINO" -> ConteudoAceita(
                    textoBotao = "➜ Ir ao destino · Waze",
                )
                "AVISO" -> {
                    if (ui.aviso.isNullOrBlank()) {
                        AvisoFaixa(
                            texto = "⚠ Nota do passageiro",
                            mostrarFechar = true,
                        )
                    }
                }
                else -> ConteudoOferta(ui = ui)
            }
        }
    }
}

@Composable
private fun AvisoFaixa(
    texto: String,
    mostrarFechar: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RoxouRadius.sm),
        color = RoxouColors.Red.copy(alpha = 0.20f),
        border = BorderStroke(1.dp, RoxouColors.Red.copy(alpha = 0.65f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = RoxouSpacing.md,
                    end = if (mostrarFechar) RoxouSpacing.xs else RoxouSpacing.md,
                    top = if (mostrarFechar) 0.dp else RoxouSpacing.sm,
                    bottom = if (mostrarFechar) 0.dp else RoxouSpacing.sm,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = RoxouColors.Red,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (mostrarFechar) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fechar",
                        tint = RoxouColors.TextPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConteudoOferta(ui: JanelaModeloUi) {
    val corVeredito = corDoVeredito(ui.veredito)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
    ) {
        // Topo: veredito à esquerda, ✕ no canto oposto (bem afastado do ✔ embaixo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(corVeredito.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = ui.veredito.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = corVeredito,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            BotaoFecharDiscreto()
        }

        // Valor por km bem grande
        Text(
            text = ui.valorKm,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 34.sp,
                lineHeight = 38.sp,
                letterSpacing = (-1).sp,
            ),
            fontWeight = FontWeight.Bold,
            color = RoxouColors.TextPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )

        // Linha menor com R$/h · tempo · nota
        Text(
            text = ui.linhaDetalhe,
            style = MaterialTheme.typography.bodyMedium,
            color = RoxouColors.TextSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(RoxouSpacing.xs))

        // Embaixo: Embarque, Destino e ✔ (Aceitei) — ✕ fica no topo, longe do ✔
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BotaoSecundarioJanela(
                texto = "Embarque",
                modifier = Modifier.weight(1f),
            )
            BotaoSecundarioJanela(
                texto = "Destino",
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {},
                shape = RoundedCornerShape(RoxouRadius.sm),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoxouColors.Green,
                    contentColor = RoxouColors.Background,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Aceitei",
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun ConteudoPergunta(ui: JanelaModeloUi) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RoxouSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RoxouSpacing.xs)) {
            Text(
                text = "Aceitou esta corrida?",
                style = MaterialTheme.typography.titleLarge,
                color = RoxouColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            if (ui.valorKm.isNotBlank() || ui.linhaDetalhe.isNotBlank()) {
                val resumo = listOf(ui.valorKm, ui.linhaDetalhe)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                Text(
                    text = resumo,
                    style = MaterialTheme.typography.labelMedium,
                    color = RoxouColors.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {},
                shape = RoundedCornerShape(RoxouRadius.sm),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoxouColors.Green,
                    contentColor = RoxouColors.Background,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                Text(
                    text = "Sim",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Spacer(modifier = Modifier.width(RoxouSpacing.lg))

            BotaoFecharDiscreto()
        }
    }
}

@Composable
private fun ConteudoAceita(textoBotao: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RoxouSpacing.sm),
    ) {
        Button(
            onClick = {},
            shape = RoundedCornerShape(RoxouRadius.sm),
            contentPadding = PaddingValues(horizontal = RoxouSpacing.md),
            colors = ButtonDefaults.buttonColors(
                containerColor = RoxouColors.Purple,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
        ) {
            Text(
                text = textoBotao,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }

        BotaoFecharDiscreto()
    }
}

@Composable
private fun BotaoSecundarioJanela(
    texto: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = {},
        shape = RoundedCornerShape(RoxouRadius.sm),
        color = RoxouColors.SurfaceCard,
        border = BorderStroke(1.dp, RoxouColors.CardBorder),
        modifier = modifier.height(48.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = RoxouSpacing.sm),
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = RoxouColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BotaoFecharDiscreto() {
    Surface(
        onClick = {},
        shape = RoundedCornerShape(RoxouRadius.sm),
        color = RoxouColors.SurfaceCard,
        border = BorderStroke(1.dp, RoxouColors.CardBorder),
        modifier = Modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Fechar",
                tint = RoxouColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun corDoVeredito(veredito: String): Color {
    val norm = veredito.trim().uppercase()
    return when {
        norm.contains("NÃO") || norm.contains("NAO") || norm.contains("RUIM") -> RoxouColors.Red
        norm.contains("TALVEZ") -> RoxouColors.Yellow
        else -> RoxouColors.Green
    }
}

@Preview(name = "Janela - Oferta COMPENSA", showBackground = true, backgroundColor = 0xFF1F1F24)
@Composable
private fun PreviewJanelaOfertaCompensa() {
    RoxouDesignTheme {
        Box(modifier = Modifier.padding(12.dp)) {
            JanelaFlutuanteModelo(
                ui = JanelaModeloUi(
                    veredito = "COMPENSA",
                    valorKm = "R$ 1,95/km",
                    linhaDetalhe = "R$ 56/h · 13 min · ★ 4,95",
                    aviso = null,
                    modo = "OFERTA",
                ),
            )
        }
    }
}

@Preview(name = "Janela - Oferta TALVEZ", showBackground = true, backgroundColor = 0xFF1F1F24)
@Composable
private fun PreviewJanelaOfertaTalvez() {
    RoxouDesignTheme {
        Box(modifier = Modifier.padding(12.dp)) {
            JanelaFlutuanteModelo(
                ui = JanelaModeloUi(
                    veredito = "TALVEZ",
                    valorKm = "R$ 1,62/km",
                    linhaDetalhe = "R$ 36/h · 18 min · ★ 4,84",
                    aviso = null,
                    modo = "OFERTA",
                ),
            )
        }
    }
}

@Preview(name = "Janela - Oferta NÃO COMPENSA + Bairro", showBackground = true, backgroundColor = 0xFF1F1F24)
@Composable
private fun PreviewJanelaOfertaNaoCompensaComAviso() {
    RoxouDesignTheme {
        Box(modifier = Modifier.padding(12.dp)) {
            JanelaFlutuanteModelo(
                ui = JanelaModeloUi(
                    veredito = "NÃO COMPENSA",
                    valorKm = "R$ 1,18/km",
                    linhaDetalhe = "R$ 24/h · 22 min · ★ 4,62",
                    aviso = "Destino: Jardim Leonor",
                    modo = "OFERTA",
                ),
            )
        }
    }
}

@Preview(name = "Janela - Pergunta", showBackground = true, backgroundColor = 0xFF1F1F24)
@Composable
private fun PreviewJanelaPergunta() {
    RoxouDesignTheme {
        Box(modifier = Modifier.padding(12.dp)) {
            JanelaFlutuanteModelo(
                ui = JanelaModeloUi(
                    veredito = "COMPENSA",
                    valorKm = "R$ 1,95/km",
                    linhaDetalhe = "R$ 56/h · 13 min",
                    aviso = null,
                    modo = "PERGUNTA",
                ),
            )
        }
    }
}

@Preview(name = "Janela - Aceita Embarque", showBackground = true, backgroundColor = 0xFF1F1F24)
@Composable
private fun PreviewJanelaAceitaEmbarque() {
    RoxouDesignTheme {
        Box(modifier = Modifier.padding(12.dp)) {
            JanelaFlutuanteModelo(
                ui = JanelaModeloUi(
                    veredito = "COMPENSA",
                    valorKm = "R$ 1,95/km",
                    linhaDetalhe = "",
                    aviso = null,
                    modo = "ACEITA_EMBARQUE",
                ),
            )
        }
    }
}

@Preview(name = "Janela - Aceita Destino", showBackground = true, backgroundColor = 0xFF1F1F24)
@Composable
private fun PreviewJanelaAceitaDestino() {
    RoxouDesignTheme {
        Box(modifier = Modifier.padding(12.dp)) {
            JanelaFlutuanteModelo(
                ui = JanelaModeloUi(
                    veredito = "COMPENSA",
                    valorKm = "R$ 1,95/km",
                    linhaDetalhe = "",
                    aviso = null,
                    modo = "ACEITA_DESTINO",
                ),
            )
        }
    }
}

@Preview(name = "Janela - Aviso isolado", showBackground = true, backgroundColor = 0xFF1F1F24)
@Composable
private fun PreviewJanelaAviso() {
    RoxouDesignTheme {
        Box(modifier = Modifier.padding(12.dp)) {
            JanelaFlutuanteModelo(
                ui = JanelaModeloUi(
                    veredito = "NÃO COMPENSA",
                    valorKm = "",
                    linhaDetalhe = "",
                    aviso = "⚠ Nota do passageiro",
                    modo = "AVISO",
                ),
            )
        }
    }
}
