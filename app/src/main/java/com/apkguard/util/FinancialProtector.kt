package com.apkguard.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log

object FinancialProtector {

    private const val CARD_REGEX = ".*\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}.*"

    fun startClipboardMonitor(context: Context, onCardDetected: (String) -> Unit) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener {
            try {
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text?.toString() ?: ""
                    if (text.matches(Regex(CARD_REGEX))) {
                        // Karta raqami aniqlandi, uni buferdan tozalash (xavfsizlik uchun)
                        clipboard.setPrimaryClip(ClipData.newPlainText("Secured", ""))
                        onCardDetected(text)
                    }
                }
            } catch (e: Exception) {
                Log.e("FinancialProtector", "Error monitoring clipboard", e)
            }
        }
    }
}
