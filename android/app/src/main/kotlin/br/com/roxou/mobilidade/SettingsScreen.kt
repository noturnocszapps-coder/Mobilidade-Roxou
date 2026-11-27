package br.com.roxou.mobilidade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.domain.goals.DailyGoalPlan
import br.com.roxou.mobilidade.domain.rental.RentalPlan
import br.com.roxou.mobilidade.domain.rental.VehicleOwnership
import br.com.roxou.mobilidade.domain.settings.SettingsCodec
import br.com.roxou.mobilidade.domain.summary.AppSettings
import br.com.roxou.mobilidade.domain.trips.ManualTripEntry
import br.com.roxou.mobilidade.ui.ChoiceChips
import br.com.roxou.mobilidade.ui.NumberField
import br.com.roxou.mobilidade.ui.SectionCard
import br.com.roxou.mobilidade.ui.shortPt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

class SettingsViewModel(private val repo: RoxouRepository) : ViewModel() {
    val settings: StateFlow<AppSettings?> = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun save(s: AppSettings) = viewModelScope.launch { repo.updateSettings { s } }
}

private fun centsText(c: Long) = String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", c / 100.0)

@Composable
fun SettingsScreen(repo: RoxouRepository) {
    val vm: SettingsViewModel = viewModel { SettingsViewModel(repo) }
    val current by vm.settings.collectAsStateWithLifecycle()
    val s = current ?: return
    SettingsForm(s, onSave = vm::save)
}

