package com.antigravity.tvbrowser.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.antigravity.tvbrowser.model.SavedCredential
import org.json.JSONArray
import org.json.JSONObject

class EncryptedVaultManager(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "agy_tv_encrypted_vault",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCredential(domain: String, username: String, secret: String) {
        try {
            val list = getAllCredentials().toMutableList()
            val existingIndex = list.indexOfFirst { it.domain.equals(domain, ignoreCase = true) }
            val newEntry = SavedCredential(
                id = "${domain}_$username",
                domain = domain,
                username = username,
                secretHash = secret
            )

            if (existingIndex >= 0) {
                list[existingIndex] = newEntry
            } else {
                list.add(newEntry)
            }

            saveList(list)
            Log.d(TAG, "Securely saved credential for $domain in Encrypted Vault")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving credential", e)
        }
    }

    fun getCredential(domain: String): SavedCredential? {
        return getAllCredentials().firstOrNull { it.domain.equals(domain, ignoreCase = true) }
    }

    fun getAllCredentials(): List<SavedCredential> {
        val jsonStr = prefs.getString(KEY_VAULT_ITEMS, "[]") ?: "[]"
        val list = mutableListOf<SavedCredential>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SavedCredential(
                        id = obj.getString("id"),
                        domain = obj.getString("domain"),
                        username = obj.getString("username"),
                        secretHash = obj.getString("secretHash")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing vault JSON", e)
        }
        return list
    }

    fun deleteCredential(id: String) {
        val list = getAllCredentials().filterNot { it.id == id }
        saveList(list)
    }

    private fun saveList(list: List<SavedCredential>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("domain", item.domain)
                put("username", item.username)
                put("secretHash", item.secretHash)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_VAULT_ITEMS, array.toString()).apply()
    }

    companion object {
        private const val TAG = "EncryptedVaultManager"
        private const val KEY_VAULT_ITEMS = "vault_credentials_json"
    }
}
