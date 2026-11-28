package br.com.roxou.mobilidade.domain.settings

import br.com.roxou.mobilidade.domain.goals.DailyGoalPlan
import br.com.roxou.mobilidade.domain.goals.DiscountTier
import br.com.roxou.mobilidade.domain.goals.TierTable
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.offer.OfferRules
import br.com.roxou.mobilidade.domain.rental.RentalPlan
import br.com.roxou.mobilidade.domain.rental.VehicleOwnership
import br.com.roxou.mobilidade.domain.summary.AppSettings
import br.com.roxou.mobilidade.domain.time.TurnoverConfig
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Converte [AppSettings] em pares chave/texto (para o DataStore) e de volta.
 * Valor ausente ou inválido cai no padrão, para o app nunca quebrar por uma configuração ruim.
 */
object SettingsCodec {
    const val OWNERSHIP = "ownership"
    const val RENTAL_COMPANY = "rental_company"
    const val RENTAL_INSTALLMENT = "rental_installment_cents"
    const val RENTAL_KM = "rental_km_allowance"
    const val RENTAL_EXTRA_KM = "rental_extra_km_cents"
    const val RENTAL_DEPOSIT = "rental_deposit_cents"
    const val RENTAL_TIERS = "rental_tiers"
    const val RENTAL_TIER_PLATFORMS = "rental_tier_platforms"
    const val GOALS = "goals_by_day"
    // v2 (27/11): a 99 passou a contar na meta diária; o valor antigo (só Uber) é ignorado.
    const val GOAL_PLATFORMS = "goal_platforms_v2"
    const val CARRY_SHORTFALL = "carry_shortfall"
    const val KM_RESET_DAY = "km_reset_day"
    const val KM_RESET_TIME = "km_reset_time"
    const val RIDE_RESET_DAY = "ride_reset_day"
    const val RIDE_RESET_TIME = "ride_reset_time"
    const val WORKDAY_END = "workday_end_time"
    const val OFFER_MIN_KM = "offer_min_per_km_cents"
    const val OFFER_GOOD_KM = "offer_good_per_km_cents"
    const val OFFER_MIN_HOUR = "offer_min_per_hour_cents"
    const val OFFER_GOOD_HOUR = "offer_good_per_hour_cents"
    const val OFFER_COST_KM = "offer_cost_per_km_cents"
    const val OFFER_MAX_PICKUP = "offer_max_pickup_km"
    const val OFFER_TIER_HELP = "offer_tier_help_rides"
    const val OFFER_BLOCKED = "offer_blocked_areas"
    const val OFFER_CODES = "offer_area_codes"
    const val OFFER_MIN_RATING = "offer_min_passenger_rating"

    fun encode(s: AppSettings): Map<String, String> = mapOf(
        OWNERSHIP to s.ownership.name,
        RENTAL_COMPANY to s.rental.companyName,
        RENTAL_INSTALLMENT to s.rental.weeklyInstallmentCents.toString(),
        RENTAL_KM to s.rental.weeklyKmAllowance.toString(),
        RENTAL_EXTRA_KM to s.rental.extraKmRateCents.toString(),
        RENTAL_DEPOSIT to s.rental.depositCents.toString(),
        RENTAL_TIERS to encodeTiers(s.rental.tiers),
        RENTAL_TIER_PLATFORMS to s.rental.tierPlatforms.joinToString(",") { it.name },
        GOALS to DayOfWeek.entries.joinToString(",") { "${it.name}:${s.goals.target(it)}" },
        GOAL_PLATFORMS to s.goalPlatforms.joinToString(",") { it.name },
        CARRY_SHORTFALL to s.carryShortfall.toString(),
        KM_RESET_DAY to s.turnover.kmResetDay.name,
        KM_RESET_TIME to s.turnover.kmResetTime.toString(),
        RIDE_RESET_DAY to s.turnover.rideWeekResetDay.name,
        RIDE_RESET_TIME to s.turnover.rideWeekResetTime.toString(),
        WORKDAY_END to s.turnover.workdayEndTime.toString(),
        OFFER_MIN_KM to s.offer.minPerKmCents.toString(),
        OFFER_GOOD_KM to s.offer.goodPerKmCents.toString(),
        OFFER_MIN_HOUR to s.offer.minPerHourCents.toString(),
        OFFER_GOOD_HOUR to s.offer.goodPerHourCents.toString(),
        OFFER_COST_KM to s.offer.costPerKmCents.toString(),
        OFFER_MAX_PICKUP to s.offer.maxPickupKm.toString(),
        OFFER_TIER_HELP to s.offer.tierHelpRides.toString(),
        OFFER_BLOCKED to s.offer.blockedAreas.joinToString(";"),
        OFFER_CODES to encodeCodes(s.offer.areaCodes),
        OFFER_MIN_RATING to s.offer.minPassengerRating.toString(),
    )

