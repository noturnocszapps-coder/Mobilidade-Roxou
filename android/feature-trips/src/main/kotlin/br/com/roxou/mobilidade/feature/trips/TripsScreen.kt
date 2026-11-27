package br.com.roxou.mobilidade.feature.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import br.com.roxou.mobilidade.domain.model.PaymentMethod
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.model.Trip
import br.com.roxou.mobilidade.domain.trips.ManualTripForm
import br.com.roxou.mobilidade.ui.ChoiceChips
import br.com.roxou.mobilidade.ui.NumberField
import br.com.roxou.mobilidade.ui.SectionCard
import br.com.roxou.mobilidade.ui.defaultDateText
import br.com.roxou.mobilidade.ui.defaultTimeText
import br.com.roxou.mobilidade.ui.formatBrl
import br.com.roxou.mobilidade.ui.formatDateTime
import br.com.roxou.mobilidade.ui.formatKm
import br.com.roxou.mobilidade.ui.label
import br.com.roxou.mobilidade.ui.parseBrasiliaDateTime

@Composable
fun TripsScreen(repo: RoxouRepository) {
    val vm: TripsViewModel = viewModel { TripsViewModel(repo) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return
    val weekTrips = s.records.trips.filter { it.endedAt in s.snapshot.rideWeek }

    LazyColumn(Modifier.fillMaxSize()) {
        item { ManualTripCard(vm) }
        item {
            Text(
                "Corridas da semana (${weekTrips.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        items(weekTrips, key = { it.id }) { trip ->
            TripRow(trip, onConfirm = { vm.setConfirmed(trip.id, it) }, onDelete = { vm.delete(trip.id) })
        }
    }
}

@Composable
private fun ManualTripCard(vm: TripsViewModel) {
    var platform by remember { mutableStateOf(Platform.UBER) }
    var payment by remember { mutableStateOf(PaymentMethod.APP) }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(defaultDateText(vm.now())) }
    var time by remember { mutableStateOf(defaultTimeText(vm.now())) }
    var pickupKm by remember { mutableStateOf("") }
    var tripKm by remember { mutableStateOf("") }
    var pickupMin by remember { mutableStateOf("") }
    var tripMin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    SectionCard("Lançar corrida") {
        ChoiceChips(listOf(Platform.UBER, Platform.NINETY_NINE, Platform.PRIVATE), platform, { it.label() }) { platform = it }
        ChoiceChips(PaymentMethod.entries, payment, { if (it == PaymentMethod.APP) "Pelo app" else "Dinheiro" }) { payment = it }
        NumberField(amount, { amount = it }, "Valor recebido (R$)", Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(date, { date = it }, label = { Text("Dia (dd/MM)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(time, { time = it }, label = { Text("Fim (HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(pickupKm, { pickupKm = it }, "Km até passageiro", Modifier.weight(1f))
            NumberField(tripKm, { tripKm = it }, "Km da viagem", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(pickupMin, { pickupMin = it }, "Min até passageiro", Modifier.weight(1f), decimal = false)
            NumberField(tripMin, { tripMin = it }, "Min da viagem", Modifier.weight(1f), decimal = false)
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                val endedAt = parseBrasiliaDateTime(date, time, vm.now())
                if (endedAt == null) {
                    message = "Dia ou horário inválido."
                    return@Button
                }
                vm.addManual(
                    ManualTripForm(platform, amount, endedAt, payment, pickupKm, tripKm, pickupMin, tripMin),
                ) { error ->
                    message = error
                    if (error == null) {
                        amount = ""; pickupKm = ""; tripKm = ""; pickupMin = ""; tripMin = ""
                        date = defaultDateText(vm.now()); time = defaultTimeText(vm.now())
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Salvar corrida") }
    }
}

@Composable
private fun TripRow(trip: Trip, onConfirm: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${trip.platform.label()} · ${formatBrl(trip.amountCents)}", style = MaterialTheme.typography.bodyLarge)
                val details = buildList {
                    add(formatDateTime(trip.endedAt))
                    trip.totalKm?.let { add(formatKm(it)) }
                    if (trip.paymentMethod == PaymentMethod.CASH) add("dinheiro")
                    add(if (trip.source.name == "MANUAL") "manual" else "automático")
                }
                Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Checkbox(checked = trip.confirmed, onCheckedChange = onConfirm)
                Text("conta", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = onDelete) { Text("Excluir") }
        }
    }
}
