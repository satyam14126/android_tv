package com.antigravity.tvbrowser.bookmark

import android.content.Context
import android.util.Log
import com.antigravity.tvbrowser.model.Bookmark
import org.json.JSONArray
import org.json.JSONObject

class BookmarkManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("agy_tv_bookmarks", Context.MODE_PRIVATE)

    init {
        if (prefs.getBoolean("seeded", false).not()) {
            seedDefaultBookmarks()
            prefs.edit().putBoolean("seeded", true).apply()
        }
    }

    fun getAllBookmarks(): List<Bookmark> {
        val jsonStr = prefs.getString(KEY_BOOKMARKS, "[]") ?: "[]"
        val list = mutableListOf<Bookmark>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Bookmark(
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        icon = obj.optString("icon", ""),
                        category = obj.optString("category", "General")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing bookmarks JSON", e)
        }
        return list
    }

    fun isBookmarked(url: String): Boolean {
        return getAllBookmarks().any { it.url.equals(url, ignoreCase = true) }
    }

    fun saveBookmark(title: String, url: String, icon: String = "", category: String = "General"): Boolean {
        if (isBookmarked(url)) return false
        val list = getAllBookmarks().toMutableList()
        list.add(0, Bookmark(title = title, url = url, icon = icon, category = category))
        saveList(list)
        Log.d(TAG, "Bookmark saved: $title -> $url")
        return true
    }

    fun deleteBookmark(url: String) {
        val list = getAllBookmarks().filterNot { it.url.equals(url, ignoreCase = true) }
        saveList(list)
    }

    private fun saveList(list: List<Bookmark>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("title", item.title)
                put("url", item.url)
                put("icon", item.icon)
                put("category", item.category)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_BOOKMARKS, array.toString()).apply()
    }

    private fun seedDefaultBookmarks() {
        try {
            val jsonStr = context.assets.open("default_bookmarks.json")
                .bufferedReader()
                .use { it.readText() }
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Bookmark>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Bookmark(
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        icon = obj.optString("icon", ""),
                        category = obj.optString("category", "General")
                    )
                )
            }
            saveList(list)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed default bookmarks", e)
        }
    }

    companion object {
        private const val TAG = "BookmarkManager"
        private const val KEY_BOOKMARKS = "bookmarks_json"
    }
}