    fun decode(m: Map<String, String>): AppSettings {
        val d = AppSettings()
        val rental = RentalPlan(
            companyName = m[RENTAL_COMPANY]?.takeIf { it.isNotBlank() } ?: d.rental.companyName,
            weeklyInstallmentCents = m[RENTAL_INSTALLMENT]?.toLongOrNull()?.takeIf { it >= 0 } ?: d.rental.weeklyInstallmentCents,
            weeklyKmAllowance = m[RENTAL_KM]?.toIntOrNull()?.takeIf { it >= 0 } ?: d.rental.weeklyKmAllowance,
            extraKmRateCents = m[RENTAL_EXTRA_KM]?.toLongOrNull()?.takeIf { it >= 0 } ?: d.rental.extraKmRateCents,
            depositCents = m[RENTAL_DEPOSIT]?.toLongOrNull()?.takeIf { it >= 0 } ?: d.rental.depositCents,
            tiers = m[RENTAL_TIERS]?.let(::decodeTiers) ?: d.rental.tiers,
            tierPlatforms = m[RENTAL_TIER_PLATFORMS]?.let(::decodePlatforms) ?: d.rental.tierPlatforms,
        )
        val turnover = TurnoverConfig(
            kmResetDay = m[KM_RESET_DAY].toDay() ?: d.turnover.kmResetDay,
            kmResetTime = m[KM_RESET_TIME].toTime() ?: d.turnover.kmResetTime,
            rideWeekResetDay = m[RIDE_RESET_DAY].toDay() ?: d.turnover.rideWeekResetDay,
            rideWeekResetTime = m[RIDE_RESET_TIME].toTime() ?: d.turnover.rideWeekResetTime,
            workdayEndTime = m[WORKDAY_END].toTime() ?: d.turnover.workdayEndTime,
        )
        return AppSettings(
            turnover = turnover,
            goals = m[GOALS]?.let(::decodeGoals) ?: d.goals,
            goalPlatforms = m[GOAL_PLATFORMS]?.let(::decodePlatforms) ?: d.goalPlatforms,
            carryShortfall = m[CARRY_SHORTFALL]?.toBooleanStrictOrNull() ?: d.carryShortfall,
            ownership = m[OWNERSHIP]?.let { runCatching { VehicleOwnership.valueOf(it) }.getOrNull() } ?: d.ownership,
            rental = rental,
            offer = OfferRules(
                minPerKmCents = m[OFFER_MIN_KM].cents() ?: d.offer.minPerKmCents,
                goodPerKmCents = m[OFFER_GOOD_KM].cents() ?: d.offer.goodPerKmCents,
                minPerHourCents = m[OFFER_MIN_HOUR].cents() ?: d.offer.minPerHourCents,
                goodPerHourCents = m[OFFER_GOOD_HOUR].cents() ?: d.offer.goodPerHourCents,
                costPerKmCents = m[OFFER_COST_KM].cents() ?: d.offer.costPerKmCents,
                maxPickupKm = m[OFFER_MAX_PICKUP]?.toDoubleOrNull()?.takeIf { it >= 0 } ?: d.offer.maxPickupKm,
                tierHelpRides = m[OFFER_TIER_HELP]?.toIntOrNull()?.takeIf { it >= 0 } ?: d.offer.tierHelpRides,
                blockedAreas = m[OFFER_BLOCKED]?.let(::decodeList) ?: d.offer.blockedAreas,
                areaCodes = m[OFFER_CODES]?.let(::decodeCodes) ?: d.offer.areaCodes,
                minPassengerRating = m[OFFER_MIN_RATING]?.toDoubleOrNull()?.takeIf { it in 0.0..5.0 } ?: d.offer.minPassengerRating,
            ),
        )
    }

    /** "P5S3=Parque dos Pinheiros;P4S2=" (bairro vazio = falta traduzir). */
    fun encodeCodes(c: Map<String, String>): String = c.entries.joinToString(";") { "${it.key.trim()}=${it.value.trim()}" }

    fun decodeCodes(raw: String): Map<String, String> = raw.split(';', '\n').mapNotNull { part ->
        val i = part.indexOf('=')
        if (i <= 0) null else part.substring(0, i).trim().takeIf { it.isNotEmpty() }?.let { it to part.substring(i + 1).trim() }
    }.toMap(LinkedHashMap())

    fun decodeList(raw: String): List<String> = raw.split(';', ',', '\n').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    private fun String?.cents(): Long? = this?.toLongOrNull()?.takeIf { it >= 0 }

    /** "40:8000,60:16000,80:25000" (corridas:centavos). */
    fun encodeTiers(t: TierTable): String = t.sorted.joinToString(",") { "${it.minRides}:${it.discountCents}" }

    fun decodeTiers(raw: String): TierTable? {
        if (raw.isBlank()) return TierTable.NONE
        return runCatching {
            TierTable(
                raw.split(',').map { part ->
                    val (rides, cents) = part.trim().split(':')
                    DiscountTier(rides.trim().toInt(), cents.trim().toLong())
                },
            )
        }.getOrNull()
    }

    private fun decodeGoals(raw: String): DailyGoalPlan? = runCatching {
        DailyGoalPlan(
            raw.split(',').associate { part ->
                val (day, n) = part.split(':')
                DayOfWeek.valueOf(day.trim()) to n.trim().toInt()
            },
        )
    }.getOrNull()

    private fun decodePlatforms(raw: String): Set<Platform>? =
        raw.split(',').filter { it.isNotBlank() }
            .mapNotNull { runCatching { Platform.valueOf(it.trim()) }.getOrNull() }
            .toSet().takeIf { it.isNotEmpty() }

    private fun String?.toDay(): DayOfWeek? = this?.let { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
    private fun String?.toTime(): LocalTime? = this?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
}
