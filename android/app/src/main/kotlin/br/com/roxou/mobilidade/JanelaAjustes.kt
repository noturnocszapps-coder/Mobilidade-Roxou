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
import br.com.roxou.mobilidade.design.JanelaAjustesSheet
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

/** Ajustes da janela flutuante (visual do AI Studio: design/JanelaAjustes.kt). */
@Composable
fun JanelaAjustesDialog(repo: RoxouRepository, notaMinima: Double, onFechar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var posicao by remember { mutableStateOf(PanelPrefs.position(context)) }
    var segundos by remember { mutableIntStateOf(PanelPrefs.seconds(context)) }
    var waze by remember { mutableStateOf(PanelPrefs.wazeAfterAccept(context)) }
    var nota by remember { mutableStateOf(notaMinima) }
    var mapa by remember { mutableStateOf(PanelPrefs.mapApp(context)) }
    var perguntar by remember { mutableStateOf(PanelPrefs.askAccept(context)) }

    JanelaAjustesSheet(
        posicao = posicao.ordinal,
        segundos = segundos,
        notaMinima = nota,
        wazeDepoisDoAceite = waze,
        onPosicao = { i -> PanelPrefs.Position.entries.getOrNull(i)?.let { posicao = it; PanelPrefs.setPosition(context, it) } },
        onSegundos = { segundos = it; PanelPrefs.setSeconds(context, it) },
        onNotaMinima = { v ->
            nota = v
            scope.launch { repo.updateSettings { it.copy(offer = it.offer.copy(minPassengerRating = v)) } }
        },
        onWaze = { waze = it; PanelPrefs.setWazeAfterAccept(context, it) },
        onFechar = onFechar,
        perguntarAceite = perguntar,
        onPerguntarAceite = { perguntar = it; PanelPrefs.setAskAccept(context, it) },
        mapaPadrao = mapa.label,
        onMapaPadrao = { nome ->
            Waze.MapApp.entries.firstOrNull { it.label.equals(nome, ignoreCase = true) }?.let { mapa = it; PanelPrefs.setMapApp(context, it) }
        },
    )
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
