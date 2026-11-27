package br.com.roxou.mobilidade.feature.finance

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import br.com.roxou.mobilidade.domain.fuel.FuelEngine
import br.com.roxou.mobilidade.domain.model.EarningCategory
import br.com.roxou.mobilidade.domain.model.ExpenseCategory
import br.com.roxou.mobilidade.domain.model.PaymentMethod
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.ui.ChoiceChips
import br.com.roxou.mobilidade.ui.NumberField
import br.com.roxou.mobilidade.ui.RoxouGreen
import br.com.roxou.mobilidade.ui.RoxouRed
import br.com.roxou.mobilidade.ui.SectionCard
import br.com.roxou.mobilidade.ui.StatRow
import br.com.roxou.mobilidade.ui.formatBrl
import br.com.roxou.mobilidade.ui.formatDateTime
import br.com.roxou.mobilidade.ui.label

@Composable
fun FinanceScreen(repo: RoxouRepository) {
    val vm: FinanceViewModel = viewModel { FinanceViewModel(repo) }
    val state by vm.state.collectAsStateWithLifecycle()
    val st = state ?: return
    val f = st.snapshot.financeWeek
    val week = st.snapshot.rideWeek

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionCard("Faturamento da semana") {
            EarningCategory.entries.forEach { c ->
                val v = f.revenueByCategory[c] ?: 0
                if (v != 0L) StatRow(c.label(), formatBrl(v))
            }
            StatRow("Total faturado", formatBrl(f.grossRevenueCents), bold = true)
            StatRow("Recebido em dinheiro", formatBrl(f.cashReceivedCents))
            StatRow("Recebido pelo app", formatBrl(f.appReceivedCents))
        }
        SectionCard("Caixa da semana") {
            StatRow("Faturamento", formatBrl(f.grossRevenueCents))
            StatRow("Locação (retida ou paga)", "- ${formatBrl(f.rentalOutflowCents)}")
            StatRow("Combustível", "- ${formatBrl(f.fuelCents)}")
            StatRow("Outras despesas", "- ${formatBrl(f.otherExpensesCents)}")
            StatRow("Dinheiro livre", formatBrl(f.availableCashCents), if (f.availableCashCents >= 0) RoxouGreen else RoxouRed, bold = true)
            Text("Desconto de faixa nunca entra no caixa: ele só reduz a parcela da semana seguinte.", style = MaterialTheme.typography.bodySmall)
        }
        val consumption = FuelEngine.fullToFullConsumption(st.records.fuelLogs)
        SectionCard("Consumo") {
            val kmPerL = consumption.recentKmPerL
            if (kmPerL != null) StatRow("Consumo medido (tanque cheio a cheio)", String.format("%.1f km/l", kmPerL))
            else Text("Complete pelo menos 2 tanques cheios com odômetro para medir o consumo real.", style = MaterialTheme.typography.bodySmall)
        }
        ExtraForm(vm)
        FuelForm(vm)
        ExpenseForm(vm)
        SectionCard("Lançamentos da semana") {
            st.records.extras.filter { it.receivedAt in week }.forEach { e ->
                EntryRow("${e.category.label()} ${e.platform.label()} · ${formatBrl(e.amountCents)}", formatDateTime(e.receivedAt)) { vm.deleteExtra(e.id) }
            }
            st.records.fuelLogs.filter { it.filledAt in week }.forEach { l ->
                EntryRow("Combustível · ${formatBrl(l.totalAmountCents)} · ${l.liters} L", formatDateTime(l.filledAt)) { vm.deleteFuel(l.id) }
            }
            st.records.expenses.filter { it.paidAt in week }.forEach { x ->
                EntryRow("${x.category.label()} · ${formatBrl(x.amountCents)}", formatDateTime(x.paidAt)) { vm.deleteExpense(x.id) }
            }
        }
    }
}

@Composable
private fun EntryRow(title: String, subtitle: String, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onDelete) { Text("Excluir") }
    }
}

@Composable
private fun ExtraForm(vm: FinanceViewModel) {
    var platform by remember { mutableStateOf(Platform.NINETY_NINE) }
    var category by remember { mutableStateOf(EarningCategory.INCENTIVE) }
    var payment by remember { mutableStateOf(PaymentMethod.APP) }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val extraCategories = EarningCategory.entries.filter { !it.isRideRevenue }
    SectionCard("Lançar ganho extra") {
        ChoiceChips(listOf(Platform.UBER, Platform.NINETY_NINE, Platform.OTHER), platform, { it.label() }) { platform = it }
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            ChoiceChips(extraCategories, category, { it.label() }) { category = it }
        }
        ChoiceChips(PaymentMethod.entries, payment, { if (it == PaymentMethod.APP) "Pelo app" else "Dinheiro" }) { payment = it }
        NumberField(amount, { amount = it }, "Valor (R$)", Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            error = vm.addExtra(platform, category, amount, payment)
            if (error == null) amount = ""
        }, modifier = Modifier.fillMaxWidth()) { Text("Salvar ganho") }
    }
}

@Composable
private fun FuelForm(vm: FinanceViewModel) {
    var amount by remember { mutableStateOf("") }
    var liters by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("") }
    var full by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    SectionCard("Abastecimento") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(amount, { amount = it }, "Valor (R$)", Modifier.weight(1f))
            NumberField(liters, { liters = it }, "Litros", Modifier.weight(1f))
        }
        NumberField(odometer, { odometer = it }, "Odômetro (km)", Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = full, onCheckedChange = { full = it })
            Text("Completei o tanque")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            error = vm.addFuel(amount, liters, odometer, full)
            if (error == null) { amount = ""; liters = ""; odometer = "" }
        }, modifier = Modifier.fillMaxWidth()) { Text("Salvar abastecimento") }
    }
}

@Composable
private fun ExpenseForm(vm: FinanceViewModel) {
    var category by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    SectionCard("Outra despesa") {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            ChoiceChips(ExpenseCategory.entries, category, { it.label() }) { category = it }
        }
        NumberField(amount, { amount = it }, "Valor (R$)", Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            error = vm.addExpense(category, amount)
            if (error == null) amount = ""
        }, modifier = Modifier.fillMaxWidth()) { Text("Salvar despesa") }
    }
}
