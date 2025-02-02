package com.paperpig.maimaidata.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.paperpig.maimaidata.BuildConfig
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.AboutActivityBinding

class AboutActivity : AppCompatActivity() {
    companion object{
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

            findPreference<Preference>("version")?.summary = BuildConfig.VERSION_NAME
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
}