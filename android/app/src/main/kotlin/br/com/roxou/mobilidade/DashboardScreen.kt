package br.com.roxou.mobilidade

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.roxou.mobilidade.data.AppState
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.feature.goals.tierRemainingText
import br.com.roxou.mobilidade.ui.Progress
import br.com.roxou.mobilidade.ui.RoxouGreen
import br.com.roxou.mobilidade.ui.RoxouRed
import br.com.roxou.mobilidade.ui.SectionCard
import br.com.roxou.mobilidade.ui.StatRow
import br.com.roxou.mobilidade.ui.formatBrl
import br.com.roxou.mobilidade.ui.formatDayMonth
import br.com.roxou.mobilidade.ui.formatKm
import br.com.roxou.mobilidade.ui.shortPt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(repo: RoxouRepository) : ViewModel() {
    val state: StateFlow<AppState?> = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Painel do turno: corridas do dia, quanto falta para as faixas, parcela pendente e caixa. */
@Composable
fun DashboardScreen(repo: RoxouRepository) {
    val vm: DashboardViewModel = viewModel { DashboardViewModel(repo) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state?.snapshot ?: return

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionCard("Hoje · ${s.workday.dayOfWeek.shortPt()} ${formatDayMonth(s.workday)}") {
            StatRow("Corridas Uber", "${s.tripsTodayByPlatform[Platform.UBER] ?: 0}", bold = true)
            StatRow("Corridas 99", "${s.tripsTodayByPlatform[Platform.NINETY_NINE] ?: 0}", bold = true)
            StatRow("Faturamento do dia", formatBrl(s.financeToday.grossRevenueCents))
            s.goals.today?.let { StatRow("Meta do dia", "${it.done} de ${it.adjustedTarget} (faltam ${it.remaining})") }
            Text("O dia de trabalho vai até as 04h.", style = MaterialTheme.typography.bodySmall)
        }
        s.tiers?.let { tiers ->
            SectionCard("Semana · ${s.rideCount.official} corridas Uber") {
                Text(tierRemainingText(tiers), style = MaterialTheme.typography.titleMedium)
                StatRow("Desconto garantido (semana seguinte)", formatBrl(tiers.achievedDiscountCents), RoxouGreen)
                tiers.next?.let { Progress(s.rideCount.official.toFloat() / it.minRides) }
            }
        }
        s.installment?.let { inst ->
            SectionCard("Locação") {
                StatRow("Parcela pendente", formatBrl(inst.pendingCents), if (inst.isPaid) RoxouGreen else RoxouRed, bold = true)
                StatRow("Devido na semana", formatBrl(inst.dueCents))
            }
        }
        SectionCard("Caixa") {
            StatRow("Dinheiro livre na semana", formatBrl(s.financeWeek.availableCashCents), bold = true)
            StatRow("Faturamento da semana", formatBrl(s.financeWeek.grossRevenueCents))
        }
        val c = s.cascadeToday
        SectionCard("Meta do dia em cascata") {
            StatRow("1. Combustível", "${formatBrl(c.fuelCoveredCents)} de ${formatBrl(c.fuelTotalCents)}")
            StatRow("2. Locação", "${formatBrl(c.rentalCoveredCents)} de ${formatBrl(c.rentalTargetCents)}")
            StatRow("3. Lucro livre", formatBrl(c.freeProfitCents), RoxouGreen, bold = true)
        }
        s.km?.let { km ->
            SectionCard("Franquia de km") {
                StatRow("Restante", "${formatKm(km.remainingKm)} de ${km.limitKm} km")
                Progress((km.usedPercent / 100).toFloat())
            }
        }
    }
}
