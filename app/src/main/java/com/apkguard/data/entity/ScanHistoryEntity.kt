package com.apkguard.data.entity

import androidx.room.*
import com.apkguard.model.RiskLevel
import com.apkguard.model.UserDecision
import kotlinx.coroutines.flow.Flow

// ─── Entity ────────────────────────────────────────────────────────────────────

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val apkName: String,
    val packageName: String,
    val riskScore: Int,
    val riskLevel: String,          // RiskLevel.name
    val decision: String,           // UserDecision.name
    val scannedAt: Long,
    val resultJson: String          // Full ScanResult serialized as JSON
)

// ─── DAO ───────────────────────────────────────────────────────────────────────

@Dao
interface ScanHistoryDao {

    @Query("SELECT * FROM scan_history ORDER BY scannedAt DESC")
    fun getAllHistory(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE riskScore >= 100 ORDER BY scannedAt DESC")
    fun getCriticalThreats(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE id = :id")
    suspend fun getById(id: Long): ScanHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScanHistoryEntity): Long

    @Query("UPDATE scan_history SET decision = :decision WHERE id = :id")
    suspend fun updateDecision(id: Long, decision: String)

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM scan_history")
    suspend fun count(): Int
}
