package br.com.roxou.mobilidade.domain.model

import java.time.Instant

/** Valores em centavos de real, para nunca perder precisão nem contar dinheiro duas vezes. */
typealias Cents = Long

enum class Platform { UBER, NINETY_NINE, PRIVATE, OTHER }

enum class PaymentMethod { APP, CASH }

/** Origem do registro. Correção manual tem prioridade sobre leitura automática na sincronização. */
enum class RecordSource { MANUAL, NOTIFICATION, SCREEN_READER }

/**
 * Categorias de ganho separadas, como pede o documento-mestre.
 * Ex.: R$ 750 de indicação da 99 entram em [REFERRAL], não em receita de corrida.
 */
enum class EarningCategory(val isRideRevenue: Boolean) {
    UBER_RIDE(true),
    NINETY_NINE_RIDE(true),
    INCENTIVE(false),
    REFERRAL(false),
    TIP(false),
    ADJUSTMENT(false),
    OTHER(false),
}

/** Uma corrida individual. Lançamento manual hoje; leitura de tela nas próximas etapas. */
data class Trip(
    val id: String,
    val platform: Platform,
    val endedAt: Instant,
    val amountCents: Cents,
    val paymentMethod: PaymentMethod = PaymentMethod.APP,
    val pickupKm: Double? = null,
    val tripKm: Double? = null,
    val pickupMinutes: Int? = null,
    val tripMinutes: Int? = null,
    val source: RecordSource = RecordSource.MANUAL,
    /** Só corridas confirmadas contam para a meta (sem evidência de conclusão, pede confirmação). */
    val confirmed: Boolean = true,
    val notes: String? = null,
) {
    val totalKm: Double? get() = if (pickupKm == null && tripKm == null) null else (pickupKm ?: 0.0) + (tripKm ?: 0.0)
    val category: EarningCategory
        get() = when (platform) {
            Platform.UBER -> EarningCategory.UBER_RIDE
            Platform.NINETY_NINE -> EarningCategory.NINETY_NINE_RIDE
            else -> EarningCategory.OTHER
        }
}

/** Ganho que não é corrida: incentivo, indicação, gorjeta, ajuste, outros. */
data class ExtraEarning(
    val id: String,
    val platform: Platform,
    val category: EarningCategory,
    val receivedAt: Instant,
    val amountCents: Cents,
    val paymentMethod: PaymentMethod = PaymentMethod.APP,
    val notes: String? = null,
)

/** Combustível fica em [br.com.roxou.mobilidade.domain.fuel.FuelLog], nunca aqui, para não somar duas vezes. */
enum class ExpenseCategory { MAINTENANCE, FOOD, CAR_WASH, TOLL_PARKING, OTHER }

data class Expense(
    val id: String,
    val category: ExpenseCategory,
    val paidAt: Instant,
    val amountCents: Cents,
    val notes: String? = null,
)
