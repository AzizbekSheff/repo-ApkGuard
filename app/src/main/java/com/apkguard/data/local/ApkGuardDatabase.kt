package com.apkguard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.apkguard.data.entity.BlacklistDao
import com.apkguard.data.entity.BlacklistEntity
import com.apkguard.data.entity.ScanHistoryDao
import com.apkguard.data.entity.ScanHistoryEntity

@Database(
    entities = [ScanHistoryEntity::class, BlacklistEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ApkGuardDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun blacklistDao(): BlacklistDao

    companion object {
        @Volatile private var INSTANCE: ApkGuardDatabase? = null

        fun getInstance(context: Context): ApkGuardDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ApkGuardDatabase::class.java,
                    "apkguard.db"
                )
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
    }
}
