package br.com.roxou.mobilidade.data

import br.com.roxou.mobilidade.domain.fuel.FuelLog
import br.com.roxou.mobilidade.domain.km.KmRecord
import br.com.roxou.mobilidade.domain.model.EarningCategory
import br.com.roxou.mobilidade.domain.model.Expense
import br.com.roxou.mobilidade.domain.model.ExpenseCategory
import br.com.roxou.mobilidade.domain.model.ExtraEarning
import br.com.roxou.mobilidade.domain.model.PaymentMethod
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.model.RecordSource
import br.com.roxou.mobilidade.domain.model.Trip
import br.com.roxou.mobilidade.domain.rental.RentalPayment
import br.com.roxou.mobilidade.domain.rental.RentalPaymentSource
import java.time.Instant
import java.time.LocalDate

private inline fun <reified E : Enum<E>> enumOr(raw: String, fallback: E): E =
    runCatching { enumValueOf<E>(raw) }.getOrDefault(fallback)

private fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)

fun TripEntity.toDomain() = Trip(
    id = id,
    platform = enumOr(platform, Platform.OTHER),
    endedAt = endedAt.toInstant(),
    amountCents = amountCents,
    paymentMethod = enumOr(paymentMethod, PaymentMethod.APP),
    pickupKm = pickupKm,
    tripKm = tripKm,
    pickupMinutes = pickupMinutes,
    tripMinutes = tripMinutes,
    source = enumOr(source, RecordSource.MANUAL),
    confirmed = confirmed,
    notes = notes,
)

fun Trip.toEntity(now: Long) = TripEntity(
    id, platform.name, endedAt.toEpochMilli(), amountCents, paymentMethod.name,
    pickupKm, tripKm, pickupMinutes, tripMinutes, source.name, confirmed, notes, now,
)

fun ExtraEarningEntity.toDomain() = ExtraEarning(
    id, enumOr(platform, Platform.OTHER), enumOr(category, EarningCategory.OTHER),
    receivedAt.toInstant(), amountCents, enumOr(paymentMethod, PaymentMethod.APP), notes,
)

fun ExtraEarning.toEntity(now: Long) = ExtraEarningEntity(
    id, platform.name, category.name, receivedAt.toEpochMilli(), amountCents, paymentMethod.name, notes, now,
)

fun ExpenseEntity.toDomain() = Expense(id, enumOr(category, ExpenseCategory.OTHER), paidAt.toInstant(), amountCents, notes)

fun Expense.toEntity(now: Long) = ExpenseEntity(id, category.name, paidAt.toEpochMilli(), amountCents, notes, now)

fun FuelLogEntity.toDomain() = FuelLog(id, vehicleId, filledAt.toInstant(), odometerKm, liters, totalAmountCents, isFullTank, notes)

fun FuelLog.toEntity(now: Long) = FuelLogEntity(
    id, vehicleId, filledAt.toEpochMilli(), odometerKm, liters, totalAmountCents, isFullTank, notes, now,
)

fun RentalPaymentEntity.toDomain() = RentalPayment(
    id, LocalDate.parse(weekId), amountCents, enumOr(source, RentalPaymentSource.MANUAL), paidAt.toInstant(), confirmed, notes,
)

fun RentalPayment.toEntity(now: Long) = RentalPaymentEntity(
    id, weekId.toString(), amountCents, source.name, paidAt.toEpochMilli(), confirmed, notes, now,
)

fun KmRecordEntity.toDomain() = KmRecord(id, at.toInstant(), workKm, privateKm)

fun KmRecord.toEntity(now: Long) = KmRecordEntity(id, at.toEpochMilli(), workKm, privateKm, now)
