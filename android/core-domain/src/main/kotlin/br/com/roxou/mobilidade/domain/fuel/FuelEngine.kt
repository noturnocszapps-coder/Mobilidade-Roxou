package br.com.roxou.mobilidade.domain.fuel

import br.com.roxou.mobilidade.domain.model.Cents
import java.time.Instant

data class FuelLog(
    val id: String,
    val vehicleId: String? = null,
    val filledAt: Instant,
    val odometerKm: Double,
    val liters: Double,
    val totalAmountCents: Cents,
    val isFullTank: Boolean,
    val notes: String? = null,
)

data class ChargingLog(
    val id: String,
    val chargedAt: Instant,
    val odometerKm: Double,
    val energyKwh: Double,
    val costCents: Cents,
)

data class FullToFullCycle(
    val startOdometer: Double,
    val endOdometer: Double,
    val distance: Double,
    val liters: Double,
    val kmPerL: Double,
    val startAt: Instant,
    val endAt: Instant,
)

data class FullToFullResult(
    val hasValidMeasurements: Boolean,
    val recentKmPerL: Double? = null,
    val historicalKmPerL: Double? = null,
    val totalMeasuredKm: Double = 0.0,
    val totalMeasuredLiters: Double = 0.0,
    val cycles: List<FullToFullCycle> = emptyList(),
) {
    val measuredWindowsCount: Int get() = cycles.size
}

/** Porte de `src/lib/fuelEngine.ts` do Lucro App (núcleo de consumo e preço médio). */
object FuelEngine {
    private const val MIN_PLAUSIBLE_KM_PER_L = 2.0
    private const val MAX_PLAUSIBLE_KM_PER_L = 50.0

    /**
     * Consumo real pelo método tanque cheio a tanque cheio. Abastecimentos parciais entre dois
     * cheios entram na soma de litros. Ciclos com odômetro regressivo, distância <= 0 ou consumo
     * fisicamente impossível (< 2 ou > 50 km/l) são ignorados. A média recente usa os últimos 3 a 5 ciclos.
     */
    fun fullToFullConsumption(logs: List<FuelLog>, vehicleId: String? = null): FullToFullResult {
        val target = if (vehicleId != null) logs.filter { it.vehicleId == vehicleId } else logs
        val valid = target.filter { it.odometerKm > 0 && it.liters > 0 }
            .sortedWith(compareBy<FuelLog> { it.filledAt }.thenBy { it.odometerKm })
        if (valid.size < 2) return FullToFullResult(false)

        val fullIndices = valid.indices.filter { valid[it].isFullTank }
        if (fullIndices.size < 2) return FullToFullResult(false)

        val cycles = mutableListOf<FullToFullCycle>()
        for (w in 1 until fullIndices.size) {
            val prevIdx = fullIndices[w - 1]
            val currIdx = fullIndices[w]
            val prev = valid[prevIdx]
            val curr = valid[currIdx]
            val distance = curr.odometerKm - prev.odometerKm
            if (distance <= 0) continue

            var liters = 0.0
            var regressive = false
            var lastOdo = prev.odometerKm
            for (i in prevIdx + 1..currIdx) {
                val step = valid[i]
                if (step.odometerKm < lastOdo) { regressive = true; break }
                lastOdo = step.odometerKm
                liters += step.liters
            }
            if (regressive || liters <= 0) continue
            val kmPerL = distance / liters
            if (kmPerL < MIN_PLAUSIBLE_KM_PER_L || kmPerL > MAX_PLAUSIBLE_KM_PER_L) continue
            cycles += FullToFullCycle(prev.odometerKm, curr.odometerKm, distance, liters, kmPerL, prev.filledAt, curr.filledAt)
        }
        if (cycles.isEmpty()) return FullToFullResult(false)

        val totalKm = cycles.sumOf { it.distance }
        val totalLiters = cycles.sumOf { it.liters }
        val historical = if (totalKm > 0 && totalLiters > 0) totalKm / totalLiters else null
        val windowSize = minOf(5, maxOf(3, cycles.size))
        val recent = cycles.takeLast(windowSize)
        val recentKm = recent.sumOf { it.distance }
        val recentLiters = recent.sumOf { it.liters }
        val recentKmPerL = if (recentKm > 0 && recentLiters > 0) recentKm / recentLiters else historical
        return FullToFullResult(true, recentKmPerL, historical, totalKm, totalLiters, cycles)
    }

    /** Preço médio ponderado do litro, em centavos. */
    fun weightedAveragePricePerLiterCents(logs: List<FuelLog>): Double {
        val valid = logs.filter { it.liters > 0 && it.totalAmountCents > 0 }
        val liters = valid.sumOf { it.liters }
        return if (liters > 0) valid.sumOf { it.totalAmountCents }.toDouble() / liters else 0.0
    }

    /** Custo de combustível por km, em centavos (null sem medição real). */
    fun fuelCostPerKmCents(logs: List<FuelLog>, vehicleId: String? = null): Double? {
        val result = fullToFullConsumption(logs, vehicleId)
        val kmPerL = result.recentKmPerL ?: result.historicalKmPerL ?: return null
        val price = weightedAveragePricePerLiterCents(logs)
        return if (kmPerL > 0 && price > 0) price / kmPerL else null
    }

    data class EvConsumption(val kwhPer100Km: Double, val kmPerKwh: Double, val totalKm: Double, val totalKwh: Double)

    /** Consumo de carro elétrico: energia adicionada depois do primeiro ponto, sobre a distância total. */
    fun evConsumption(logs: List<ChargingLog>): EvConsumption? {
        val sorted = logs.filter { it.odometerKm > 0 && it.energyKwh > 0 }.sortedBy { it.odometerKm }
        if (sorted.size < 2) return null
        val totalKm = sorted.last().odometerKm - sorted.first().odometerKm
        val totalKwh = sorted.drop(1).sumOf { it.energyKwh }
        if (totalKm <= 0 || totalKwh <= 0) return null
        return EvConsumption(totalKwh / totalKm * 100, totalKm / totalKwh, totalKm, totalKwh)
    }
}
