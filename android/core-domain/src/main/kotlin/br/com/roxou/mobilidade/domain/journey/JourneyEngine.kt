package br.com.roxou.mobilidade.domain.journey

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

enum class JourneyStatus { ACTIVE, PAUSED, COMPLETED, CANCELLED }

data class JourneyPause(
    val id: String,
    val pausedAt: Instant,
    val resumedAt: Instant? = null,
    /** Horário original de retorno, guardado quando uma pausa esquecida é corrigida. */
    val originalResumedAt: Instant? = null,
    val correctionNotes: String? = null,
)

/** Odômetro registrado numa virada (ex.: segunda 00h, para a franquia de km). */
data class OdometerCheckpoint(val at: Instant, val odometerKm: Double)

data class Journey(
    val id: String,
    /** Dia de trabalho (termina às 04h) em que a jornada começou. */
    val workday: LocalDate,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val status: JourneyStatus,
    val startOdometerKm: Double? = null,
    val endOdometerKm: Double? = null,
    val pauses: List<JourneyPause> = emptyList(),
    val checkpoints: List<OdometerCheckpoint> = emptyList(),
)

data class JourneyTimeBreakdown(
    val grossSeconds: Long,
    val pauseSeconds: Long,
    val currentPauseSeconds: Long,
    val isCurrentlyPaused: Boolean,
) {
    val effectiveSeconds: Long get() = (grossSeconds - pauseSeconds).coerceAtLeast(0)
    /** Horas efetivas com 2 casas, para R$/hora. */
    val effectiveHours: Double get() = Math.round(effectiveSeconds / 3600.0 * 100) / 100.0
}

/** Porte de `src/lib/journeyEngine.ts` do Lucro App. */
object JourneyEngine {

    fun breakdown(journey: Journey?, now: Instant): JourneyTimeBreakdown {
        if (journey == null) return JourneyTimeBreakdown(0, 0, 0, false)
        val ended = journey.status == JourneyStatus.COMPLETED || journey.status == JourneyStatus.CANCELLED || journey.endedAt != null
        val end = if (ended && journey.endedAt != null) journey.endedAt else now
        val gross = seconds(journey.startedAt, end)
        var pause = 0L
        var current = 0L
        for (p in journey.pauses) {
            if (p.resumedAt != null) {
                pause += seconds(p.pausedAt, p.resumedAt)
            } else {
                val d = seconds(p.pausedAt, if (ended) end else now)
                pause += d
                current = d
            }
        }
        return JourneyTimeBreakdown(gross, pause, current, journey.status == JourneyStatus.PAUSED)
    }

    sealed interface Validation {
        data object Valid : Validation
        data class Invalid(val reason: String) : Validation
    }

    /** Regras para correção de horários: fim > início, pausas dentro da jornada, sem sobreposição. */
    fun validate(startedAt: Instant, endedAt: Instant?, pauses: List<JourneyPause>): Validation {
        if (endedAt != null && !endedAt.isAfter(startedAt)) {
            return Validation.Invalid("O horário de término deve ser posterior ao horário de início.")
        }
        val sorted = pauses.sortedBy { it.pausedAt }
        sorted.forEachIndexed { i, p ->
            if (p.pausedAt.isBefore(startedAt)) return Validation.Invalid("O início de uma pausa não pode ser anterior ao início da jornada.")
            if (endedAt != null && p.pausedAt.isAfter(endedAt)) return Validation.Invalid("O início de uma pausa não pode ser posterior ao encerramento da jornada.")
            val r = p.resumedAt
            if (r != null) {
                if (!r.isAfter(p.pausedAt)) return Validation.Invalid("O horário de retorno da pausa deve ser posterior ao início da pausa.")
                if (endedAt != null && r.isAfter(endedAt)) return Validation.Invalid("O retorno de uma pausa não pode ser posterior ao encerramento da jornada.")
            }
            val next = sorted.getOrNull(i + 1)
            if (next != null && (r == null || next.pausedAt.isBefore(r))) {
                return Validation.Invalid("Os intervalos de pausa não podem se sobrepor.")
            }
        }
        return Validation.Valid
    }

    /** Pausa esquecida: pausada há mais de [thresholdMinutes] minutos. */
    fun isProlongedPause(journey: Journey?, now: Instant, thresholdMinutes: Int = 20): Boolean {
        if (journey == null || journey.status != JourneyStatus.PAUSED) return false
        return breakdown(journey, now).currentPauseSeconds / 60 >= thresholdMinutes
    }

    /** Corrige uma pausa esquecida informando quando o motorista realmente voltou. Guarda o horário original. */
    fun correctForgottenPause(journey: Journey, pauseId: String, effectiveResumedAt: Instant, now: Instant, notes: String? = null): Journey {
        if (journey.pauses.none { it.id == pauseId }) return journey
        val pauses = journey.pauses.map {
            if (it.id != pauseId) it
            else it.copy(
                originalResumedAt = it.originalResumedAt ?: it.resumedAt ?: now,
                resumedAt = effectiveResumedAt,
                correctionNotes = notes ?: "Retorno de descanso ajustado retroativamente pelo motorista.",
            )
        }
        return journey.copy(
            pauses = pauses,
            status = if (journey.status == JourneyStatus.PAUSED) JourneyStatus.ACTIVE else journey.status,
        )
    }

    /** Registra (ou substitui) o odômetro na virada da franquia. */
    fun recordCheckpoint(journey: Journey, checkpoint: OdometerCheckpoint): Journey =
        journey.copy(checkpoints = journey.checkpoints.filter { it.at != checkpoint.at } + checkpoint)

    private fun seconds(from: Instant, to: Instant): Long = Duration.between(from, to).seconds.coerceAtLeast(0)
}
