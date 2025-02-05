package com.paperpig.maimaidata.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.paperpig.maimaidata.BuildConfig
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.AboutActivityBinding
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import java.text.SimpleDateFormat
import java.util.Locale

class AboutActivity : AppCompatActivity() {
    companion object {
        const val PROJECT_URL = "https://github.com/PaperPig/MaimaiData"
        const val FEEDBACK_URL = "https://github.com/PaperPig/MaimaiData/issues"
    }

    private lateinit var binding: AboutActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AboutActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.about_container, SettingsFragment())
                .commit()
        }

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.apply {
            setTitle(R.string.about)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val spUtils = SharePreferencesUtils(
                requireContext(),
                SharePreferencesUtils.PREF_NAME_VERSION_INFO
            )

            findPreference<Preference>("version")?.summary = BuildConfig.VERSION_NAME
            findPreference<Preference>("base_data_version")?.summary = spUtils.getDataVersion()
            findPreference<Preference>("last_time_update_chart_stats")?.summary =
                spUtils.getLastUpdateChartStats().let {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return true
    }
}