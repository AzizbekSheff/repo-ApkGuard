package com.apkguard.model

data class InstalledApp(
    val name: String,
    val packageName: String,
    val sourceDir: String,
    val versionName: String,
    val versionCode: Long
)
