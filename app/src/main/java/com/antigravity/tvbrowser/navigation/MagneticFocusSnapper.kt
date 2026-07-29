package com.antigravity.tvbrowser.navigation

import android.webkit.WebView
import org.json.JSONObject

class MagneticFocusSnapper(private val webView: WebView) {

    interface SnapCallback {
        fun onSnap(targetX: Float, targetY: Float)
    }

    fun checkMagneticSnap(currentX: Float, currentY: Float, callback: SnapCallback) {
        val density = webView.context.resources.displayMetrics.density
        val cssX = (currentX / density).toInt()
        val cssY = (currentY / density).toInt()

        val jsScript = """
            (function() {
                var el = document.elementFromPoint($cssX, $cssY);
                if (!el) return null;
                var interactive = el.closest('a, button, input, select, textarea, [role="button"], [onclick]');
                if (interactive) {
                    var rect = interactive.getBoundingClientRect();
                    return JSON.stringify({
                        x: (rect.left + rect.width / 2) * $density,
                        y: (rect.top + rect.height / 2) * $density
                    });
                }
                return null;
            })();
        """.trimIndent()

        webView.evaluateJavascript(jsScript) { resultJson ->
            if (resultJson != null && resultJson != "null" && resultJson.length > 2) {
                try {
                    val cleanJson = resultJson.removeSurrounding("\"").replace("\\\"", "\"")
                    val obj = JSONObject(cleanJson)
                    val snapX = obj.getDouble("x").toFloat()
                    val snapY = obj.getDouble("y").toFloat()
                    callback.onSnap(snapX, snapY)
                } catch (e: Exception) {
                    // Ignore parse exceptions silently
                }
            }
        }
    }
}
