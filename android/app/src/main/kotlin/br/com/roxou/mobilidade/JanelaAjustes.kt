package br.com.roxou.mobilidade

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import br.com.roxou.mobilidade.overlay.Waze
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.design.RoxouColors
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.offer.OfferStats
import br.com.roxou.mobilidade.overlay.PanelPrefs
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.Locale

private val pt: Locale = Locale.forLanguageTag("pt-BR")

@Composable
private fun Titulo(t: String) =
    Text(t, color = RoxouColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))

@Composable
private fun Ajuda(t: String, modifier: Modifier = Modifier) = Text(t, color = RoxouColors.TextSecondary, fontSize = 13.sp, modifier = modifier)

@Composable
private fun Opcao(texto: String, marcado: Boolean, onClick: () -> Unit) {
    Text(
        texto,
        color = if (marcado) Color.White else RoxouColors.TextSecondary,
        fontSize = 14.sp,
        fontWeight = if (marcado) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (marcado) RoxouColors.Purple else RoxouColors.SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** Ajustes da janela flutuante: posição, tempo na tela, nota mínima e Waze depois do aceite. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JanelaAjustesDialog(repo: RoxouRepository, notaMinima: Double, onFechar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var posicao by remember { mutableStateOf(PanelPrefs.position(context)) }
    var segundos by remember { mutableIntStateOf(PanelPrefs.seconds(context)) }
    var waze by remember { mutableStateOf(PanelPrefs.wazeAfterAccept(context)) }
    var nota by remember { mutableStateOf(notaMinima) }
    var mapa by remember { mutableStateOf(PanelPrefs.mapApp(context)) }
    var notificacao by remember { mutableStateOf(PanelPrefs.rideNotification(context)) }
    val pedirNotificacao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = RoxouColors.SurfaceCard,
        confirmButton = { TextButton(onClick = onFechar) { Text("Pronto", color = RoxouColors.PurpleSoft) } },
        title = { Text("Janela das corridas", color = RoxouColors.TextPrimary) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Titulo("Onde fica na tela")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PanelPrefs.Position.entries.forEach { p ->
                        Opcao(p.label, posicao == p) { posicao = p; PanelPrefs.setPosition(context, p) }
                    }
                }
                Ajuda("Para subir ou descer, arraste a janela.")

                Titulo("Quanto tempo fica na tela")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PanelPrefs.SECONDS_OPTIONS.forEach { s ->
                        Opcao("$s s", segundos == s) { segundos = s; PanelPrefs.setSeconds(context, s) }
                    }
                }
                Ajuda("Depois some sozinha. Um toque na janela segura ela até a oferta sumir.")

                Titulo("Nota mínima do passageiro")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.0, 4.70, 4.80, 4.85, 4.90).forEach { v ->
                        val label = if (v == 0.0) "Não avisar" else String.format(pt, "%.2f", v)
                        Opcao(label, nota == v) {
                            nota = v
                            scope.launch { repo.updateSettings { it.copy(offer = it.offer.copy(minPassengerRating = v)) } }
                        }
                    }
                }
                Ajuda("Abaixo dessa nota a janela avisa e a cor piora um nível.")

                Titulo("Mapa para Embarque e Destino")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Waze.MapApp.entries.forEach { m ->
                        Opcao(m.label, mapa == m) { mapa = m; PanelPrefs.setMapApp(context, m) }
                    }
                }

                Titulo("Notificação da corrida")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ajuda("Depois do aceite, deixa Embarque e Destino na barra de notificações, para abrir o mapa a qualquer hora", Modifier.weight(1f))
                    Switch(
                        checked = notificacao,
                        onCheckedChange = {
                            notificacao = it
                            PanelPrefs.setRideNotification(context, it)
                            if (it && Build.VERSION.SDK_INT >= 33) pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = RoxouColors.Green, checkedThumbColor = Color.White),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Titulo("Botões na tela depois do aceite")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ajuda("Deixa uma janelinha com Embarque e Destino em cima do app da Uber/99", Modifier.weight(1f))
                    Switch(
                        checked = waze,
                        onCheckedChange = { waze = it; PanelPrefs.setWazeAfterAccept(context, it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = RoxouColors.Green, checkedThumbColor = Color.White),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
    )
}

/** Resumo das ofertas que apareceram (aceitas e recusadas). */
@Composable
fun ResumoOfertasDialog(s: OfferStats, onFechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = RoxouColors.SurfaceCard,
        confirmButton = { TextButton(onClick = onFechar) { Text("Fechar", color = RoxouColors.PurpleSoft) } },
        title = { Text("Ofertas que tocaram", color = RoxouColors.TextPrimary) },
        text = {
            Column {
                fun pct(v: Double) = String.format(pt, "%.0f%%", v * 100)
                Linha("Hoje", "${s.todayTotal} ofertas · ${s.todayAccepted} aceitas")
                Linha("Média por dia", String.format(pt, "%.1f ofertas", s.perDay))
                Linha("Você aceita", pct(s.acceptRate))
                s.bestWeekday?.let { Linha("Dia que mais toca", diaNome(it)) }
                s.busiestHour?.let { Linha("Horário que mais toca", "${it}h às ${it + 1}h") }
                Linha("Uber · 99", "${s.byPlatform[Platform.UBER] ?: 0} · ${s.byPlatform[Platform.NINETY_NINE] ?: 0}")
                Spacer(Modifier.height(8.dp))
                Ajuda("Conta desde que o Radar começou a guardar as ofertas (${s.days} dia${if (s.days == 1) "" else "s"}).")
            }
        },
    )
}

