package br.com.roxou.mobilidade.leitura.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {

    @Insert
    suspend fun insert(snapshot: SnapshotEntity): Long

    @Query("SELECT * FROM snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM snapshots WHERE dateString = :date ORDER BY timestamp DESC")
    fun getSnapshotsByDate(date: String): Flow<List<SnapshotEntity>>

    @Query("SELECT DISTINCT dateString FROM snapshots ORDER BY dateString DESC")
    fun getExportableDates(): Flow<List<String>>

    @Query("SELECT * FROM snapshots WHERE dateString = :date ORDER BY timestamp DESC")
    suspend fun getSnapshotsForExportSync(date: String): List<SnapshotEntity>

    @Query("SELECT COUNT(*) FROM snapshots WHERE dateString = :date")
    fun getCountForDate(date: String): Flow<Int>

    @Query("DELETE FROM snapshots")
    suspend fun deleteAll()
}
