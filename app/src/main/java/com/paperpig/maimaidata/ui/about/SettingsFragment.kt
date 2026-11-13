package com.paperpig.maimaidata.ui.about

import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.utils.applyBottomSystemBarsPadding

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        findPreference<EditTextPreference>("nickname")?.summaryProvider =
            EditTextPreference.SimpleSummaryProvider.getInstance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.apply {
            clipToPadding = false
            applyBottomSystemBarsPadding(includeIme = true, includeHorizontal = true)
        }
    }
}