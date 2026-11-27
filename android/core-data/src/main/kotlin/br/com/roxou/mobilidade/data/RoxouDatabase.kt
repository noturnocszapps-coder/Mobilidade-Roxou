package br.com.roxou.mobilidade.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TripEntity::class,
        ExtraEarningEntity::class,
        ExpenseEntity::class,
        FuelLogEntity::class,
        RentalPaymentEntity::class,
        KmRecordEntity::class,
        UberPanelCountEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class RoxouDatabase : RoomDatabase() {
    abstract fun trips(): TripDao
    abstract fun extras(): ExtraEarningDao
    abstract fun expenses(): ExpenseDao
    abstract fun fuel(): FuelLogDao
    abstract fun rentalPayments(): RentalPaymentDao
    abstract fun kmRecords(): KmRecordDao
    abstract fun uberPanelCounts(): UberPanelCountDao

    companion object {
        fun create(context: Context): RoxouDatabase =
            Room.databaseBuilder(context.applicationContext, RoxouDatabase::class.java, "roxou-mobilidade.db").build()
    }
}
