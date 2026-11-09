package com.apkguard.service

import android.content.Context
import android.net.Uri
import com.apkguard.analyzer.ApkScanner
import com.apkguard.model.ScanResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeScanManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scanner = ApkScanner(context)

    suspend fun scanApk(uri: Uri): ScanResult? {
        return try {
            scanner.scan(uri)
                .mapNotNull { progress ->
                    if (progress is ApkScanner.Progress.Result) progress.result
                    else null
                }
                .first()
        } catch (e: Exception) {
            null
        }
    }
}
