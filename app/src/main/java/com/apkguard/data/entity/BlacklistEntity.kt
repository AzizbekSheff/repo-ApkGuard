package com.apkguard.data.entity

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "blacklist")
data class BlacklistEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val addedAt: Long = System.currentTimeMillis(),
    val reason: String
)

@Dao
interface BlacklistDao {
    @Query("SELECT * FROM blacklist")
    fun getAll(): Flow<List<BlacklistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM blacklist WHERE packageName = :packageName)")
    suspend fun isBlacklisted(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlacklistEntity)

    @Delete
    suspend fun delete(entity: BlacklistEntity)
}
