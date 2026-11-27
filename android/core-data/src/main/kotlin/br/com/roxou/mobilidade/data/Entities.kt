package br.com.roxou.mobilidade.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Todas as tabelas seguem o mesmo padrão, pensado para a sincronização da Etapa 5:
 * - id: UUID gerado no celular (não duplica ao sincronizar);
 * - updatedAt: epoch em ms da última alteração (vale a mais recente em conflito);
 * - deleted: exclusão lógica, para a exclusão também ser sincronizada;
 * - source: MANUAL tem prioridade sobre leituras automáticas.
 * Instantes são epoch em ms (UTC); enums e datas são texto.
 */

@Entity(tableName = "trips", indices = [Index("endedAt")])
data class TripEntity(
    @PrimaryKey val id: String,
    val platform: String,
    val endedAt: Long,
    val amountCents: Long,
    val paymentMethod: String,
    val pickupKm: Double?,
    val tripKm: Double?,
    val pickupMinutes: Int?,
    val tripMinutes: Int?,
    val source: String,
    val confirmed: Boolean,
    val notes: String?,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Entity(tableName = "extra_earnings", indices = [Index("receivedAt")])
data class ExtraEarningEntity(
    @PrimaryKey val id: String,
    val platform: String,
    val category: String,
    val receivedAt: Long,
    val amountCents: Long,
    val paymentMethod: String,
    val notes: String?,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Entity(tableName = "expenses", indices = [Index("paidAt")])
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val category: String,
    val paidAt: Long,
    val amountCents: Long,
    val notes: String?,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Entity(tableName = "fuel_logs", indices = [Index("filledAt")])
data class FuelLogEntity(
    @PrimaryKey val id: String,
    val vehicleId: String?,
    val filledAt: Long,
    val odometerKm: Double,
    val liters: Double,
    val totalAmountCents: Long,
    val isFullTank: Boolean,
    val notes: String?,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Entity(tableName = "rental_payments", indices = [Index("weekId")])
data class RentalPaymentEntity(
    @PrimaryKey val id: String,
    /** Data ISO (yyyy-MM-dd) do início da semana de corridas. */
    val weekId: String,
    val amountCents: Long,
    val source: String,
    val paidAt: Long,
    val confirmed: Boolean,
    val notes: String?,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Entity(tableName = "km_records", indices = [Index("at")])
data class KmRecordEntity(
    @PrimaryKey val id: String,
    val at: Long,
    val workKm: Double,
    val privateKm: Double,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

/** Total de corridas que o painel da Uber mostra para a semana (corrige a contagem do app). */
@Entity(tableName = "uber_panel_counts")
data class UberPanelCountEntity(
    @PrimaryKey val weekId: String,
    val rides: Int,
    val updatedAt: Long,
)
