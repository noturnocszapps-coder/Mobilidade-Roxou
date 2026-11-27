package br.com.roxou.mobilidade

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import br.com.roxou.mobilidade.data.AppState
import br.com.roxou.mobilidade.data.RoxouRepository
import br.com.roxou.mobilidade.design.FinancasScreen
import br.com.roxou.mobilidade.design.FinancasUi
import br.com.roxou.mobilidade.design.GastoItem
import br.com.roxou.mobilidade.design.OfertasResumoScreen
import br.com.roxou.mobilidade.design.OfertasResumoUi
import br.com.roxou.mobilidade.domain.model.Expense
import br.com.roxou.mobilidade.domain.model.ExpenseCategory
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.offer.OfferStats
import br.com.roxou.mobilidade.ui.formatDayMonth
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.ZoneId

private const val FUEL_NOTE = "Combustível"

/** Tela de Finanças (visual do AI Studio) com os dados da semana de corridas. */
@Composable
fun FinancasRoute(repo: RoxouRepository, state: AppState, onVoltar: () -> Unit) {
    val scope = rememberCoroutineScope()
    FinancasScreen(
        ui = state.toFinancasUi(),
        onLancarGasto = { descricao, valor, combustivel ->
            val nota = when {
                combustivel && descricao.isBlank() -> FUEL_NOTE
                combustivel -> "$FUEL_NOTE: ${descricao.trim()}"
                else -> descricao.trim().ifBlank { null }
            }
            scope.launch {
                repo.saveExpense(Expense(repo.newId(), ExpenseCategory.OTHER, repo.now(), valor, nota))
            }
        },
        onApagarGasto = { id ->
            scope.launch {
                when {
                    id.startsWith("f:") -> repo.deleteFuel(id.removePrefix("f:"))
                    else -> repo.deleteExpense(id.removePrefix("e:"))
                }
            }
        },
        onVoltar = onVoltar,
    )
}

private fun AppState.toFinancasUi(): FinancasUi {
    val s = snapshot
    val week = s.rideWeek
    val zone = ZoneId.systemDefault()
    fun dia(i: java.time.Instant) = formatDayMonth(i.atZone(zone).toLocalDate())
    val expenses = records.expenses.filter { it.paidAt in week }
    val fuel = records.fuelLogs.filter { it.filledAt in week }
    fun isFuel(e: Expense) = e.notes?.startsWith(FUEL_NOTE) == true
    val gastos = (
        expenses.map { e ->
            Triple(e.paidAt, GastoItem("e:${e.id}", e.notes ?: "Gasto", e.amountCents, dia(e.paidAt), isFuel(e)), Unit)
        } + fuel.map { f ->
            Triple(f.filledAt, GastoItem("f:${f.id}", f.notes ?: FUEL_NOTE, f.totalAmountCents, dia(f.filledAt), true), Unit)
        }
        ).sortedByDescending { it.first }.map { it.second }
    val combustivel = fuel.sumOf { it.totalAmountCents } + expenses.filter(::isFuel).sumOf { it.amountCents }
    val outros = expenses.filterNot(::isFuel).sumOf { it.amountCents }
    return FinancasUi(
        ganhosSemanaCentavos = s.financeWeek.grossRevenueCents,
        gastosSemanaCentavos = combustivel + outros,
        combustivelSemanaCentavos = combustivel,
        outrosGastosSemanaCentavos = outros,
        dinheiroLivreCentavos = s.financeWeek.availableCashCents,
        parcelaFaltaCentavos = s.installment?.pendingCents,
        gastos = gastos,
    )
}

/** Resumo das ofertas que tocaram (visual do AI Studio). */
@Composable
fun OfertasResumoRoute(stats: OfferStats, onVoltar: () -> Unit) {
    val dias = listOf(
        DayOfWeek.MONDAY to "Seg", DayOfWeek.TUESDAY to "Ter", DayOfWeek.WEDNESDAY to "Qua", DayOfWeek.THURSDAY to "Qui",
        DayOfWeek.FRIDAY to "Sex", DayOfWeek.SATURDAY to "Sáb", DayOfWeek.SUNDAY to "Dom",
    )
    val nomes = mapOf(
        DayOfWeek.MONDAY to "Segunda", DayOfWeek.TUESDAY to "Terça", DayOfWeek.WEDNESDAY to "Quarta", DayOfWeek.THURSDAY to "Quinta",
        DayOfWeek.FRIDAY to "Sexta", DayOfWeek.SATURDAY to "Sábado", DayOfWeek.SUNDAY to "Domingo",
    )
    OfertasResumoScreen(
        ui = OfertasResumoUi(
            hojeTotal = stats.todayTotal,
            hojeAceitas = stats.todayAccepted,
            mediaPorDia = stats.perDay,
            taxaAceite = stats.acceptRate,
            melhorDiaTexto = stats.bestWeekday?.let { nomes[it] },
            melhorHorarioTexto = stats.busiestHour?.let { "${it}h às ${it + 1}h" },
            porDiaDaSemana = dias.map { (d, n) -> n to (stats.perWeekday[d] ?: 0.0) },
            uber = stats.byPlatform[Platform.UBER] ?: 0,
            noventaENove = stats.byPlatform[Platform.NINETY_NINE] ?: 0,
        ),
        onVoltar = onVoltar,
    )
}
