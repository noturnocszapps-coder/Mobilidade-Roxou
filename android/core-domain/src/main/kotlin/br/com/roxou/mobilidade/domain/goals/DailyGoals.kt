package br.com.roxou.mobilidade.domain.goals

import java.time.DayOfWeek
import java.time.LocalDate

/** Meta de corridas por dia da semana. Padrão do Fernando: seg 8, ter 6, qua 6, qui 6, sex 18, sáb 20, dom 16. */
data class DailyGoalPlan(val ridesByDay: Map<DayOfWeek, Int>) {
    init {
        require(ridesByDay.values.all { it >= 0 }) { "meta diária não pode ser negativa" }
    }

    fun target(day: DayOfWeek): Int = ridesByDay[day] ?: 0
    val weeklyTotal: Int get() = DayOfWeek.entries.sumOf { target(it) }

    companion object {
        val DEFAULT = DailyGoalPlan(
            mapOf(
                DayOfWeek.MONDAY to 8,
                DayOfWeek.TUESDAY to 6,
                DayOfWeek.WEDNESDAY to 6,
                DayOfWeek.THURSDAY to 6,
                DayOfWeek.FRIDAY to 18,
                DayOfWeek.SATURDAY to 20,
                DayOfWeek.SUNDAY to 16,
            ),
        )
    }
}

data class DayGoal(
    val date: LocalDate,
    val originalTarget: Int,
    /** Meta depois da redistribuição. Para dias passados, igual à original. */
    val adjustedTarget: Int,
    val done: Int,
    val isPast: Boolean,
    val isToday: Boolean,
) {
    val remaining: Int get() = (adjustedTarget - done).coerceAtLeast(0)
}

data class WeeklyGoalStatus(
    val weeklyTarget: Int,
    val done: Int,
    val days: List<DayGoal>,
) {
    val remaining: Int get() = (weeklyTarget - done).coerceAtLeast(0)
    val today: DayGoal? get() = days.firstOrNull { it.isToday }
}

/**
 * Redistribuição da meta semanal: o excedente de um dia reduz a meta dos dias seguintes.
 * Por padrão o atraso NÃO aumenta os dias seguintes (o documento só pede a redução);
 * [carryShortfall] liga esse comportamento.
 */
object GoalRedistribution {

    /**
     * @param workdays os 7 dias de trabalho da semana, em ordem.
     * @param ridesByWorkday corridas que contam para a meta, por dia de trabalho.
     * @param today dia de trabalho atual. Dias antes dele estão fechados.
     */
    fun compute(
        plan: DailyGoalPlan,
        workdays: List<LocalDate>,
        ridesByWorkday: Map<LocalDate, Int>,
        today: LocalDate,
        weeklyTarget: Int = plan.weeklyTotal,
        carryShortfall: Boolean = false,
    ): WeeklyGoalStatus {
        require(workdays.size == 7) { "uma semana tem 7 dias de trabalho" }
        val original = workdays.map { plan.target(it.dayOfWeek) }
        val done = workdays.map { ridesByWorkday[it] ?: 0 }
        val pastIdx = workdays.indices.filter { workdays[it].isBefore(today) }
        val openIdx = workdays.indices.filter { !workdays[it].isBefore(today) }

        val donePast = pastIdx.sumOf { done[it] }
        val plannedPast = pastIdx.sumOf { original[it] }
        val plannedOpen = openIdx.sumOf { original[it] }
        // Quanto ainda falta distribuir entre hoje e os próximos dias.
        val neededOpen = (weeklyTarget - donePast).coerceAtLeast(0)
        val openBudget = if (carryShortfall) neededOpen else minOf(neededOpen, plannedOpen + (weeklyTarget - plannedPast - plannedOpen).coerceAtLeast(0))

        val adjustedOpen = distribute(openBudget, openIdx.map { original[it] })
        val adjusted = IntArray(7) { original[it] }
        openIdx.forEachIndexed { k, idx -> adjusted[idx] = adjustedOpen[k] }

        val days = workdays.indices.map { i ->
            DayGoal(
                date = workdays[i],
                originalTarget = original[i],
                adjustedTarget = adjusted[i],
                done = done[i],
                isPast = workdays[i].isBefore(today),
                isToday = workdays[i] == today,
            )
        }
        return WeeklyGoalStatus(weeklyTarget, done.sum(), days)
    }

    /** Divide [total] proporcionalmente aos pesos (maior resto), mantendo números inteiros. */
    internal fun distribute(total: Int, weights: List<Int>): List<Int> {
        if (weights.isEmpty()) return emptyList()
        val weightSum = weights.sum()
        if (weightSum == 0) {
            val base = total / weights.size
            val extra = total % weights.size
            return weights.indices.map { base + if (it < extra) 1 else 0 }
        }
        val exact = weights.map { it.toDouble() * total / weightSum }
        val floors = exact.map { kotlin.math.floor(it).toInt() }.toMutableList()
        var leftover = total - floors.sum()
        val order = exact.indices.sortedWith(compareByDescending<Int> { exact[it] - floors[it] }.thenBy { it })
        for (i in order) {
            if (leftover <= 0) break
            floors[i] += 1
            leftover--
        }
        return floors
    }
}
