package br.com.roxou.mobilidade

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.roxou.mobilidade.data.AppState
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.design.PainelScreen
import br.com.roxou.mobilidade.design.PainelUi
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
fun DashboardScreen(repo: RoxouRepository) {
    val vm: DashboardViewModel = viewModel { DashboardViewModel(repo) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state?.snapshot ?: return
    RoxouDesignTheme { PainelScreen(s.toPainelUi()) }
}

internal fun Snapshot.toPainelUi(): PainelUi {
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
    )
}
