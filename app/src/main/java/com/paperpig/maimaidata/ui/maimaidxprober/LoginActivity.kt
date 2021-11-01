package com.paperpig.maimaidata.ui.maimaidxprober

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityLoginBinding
import com.paperpig.maimaidata.model.ResponseErrorBody
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.utils.SharePreferencesUtils

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        supportActionBar?.title = getString(R.string.login)


        binding.username.setText(SharePreferencesUtils(this).getUserName())
        binding.password.setText(SharePreferencesUtils(this).getPassword())


        binding.loginBtn.setOnClickListener {
            val username = binding.username.text.toString()
            val password = binding.password.text.toString()
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.loading.visibility = View.VISIBLE
            MaimaiDataRequests.login(username, password)
                .subscribe({
                    binding.loading.visibility = View.GONE

                    if (it.code() == 200) {
                        val cookie = it.headers()["set-cookie"] ?: String()
                        if (cookie.isNotBlank()) {
                            SharePreferencesUtils(this).putLoginInfo(username, password, cookie)
                            startActivity(Intent(this, ProberActivity::class.java))
                            finish()
                        }
                    } else {
                        val errorString = it.errorBody()?.string()
                        val errorEntity =
                            Gson().fromJson(errorString, ResponseErrorBody::class.java)
                        Toast.makeText(this, errorEntity.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                }, {
                    binding.loading.visibility = View.GONE
                    it.printStackTrace()
                })
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                finish()
        }
        return true
    }
}
