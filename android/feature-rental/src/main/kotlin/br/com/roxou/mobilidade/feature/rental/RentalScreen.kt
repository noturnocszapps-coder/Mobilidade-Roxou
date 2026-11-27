package br.com.roxou.mobilidade.feature.rental

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.domain.km.KmStatus
import br.com.roxou.mobilidade.domain.rental.RentalPaymentSource
import br.com.roxou.mobilidade.ui.ChoiceChips
import br.com.roxou.mobilidade.ui.NumberField
import br.com.roxou.mobilidade.ui.Progress
import br.com.roxou.mobilidade.ui.RoxouGreen
import br.com.roxou.mobilidade.ui.RoxouRed
import br.com.roxou.mobilidade.ui.RoxouYellow
import br.com.roxou.mobilidade.ui.SectionCard
import br.com.roxou.mobilidade.ui.StatRow
import br.com.roxou.mobilidade.ui.formatBrl
import br.com.roxou.mobilidade.ui.formatDateTime
import br.com.roxou.mobilidade.ui.formatDayMonth
import br.com.roxou.mobilidade.ui.formatKm

@Composable
fun RentalScreen(repo: RoxouRepository) {
    val vm: RentalViewModel = viewModel { RentalViewModel(repo) }
    val state by vm.state.collectAsStateWithLifecycle()
    val st = state ?: return
    val s = st.snapshot
    val inst = s.installment

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (inst == null) {
            SectionCard("Locação") { Text("Carro próprio: sem parcela de locação. Mude em Configurações se o carro for alugado.") }
            return@Column
        }
        SectionCard("Parcela da semana de ${formatDayMonth(inst.weekId)} · ${st.settings.rental.companyName}") {
            StatRow("Parcela base", formatBrl(inst.baseInstallmentCents))
            StatRow("Desconto da semana anterior", "- ${formatBrl(inst.discountAppliedCents)}", RoxouGreen)
            StatRow("Devido nesta semana", formatBrl(inst.dueCents), bold = true)
            StatRow("Pago (confirmado)", formatBrl(inst.confirmedPaidCents))
            if (inst.unconfirmedCents > 0) StatRow("Aguardando confirmação", formatBrl(inst.unconfirmedCents), RoxouYellow)
            StatRow("Pendente", formatBrl(inst.pendingCents), if (inst.isPaid) RoxouGreen else RoxouRed, bold = true)
            if (inst.overpaidCents > 0) StatRow("Pago a mais", formatBrl(inst.overpaidCents), RoxouYellow)
            Progress(if (inst.dueCents > 0) inst.confirmedPaidCents.toFloat() / inst.dueCents else 1f)
            Text(
                "Desconto já garantido para a próxima semana: ${formatBrl(inst.discountSecuredForNextWeekCents)} (não é caixa).",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        PaymentForm { amount, source, confirmed -> vm.addPayment(inst.weekId, amount, source, confirmed) }
        val payments = st.records.rentalPayments.filter { it.weekId == inst.weekId }
        if (payments.isNotEmpty()) {
            SectionCard("Abatimentos desta semana") {
                payments.forEach { p ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(formatBrl(p.amountCents))
                            Text(
                                (if (p.source == RentalPaymentSource.UBER_WITHHOLDING) "Retido pela Uber" else "Pago por mim") + " · " + formatDateTime(p.paidAt),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Checkbox(checked = p.confirmed, onCheckedChange = { vm.setConfirmed(p.id, it) })
                        TextButton(onClick = { vm.deletePayment(p.id) }) { Text("Excluir") }
                    }
                }
            }
        }
        s.km?.let { km ->
            SectionCard("Franquia de km (vira segunda 00h)") {
                StatRow("Usado", "${formatKm(km.totalKm)} de ${km.limitKm} km", bold = true)
                StatRow("Trabalho / particular", "${formatKm(km.workKm)} / ${formatKm(km.privateKm)}")
                StatRow("Restante", formatKm(km.remainingKm))
                if (km.exceededKm > 0) StatRow("Excedente", "${formatKm(km.exceededKm)} · ${formatBrl(km.exceededCostCents)}", RoxouRed)
                val color = when (km.status) { KmStatus.NORMAL -> RoxouGreen; KmStatus.ATTENTION -> RoxouYellow; KmStatus.LIMIT_REACHED -> RoxouRed }
                Progress((km.usedPercent / 100).toFloat(), color)
            }
            KmForm { w, p -> vm.addKm(w, p) }
        }
    }
}

@Composable
private fun PaymentForm(onAdd: (String, RentalPaymentSource, Boolean) -> String?) {
    var amount by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(RentalPaymentSource.UBER_WITHHOLDING) }
    var confirmed by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    SectionCard("Registrar abatimento") {
        ChoiceChips(RentalPaymentSource.entries, source, { if (it == RentalPaymentSource.UBER_WITHHOLDING) "Retido pela Uber" else "Pago por mim" }) { source = it }
        NumberField(amount, { amount = it }, "Valor (R$)", Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
            Text("Confirmado no extrato")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            error = onAdd(amount, source, confirmed)
            if (error == null) amount = ""
        }, modifier = Modifier.fillMaxWidth()) { Text("Salvar abatimento") }
    }
}

@Composable
private fun KmForm(onAdd: (String, String) -> String?) {
    var work by remember { mutableStateOf("") }
    var privateKm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    SectionCard("Lançar km") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(work, { work = it }, "Km trabalho", Modifier.weight(1f))
            NumberField(privateKm, { privateKm = it }, "Km particular", Modifier.weight(1f))
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) }
        Button(onClick = {
            error = onAdd(work, privateKm)
            if (error == null) { work = ""; privateKm = "" }
        }, modifier = Modifier.fillMaxWidth()) { Text("Salvar km") }
    }
}
