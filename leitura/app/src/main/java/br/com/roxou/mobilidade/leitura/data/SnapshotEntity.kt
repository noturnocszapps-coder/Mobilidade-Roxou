package br.com.roxou.mobilidade.leitura.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val dateString: String,
    val timeString: String,
    val packageName: String,
    val eventType: String,
    val nodeDumpJson: String,
    val contentHash: Int
)

data class NodeInfoItem(
    val text: String?,
    val contentDescription: String?,
    val viewIdResourceName: String?,
    val className: String?,
    val bounds: String?,
    val depth: Int
)
