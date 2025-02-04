package com.paperpig.maimaidata.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SharePreferencesUtils(
    val context: Context,
    private val pref: String = PREF_NAME_USER_INFO
) {

    companion object{
        const val PREF_NAME_USER_INFO = "userInfo"
        const val PREF_NAME_VERSION_INFO = "version"
    }

    private val prefs by lazy {
        context.getSharedPreferences(pref, Context.MODE_PRIVATE)
    }

    fun getUserName(): String {
        return prefs.getString("username", "") ?: ""
    }

    fun getPassword(): String {
        return prefs.getString("password", "") ?: ""
    }

    fun getCookie(): String {
        return prefs.getString("cookie", "") ?: ""

    }

    fun putLoginInfo(username: String, password: String, cookie: String) {
        prefs.edit().apply {
            putString("username", username)
            putString("password", password)
            putString("cookie", cookie).apply()
        }
        saveAccountToHistory(username, password)
    }

    fun isFavorite(id: String): Boolean {
        return prefs.getBoolean(id, false)
    }

    fun setFavorite(id: String, isFav: Boolean) {
        prefs.edit().apply {
            putBoolean(id, isFav)
        }.apply()
    }

    fun getDataVersion(): String {
        return prefs.getString("version", "0") ?: "0"
    }

    fun setDataVersion(version: String) {
        prefs.edit().apply {
            putString("version", version).apply()
        }
    }

    fun saveAccountToHistory(username: String, password: String) {
        val history = getAccountHistory().toMutableList()
        if (!history.any { it.first == username }) {
            history.add(Pair(username, password))
            prefs.edit().putString("account_history", Gson().toJson(history)).apply()
        }
    }

    fun getAccountHistory(): List<Pair<String, String>> {
        val json = prefs.getString("account_history", "[]") ?: "[]"
        val type = object : TypeToken<List<Pair<String, String>>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun removeAccount(username: String) {
        val history = getAccountHistory().toMutableList()
        history.removeAll { it.first == username }
        prefs.edit().putString("account_history", Gson().toJson(history)).apply()
    }

    fun saveLastUpdateChartStats(time: Long) {
        prefs.edit().apply {
            putLong("last_update_time", time).apply()
        }
    }

    fun getLastUpdateChartStats(): Long {
        return prefs.getLong("last_update_time", 0)
    }
}