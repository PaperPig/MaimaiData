package com.paperpig.maimaidata.widgets

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager


enum class ProberUploadTarget(val prefValue: String) {
    DIVING_FISH("diving_fish"),
    LXNS("lxns"),
    BOTH("both");

    fun includesDivingFish(): Boolean = this == DIVING_FISH || this == BOTH

    fun includesLxns(): Boolean = this == LXNS || this == BOTH

    companion object {
        fun fromPrefValue(value: String?): ProberUploadTarget {
            return entries.firstOrNull { it.prefValue == value } ?: DIVING_FISH
        }
    }
}


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

    private const val KEY_CHARTER_SEARCH = "enable_charter_search"
    private const val DEFAULT_CHARTER_SEARCH = false

    private const val KEY_SHOW_ALIAS = "enable_show_alias"
    private const val DEFAULT_SHOW_ALIAS = true

    private const val KEY_USE_DIVING_FISH_NICKNAME = "enable_diving_fish_nickname"
    private const val DEFAULT_USE_DIVING_FISH_NICKNAME = true

    private const val KEY_NICKNAME = "nickname"
    private const val DEFAULT_NICKNAME = ""

    private const val KEY_SELECT_DIFFICULTIES = "select_difficulties"
    private val DEFAULT_SELECT = setOf("2", "3", "4")

    private const val KEY_PROBER_UPLOAD_TARGET = "prober_upload_target"
    private const val DEFAULT_PROBER_UPLOAD_TARGET = "diving_fish"


    fun getEnableAliasSearch() =
        settingsPre.getBoolean(KEY_ALIAS_SEARCH, DEFAULT_ALIAS_SEARCH)

    fun getEnableCharterSearch() =
        settingsPre.getBoolean(KEY_CHARTER_SEARCH, DEFAULT_CHARTER_SEARCH)

    fun getEnableShowAlias() =
        settingsPre.getBoolean(KEY_SHOW_ALIAS, DEFAULT_SHOW_ALIAS)

    fun getEnableDivingFishNickname() =
        settingsPre.getBoolean(KEY_USE_DIVING_FISH_NICKNAME, DEFAULT_USE_DIVING_FISH_NICKNAME)

    fun getNickname(): String =
        settingsPre.getString(KEY_NICKNAME, DEFAULT_NICKNAME) ?: DEFAULT_NICKNAME

    fun getUpdateDifficulty(): Set<Int> {
        val selectedSet =
            settingsPre.getStringSet(KEY_SELECT_DIFFICULTIES, DEFAULT_SELECT) ?: DEFAULT_SELECT
        return selectedSet.mapNotNull { it.toIntOrNull() }.toSet()

    }

    fun setUpdateDifficulty(difficulties: Set<Int>) {
        settingsPre.edit {
            putStringSet(KEY_SELECT_DIFFICULTIES, difficulties.map { it.toString() }.toSet())
        }
    }

    fun getProberUploadTarget(): ProberUploadTarget {
        val raw = settingsPre.getString(KEY_PROBER_UPLOAD_TARGET, DEFAULT_PROBER_UPLOAD_TARGET)
        return ProberUploadTarget.fromPrefValue(raw)
    }

    fun setProberUploadTarget(target: ProberUploadTarget) {
        settingsPre.edit {
            putString(KEY_PROBER_UPLOAD_TARGET, target.prefValue)
        }
    }
}