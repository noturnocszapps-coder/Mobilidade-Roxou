package br.com.roxou.mobilidade.domain.rental

import br.com.roxou.mobilidade.domain.goals.TierEngine
import br.com.roxou.mobilidade.domain.goals.TierTable
import br.com.roxou.mobilidade.domain.model.Cents
import br.com.roxou.mobilidade.domain.model.Platform
import java.time.Instant
import java.time.LocalDate

enum class VehicleOwnership { RENTED, OWN }

/**
 * Plano da locadora. Nada fica fixo no código: estes são só os valores de referência
 * do documento-mestre (Localiza: ~R$ 1.149/semana, 1.500 km; Movida Grupo FX: R$ 878,98, 1.167 km, R$ 0,71/km).
 */
data class RentalPlan(
    val companyName: String,
    val weeklyInstallmentCents: Cents,
    val weeklyKmAllowance: Int,
    val extraKmRateCents: Cents,
    val depositCents: Cents = 0,
    val tiers: TierTable = TierTable.NONE,
    /** Plataformas cujas corridas contam para as faixas (Localiza: só Uber). */
    val tierPlatforms: Set<Platform> = setOf(Platform.UBER),
) {
    companion object {
        val LOCALIZA_ZARP = RentalPlan(
            companyName = "Localiza Zarp",
            weeklyInstallmentCents = 1_149_00,
            weeklyKmAllowance = 1500,
            extraKmRateCents = 0,
            tiers = TierTable.LOCALIZA_ZARP,
        )
        val MOVIDA_FX = RentalPlan(
            companyName = "Movida Grupo FX",
            weeklyInstallmentCents = 878_98,
            weeklyKmAllowance = 1167,
            extraKmRateCents = 71,
            depositCents = 400_00,
        )
    }
}

enum class RentalPaymentSource {
    /** A Uber desconta a parcela direto dos ganhos no começo do ciclo. */
    UBER_WITHHOLDING,
    /** Pagamento feito pelo motorista (Pix, boleto, dinheiro). */
    MANUAL,
}

/**
 * Um abatimento da parcela. Só conta como pago quando [confirmed] (lançamento ou registro manual).
 * [weekId] é a data de início da semana de corridas a que a parcela pertence.
 */
data class RentalPayment(
    val id: String,
    val weekId: LocalDate,
    val amountCents: Cents,
    val source: RentalPaymentSource,
    val paidAt: Instant,
    val confirmed: Boolean,
    val notes: String? = null,
)

data class InstallmentStatus(
    val weekId: LocalDate,
    val baseInstallmentCents: Cents,
    /** Desconto conquistado na semana ANTERIOR, aplicado nesta semana. */
    val discountAppliedCents: Cents,
    /** Desconto que a semana atual já garante para a semana seguinte (nunca é caixa). */
    val discountSecuredForNextWeekCents: Cents,
    val confirmedPaidCents: Cents,
    val unconfirmedCents: Cents,
) {
    val dueCents: Cents get() = (baseInstallmentCents - discountAppliedCents).coerceAtLeast(0)
    val pendingCents: Cents get() = (dueCents - confirmedPaidCents).coerceAtLeast(0)
    val isPaid: Boolean get() = pendingCents == 0L
    /** Valor pago acima do devido (ex.: Uber descontou a parcela cheia sem considerar o desconto). */
    val overpaidCents: Cents get() = (confirmedPaidCents - dueCents).coerceAtLeast(0)
}

object RentalEngine {
    /**
     * @param ridesPreviousWeek contagem oficial (Uber) da semana anterior: define o desconto desta semana.
     * @param ridesThisWeek contagem oficial desta semana: define o desconto da semana seguinte.
     */
    fun installment(
        plan: RentalPlan,
        weekId: LocalDate,
        ridesPreviousWeek: Int,
        ridesThisWeek: Int,
        payments: List<RentalPayment>,
    ): InstallmentStatus {
        val weekPayments = payments.filter { it.weekId == weekId }
        return InstallmentStatus(
            weekId = weekId,
            baseInstallmentCents = plan.weeklyInstallmentCents,
            discountAppliedCents = TierEngine.discountEarned(ridesPreviousWeek, plan.tiers)
                .coerceAtMost(plan.weeklyInstallmentCents),
            discountSecuredForNextWeekCents = TierEngine.discountEarned(ridesThisWeek, plan.tiers),
            confirmedPaidCents = weekPayments.filter { it.confirmed }.sumOf { it.amountCents },
            unconfirmedCents = weekPayments.filter { !it.confirmed }.sumOf { it.amountCents },
        )
    }
}
