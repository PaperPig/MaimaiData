package com.paperpig.maimaidata.ui.about

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivitySettingsBinding
import com.paperpig.maimaidata.ui.BaseActivity

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    override fun inflateBinding() = ActivitySettingsBinding.inflate(layoutInflater)

    override fun topInsetTargetView(): View = binding.toolbarLayout.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.about_container, SettingsFragment())
                .commit()
        }

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.apply {
            setTitle(R.string.settings)
            setDisplayHomeAsUpEnabled(true)
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return true
    }
}