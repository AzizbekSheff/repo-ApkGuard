package com.apkguard

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.apkguard.security.TelegramScanInterceptor
import com.apkguard.util.LocaleHelper
import com.apkguard.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ApkGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        TelegramScanInterceptor.start(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleHelper.onAttach(this)
    }
}
