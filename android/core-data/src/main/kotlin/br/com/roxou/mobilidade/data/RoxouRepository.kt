package br.com.roxou.mobilidade.data

import android.content.Context
import br.com.roxou.mobilidade.domain.fuel.FuelLog
import br.com.roxou.mobilidade.domain.km.KmRecord
import br.com.roxou.mobilidade.domain.model.Expense
import br.com.roxou.mobilidade.domain.model.ExtraEarning
import br.com.roxou.mobilidade.domain.model.Trip
import br.com.roxou.mobilidade.domain.rental.RentalPayment
import br.com.roxou.mobilidade.domain.summary.AppSettings
import br.com.roxou.mobilidade.domain.summary.Records
import br.com.roxou.mobilidade.domain.summary.Snapshot
import br.com.roxou.mobilidade.domain.summary.SnapshotEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Estado completo da tela: configurações, registros e o resumo calculado. */
data class AppState(val settings: AppSettings, val records: Records, val snapshot: Snapshot)

/**
 * Fonte única de dados do app. Tudo é gravado primeiro no banco local (funciona sem internet);
 * a fila de sincronização com o servidor do Roxou entra na Etapa 5.
 * A leitura de tela das próximas etapas também vai gravar por aqui, nunca direto no financeiro.
 */
class RoxouRepository(
    private val db: RoxouDatabase,
    private val settingsStore: SettingsStore,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun now(): Instant = clock.instant()
    fun newId(): String = UUID.randomUUID().toString()
    private fun stamp(): Long = clock.millis()

    val settings: Flow<AppSettings> = settingsStore.settings

    val records: Flow<Records> = combine(
        combine(db.trips().observeAll(), db.extras().observeAll(), db.expenses().observeAll(), db.fuel().observeAll()) { t, e, x, f ->
            Records(
                trips = t.map { it.toDomain() },
                extras = e.map { it.toDomain() },
                expenses = x.map { it.toDomain() },
                fuelLogs = f.map { it.toDomain() },
            )
        },
        db.rentalPayments().observeAll(),
        db.kmRecords().observeAll(),
        db.uberPanelCounts().observeAll(),
    ) { base, payments, km, panel ->
        base.copy(
            rentalPayments = payments.map { it.toDomain() },
            kmRecords = km.map { it.toDomain() },
            uberPanelOverrides = panel.associate { LocalDate.parse(it.weekId) to it.rides },
        )
    }

    /** Relógio que avança a cada 30 s, para as viradas (00h, 04h) aparecerem sem reabrir o app. */
    private val ticker: Flow<Instant> = flow {
        while (true) {
            emit(now())
            delay(30_000)
        }
    }

    val state: Flow<AppState> = combine(settings, records, ticker) { s, r, now ->
        AppState(s, r, SnapshotEngine.compute(s, r, now))
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsStore.update(transform)

    suspend fun saveTrip(trip: Trip) = db.trips().upsert(trip.toEntity(stamp()))
    suspend fun deleteTrip(id: String) = db.trips().softDelete(id, stamp())
    suspend fun setTripConfirmed(id: String, confirmed: Boolean) = db.trips().setConfirmed(id, confirmed, stamp())

    suspend fun saveExtra(extra: ExtraEarning) = db.extras().upsert(extra.toEntity(stamp()))
    suspend fun deleteExtra(id: String) = db.extras().softDelete(id, stamp())

    suspend fun saveExpense(expense: Expense) = db.expenses().upsert(expense.toEntity(stamp()))
    suspend fun deleteExpense(id: String) = db.expenses().softDelete(id, stamp())

    suspend fun saveFuel(log: FuelLog) = db.fuel().upsert(log.toEntity(stamp()))
    suspend fun deleteFuel(id: String) = db.fuel().softDelete(id, stamp())

    suspend fun saveRentalPayment(p: RentalPayment) = db.rentalPayments().upsert(p.toEntity(stamp()))
    suspend fun setRentalPaymentConfirmed(id: String, confirmed: Boolean) = db.rentalPayments().setConfirmed(id, confirmed, stamp())
    suspend fun deleteRentalPayment(id: String) = db.rentalPayments().softDelete(id, stamp())

    suspend fun saveKm(record: KmRecord) = db.kmRecords().upsert(record.toEntity(stamp()))
    suspend fun deleteKm(id: String) = db.kmRecords().softDelete(id, stamp())

    /** Corrige a contagem da semana pelo painel da Uber. null remove a correção. */
    suspend fun setUberPanelCount(weekId: LocalDate, rides: Int?) {
        if (rides == null) db.uberPanelCounts().clear(weekId.toString())
        else db.uberPanelCounts().upsert(UberPanelCountEntity(weekId.toString(), rides, stamp()))
    }

    companion object {
        fun create(context: Context): RoxouRepository =
            RoxouRepository(RoxouDatabase.create(context), SettingsStore(context))

        @Volatile private var instance: RoxouRepository? = null

        /** Instância única, dividida entre as telas e o serviço da janela flutuante. */
        fun get(context: Context): RoxouRepository =
            instance ?: synchronized(this) { instance ?: create(context.applicationContext).also { instance = it } }
    }
}
