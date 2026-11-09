package com.apkguard.analyzer

import android.content.Context
import android.net.Uri
import android.util.Log
import com.apkguard.R
import com.apkguard.model.ScanResult
import com.apkguard.model.SignatureInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.zip.ZipInputStream

class ApkScanner(private val context: Context) {

    companion object {
        private const val TAG = "ApkScanner"
    }

    sealed class Progress {
        data class Step(val message: String, val fraction: Float) : Progress()
        data class Result(val result: ScanResult) : Progress()
        data class Failure(val error: String) : Progress()
    }

    private val manifestAnalyzer = ManifestAnalyzer()
    private val dexAnalyzer = DexAnalyzer()
    private val signatureAnalyzer = SignatureAnalyzer(context)
    private val riskEngine = RiskEngine(context)

    fun scan(uri: Uri): Flow<Progress> = flow {
        val startTime = System.currentTimeMillis()

        try {
            emit(Progress.Step(context.getString(R.string.step_copy), 0.05f))
            val apkFile = copyApkToCache(uri) ?: run {
                emit(Progress.Failure("Cannot read APK file. Check storage permission."))
                return@flow
            }

            emit(Progress.Step(context.getString(R.string.step_sig), 0.15f))
            val signatureInfo: SignatureInfo? = try {
                signatureAnalyzer.analyze(apkFile)
            } catch (e: Exception) {
                Log.w(TAG, "Signature analysis failed", e)
                null
            }

            emit(Progress.Step(context.getString(R.string.step_extract), 0.25f))
            var unzipFailed = false
            val extractDir = extractApk(apkFile)
            if (extractDir.listFiles()?.isEmpty() == true) {
                unzipFailed = true
            }

            emit(Progress.Step(context.getString(R.string.step_manifest), 0.40f))
            val packageInfo = context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                android.content.pm.PackageManager.GET_PERMISSIONS
            )
            
            val manifestData = if (packageInfo != null) {
                manifestAnalyzer.convert(packageInfo)
            } else {
                ManifestAnalyzer.ManifestData(
                    packageName = "unknown", versionName = "N/A", versionCode = 0L,
                    minSdkVersion = "N/A", targetSdkVersion = "N/A", permissions = emptyList(),
                    hasDebuggable = false, hasAllowBackup = false,
                    exportedComponents = emptyList()
                )
            }

            emit(Progress.Step(context.getString(R.string.step_dex), 0.60f))
            val dexFiles = extractDir.walkTopDown().filter {
                it.isFile && it.extension == "dex"
            }.toList()

            val dexResults = dexFiles.map { dexAnalyzer.analyze(it) }
            val combinedDexResult = DexAnalyzer.DexAnalysisResult(
                foundUrls = dexResults.flatMap { it.foundUrls }.distinctBy { it.url },
                codePatterns = mergePatterns(dexResults.flatMap { it.codePatterns }),
                malwareHits = dexResults.flatMap { it.malwareHits }.distinctBy { it.keyword }
            )

            emit(Progress.Step(context.getString(R.string.step_risk), 0.85f))
            val assessment = riskEngine.assess(apkFile, manifestData, combinedDexResult, signatureInfo, unzipFailed)

            emit(Progress.Step(context.getString(R.string.step_final), 0.95f))

            val result = ScanResult(
                apkName = apkFile.name,
                apkPath = apkFile.absolutePath,
                apkSizeBytes = apkFile.length(),
                packageName = manifestData.packageName,
                versionName = manifestData.versionName,
                versionCode = manifestData.versionCode,
                minSdkVersion = manifestData.minSdkVersion,
                targetSdkVersion = manifestData.targetSdkVersion,
                permissions = manifestData.permissions,
                foundUrls = combinedDexResult.foundUrls,
                codePatterns = combinedDexResult.codePatterns,
                malwareHits = combinedDexResult.malwareHits,
                signatureInfo = signatureInfo,
                riskScore = assessment.score,
                riskLevel = assessment.level,
                riskReasons = assessment.reasons,
                scanDurationMs = System.currentTimeMillis() - startTime
            )

            extractDir.deleteRecursively()
            emit(Progress.Result(result))

        } catch (e: Exception) {
            Log.e(TAG, "Scan failed", e)
            emit(Progress.Failure("Scan failed: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun copyApkToCache(uri: Uri): File? {
        return try {
            val cacheDir = File(context.cacheDir, "apk_scans").apply { mkdirs() }
            val fileName = resolveFileName(uri)
            val dest = File(cacheDir, fileName)
            
            // Uri'dan o'qishda ehtiyotkorona yondashuv (Telegram va boshqalar uchun)
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            
            if (dest.exists() && dest.length() > 0) dest else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy APK: ${e.message}", e)
            null
        }
    }

    private fun resolveFileName(uri: Uri): String {
        val path = uri.lastPathSegment ?: "unknown.apk"
        return if (path.endsWith(".apk")) path.substringAfterLast("/")
        else "scanned_${System.currentTimeMillis()}.apk"
    }

    private fun extractApk(apkFile: File): File {
        val extractDir = File(context.cacheDir, "apk_extracted_${System.currentTimeMillis()}")
        extractDir.mkdirs()

        try {
            ZipInputStream(apkFile.inputStream().buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    try {
                        val name = entry.name
                        if (!entry.isDirectory && (
                            name == "AndroidManifest.xml" ||
                            name.endsWith(".dex") ||
                            name.startsWith("META-INF/")
                        )) {
                            val outFile = File(extractDir, name.replace("/", "_"))
                            outFile.outputStream().use { out ->
                                zip.copyTo(out)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to extract entry: ${entry.name}", e)
                    } finally {
                        zip.closeEntry()
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unzip APK", e)
            // Agar unzip butunlay o'xshamasa, hech bo'lmaganda bo'sh papka qaytaramiz
            // yoki xatoni tepaga otamiz. Bu yerda ZipInputStream shifrlangan entrylarni 
            // ko'tara olmasligi mumkin.
        }
        return extractDir
    }

    private fun mergePatterns(patterns: List<com.apkguard.model.CodePattern>): List<com.apkguard.model.CodePattern> {
        return patterns.groupBy { it.name }.map { (_, group) ->
            group.first().copy(occurrences = group.sumOf { it.occurrences })
        }
    }
}
