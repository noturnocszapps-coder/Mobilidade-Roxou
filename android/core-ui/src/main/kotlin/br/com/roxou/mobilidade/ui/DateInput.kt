package br.com.roxou.mobilidade.ui

import br.com.roxou.mobilidade.domain.time.BRASILIA
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.format.DateTimeFormatter

private val dm = DateTimeFormatter.ofPattern("dd/MM")
private val hm = DateTimeFormatter.ofPattern("HH:mm")

fun defaultDateText(now: Instant): String = dm.format(now.atZone(BRASILIA))
fun defaultTimeText(now: Instant): String = hm.format(now.atZone(BRASILIA))

/**
 * "27/11" + "01:30" em horário de Brasília. Usa o ano atual; se a data ficar mais de um dia
 * no futuro (ex.: lançar em janeiro uma corrida de 31/12), volta um ano. null se inválido.
 */
fun parseBrasiliaDateTime(dateText: String, timeText: String, now: Instant): Instant? {
    val md = runCatching { MonthDay.parse(dateText.trim(), dm) }.getOrNull() ?: return null
    val time = runCatching { LocalTime.parse(timeText.trim(), hm) }.getOrNull() ?: return null
    val nowLocal = now.atZone(BRASILIA)
    var candidate = LocalDateTime.of(md.atYear(nowLocal.year), time).atZone(BRASILIA)
    if (candidate.isAfter(nowLocal.plusDays(1))) candidate = candidate.minusYears(1)
    return candidate.toInstant()
}
