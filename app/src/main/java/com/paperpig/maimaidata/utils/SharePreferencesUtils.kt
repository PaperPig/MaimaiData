package com.paperpig.maimaidata.utils

import android.content.Context

class SharePreferencesUtils(
    val context: Context,
    private val pref: String = "userInfo"
) {

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
    }

    fun isFavorite(id: String): Boolean {
        return prefs.getBoolean(id, false)
    }

    fun setFavorite(id: String, isFav: Boolean) {
        prefs.edit().apply {
            putBoolean(id, isFav)
        }.apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}