@Composable
private fun Linha(rotulo: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(rotulo, color = RoxouColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(valor, color = RoxouColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

private fun diaNome(d: DayOfWeek) = when (d) {
    DayOfWeek.MONDAY -> "Segunda"
    DayOfWeek.TUESDAY -> "Terça"
    DayOfWeek.WEDNESDAY -> "Quarta"
    DayOfWeek.THURSDAY -> "Quinta"
    DayOfWeek.FRIDAY -> "Sexta"
    DayOfWeek.SATURDAY -> "Sábado"
    DayOfWeek.SUNDAY -> "Domingo"
}

/** O celular não está limitando o Roxou na economia de bateria? */
fun bateriaLiberada(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

@SuppressLint("BatteryLife")
fun pedirBateriaLiberada(context: Context) {
    val pedido = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
    val lista = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    runCatching { context.startActivity(pedido) }.recoverCatching { context.startActivity(lista) }
}

/** Passo a passo para Samsung e Xiaomi não fecharem o Radar. */
@Composable
fun SegundoPlanoDialog(onFechar: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = RoxouColors.SurfaceCard,
        confirmButton = {
            TextButton(onClick = { pedirBateriaLiberada(context); onFechar() }) { Text("Liberar bateria", color = RoxouColors.PurpleSoft) }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Depois", color = RoxouColors.TextSecondary) } },
        title = { Text("Radar sempre ligado", color = RoxouColors.TextPrimary) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Ajuda("Alguns celulares fecham apps para economizar bateria. Para o Radar não parar no meio do turno:")
                Titulo("1. Liberar a bateria")
                Ajuda("Toque em \"Liberar bateria\" e escolha Permitir.")
                Titulo("Samsung")
                Ajuda("Configurações > Bateria > Limites de uso em segundo plano > Apps que nunca suspendem > adicione o Roxou.")
                Titulo("Xiaomi / Redmi / Poco")
                Ajuda("Configurações > Apps > Roxou > Início automático: ligado. Em Economia de bateria escolha \"Sem restrições\". Na tela de apps recentes, segure o Roxou e toque no cadeado.")
                Titulo("Motorola e outros")
                Ajuda("Configurações > Apps > Roxou > Bateria > Sem restrições.")
            }
        },
    )
}
