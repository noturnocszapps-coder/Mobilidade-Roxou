package br.com.roxou.mobilidade.domain.goals

import br.com.roxou.mobilidade.domain.model.Cents

/** Uma faixa de desconto: atingindo [minRides] corridas na semana, ganha [discountCents] na semana seguinte. */
data class DiscountTier(val minRides: Int, val discountCents: Cents) {
    init {
        require(minRides > 0) { "minRides deve ser positivo" }
        require(discountCents >= 0) { "discountCents não pode ser negativo" }
    }
}

/** Tabela de faixas da locadora. Sem faixa intermediária: só vale o que está na tabela. */
class TierTable(tiers: List<DiscountTier>) {
    val sorted: List<DiscountTier> = tiers.sortedBy { it.minRides }

    init {
        require(sorted.map { it.minRides }.distinct().size == sorted.size) { "faixas repetidas" }
    }

    override fun equals(other: Any?): Boolean = other is TierTable && other.sorted == sorted
    override fun hashCode(): Int = sorted.hashCode()
    override fun toString(): String = "TierTable($sorted)"

    companion object {
        /** Localiza Zarp (confirmado): 40 = R$ 80, 60 = R$ 160, 80 = R$ 250. */
        val LOCALIZA_ZARP = TierTable(
            listOf(
                DiscountTier(40, 80_00),
                DiscountTier(60, 160_00),
                DiscountTier(80, 250_00),
            ),
        )
        val NONE = TierTable(emptyList())
    }
}

data class TierStep(val tier: DiscountTier, val ridesRemaining: Int)

data class TierProgress(
    val rides: Int,
    /** Faixa garantida com as corridas atuais (null = abaixo da primeira). */
    val achieved: DiscountTier?,
    /** Próxima faixa ainda não alcançada. */
    val next: DiscountTier?,
    /** Todas as faixas acima da atual, com quantas corridas faltam para cada. */
    val upcoming: List<TierStep>,
) {
    val achievedDiscountCents: Cents get() = achieved?.discountCents ?: 0
    val ridesToNext: Int? get() = upcoming.firstOrNull()?.ridesRemaining
}

object TierEngine {
    fun evaluate(rides: Int, table: TierTable): TierProgress {
        val safeRides = rides.coerceAtLeast(0)
        val achieved = table.sorted.lastOrNull { safeRides >= it.minRides }
        val upcoming = table.sorted.filter { safeRides < it.minRides }
            .map { TierStep(it, it.minRides - safeRides) }
        return TierProgress(safeRides, achieved, upcoming.firstOrNull()?.tier, upcoming)
    }

    /** Desconto conquistado numa semana, que abate a parcela da semana SEGUINTE. */
    fun discountEarned(rides: Int, table: TierTable): Cents = evaluate(rides, table).achievedDiscountCents
}

/**
 * Contagem oficial de corridas da semana. A Uber é quem informa à locadora, então o número
 * do painel da Uber (quando informado) substitui a contagem do app.
 */
data class WeeklyRideCount(val countedByApp: Int, val uberPanelOverride: Int? = null) {
    val official: Int get() = uberPanelOverride ?: countedByApp
    val isCorrected: Boolean get() = uberPanelOverride != null && uberPanelOverride != countedByApp
}
