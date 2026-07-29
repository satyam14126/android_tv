package com.antigravity.tvbrowser.model

data class Bookmark(
    val title: String,
    val url: String,
    val icon: String = "",
    val category: String = "General"
)

data class SavedCredential(
    val id: String,
    val domain: String,
    val username: String,
    val secretHash: String
)
