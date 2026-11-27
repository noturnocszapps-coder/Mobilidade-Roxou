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

class DashboardViewModel(repo: RoxouRepository) : ViewModel() {
    val state: StateFlow<AppState?> = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Painel do turno. O visual vem do AI Studio (pasta design/); aqui só liga os dados reais. */
@Composable
fun DashboardScreen(repo: RoxouRepository, recursos: Recursos) {
    val vm: DashboardViewModel = viewModel { DashboardViewModel(repo) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state?.snapshot ?: return
    RoxouDesignTheme {
        Column(Modifier.fillMaxSize().background(RoxouColors.Background)) {
            if (recursos.janelaFlutuante) RadarToggle()
            PainelScreen(s.toPainelUi(recursos), Modifier.weight(1f))
        }
    }
}

/** Botão grande do Radar de corridas: liga e desliga a leitura e a janela com um toque. */
@Composable
private fun RadarToggle() {
    val context = LocalContext.current
    val on by Radar.flow(context).collectAsStateWithLifecycle()
    val ligado = on ?: true
    val cor = if (ligado) RoxouColors.Green else RoxouColors.TextMuted
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(RoxouColors.SurfaceCard)
            .clickable { Radar.set(context, !ligado) }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(cor))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Radar de corridas", color = RoxouColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(if (ligado) "Ligado: avaliando as ofertas" else "Desligado", color = RoxouColors.TextSecondary, fontSize = 13.sp, maxLines = 1)
        }
        Switch(
            checked = ligado,
            onCheckedChange = { Radar.set(context, it) },
            colors = SwitchDefaults.colors(checkedTrackColor = RoxouColors.Green, checkedThumbColor = Color.White),
        )
    }
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
