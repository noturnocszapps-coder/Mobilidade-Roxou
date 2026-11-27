package br.com.roxou.mobilidade.domain.time

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** Fuso oficial do app: horário de Brasília. Porte de `src/lib/timezone.ts` do Lucro App. */
val BRASILIA: ZoneId = ZoneId.of("America/Sao_Paulo")

/**
 * Horários de virada. Todos configuráveis; os padrões seguem o documento-mestre:
 * - franquia de km zera na segunda às 00h;
 * - contagem de corridas e parcela da locação seguem o fechamento da Uber, segunda às 04h;
 * - o dia de trabalho termina às 04h (uma corrida de sexta à 1h ainda é sexta).
 */
data class TurnoverConfig(
    val zone: ZoneId = BRASILIA,
    val kmResetDay: DayOfWeek = DayOfWeek.MONDAY,
    val kmResetTime: LocalTime = LocalTime.MIDNIGHT,
    val rideWeekResetDay: DayOfWeek = DayOfWeek.MONDAY,
    val rideWeekResetTime: LocalTime = LocalTime.of(4, 0),
    val workdayEndTime: LocalTime = LocalTime.of(4, 0),
)

/**
 * Uma janela semanal [start, endExclusive). [id] é a data local em que a janela começa
 * e serve de chave estável para gravar dados da semana (ex.: correção pelo painel da Uber).
 */
data class WeekWindow(val start: ZonedDateTime, val endExclusive: ZonedDateTime) {
    val id: LocalDate get() = start.toLocalDate()
    operator fun contains(instant: Instant): Boolean =
        !instant.isBefore(start.toInstant()) && instant.isBefore(endExclusive.toInstant())
    fun previous(): WeekWindow = WeekWindow(start.minusWeeks(1), start)
    fun next(): WeekWindow = WeekWindow(endExclusive, endExclusive.plusWeeks(1))
}

class Turnover(val config: TurnoverConfig = TurnoverConfig()) {

    /** Dia de trabalho ao qual o instante pertence (o dia termina em [TurnoverConfig.workdayEndTime]). */
    fun workday(instant: Instant): LocalDate {
        val local = instant.atZone(config.zone).toLocalDateTime()
        return if (local.toLocalTime().isBefore(config.workdayEndTime)) local.toLocalDate().minusDays(1)
        else local.toLocalDate()
    }

    /** Semana de corridas e parcela (padrão: segunda 04h até a segunda seguinte 04h). */
    fun rideWeek(instant: Instant): WeekWindow =
        weekContaining(instant, config.rideWeekResetDay, config.rideWeekResetTime)

    /** Semana da franquia de km (padrão: segunda 00h até a segunda seguinte 00h). */
    fun kmWeek(instant: Instant): WeekWindow =
        weekContaining(instant, config.kmResetDay, config.kmResetTime)

    /** Semana de corridas identificada pela data de início (ex.: segunda 24/11/2026). */
    fun rideWeekStartingOn(date: LocalDate): WeekWindow {
        val start = ZonedDateTime.of(date, config.rideWeekResetTime, config.zone)
        return WeekWindow(start, start.plusWeeks(1))
    }

    /** Dias de trabalho cobertos por uma semana de corridas, na ordem. */
    fun workdaysOf(week: WeekWindow): List<LocalDate> =
        (0L until 7L).map { workday(week.start.toInstant()).plusDays(it) }

    private fun weekContaining(instant: Instant, day: DayOfWeek, time: LocalTime): WeekWindow {
        val local = instant.atZone(config.zone)
        var startDate = local.toLocalDate().with(TemporalAdjusters.previousOrSame(day))
        var start = ZonedDateTime.of(startDate, time, config.zone)
        if (start.toInstant().isAfter(instant)) {
            startDate = startDate.minusWeeks(1)
            start = ZonedDateTime.of(startDate, time, config.zone)
        }
        val end = ZonedDateTime.of(startDate.plusWeeks(1), time, config.zone)
        return WeekWindow(start, end)
    }
}
