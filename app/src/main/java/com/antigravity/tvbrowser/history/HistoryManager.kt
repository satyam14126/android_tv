package com.antigravity.tvbrowser.history

import android.content.Context
import android.util.Log
import com.antigravity.tvbrowser.model.HistoryEntry
import org.json.JSONArray
import org.json.JSONObject

class HistoryManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("agy_tv_history", Context.MODE_PRIVATE)

    fun recordVisit(title: String, url: String) {
        if (url.isBlank()) return
        val now = System.currentTimeMillis()
        val list = getAllHistory().toMutableList()

        val existingIndex = list.indexOfFirst { it.url.equals(url, ignoreCase = true) }
        val entry = HistoryEntry(id = "${url}_$now", title = title, url = url, timestamp = now)
        if (existingIndex >= 0) {
            list.removeAt(existingIndex)
        }
        list.add(0, entry)

        if (list.size > MAX_ENTRIES) {
            val trimmed = list.subList(0, MAX_ENTRIES)
            saveList(trimmed)
        } else {
            saveList(list)
        }
    }

    fun getAllHistory(): List<HistoryEntry> {
        val jsonStr = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val list = mutableListOf<HistoryEntry>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HistoryEntry(
                        id = obj.getString("id"),
                        title = obj.optString("title"),
                        url = obj.getString("url"),
                        timestamp = obj.optLong("timestamp", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing history JSON", e)
        }
        return list
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
        Log.d(TAG, "Browsing history cleared")
    }

    private fun saveList(list: List<HistoryEntry>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("url", item.url)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    companion object {
        private const val TAG = "HistoryManager"
        private const val KEY_HISTORY = "browsing_history_json"
        private const val MAX_ENTRIES = 200
    }
}
