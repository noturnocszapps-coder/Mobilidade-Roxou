package br.com.roxou.mobilidade.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips WHERE deleted = 0 ORDER BY endedAt DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun get(id: String): TripEntity?

    @Upsert suspend fun upsert(entity: TripEntity)

    @Query("UPDATE trips SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE trips SET confirmed = :confirmed, updatedAt = :now WHERE id = :id")
    suspend fun setConfirmed(id: String, confirmed: Boolean, now: Long)
}

@Dao
interface ExtraEarningDao {
    @Query("SELECT * FROM extra_earnings WHERE deleted = 0 ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<ExtraEarningEntity>>

    @Upsert suspend fun upsert(entity: ExtraEarningEntity)

    @Query("UPDATE extra_earnings SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE deleted = 0 ORDER BY paidAt DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Upsert suspend fun upsert(entity: ExpenseEntity)

    @Query("UPDATE expenses SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface FuelLogDao {
    @Query("SELECT * FROM fuel_logs WHERE deleted = 0 ORDER BY filledAt DESC")
    fun observeAll(): Flow<List<FuelLogEntity>>

    @Upsert suspend fun upsert(entity: FuelLogEntity)

    @Query("UPDATE fuel_logs SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface RentalPaymentDao {
    @Query("SELECT * FROM rental_payments WHERE deleted = 0 ORDER BY paidAt DESC")
    fun observeAll(): Flow<List<RentalPaymentEntity>>

    @Upsert suspend fun upsert(entity: RentalPaymentEntity)

    @Query("UPDATE rental_payments SET confirmed = :confirmed, updatedAt = :now WHERE id = :id")
    suspend fun setConfirmed(id: String, confirmed: Boolean, now: Long)

    @Query("UPDATE rental_payments SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface KmRecordDao {
    @Query("SELECT * FROM km_records WHERE deleted = 0 ORDER BY at DESC")
    fun observeAll(): Flow<List<KmRecordEntity>>

    @Upsert suspend fun upsert(entity: KmRecordEntity)

    @Query("UPDATE km_records SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

@Dao
interface UberPanelCountDao {
    @Query("SELECT * FROM uber_panel_counts")
    fun observeAll(): Flow<List<UberPanelCountEntity>>

    @Upsert suspend fun upsert(entity: UberPanelCountEntity)

    @Query("DELETE FROM uber_panel_counts WHERE weekId = :weekId")
    suspend fun clear(weekId: String)
}
