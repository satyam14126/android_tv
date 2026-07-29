package com.antigravity.tvbrowser.cache

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import java.io.File

class CacheManager(private val context: Context) {

    fun clearWebCache(webView: WebView) {
        try {
            webView.clearCache(true)
            webView.clearFormData()
            webView.clearHistory()
            Log.d(TAG, "Cleared WebView cache & form data")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing WebView cache", e)
        }
    }

    fun clearCookies() {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            Log.d(TAG, "Cleared all cookies")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cookies", e)
        }
    }

    fun clearWebStorage() {
        try {
            WebStorage.getInstance().deleteAllData()
            Log.d(TAG, "Cleared WebStorage and IndexedDB")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing WebStorage", e)
        }
    }

    fun calculateCacheSizeMB(): Double {
        return try {
            val cacheDir = context.cacheDir
            val sizeBytes = getDirSize(cacheDir)
            sizeBytes.toDouble() / (1024 * 1024)
        } catch (e: Exception) {
            0.0
        }
    }

    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    fun configureCacheSettings(settings: WebSettings) {
        settings.apply {
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
    }

    companion object {
        private const val TAG = "CacheManager"
    }
}
