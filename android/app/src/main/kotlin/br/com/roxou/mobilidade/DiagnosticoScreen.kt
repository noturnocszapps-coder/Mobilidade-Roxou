package br.com.roxou.mobilidade

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.roxou.mobilidade.design.RoxouColors
import br.com.roxou.mobilidade.overlay.Diag
import br.com.roxou.mobilidade.overlay.PanelPrefs
import br.com.roxou.mobilidade.overlay.Radar
import kotlinx.coroutines.delay

/**
 * Central de diagnóstico do Radar: mostra em qual etapa a leitura falhou
 * (serviço, evento, cartão, leitura, cálculo, janela) e exporta o registro sem dados de passageiro.
 */
@Composable
fun DiagnosticoScreen(onVoltar: () -> Unit) {
    val context = LocalContext.current
    val d by Diag.state.collectAsStateWithLifecycle()
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var registro by remember { mutableStateOf(Diag.readLog(context).lines().takeLast(40).joinToString("\n")) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            agora = System.currentTimeMillis()
            registro = Diag.readLog(context).lines().takeLast(40).joinToString("\n")
        }
    }
    fun ha(t: Long) = if (t == 0L) "nunca" else {
        val s = (agora - t) / 1000
        when {
            s < 60 -> "há ${s}s"
            s < 3600 -> "há ${s / 60} min"
            else -> "há ${s / 3600} h"
        }
    }
    val acess = Diag.accessibilityEnabled(context)

    Column(
        Modifier.fillMaxSize().background(RoxouColors.Background).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row {
            Text("Diagnóstico do Radar", color = RoxouColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onVoltar) { Text("Voltar", color = RoxouColors.PurpleSoft) }
        }
        Text("Mostra em qual etapa a leitura falha. Não guarda endereço nem nome de passageiro.", color = RoxouColors.TextSecondary, fontSize = 13.sp)

        Etapa("1. Chave do Radar", if (Radar.isOn(context)) "ligada" else "desligada", Radar.isOn(context))
        Etapa("2. Leitura no Android (acessibilidade)", if (acess) "ligada" else "DESLIGADA", acess)
        Etapa("3. Serviço de leitura", if (d.serviceAlive) "rodando desde ${ha(d.serviceConnectedAt)}" else "parado", d.serviceAlive)
        Etapa("4. Último evento da Uber/99", if (d.lastEventAt == 0L) "nenhum" else "${d.lastEventApp}, ${ha(d.lastEventAt)} (${d.eventsCount} no total)", d.lastEventAt != 0L)
        Etapa("5. Último cartão de oferta", if (d.lastOfferSeenAt == 0L) "nenhum" else "${d.lastOfferApp}, ${ha(d.lastOfferSeenAt)}, por ${d.lastOfferHow}", d.lastOfferSeenAt != 0L)
        Etapa("6. Leitura da oferta", d.lastParse.ifBlank { "nenhuma ainda" }, d.lastParse.isNotBlank())
        Etapa("7. Cálculo", d.lastResult.ifBlank { "-" }, d.lastResult.isNotBlank())
        Etapa("8. Janela flutuante", d.panel.ifBlank { "ainda não apareceu" } + " · posição ${PanelPrefs.position(context).label}, ${PanelPrefs.seconds(context)} s", true)
        Etapa("Ofertas lidas / falhas", "${d.offersRead} lidas · ${d.readFailures} falhas", d.readFailures == 0 || d.offersRead > 0)
        if (d.lastDiscard.isNotBlank()) Etapa("Último descarte", d.lastDiscard, false)
        if (d.lastError.isNotBlank()) Etapa("Último erro (${d.errors})", d.lastError, false)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Button(
                onClick = {
                    val texto = "Roxou Mobilidade · diagnóstico\n" + resumo(d, acess, Radar.isOn(context)) + "\n\n" + Diag.readLog(context).takeLast(60_000)
                    val share = Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, "Diagnóstico do Roxou")
                        .putExtra(Intent.EXTRA_TEXT, texto)
                    context.startActivity(Intent.createChooser(share, "Enviar diagnóstico").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoxouColors.Purple),
            ) { Text("Exportar registro") }
            OutlinedButton(onClick = { Diag.clear(context); registro = "" }) { Text("Limpar", color = RoxouColors.TextSecondary) }
        }
        Text("Registro (últimas linhas)", color = RoxouColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Text(registro.ifBlank { "vazio" }, color = RoxouColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

private fun resumo(d: Diag.State, acess: Boolean, radar: Boolean) = buildString {
    appendLine("Radar: ${if (radar) "ligado" else "desligado"} · acessibilidade: ${if (acess) "ligada" else "desligada"} · serviço: ${if (d.serviceAlive) "rodando" else "parado"}")
    appendLine("Eventos: ${d.eventsCount} · ofertas lidas: ${d.offersRead} · falhas: ${d.readFailures} · erros: ${d.errors}")
    appendLine("Última leitura: ${d.lastParse}")
    appendLine("Último cálculo: ${d.lastResult}")
    appendLine("Último descarte: ${d.lastDiscard}")
    append("Último erro: ${d.lastError}")
}

@Composable
private fun Etapa(titulo: String, valor: String, ok: Boolean) {
    Column(Modifier.fillMaxWidth().background(RoxouColors.SurfaceCard).padding(12.dp)) {
        Text((if (ok) "✓ " else "✗ ") + titulo, color = if (ok) RoxouColors.Green else RoxouColors.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(valor, color = RoxouColors.TextPrimary, fontSize = 14.sp)
    }
}
