package br.com.roxou.mobilidade.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.domain.goals.TierProgress
import br.com.roxou.mobilidade.ui.NumberField
import br.com.roxou.mobilidade.ui.Progress
import br.com.roxou.mobilidade.ui.RoxouGreen
import br.com.roxou.mobilidade.ui.SectionCard
import br.com.roxou.mobilidade.ui.StatRow
import br.com.roxou.mobilidade.ui.formatBrl
import br.com.roxou.mobilidade.ui.formatDayMonth
import br.com.roxou.mobilidade.ui.shortPt

/** Texto da janela flutuante e do painel: "faltam 6 para R$ 160 e 26 para R$ 250". */
fun tierRemainingText(p: TierProgress): String =
    if (p.upcoming.isEmpty()) "Faixa máxima garantida!"
    else "Faltam " + p.upcoming.joinToString(" e ") { "${it.ridesRemaining} para ${formatBrl(it.tier.discountCents)}" }

@Composable
fun GoalsScreen(repo: RoxouRepository) {
    val vm: GoalsViewModel = viewModel { GoalsViewModel(repo) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state?.snapshot ?: return
    val goals = s.goals

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        s.tiers?.let { tiers ->
            SectionCard("Faixas da locadora") {
                StatRow("Corridas na semana (oficial)", "${s.rideCount.official}", bold = true)
                if (s.rideCount.isCorrected) Text("Corrigido pelo painel da Uber (app contou ${s.rideCount.countedByApp})", style = MaterialTheme.typography.bodySmall)
                StatRow("Desconto garantido p/ semana seguinte", formatBrl(tiers.achievedDiscountCents), RoxouGreen, bold = true)
                Text(tierRemainingText(tiers))
                tiers.next?.let { Progress(s.rideCount.official.toFloat() / it.minRides) }
            }
        }
        SectionCard("Meta semanal") {
            StatRow("Feitas", "${goals.done} de ${goals.weeklyTarget}", bold = true)
            StatRow("Faltam", "${goals.remaining}")
            Progress(if (goals.weeklyTarget > 0) goals.done.toFloat() / goals.weeklyTarget else 1f)
        }
        SectionCard("Dia a dia") {
            Text("O excedente de um dia reduz a meta dos dias seguintes.", style = MaterialTheme.typography.bodySmall)
            goals.days.forEach { d ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val weight = if (d.isToday) FontWeight.Bold else FontWeight.Normal
                    Text("${d.date.dayOfWeek.shortPt()} ${formatDayMonth(d.date)}${if (d.isToday) " (hoje)" else ""}", fontWeight = weight)
                    val target = if (d.adjustedTarget != d.originalTarget) "${d.adjustedTarget} (era ${d.originalTarget})" else "${d.adjustedTarget}"
                    Text("${d.done} / $target", fontWeight = weight, color = if (d.done >= d.adjustedTarget && d.adjustedTarget > 0) RoxouGreen else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        UberPanelCard(currentOverride = s.rideCount.uberPanelOverride) { vm.setUberPanelCount(s.rideWeek.id, it) }
    }
}

@Composable
private fun UberPanelCard(currentOverride: Int?, onSave: (String) -> Unit) {
    var text by remember(currentOverride) { mutableStateOf(currentOverride?.toString() ?: "") }
    SectionCard("Corrigir pelo painel da Uber") {
        Text("A contagem oficial é a que a Uber informa à locadora. Deixe vazio para usar a contagem do app.", style = MaterialTheme.typography.bodySmall)
        NumberField(text, { text = it }, "Viagens no painel da Uber", Modifier.fillMaxWidth(), decimal = false)
        Button(onClick = { onSave(text) }, modifier = Modifier.fillMaxWidth()) { Text("Salvar correção") }
    }
}
