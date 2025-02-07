package com.paperpig.maimaidata.ui.about

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.paperpig.maimaidata.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
    }
}