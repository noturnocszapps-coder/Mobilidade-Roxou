package br.com.roxou.mobilidade.domain.restday

import br.com.roxou.mobilidade.domain.model.Cents
import java.time.DayOfWeek
import java.time.LocalDate

/** Resumo de um dia de trabalho fechado (ou folga). */
data class DayResult(
    val workday: LocalDate,
    val worked: Boolean,
    val grossCents: Cents,
    val costsCents: Cents,
    val hours: Double,
    val km: Double,
) {
    val netCents: Cents get() = grossCents - costsCents
}

data class DayOfWeekStats(
    val day: DayOfWeek,
    val workedDays: Int,
    val avgNetCents: Double,
    val avgNetPerHourCents: Double,
    val avgHours: Double,
)

enum class Confidence { LEARNING, LOW, MEDIUM, HIGH }

data class RestDayRecommendation(
    val hasSufficientData: Boolean,
    val confidence: Confidence,
    val best: DayOfWeek? = null,
    val secondBest: DayOfWeek? = null,
    val stats: List<DayOfWeekStats>,
)

/** Porte de `src/lib/restDayEngine.ts`: o melhor dia de folga é o de menor R$/hora líquido no histórico. */
object RestDayEngine {
    fun recommend(history: List<DayResult>): RestDayRecommendation {
        val stats = DayOfWeek.entries.map { dow ->
            val worked = history.filter { it.workday.dayOfWeek == dow && it.worked && (it.km > 0 || it.grossCents > 0) }
            val n = worked.size
            val totalNet = worked.sumOf { it.netCents }.toDouble()
            val totalHours = worked.sumOf { it.hours }
            DayOfWeekStats(
                day = dow,
                workedDays = n,
                avgNetCents = if (n > 0) totalNet / n else 0.0,
                avgNetPerHourCents = when {
                    n == 0 -> 0.0
                    totalHours > 0 -> totalNet / totalHours
                    else -> totalNet / (n * 6.0)
                },
                avgHours = if (n > 0) totalHours / n else 0.0,
            )
        }
        val totalWorked = history.count { it.worked }
        if (totalWorked < 5) return RestDayRecommendation(false, Confidence.LEARNING, stats = stats)
        val withData = stats.filter { it.workedDays >= 1 }
        if (withData.size < 3) return RestDayRecommendation(false, Confidence.LOW, stats = stats)
        val sorted = withData.sortedWith(compareBy<DayOfWeekStats> { it.avgNetPerHourCents }.thenBy { it.avgNetCents })
        val confidence = when {
            totalWorked >= 20 -> Confidence.HIGH
            totalWorked >= 10 -> Confidence.MEDIUM
            else -> Confidence.LOW
        }
        return RestDayRecommendation(true, confidence, sorted[0].day, sorted.getOrNull(1)?.day, stats)
    }
}
