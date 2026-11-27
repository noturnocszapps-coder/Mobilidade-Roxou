package br.com.roxou.mobilidade.domain.finance

import br.com.roxou.mobilidade.domain.fuel.FuelLog
import br.com.roxou.mobilidade.domain.model.Cents
import br.com.roxou.mobilidade.domain.model.EarningCategory
import br.com.roxou.mobilidade.domain.model.Expense
import br.com.roxou.mobilidade.domain.model.ExtraEarning
import br.com.roxou.mobilidade.domain.model.PaymentMethod
import br.com.roxou.mobilidade.domain.model.Trip
import br.com.roxou.mobilidade.domain.rental.RentalPayment

/**
 * Separa as três coisas que o motorista costuma misturar: faturamento, caixa e dívida da locação.
 *
 * Regras:
 * - Dinheiro retido para a locadora conta no faturamento, mas não no caixa.
 * - Desconto de faixa nunca é caixa (nem aparece aqui: ele só reduz a parcela seguinte).
 * - Cada real sai do caixa uma única vez: parcela (retida ou paga), combustível ou despesa.
 */
data class FinanceSummary(
    val revenueByCategory: Map<EarningCategory, Cents>,
    val cashReceivedCents: Cents,
    val appReceivedCents: Cents,
    val rentalOutflowCents: Cents,
    val fuelCents: Cents,
    val otherExpensesCents: Cents,
) {
    val grossRevenueCents: Cents get() = revenueByCategory.values.sum()
    val rideRevenueCents: Cents get() = revenueByCategory.filterKeys { it.isRideRevenue }.values.sum()
    val extraRevenueCents: Cents get() = grossRevenueCents - rideRevenueCents

    /** Dinheiro realmente livre: tudo o que entrou, menos o que foi para a locadora, combustível e despesas. */
    val availableCashCents: Cents get() = grossRevenueCents - rentalOutflowCents - fuelCents - otherExpensesCents

    /** Lucro líquido estimado do período (mesma conta, sem separar o que já foi pago). */
    val estimatedNetProfitCents: Cents get() = availableCashCents
}

object FinanceEngine {
    fun summarize(
        trips: List<Trip>,
        extras: List<ExtraEarning>,
        rentalPayments: List<RentalPayment>,
        fuelLogs: List<FuelLog>,
        expenses: List<Expense>,
    ): FinanceSummary {
        val byCategory = mutableMapOf<EarningCategory, Cents>()
        trips.forEach { byCategory.merge(it.category, it.amountCents, Long::plus) }
        extras.forEach { byCategory.merge(it.category, it.amountCents, Long::plus) }
        val cash = trips.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.amountCents } +
            extras.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.amountCents }
        val total = byCategory.values.sum()
        return FinanceSummary(
            revenueByCategory = byCategory,
            cashReceivedCents = cash,
            appReceivedCents = total - cash,
            rentalOutflowCents = rentalPayments.sumOf { it.amountCents },
            fuelCents = fuelLogs.sumOf { it.totalAmountCents },
            otherExpensesCents = expenses.sumOf { it.amountCents },
        )
    }
}

/**
 * Meta do dia em cascata (porte de `operationalProgress.ts`): o faturamento do dia
 * primeiro cobre o combustível, depois a meta da locação; o resto é lucro livre.
 */
data class DailyCascade(
    val grossCents: Cents,
    val fuelTotalCents: Cents,
    val fuelCoveredCents: Cents,
    val rentalTargetCents: Cents,
    val rentalCoveredCents: Cents,
    val freeProfitCents: Cents,
) {
    val fuelRemainingCents: Cents get() = (fuelTotalCents - fuelCoveredCents).coerceAtLeast(0)
    val rentalRemainingCents: Cents get() = (rentalTargetCents - rentalCoveredCents).coerceAtLeast(0)
    val totalTargetCents: Cents get() = fuelTotalCents + rentalTargetCents
    val totalRemainingCents: Cents get() = fuelRemainingCents + rentalRemainingCents
    val fuelProgress: Double get() = if (fuelTotalCents > 0) fuelCoveredCents.toDouble() / fuelTotalCents else 1.0
    val rentalProgress: Double get() = if (rentalTargetCents > 0) rentalCoveredCents.toDouble() / rentalTargetCents else 1.0
    val isAllTargetMet: Boolean get() = if (totalTargetCents > 0) totalRemainingCents == 0L else grossCents > 0
}

object DailyCascadeEngine {
    fun compute(grossCents: Cents, fuelTotalCents: Cents, rentalTargetCents: Cents): DailyCascade {
        val gross = grossCents.coerceAtLeast(0)
        val fuelCovered = minOf(gross, fuelTotalCents.coerceAtLeast(0))
        val afterFuel = gross - fuelCovered
        val rentalCovered = minOf(afterFuel, rentalTargetCents.coerceAtLeast(0))
        return DailyCascade(gross, fuelTotalCents, fuelCovered, rentalTargetCents, rentalCovered, afterFuel - rentalCovered)
    }

    /**
     * Meta diária da locação: o que falta da parcela dividido pelos dias de trabalho que restam
     * na semana (incluindo hoje). Diferente do Lucro App, que dividia a parcela por 7 sempre.
     */
    fun rentalDailyTarget(pendingInstallmentCents: Cents, remainingWorkdaysIncludingToday: Int): Cents {
        if (pendingInstallmentCents <= 0 || remainingWorkdaysIncludingToday <= 0) return 0
        val days = remainingWorkdaysIncludingToday.toLong()
        return (pendingInstallmentCents + days - 1) / days
    }
}
