package com.paperpig.maimaidata.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.paperpig.maimaidata.BuildConfig
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.utils.SpUtil
import java.text.SimpleDateFormat
import java.util.Locale


class AboutFragment : PreferenceFragmentCompat() {
    companion object {
        const val PROJECT_URL = "https://github.com/PaperPig/MaimaiData"
        const val FEEDBACK_URL = "https://github.com/PaperPig/MaimaiData/issues"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.abot_preferences, rootKey)



        findPreference<Preference>("version")?.summary = BuildConfig.VERSION_NAME
        findPreference<Preference>("base_data_version")?.summary = SpUtil.getDataVersion()
        findPreference<Preference>("last_time_update_chart_stats")?.summary =
            SpUtil.getLastUpdateChartStats().let {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(it)
            }
        findPreference<Preference>("project_url")?.apply {
            summary = PROJECT_URL
            setOnPreferenceClickListener {
                openUrl(PROJECT_URL)
                true
            }
        }
        findPreference<Preference>("feedback")?.apply {
            summary = FEEDBACK_URL
            setOnPreferenceClickListener {
                openUrl(FEEDBACK_URL)
                true
            }
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        })
    }
}