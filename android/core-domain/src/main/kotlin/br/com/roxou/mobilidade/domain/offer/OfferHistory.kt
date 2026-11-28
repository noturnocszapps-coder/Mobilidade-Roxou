package br.com.roxou.mobilidade.domain.offer

import br.com.roxou.mobilidade.domain.model.Cents
import br.com.roxou.mobilidade.domain.model.Platform
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class OfferOutcome { ACCEPTED, REJECTED }

/**
 * Uma oferta que apareceu na tela (aceita ou não). Base do resumo de ofertas e da IA da Etapa 7.
 * Guarda só números e o nome do bairro; nunca o endereço.
 */
data class OfferRecord(
    val id: String,
    val at: Instant,
    val platform: Platform,
    val fareCents: Cents,
    val totalKm: Double,
    val totalMinutes: Int,
    val perKmCents: Cents,
    val rating: Double?,
    val verdict: Verdict,
    val pickupArea: String?,
    val dropoffArea: String?,
    val outcome: OfferOutcome,
) {
    companion object {
        fun from(id: String, at: Instant, e: OfferEvaluation, outcome: OfferOutcome) = OfferRecord(
            id = id,
            at = at,
            platform = e.offer.platform,
            fareCents = e.offer.fareCents,
            totalKm = e.totalKm,
            totalMinutes = e.totalMinutes,
            perKmCents = e.perKmCents,
            rating = e.offer.passengerRating,
            verdict = if (e.blocked) Verdict.BAD else e.verdict,
            pickupArea = e.pickupArea.area,
            dropoffArea = e.dropoffArea.area,
            outcome = outcome,
        )
    }
}

/** Uma oferta por linha, campos separados por "|". Linha repetida com o mesmo id: vale a última. */
object OfferRecordCodec {
    private fun clean(s: String?) = s?.replace('|', ' ')?.replace('\n', ' ')?.trim().orEmpty()

    fun encode(r: OfferRecord): String = listOf(
        r.id, r.at.toEpochMilli().toString(), r.platform.name, r.fareCents.toString(),
        r.totalKm.toString(), r.totalMinutes.toString(), r.perKmCents.toString(),
        r.rating?.toString().orEmpty(), r.verdict.name, clean(r.pickupArea), clean(r.dropoffArea), r.outcome.name,
    ).joinToString("|")

    fun decode(line: String): OfferRecord? = runCatching {
        val f = line.split('|')
        if (f.size < 12) return null
        OfferRecord(
            id = f[0],
            at = Instant.ofEpochMilli(f[1].toLong()),
            platform = Platform.valueOf(f[2]),
            fareCents = f[3].toLong(),
            totalKm = f[4].toDouble(),
            totalMinutes = f[5].toInt(),
            perKmCents = f[6].toLong(),
            rating = f[7].toDoubleOrNull(),
            verdict = Verdict.valueOf(f[8]),
            pickupArea = f[9].ifBlank { null },
            dropoffArea = f[10].ifBlank { null },
            outcome = OfferOutcome.valueOf(f[11]),
        )
    }.getOrNull()

    fun decodeAll(lines: Sequence<String>): List<OfferRecord> {
        val byId = LinkedHashMap<String, OfferRecord>()
        lines.mapNotNull(::decode).forEach { byId[it.id] = it }
        return byId.values.toList()
    }
}

data class OfferStats(
    val total: Int,
    val accepted: Int,
    val days: Int,
    val todayTotal: Int,
    val todayAccepted: Int,
    /** Média de ofertas por dia trabalhado (dia com pelo menos uma oferta). */
    val perDay: Double,
    /** Média de ofertas por dia da semana, só dos dias em que trabalhou. */
    val perWeekday: Map<DayOfWeek, Double>,
    /** Hora do dia (0–23) com mais ofertas, se houver. */
    val busiestHour: Int?,
    val byPlatform: Map<Platform, Int>,
) {
    val acceptRate: Double get() = if (total == 0) 0.0 else accepted.toDouble() / total
    val bestWeekday: DayOfWeek? get() = perWeekday.maxByOrNull { it.value }?.key
}

object OfferStatsEngine {
    fun compute(records: List<OfferRecord>, zone: ZoneId, today: LocalDate): OfferStats {
        val byDay = records.groupBy { it.at.atZone(zone).toLocalDate() }
        val todayList = byDay[today].orEmpty()
        val perWeekday = byDay.entries.groupBy { it.key.dayOfWeek }
            .mapValues { (_, days) -> days.sumOf { it.value.size }.toDouble() / days.size }
        val busiest = records.groupingBy { it.at.atZone(zone).hour }.eachCount().maxByOrNull { it.value }?.key
        return OfferStats(
            total = records.size,
            accepted = records.count { it.outcome == OfferOutcome.ACCEPTED },
            days = byDay.size,
            todayTotal = todayList.size,
            todayAccepted = todayList.count { it.outcome == OfferOutcome.ACCEPTED },
            perDay = if (byDay.isEmpty()) 0.0 else records.size.toDouble() / byDay.size,
            perWeekday = perWeekday,
            busiestHour = busiest,
            byPlatform = records.groupingBy { it.platform }.eachCount(),
        )
    }
}
