package com.apkguard.di

import android.content.Context
import com.apkguard.data.entity.BlacklistDao
import com.apkguard.data.entity.ScanHistoryDao
import com.apkguard.data.local.ApkGuardDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ApkGuardDatabase =
        ApkGuardDatabase.getInstance(context)

    @Provides
    fun provideScanHistoryDao(db: ApkGuardDatabase): ScanHistoryDao =
        db.scanHistoryDao()

    @Provides
    fun provideBlacklistDao(db: ApkGuardDatabase): BlacklistDao =
        db.blacklistDao()

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }
}