@Composable
private fun SettingsForm(initial: AppSettings, onSave: (AppSettings) -> Unit) {
    var ownership by remember(initial) { mutableStateOf(initial.ownership) }
    var company by remember(initial) { mutableStateOf(initial.rental.companyName) }
    var installment by remember(initial) { mutableStateOf(centsText(initial.rental.weeklyInstallmentCents)) }
    var kmAllowance by remember(initial) { mutableStateOf(initial.rental.weeklyKmAllowance.toString()) }
    var extraKm by remember(initial) { mutableStateOf(centsText(initial.rental.extraKmRateCents)) }
    var tiers by remember(initial) {
        mutableStateOf(initial.rental.tiers.sorted.joinToString(", ") { "${it.minRides}=${centsText(it.discountCents)}" })
    }
    val goals = remember(initial) { DayOfWeek.entries.associateWith { mutableStateOf(initial.goals.target(it).toString()) } }
    var carry by remember(initial) { mutableStateOf(initial.carryShortfall) }
    var kmReset by remember(initial) { mutableStateOf(initial.turnover.kmResetTime.toString()) }
    var rideReset by remember(initial) { mutableStateOf(initial.turnover.rideWeekResetTime.toString()) }
    var workdayEnd by remember(initial) { mutableStateOf(initial.turnover.workdayEndTime.toString()) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionCard("Veículo") {
            ChoiceChips(VehicleOwnership.entries, ownership, { if (it == VehicleOwnership.RENTED) "Alugado" else "Próprio" }) { ownership = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { applyPreset(RentalPlan.LOCALIZA_ZARP) { c, i, k, e, t -> company = c; installment = i; kmAllowance = k; extraKm = e; tiers = t } }) { Text("Localiza") }
                Button(onClick = { applyPreset(RentalPlan.MOVIDA_FX) { c, i, k, e, t -> company = c; installment = i; kmAllowance = k; extraKm = e; tiers = t } }) { Text("Movida FX") }
            }
        }
        if (ownership == VehicleOwnership.RENTED) {
            SectionCard("Locadora") {
                OutlinedTextField(company, { company = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                NumberField(installment, { installment = it }, "Parcela semanal (R$)", Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(kmAllowance, { kmAllowance = it }, "Franquia km/semana", Modifier.weight(1f), decimal = false)
                    NumberField(extraKm, { extraKm = it }, "R$ por km excedente", Modifier.weight(1f))
                }
                OutlinedTextField(tiers, { tiers = it }, label = { Text("Faixas (corridas=desconto)") }, modifier = Modifier.fillMaxWidth())
                Text("Ex.: 40=80, 60=160, 80=250. Deixe vazio se a locadora não tem faixas.", style = MaterialTheme.typography.bodySmall)
            }
        }
        SectionCard("Meta de corridas por dia") {
            DayOfWeek.entries.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d ->
                        val st = goals.getValue(d)
                        NumberField(st.value, { st.value = it }, d.shortPt(), Modifier.width(80.dp), decimal = false)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = carry, onCheckedChange = { carry = it })
                Text("  Atraso aumenta a meta dos dias seguintes")
            }
        }
        SectionCard("Horários de virada (Brasília)") {
            OutlinedTextField(kmReset, { kmReset = it }, label = { Text("Franquia de km zera (segunda)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(rideReset, { rideReset = it }, label = { Text("Corridas e parcela viram (segunda)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(workdayEnd, { workdayEnd = it }, label = { Text("Dia de trabalho termina às") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        SectionCard("Salvar") {
            message?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
            Button(onClick = {
                val result = buildSettings(
                    initial, ownership, company, installment, kmAllowance, extraKm, tiers,
                    goals.mapValues { it.value.value }, carry, kmReset, rideReset, workdayEnd,
                )
                message = result.fold(
                    onSuccess = { onSave(it); "Configurações salvas." },
                    onFailure = { it.message ?: "Valor inválido." },
                )
            }, modifier = Modifier.fillMaxWidth()) { Text("Salvar configurações") }
        }
    }
}

private fun applyPreset(p: RentalPlan, set: (String, String, String, String, String) -> Unit) = set(
    p.companyName,
    centsText(p.weeklyInstallmentCents),
    p.weeklyKmAllowance.toString(),
    centsText(p.extraKmRateCents),
    p.tiers.sorted.joinToString(", ") { "${it.minRides}=${centsText(it.discountCents)}" },
)

private fun buildSettings(
    base: AppSettings,
    ownership: VehicleOwnership,
    company: String,
    installment: String,
    kmAllowance: String,
    extraKm: String,
    tiers: String,
    goals: Map<DayOfWeek, String>,
    carry: Boolean,
    kmReset: String,
    rideReset: String,
    workdayEnd: String,
): Result<AppSettings> = runCatching {
    fun money(t: String, what: String) = ManualTripEntry.parseMoneyToCents(t.ifBlank { "0" })?.takeIf { it >= 0 } ?: error("$what inválido.")
    fun time(t: String, what: String) = runCatching { LocalTime.parse(t.trim()) }.getOrNull() ?: error("$what: use HH:mm.")
    val tierText = tiers.split(',').filter { it.isNotBlank() }.joinToString(",") { part ->
        val pieces = part.split('=')
        if (pieces.size != 2) error("Faixas: use corridas=desconto.")
        "${pieces[0].trim().toIntOrNull() ?: error("Faixas: número de corridas inválido.")}:${money(pieces[1], "Desconto da faixa")}"
    }
    val tierTable = SettingsCodec.decodeTiers(tierText) ?: error("Faixas inválidas.")
    val goalPlan = DailyGoalPlan(goals.mapValues { (d, v) -> v.trim().toIntOrNull()?.takeIf { it >= 0 } ?: error("Meta de ${d.shortPt()} inválida.") })
    base.copy(
        ownership = ownership,
        rental = base.rental.copy(
            companyName = company.trim().ifEmpty { "Locadora" },
            weeklyInstallmentCents = money(installment, "Parcela"),
            weeklyKmAllowance = kmAllowance.trim().toIntOrNull()?.takeIf { it >= 0 } ?: error("Franquia inválida."),
            extraKmRateCents = money(extraKm, "Valor do km excedente"),
            tiers = tierTable,
        ),
        goals = goalPlan,
        carryShortfall = carry,
        turnover = base.turnover.copy(
            kmResetTime = time(kmReset, "Virada da franquia"),
            rideWeekResetTime = time(rideReset, "Virada das corridas"),
            workdayEndTime = time(workdayEnd, "Fim do dia de trabalho"),
        ),
    )
}
