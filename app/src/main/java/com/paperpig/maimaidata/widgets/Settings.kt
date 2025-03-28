package com.paperpig.maimaidata.widgets

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager


object Settings {
    private lateinit var settingsPre: SharedPreferences

    fun init(context: Context) {
        if (!::settingsPre.isInitialized) {
            settingsPre = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        }
    }

    // 配置项键值对
    private const val KEY_ALIAS_SEARCH = "enable_alias_search"
    private const val DEFAULT_ALIAS_SEARCH = true

    private const val KEY_SHOW_ALIAS = "enable_show_alias"
    private const val DEFAULT_SHOW_ALIAS = true


    fun getEnableAliasSearch() =
        settingsPre.getBoolean(KEY_ALIAS_SEARCH, DEFAULT_ALIAS_SEARCH)


    fun getEnableShowAlias() =
        settingsPre.getBoolean(KEY_SHOW_ALIAS, DEFAULT_SHOW_ALIAS)
}