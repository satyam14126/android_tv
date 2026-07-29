package com.antigravity.tvbrowser.ui

import android.webkit.URLUtil

object AddressBarController {

    fun formatUrlOrSearch(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "https://www.google.com"

        if (URLUtil.isValidUrl(trimmed)) {
            return trimmed
        }

        if (trimmed.contains(".") && !trimmed.contains(" ")) {
            return "https://$trimmed"
        }

        return "https://www.google.com/search?q=${UriEncoder.encode(trimmed)}"
    }

    private object UriEncoder {
        fun encode(text: String): String {
            return java.net.URLEncoder.encode(text, "UTF-8")
        }
    }
}
