package com.paperpig.maimaidata.utils

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SpUtil {
    private lateinit var application: Application

    private val userInfoPrefs: SharedPreferences by lazy {
        getSharedPreferences(PREF_USER_INFO)
    }

    private val versionPrefs: SharedPreferences by lazy {
        getSharedPreferences(PREF_VERSION)
    }

    private val songInfoPrefs: SharedPreferences by lazy {
        getSharedPreferences(PREF_SONG_INFO)
    }

    private val searchHistoryPrefs: SharedPreferences by lazy {
        getSharedPreferences(PREF_SEARCH_HISTORY)
    }

    // 常量定义
    private const val PREF_USER_INFO = "userInfo"
    private const val PREF_VERSION = "version"
    private const val PREF_SONG_INFO = "songInfo"
    private const val PREF_SEARCH_HISTORY = "searchHistory"

    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_COOKIE = "cookie"
    private const val KEY_ACCOUNT_HISTORY = "account_history"
    private const val KEY_LAST_QUERY_LEVEL = "last_query_level"
    private const val KEY_LAST_QUERY_VERSION = "last_query_version"
    private const val KEY_DIVING_FISH_NICKNAME = "diving_fish_nickname"

    private const val KEY_VERSION = "version"
    private const val KEY_LAST_UPDATE_TIME = "last_update_time"

    private const val KEY_SEARCH_HISTORY = "search_history"

    fun init(appContext: Application) {
        application = appContext
    }

    private fun checkInitialization() {
        if (!::application.isInitialized) {
            throw IllegalStateException("SpUtil is not initialized, please call init() method first")
        }
    }

    private fun getSharedPreferences(name: String): SharedPreferences {
        checkInitialization()
        return application.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    // ================= USER INFO =================

    fun getUserName(): String = userInfoPrefs.getString(KEY_USERNAME, "") ?: ""

    fun getPassword(): String = userInfoPrefs.getString(KEY_PASSWORD, "") ?: ""

    fun getCookie(): String = userInfoPrefs.getString(KEY_COOKIE, "") ?: ""

    fun putLoginInfo(username: String, password: String, cookie: String) {
        userInfoPrefs.edit {
            putString(KEY_USERNAME, username)
            putString(KEY_PASSWORD, password)
            putString(KEY_COOKIE, cookie)
        }
        saveAccountToHistory(username, password)
    }

    fun saveAccountToHistory(username: String, password: String) {
        val history = getAccountHistory().toMutableList()
        if (history.none { it.first == username }) {
            history.add(Pair(username, password))
            userInfoPrefs.edit { putString(KEY_ACCOUNT_HISTORY, Gson().toJson(history)) }
        }
    }

    fun getAccountHistory(): List<Pair<String, String>> {
        val json = userInfoPrefs.getString(KEY_ACCOUNT_HISTORY, "[]") ?: "[]"
        val type = object : TypeToken<List<Pair<String, String>>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun removeAccount(username: String) {
        val history = getAccountHistory().toMutableList()
        history.removeAll { it.first == username }
        userInfoPrefs.edit { putString(KEY_ACCOUNT_HISTORY, Gson().toJson(history)) }
    }

    fun saveLastQueryLevel(level: Float) {
        userInfoPrefs.edit { putFloat(KEY_LAST_QUERY_LEVEL, level) }
    }

    fun getLastQueryLevel(): Float = userInfoPrefs.getFloat(KEY_LAST_QUERY_LEVEL, 18f)

    fun saveLastQueryVersion(version: Int) {
        userInfoPrefs.edit { putInt(KEY_LAST_QUERY_VERSION, version) }
    }

    fun getLastQueryVersion(): Int = userInfoPrefs.getInt(KEY_LAST_QUERY_VERSION, 0)

    fun saveDivingFishNickname(nickname: String) {
        userInfoPrefs.edit { putString(KEY_DIVING_FISH_NICKNAME, nickname) }
    }

    fun getDivingFishNickname(): String {
        return userInfoPrefs.getString(KEY_DIVING_FISH_NICKNAME, "") ?: ""
    }

    // ================= SONG INFO =================

    fun isFavorite(id: String): Boolean = songInfoPrefs.getBoolean(id, false)

    fun setFavorite(id: String, isFav: Boolean) {
        songInfoPrefs.edit { putBoolean(id, isFav) }
    }

    fun getFavIds(): List<String> {
        return songInfoPrefs.all
            .filter { it.value == true }
            .keys.toList()
    }

    // ================= VERSION INFO =================

    fun getDataVersion(): String = versionPrefs.getString(KEY_VERSION, "0") ?: "0"

    fun setDataVersion(version: String) {
        versionPrefs.edit { putString(KEY_VERSION, version) }
    }

    fun saveLastUpdateChartStats(time: Long) {
        versionPrefs.edit { putLong(KEY_LAST_UPDATE_TIME, time) }
    }

    fun getLastUpdateChartStats(): Long = versionPrefs.getLong(KEY_LAST_UPDATE_TIME, 0)

    // ================= SEARCH HISTORY =================

    fun saveSearchHistory(query: String) {
        val history = getSearchHistory().toMutableList()
        if (history.contains(query)) {
            history.remove(query) // 如果已存在，先移除
        }
        history.add(0, query) // 新记录添加到最前面
        if (history.size > 30) history.removeAt(history.lastIndex) // 限制最多存储 30 条记录
        searchHistoryPrefs.edit { putString(KEY_SEARCH_HISTORY, Gson().toJson(history)) }
    }

    fun getSearchHistory(): List<String> {
        val json = searchHistoryPrefs.getString(KEY_SEARCH_HISTORY, "[]") ?: "[]"
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun clearSearchHistory() {
        searchHistoryPrefs.edit { clear() }
    }
}