package com.antigravity.tvbrowser.adblock

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class AdBlockEngine private constructor(private val context: Context) {

    private val blockedHosts = HashSet<String>()
    private val hostCache = ConcurrentHashMap<String, Boolean>()
    val blockedCount = AtomicInteger(0)
    var isEnabled: Boolean = true

    init {
        loadBlocklist()
    }

    private fun loadBlocklist() {
        try {
            val inputStream = context.assets.open("adblock/easylist_hosts.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        val parts = trimmed.split("\\s+".toRegex())
                        if (parts.size >= 2) {
                            blockedHosts.add(parts[1].lowercase(java.util.Locale.ROOT))
                        } else if (parts.size == 1) {
                            blockedHosts.add(parts[0].lowercase(java.util.Locale.ROOT))
                        }
                    }
                }
            }
            Log.d(TAG, "Loaded ${blockedHosts.size} ad-blocking rules into engine")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load blocklist", e)
        }
    }

    fun shouldBlock(request: WebResourceRequest): Boolean {
        if (!isEnabled) return false
        val uri = request.url ?: return false
        val host = uri.host?.lowercase(java.util.Locale.ROOT) ?: return false

        return hostCache.getOrPut(host) {
            isHostBlocked(host) || isUrlPatternBlocked(uri.toString())
        }.also { blocked ->
            if (blocked) {
                blockedCount.incrementAndGet()
            }
        }
    }

    private fun isHostBlocked(host: String): Boolean {
        if (blockedHosts.contains(host)) return true

        // Subdomain matching check (e.g. pagead2.googlesyndication.com -> googlesyndication.com)
        var parts = host.split(".")
        while (parts.size > 2) {
            parts = parts.drop(1)
            val parentDomain = parts.joinToString(".")
            if (blockedHosts.contains(parentDomain)) return true
        }
        return false
    }

    private fun isUrlPatternBlocked(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("/pagead/") ||
               lowerUrl.contains("/adservice/") ||
               lowerUrl.contains("doubleclick.net") ||
               lowerUrl.contains("/popads/") ||
               lowerUrl.contains("googleads.g.doubleclick.net") ||
               // YouTube ad traffic
               lowerUrl.contains("/api/stats/ads") ||
               lowerUrl.contains("/get_midroll_info") ||
               lowerUrl.contains("/youtubei/v1/player/ad_") ||
               lowerUrl.contains("youtube.com/ads") ||
               (lowerUrl.contains("videoplayback") && lowerUrl.contains("ad_type=")) ||
               // Generic ad endpoints
               lowerUrl.contains("/adserver/") ||
               lowerUrl.contains("/ads/click") ||
               lowerUrl.contains("googlesyndication.com/pagead") ||
               lowerUrl.contains("googleadservices.com/pagead")
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    fun injectCosmeticHiding(webView: WebView) {
        if (!isEnabled) return
        try {
            val jsContent = context.assets.open("adblock/cosmetic_hide.js")
                .bufferedReader()
                .use { it.readText() }
            webView.evaluateJavascript(jsContent, null)
        } catch (e: Exception) {
            Log.e(TAG, "Cosmetic script injection failed", e)
        }
    }

    fun injectSiteSpecificAdBlock(webView: WebView, url: String) {
        if (!isEnabled) return
        try {
            val host = Uri.parse(url).host?.lowercase(java.util.Locale.ROOT) ?: return
            if (host.contains("youtube.com")) {
                val jsContent = context.assets.open("adblock/youtube_adblock.js")
                    .bufferedReader()
                    .use { it.readText() }
                webView.evaluateJavascript(jsContent, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Site-specific adblock injection failed", e)
        }
    }

    companion object {
        private const val TAG = "AdBlockEngine"

        @Volatile
        private var instance: AdBlockEngine? = null

        fun getInstance(context: Context): AdBlockEngine {
            return instance ?: synchronized(this) {
                instance ?: AdBlockEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
