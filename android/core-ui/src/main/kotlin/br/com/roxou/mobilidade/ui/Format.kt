package br.com.roxou.mobilidade.ui

import br.com.roxou.mobilidade.domain.model.EarningCategory
import br.com.roxou.mobilidade.domain.model.ExpenseCategory
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.time.BRASILIA
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ptBR: Locale = Locale.forLanguageTag("pt-BR")

/** 123456 -> "R$ 1.234,56". */
fun formatBrl(cents: Long): String = NumberFormat.getCurrencyInstance(ptBR).format(cents / 100.0)

fun formatKm(km: Double): String = String.format(ptBR, "%.1f km", km)

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", ptBR)
private val dayMonthFmt = DateTimeFormatter.ofPattern("dd/MM", ptBR)
private val dateTimeFmt = DateTimeFormatter.ofPattern("dd/MM HH:mm", ptBR)

fun formatTime(i: Instant): String = timeFmt.format(i.atZone(BRASILIA))
fun formatDateTime(i: Instant): String = dateTimeFmt.format(i.atZone(BRASILIA))
fun formatDayMonth(d: LocalDate): String = dayMonthFmt.format(d)

fun DayOfWeek.shortPt(): String = when (this) {
    DayOfWeek.MONDAY -> "seg"
    DayOfWeek.TUESDAY -> "ter"
    DayOfWeek.WEDNESDAY -> "qua"
    DayOfWeek.THURSDAY -> "qui"
    DayOfWeek.FRIDAY -> "sex"
    DayOfWeek.SATURDAY -> "sáb"
    DayOfWeek.SUNDAY -> "dom"
}

fun Platform.label(): String = when (this) {
    Platform.UBER -> "Uber"
    Platform.NINETY_NINE -> "99"
    Platform.PRIVATE -> "Particular"
    Platform.OTHER -> "Outros"
}

fun EarningCategory.label(): String = when (this) {
    EarningCategory.UBER_RIDE -> "Corridas Uber"
    EarningCategory.NINETY_NINE_RIDE -> "Corridas 99"
    EarningCategory.INCENTIVE -> "Incentivo"
    EarningCategory.REFERRAL -> "Indicação"
    EarningCategory.TIP -> "Gorjeta"
    EarningCategory.ADJUSTMENT -> "Ajuste"
    EarningCategory.OTHER -> "Outros"
}

fun ExpenseCategory.label(): String = when (this) {
    ExpenseCategory.MAINTENANCE -> "Manutenção"
    ExpenseCategory.FOOD -> "Alimentação"
    ExpenseCategory.CAR_WASH -> "Lavagem"
    ExpenseCategory.TOLL_PARKING -> "Pedágio/estacionamento"
    ExpenseCategory.OTHER -> "Outros"
}
