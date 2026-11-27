package br.com.roxou.mobilidade.domain.settings

import br.com.roxou.mobilidade.domain.goals.DailyGoalPlan
import br.com.roxou.mobilidade.domain.goals.DiscountTier
import br.com.roxou.mobilidade.domain.goals.TierTable
import br.com.roxou.mobilidade.domain.model.Platform
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
    const val GOAL_PLATFORMS = "goal_platforms"
    const val CARRY_SHORTFALL = "carry_shortfall"
    const val KM_RESET_DAY = "km_reset_day"
    const val KM_RESET_TIME = "km_reset_time"
    const val RIDE_RESET_DAY = "ride_reset_day"
    const val RIDE_RESET_TIME = "ride_reset_time"
    const val WORKDAY_END = "workday_end_time"

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
        )
    }

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
