package br.com.roxou.mobilidade.domain.km

import br.com.roxou.mobilidade.domain.journey.Journey
import br.com.roxou.mobilidade.domain.model.Cents
import br.com.roxou.mobilidade.domain.time.WeekWindow
import java.time.Instant

/** Km rodados num momento, separados em trabalho e particular (o mesmo km nunca entra duas vezes). */
data class KmRecord(val id: String, val at: Instant, val workKm: Double, val privateKm: Double = 0.0)

enum class KmStatus { NORMAL, ATTENTION, LIMIT_REACHED }

data class KmAllowanceProgress(
    val week: WeekWindow,
    val limitKm: Int,
    val workKm: Double,
    val privateKm: Double,
    val extraKmRateCents: Cents,
    /** Jornada que atravessou a virada sem odômetro registrado nela. */
    val pendingAttributionJourneyId: String? = null,
) {
    val isConfigured: Boolean get() = limitKm > 0
    val totalKm: Double get() = workKm + privateKm
    val remainingKm: Double get() = (limitKm - totalKm).coerceAtLeast(0.0)
    val usedPercent: Double get() = if (limitKm > 0) totalKm / limitKm * 100 else 0.0
    val exceededKm: Double get() = (totalKm - limitKm).coerceAtLeast(0.0)
    val exceededCostCents: Cents get() = Math.round(exceededKm * extraKmRateCents)
    val status: KmStatus
        get() = when {
            !isConfigured -> KmStatus.NORMAL
            usedPercent >= 100 -> KmStatus.LIMIT_REACHED
            usedPercent >= 80 -> KmStatus.ATTENTION
            else -> KmStatus.NORMAL
        }
}

/**
 * Franquia semanal de km (porte de `calculateWeeklyKmProgress`). A semana é a da franquia
 * (segunda 00h). Consome todo km, de trabalho e particular.
 *
 * Jornadas com odômetro de início e fim: se cruzam a virada e há odômetro registrado nela,
 * só os km depois da virada entram na semana nova. Sem esse registro, a jornada fica com a semana
 * em que começou e o caso é sinalizado (como no Lucro App, não se inventa divisão proporcional).
 */
object KmAllowanceEngine {
    fun progress(
        week: WeekWindow,
        limitKm: Int,
        extraKmRateCents: Cents,
        records: List<KmRecord>,
        journeys: List<Journey> = emptyList(),
    ): KmAllowanceProgress {
        val inWeek = records.filter { it.at in week }
        var work = inWeek.sumOf { it.workKm }
        val private = inWeek.sumOf { it.privateKm }
        var pending: String? = null

        for (j in journeys) {
            val startOdo = j.startOdometerKm ?: continue
            val endOdo = j.endOdometerKm ?: continue
            val total = (endOdo - startOdo).coerceAtLeast(0.0)
            val startsIn = j.startedAt in week
            val end = j.endedAt ?: j.startedAt
            val crossesStart = j.startedAt.isBefore(week.start.toInstant()) && !end.isBefore(week.start.toInstant())
            val crossesEnd = startsIn && !end.isBefore(week.endExclusive.toInstant())
            val startCheckpoint = j.checkpoints.firstOrNull { it.at == week.start.toInstant() }
            val endCheckpoint = j.checkpoints.firstOrNull { it.at == week.endExclusive.toInstant() }
            when {
                crossesStart && startCheckpoint != null -> work += (endOdo - startCheckpoint.odometerKm).coerceAtLeast(0.0)
                crossesStart -> pending = j.id
                crossesEnd && endCheckpoint != null -> work += (endCheckpoint.odometerKm - startOdo).coerceAtLeast(0.0)
                startsIn -> work += total
            }
        }
        return KmAllowanceProgress(week, limitKm, work, private, extraKmRateCents, pending)
    }
}
