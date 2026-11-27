package br.com.roxou.mobilidade.domain.trips

import br.com.roxou.mobilidade.domain.model.Cents
import br.com.roxou.mobilidade.domain.model.PaymentMethod
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.model.RecordSource
import br.com.roxou.mobilidade.domain.model.Trip
import br.com.roxou.mobilidade.domain.time.Turnover
import br.com.roxou.mobilidade.domain.time.WeekWindow
import java.time.Instant
import java.time.LocalDate

/** Campos do formulário de lançamento manual, ainda como texto digitado. */
data class ManualTripForm(
    val platform: Platform,
    val amount: String,
    val endedAt: Instant,
    val paymentMethod: PaymentMethod = PaymentMethod.APP,
    val pickupKm: String = "",
    val tripKm: String = "",
    val pickupMinutes: String = "",
    val tripMinutes: String = "",
    val notes: String = "",
)

sealed interface ManualTripResult {
    data class Ok(val trip: Trip) : ManualTripResult
    data class Error(val field: String, val message: String) : ManualTripResult
}

object ManualTripEntry {
    /**
     * Valida e converte o formulário. Aceita "12,50", "12.50" e "R$ 1.234,56".
     * [newId] vem de fora para cada registro nascer com um identificador único gerado no celular.
     */
    fun build(form: ManualTripForm, newId: String, now: Instant): ManualTripResult {
        val cents = parseMoneyToCents(form.amount)
            ?: return ManualTripResult.Error("amount", "Informe o valor da corrida.")
        if (cents <= 0) return ManualTripResult.Error("amount", "O valor deve ser maior que zero.")
        if (form.endedAt.isAfter(now.plusSeconds(300))) {
            return ManualTripResult.Error("endedAt", "A corrida não pode terminar no futuro.")
        }
        val pickupKm = parseOptionalDecimal(form.pickupKm) ?: return ManualTripResult.Error("pickupKm", "Km até o passageiro inválido.")
        val tripKm = parseOptionalDecimal(form.tripKm) ?: return ManualTripResult.Error("tripKm", "Km da viagem inválido.")
        val pickupMin = parseOptionalInt(form.pickupMinutes) ?: return ManualTripResult.Error("pickupMinutes", "Tempo até o passageiro inválido.")
        val tripMin = parseOptionalInt(form.tripMinutes) ?: return ManualTripResult.Error("tripMinutes", "Tempo da viagem inválido.")
        return ManualTripResult.Ok(
            Trip(
                id = newId,
                platform = form.platform,
                endedAt = form.endedAt,
                amountCents = cents,
                paymentMethod = form.paymentMethod,
                pickupKm = pickupKm.value,
                tripKm = tripKm.value,
                pickupMinutes = pickupMin.value,
                tripMinutes = tripMin.value,
                source = RecordSource.MANUAL,
                confirmed = true,
                notes = form.notes.trim().ifEmpty { null },
            ),
        )
    }

    /** Wrapper para distinguir "vazio" (válido, null) de "inválido" (retorno null da função). */
    data class Optional<T>(val value: T?)

    fun parseMoneyToCents(raw: String): Cents? {
        val cleaned = raw.replace("R$", "").replace(" ", "").trim()
        if (cleaned.isEmpty()) return null
        val normalized = when {
            cleaned.contains(',') -> cleaned.replace(".", "").replace(',', '.')
            cleaned.count { it == '.' } > 1 -> cleaned.replace(".", "")
            else -> cleaned
        }
        val value = normalized.toBigDecimalOrNull() ?: return null
        return value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
    }

    private fun parseOptionalDecimal(raw: String): Optional<Double>? {
        val t = raw.trim()
        if (t.isEmpty()) return Optional(null)
        val v = t.replace(',', '.').toDoubleOrNull() ?: return null
        return if (v < 0) null else Optional(v)
    }

    private fun parseOptionalInt(raw: String): Optional<Int>? {
        val t = raw.trim()
        if (t.isEmpty()) return Optional(null)
        val v = t.toIntOrNull() ?: return null
        return if (v < 0) null else Optional(v)
    }
}

object RideCounter {
    /** Só corridas confirmadas, das plataformas indicadas, dentro da janela. */
    fun countInWeek(trips: List<Trip>, week: WeekWindow, platforms: Set<Platform>): Int =
        trips.count { it.confirmed && it.platform in platforms && it.endedAt in week }

    fun countByWorkday(trips: List<Trip>, turnover: Turnover, platforms: Set<Platform>): Map<LocalDate, Int> =
        trips.filter { it.confirmed && it.platform in platforms }
            .groupingBy { turnover.workday(it.endedAt) }
            .eachCount()
}
