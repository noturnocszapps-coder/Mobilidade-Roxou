package br.com.roxou.mobilidade

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.design.CompensaScreen
import br.com.roxou.mobilidade.design.CompensaUi
import br.com.roxou.mobilidade.design.RoxouDesignTheme
import br.com.roxou.mobilidade.domain.offer.OfferRules
import br.com.roxou.mobilidade.domain.summary.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompensaViewModel(private val repo: RoxouRepository) : ViewModel() {
    val settings: StateFlow<AppSettings?> = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun save(ui: CompensaUi) = viewModelScope.launch { repo.updateSettings { it.copy(offer = it.offer.withCompensa(ui)) } }
}

/** Tela "O que compensa pra mim". O visual vem do AI Studio (pasta design/). */
@Composable
fun CompensaRoute(repo: RoxouRepository) {
    val vm: CompensaViewModel = viewModel { CompensaViewModel(repo) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val rules = settings?.offer ?: return
    val ui = rules.toCompensaUi()
    // Recria a tela quando os valores salvos mudam, para os controles mostrarem o que está gravado.
    key(ui) { RoxouDesignTheme { CompensaScreen(inicial = ui, onSalvar = vm::save) } }
}

internal fun OfferRules.toCompensaUi() = CompensaUi(
    minimoPorKmCentavos = minPerKmCents,
    bomPorKmCentavos = goodPerKmCents,
    minimoPorHoraCentavos = minPerHourCents,
    bomPorHoraCentavos = goodPerHourCents,
    custoPorKmCentavos = costPerKmCents,
    buscaMaximaKm = maxPickupKm,
    bairrosEvitados = blockedAreas,
)

internal fun OfferRules.withCompensa(ui: CompensaUi) = copy(
    minPerKmCents = ui.minimoPorKmCentavos.coerceAtLeast(0),
    goodPerKmCents = ui.bomPorKmCentavos.coerceAtLeast(ui.minimoPorKmCentavos),
    minPerHourCents = ui.minimoPorHoraCentavos.coerceAtLeast(0),
    goodPerHourCents = ui.bomPorHoraCentavos.coerceAtLeast(ui.minimoPorHoraCentavos),
    costPerKmCents = ui.custoPorKmCentavos.coerceAtLeast(0),
    maxPickupKm = ui.buscaMaximaKm.coerceAtLeast(0.0),
    blockedAreas = ui.bairrosEvitados.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
)
