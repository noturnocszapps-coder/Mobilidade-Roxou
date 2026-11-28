package br.com.roxou.mobilidade.domain.summary

import br.com.roxou.mobilidade.domain.finance.DailyCascade
import br.com.roxou.mobilidade.domain.finance.DailyCascadeEngine
import br.com.roxou.mobilidade.domain.finance.FinanceEngine
import br.com.roxou.mobilidade.domain.finance.FinanceSummary
import br.com.roxou.mobilidade.domain.fuel.FuelLog
import br.com.roxou.mobilidade.domain.goals.DailyGoalPlan
import br.com.roxou.mobilidade.domain.goals.GoalRedistribution
import br.com.roxou.mobilidade.domain.goals.TierEngine
import br.com.roxou.mobilidade.domain.goals.TierProgress
import br.com.roxou.mobilidade.domain.goals.WeeklyGoalStatus
import br.com.roxou.mobilidade.domain.goals.WeeklyRideCount
import br.com.roxou.mobilidade.domain.journey.Journey
import br.com.roxou.mobilidade.domain.km.KmAllowanceEngine
import br.com.roxou.mobilidade.domain.km.KmAllowanceProgress
import br.com.roxou.mobilidade.domain.km.KmRecord
import br.com.roxou.mobilidade.domain.model.Expense
import br.com.roxou.mobilidade.domain.model.ExtraEarning
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.model.Trip
import br.com.roxou.mobilidade.domain.rental.InstallmentStatus
import br.com.roxou.mobilidade.domain.rental.RentalEngine
import br.com.roxou.mobilidade.domain.rental.RentalPayment
import br.com.roxou.mobilidade.domain.rental.RentalPlan
import br.com.roxou.mobilidade.domain.rental.VehicleOwnership
import br.com.roxou.mobilidade.domain.time.Turnover
import br.com.roxou.mobilidade.domain.time.TurnoverConfig
import br.com.roxou.mobilidade.domain.time.WeekWindow
import java.time.Instant
import java.time.LocalDate

/** Todas as configurações que mudam os cálculos. Nenhum valor de negócio fica fixo no código. */
data class AppSettings(
    val turnover: TurnoverConfig = TurnoverConfig(),
    val goals: DailyGoalPlan = DailyGoalPlan.DEFAULT,
    /** Meta diária: Uber e 99. (A meta da locadora usa só rental.tierPlatforms, que é a Uber.) */
    val goalPlatforms: Set<Platform> = setOf(Platform.UBER, Platform.NINETY_NINE),
    val carryShortfall: Boolean = false,
    val ownership: VehicleOwnership = VehicleOwnership.RENTED,
    val rental: RentalPlan = RentalPlan.LOCALIZA_ZARP,
    val offer: br.com.roxou.mobilidade.domain.offer.OfferRules = br.com.roxou.mobilidade.domain.offer.OfferRules(),
)

data class Records(
    val trips: List<Trip> = emptyList(),
    val extras: List<ExtraEarning> = emptyList(),
    val rentalPayments: List<RentalPayment> = emptyList(),
    val fuelLogs: List<FuelLog> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val kmRecords: List<KmRecord> = emptyList(),
    val journeys: List<Journey> = emptyList(),
    /** Total de corridas do painel da Uber por semana (chave = início da semana de corridas). */
    val uberPanelOverrides: Map<LocalDate, Int> = emptyMap(),
)

/** Tudo o que o painel do turno precisa, calculado de uma vez a partir dos registros locais. */
data class Snapshot(
    val now: Instant,
    val workday: LocalDate,
    val rideWeek: WeekWindow,
    val kmWeek: WeekWindow,
    val tripsToday: List<Trip>,
    val tripsTodayByPlatform: Map<Platform, Int>,
    val rideCount: WeeklyRideCount,
    val tiers: TierProgress?,
    val goals: WeeklyGoalStatus,
    val installment: InstallmentStatus?,
    val financeWeek: FinanceSummary,
    val financeToday: FinanceSummary,
    val cascadeToday: DailyCascade,
    val km: KmAllowanceProgress?,
)

object SnapshotEngine {
    fun compute(settings: AppSettings, records: Records, now: Instant): Snapshot {
        val turnover = Turnover(settings.turnover)
        val week = turnover.rideWeek(now)
        val prevWeek = week.previous()
        val kmWeek = turnover.kmWeek(now)
        val today = turnover.workday(now)
        val rented = settings.ownership == VehicleOwnership.RENTED
        val plan = settings.rental

        fun officialCount(w: WeekWindow): WeeklyRideCount = WeeklyRideCount(
            countedByApp = records.trips.count { it.confirmed && it.platform in plan.tierPlatforms && it.endedAt in w },
            uberPanelOverride = records.uberPanelOverrides[w.id],
        )

        val count = officialCount(week)
        val prevCount = officialCount(prevWeek)

        val weekTrips = records.trips.filter { it.endedAt in week }
        val tripsToday = weekTrips.filter { turnover.workday(it.endedAt) == today }
        val ridesByWorkday = weekTrips.filter { it.confirmed && it.platform in settings.goalPlatforms }
            .groupingBy { turnover.workday(it.endedAt) }.eachCount()
        val goals = GoalRedistribution.compute(
            plan = settings.goals,
            workdays = turnover.workdaysOf(week),
            ridesByWorkday = ridesByWorkday,
            today = today,
            carryShortfall = settings.carryShortfall,
        )

        val installment = if (rented) {
            RentalEngine.installment(plan, week.id, prevCount.official, count.official, records.rentalPayments)
        } else null

        val weekExtras = records.extras.filter { it.receivedAt in week }
        val weekPayments = records.rentalPayments.filter { it.weekId == week.id }
        val weekFuel = records.fuelLogs.filter { it.filledAt in week }
        val weekExpenses = records.expenses.filter { it.paidAt in week }
        val financeWeek = FinanceEngine.summarize(weekTrips, weekExtras, weekPayments, weekFuel, weekExpenses)

        fun isToday(i: Instant) = i in week && turnover.workday(i) == today
        val financeToday = FinanceEngine.summarize(
            tripsToday,
            weekExtras.filter { isToday(it.receivedAt) },
            emptyList(),
            weekFuel.filter { isToday(it.filledAt) },
            weekExpenses.filter { isToday(it.paidAt) },
        )
        val remainingDays = turnover.workdaysOf(week).count { !it.isBefore(today) }
        val rentalDaily = installment?.let { DailyCascadeEngine.rentalDailyTarget(it.pendingCents, remainingDays) } ?: 0
        val cascade = DailyCascadeEngine.compute(financeToday.grossRevenueCents, financeToday.fuelCents, rentalDaily)

        val km = if (rented && plan.weeklyKmAllowance > 0) {
            KmAllowanceEngine.progress(kmWeek, plan.weeklyKmAllowance, plan.extraKmRateCents, records.kmRecords, records.journeys)
        } else null

        return Snapshot(
            now = now,
            workday = today,
            rideWeek = week,
            kmWeek = kmWeek,
            tripsToday = tripsToday,
            tripsTodayByPlatform = tripsToday.filter { it.confirmed }.groupingBy { it.platform }.eachCount(),
            rideCount = count,
            tiers = if (rented) TierEngine.evaluate(count.official, plan.tiers) else null,
            goals = goals,
            installment = installment,
            financeWeek = financeWeek,
            financeToday = financeToday,
            cascadeToday = cascade,
            km = km,
        )
    }
}
