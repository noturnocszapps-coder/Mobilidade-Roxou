package br.com.roxou.mobilidade

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.roxou.mobilidade.design.RoxouColors
import br.com.roxou.mobilidade.overlay.Radar
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.roxou.mobilidade.data.AppState
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.design.PainelScreen
import br.com.roxou.mobilidade.design.PainelUi
import br.com.roxou.mobilidade.design.Recursos
import br.com.roxou.mobilidade.design.RoxouDesignTheme
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.summary.Snapshot
import br.com.roxou.mobilidade.ui.formatDayMonth
import br.com.roxou.mobilidade.ui.shortPt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.activity.compose.BackHandler
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import br.com.roxou.mobilidade.overlay.PanelPrefs
import br.com.roxou.mobilidade.overlay.Diag
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.com.roxou.mobilidade.domain.offer.OfferStats
import br.com.roxou.mobilidade.domain.offer.OfferStatsEngine
import br.com.roxou.mobilidade.overlay.OfferLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class DashboardViewModel(repo: RoxouRepository) : ViewModel() {
    val state: StateFlow<AppState?> = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Painel do turno. O visual vem do AI Studio (pasta design/); aqui só liga os dados reais. */
@Composable
fun DashboardScreen(repo: RoxouRepository, recursos: Recursos) {
    val vm: DashboardViewModel = viewModel { DashboardViewModel(repo) }
    val state by vm.state.collectAsStateWithLifecycle()
    val st = state ?: return
    val s = st.snapshot
    var tela by remember { mutableStateOf<String?>(null) }
    var resumo by remember { mutableStateOf<OfferStats?>(null) }
    BackHandler(enabled = tela != null) { tela = null }
    // Estado do Radar em palavras simples, para o cartão do Início (visual do AI Studio).
    val context = LocalContext.current
    val radarOn by Radar.flow(context).collectAsStateWithLifecycle()
    val diagState by Diag.state.collectAsStateWithLifecycle()
    val radarEstado = when {
        radarOn == false -> "Desligado"
        !Diag.accessibilityEnabled(context) || !diagState.serviceAlive -> "Falta permissão"
        else -> "Lendo ofertas"
    }
    RoxouDesignTheme {
        when (tela) {
            "financas" -> FinancasRoute(repo, st) { tela = null }
            "diag" -> DiagnosticoScreen { tela = null }
            "ofertas" -> resumo?.let { OfertasResumoRoute(it) { tela = null } } ?: run { tela = null }
            else -> Column(Modifier.fillMaxSize().background(RoxouColors.Background)) {
                if (recursos.janelaFlutuante) {
                    RadarToggle(
                        repo, st.settings.offer.minPassengerRating,
                        onResumo = { stats -> resumo = stats; tela = "ofertas" },
                        onDiagnostico = { tela = "diag" },
                    )
                }
                PainelScreen(
                    s.toPainelUi(recursos),
                    Modifier.weight(1f),
                    onAbrirFinancas = { if (recursos.financas) tela = "financas" },
                    radarEstado = radarEstado,
                    onArrumarRadar = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                    onAbrirDiagnostico = { tela = "diag" },
                )
            }
        }
    }
}

/**
 * Botão grande do Radar de corridas: liga e desliga a leitura e a janela com um toque.
 * Embaixo: ajustes da janela, resumo das ofertas e o aviso de bateria (para o celular não fechar o Radar).
 */
@Composable
private fun RadarToggle(repo: RoxouRepository, notaMinima: Double, onResumo: (OfferStats) -> Unit, onDiagnostico: () -> Unit) {
    val context = LocalContext.current
    val on by Radar.flow(context).collectAsStateWithLifecycle()
    val ligado = on ?: true
    // Estado real: a chave do Radar, a acessibilidade ligada no Android e o serviço conectado de fato.
    var acessOk by remember { mutableStateOf(Diag.accessibilityEnabled(context)) }
    val diag by Diag.state.collectAsStateWithLifecycle()
    val funcionando = ligado && acessOk && diag.serviceAlive
    val cor = when {
        !ligado -> RoxouColors.TextMuted
        funcionando -> RoxouColors.Green
        else -> RoxouColors.Red
    }
    val statusTexto = when {
        !ligado -> "Desligado"
        !acessOk -> "Parado: a leitura está desligada no Android"
        !diag.serviceAlive -> "Parado: o Android fechou a leitura"
        else -> "Ligado e funcionando"
    }
    var ajustes by remember { mutableStateOf(false) }
    var segundoPlano by remember { mutableStateOf(false) }
    var bateriaOk by remember { mutableStateOf(bateriaLiberada(context)) }
    var voltou by remember { mutableStateOf(0) }
    // Pede uma vez a permissão de notificação (Android 13+), para a notificação da corrida com Embarque e Destino.
    val pedirNotificacao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && PanelPrefs.rideNotification(context) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LifecycleResumeEffect(Unit) {
        bateriaOk = bateriaLiberada(context)
        acessOk = Diag.accessibilityEnabled(context)
        voltou++
        onPauseOrDispose { }
    }
    val stats by produceState<OfferStats?>(null, voltou) {
        value = withContext(Dispatchers.IO) {
            OfferStatsEngine.compute(OfferLog.readAll(context), ZoneId.systemDefault(), LocalDate.now())
        }
    }
    Column(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(RoxouColors.SurfaceCard),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { Radar.set(context, !ligado) }.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(cor))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Radar de corridas", color = RoxouColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(statusTexto, color = if (ligado && !funcionando) RoxouColors.Red else RoxouColors.TextSecondary, fontSize = 13.sp, maxLines = 1)
            }
            Switch(
                checked = ligado,
                onCheckedChange = { Radar.set(context, it) },
                colors = SwitchDefaults.colors(checkedTrackColor = RoxouColors.Green, checkedThumbColor = Color.White),
            )
        }
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            val st = stats
            Text(
                if (st == null || st.total == 0) "Nenhuma oferta guardada ainda" else "Hoje: ${st.todayTotal} ofertas · ${st.todayAccepted} aceitas  ›",
                color = RoxouColors.TextSecondary, fontSize = 13.sp, maxLines = 1,
                modifier = Modifier.weight(1f).clickable { if (st != null && st.total > 0) onResumo(st) },
            )
            Text(
                "Diagnóstico", color = RoxouColors.TextSecondary, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onDiagnostico() }.padding(horizontal = 6.dp, vertical = 4.dp),
            )
            Text(
                "⚙ Janela", color = RoxouColors.PurpleSoft, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { ajustes = true }.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (ligado && !funcionando) {
            Text(
                "Toque aqui e ligue \"Roxou\" em Apps instalados (ou Serviços instalados)",
                color = RoxouColors.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().clickable {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
            )
        }
        if (ligado && !bateriaOk) {
            Text(
                "⚠ Toque aqui para o celular não fechar o Radar",
                color = RoxouColors.Yellow, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().clickable { segundoPlano = true }
                    .padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
            )
        }
    }
    if (ajustes) JanelaAjustesDialog(repo, notaMinima) { ajustes = false }
    if (segundoPlano) SegundoPlanoDialog { segundoPlano = false }
}

internal fun Snapshot.toPainelUi(recursos: Recursos): PainelUi {
    val day = workday.dayOfWeek.shortPt().replaceFirstChar { it.uppercase() }
    return PainelUi(
        diaTexto = "$day ${formatDayMonth(workday)}",
        corridasHoje = goals.today?.done ?: tripsToday.size,
        metaHoje = goals.today?.adjustedTarget ?: 0,
        corridasUberHoje = tripsTodayByPlatform[Platform.UBER] ?: 0,
        corridas99Hoje = tripsTodayByPlatform[Platform.NINETY_NINE] ?: 0,
        faturadoHojeCentavos = financeToday.grossRevenueCents,
        corridasSemanaUber = rideCount.official,
        proximaFaixaCorridas = tiers?.next?.minRides,
        proximaFaixaDescontoCentavos = tiers?.next?.discountCents,
        descontoGarantidoCentavos = tiers?.achievedDiscountCents ?: 0,
        parcelaFaltaCentavos = installment?.pendingCents,
        dinheiroLivreCentavos = financeWeek.availableCashCents,
        recursos = recursos,
    )
}
