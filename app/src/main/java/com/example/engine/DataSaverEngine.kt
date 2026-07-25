package com.example.engine

import android.webkit.WebSettings
import java.util.concurrent.atomic.AtomicLong

class DataSaverEngine {
    private val estimatedBytesSaved = AtomicLong(0)

    fun applySettings(settings: WebSettings, isDataSaverActive: Boolean, allowImagesOverride: Boolean = false) {
        if (isDataSaverActive) {
            val shouldLoadImages = allowImagesOverride
            settings.loadsImagesAutomatically = shouldLoadImages
            settings.blockNetworkImage = !shouldLoadImages
            settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        } else {
            settings.loadsImagesAutomatically = true
            settings.blockNetworkImage = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
        }
    }

    fun addEstimatedSavedData(bytes: Long) {
        estimatedBytesSaved.addAndGet(bytes)
    }

    fun getSavedDataFormatted(): String {
        val bytes = estimatedBytesSaved.get()
        return when {
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1048576.0)
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes Bytes"
        }
    }
}
