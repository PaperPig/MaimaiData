package com.paperpig.maimaidata.ui.about

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.paperpig.maimaidata.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        findPreference<EditTextPreference>("nickname")?.summaryProvider =
            EditTextPreference.SimpleSummaryProvider.getInstance()
    }
}