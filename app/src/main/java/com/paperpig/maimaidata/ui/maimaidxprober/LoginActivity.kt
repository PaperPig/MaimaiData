package com.paperpig.maimaidata.ui.maimaidxprober

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
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
    private lateinit var sharedPrefs: SharePreferencesUtils
    private lateinit var accountList: MutableList<Pair<String, String>>
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.login)
        }

        sharedPrefs = SharePreferencesUtils(this)
        binding.username.setText(sharedPrefs.getUserName())
        binding.password.setText(sharedPrefs.getPassword())

        setupAccountSpinner()

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
                        val cookie = it.headers()["set-cookie"] ?: ""
                        if (cookie.isNotBlank()) {
                            sharedPrefs.putLoginInfo(username, password, cookie)
                            setupAccountSpinner()
                            startActivity(Intent(this, ProberActivity::class.java))
                            finish()
                        }
                    } else {
                        val errorString = it.errorBody()?.string()
                        val errorEntity = Gson().fromJson(errorString, ResponseErrorBody::class.java)
                        Toast.makeText(this, errorEntity.message, Toast.LENGTH_SHORT).show()
                    }
                }, {
                    binding.loading.visibility = View.GONE
                    it.printStackTrace()
                })
        }
    }

    private var isSpinnerInitialized = false  // 标志位，避免默认触发

    private fun setupAccountSpinner() {
        accountList = sharedPrefs.getAccountHistory().toMutableList()
        val usernames = accountList.map { it.first }  // 只显示真实账号列表

        spinnerAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, usernames
        )

        binding.accountSpinner.adapter = spinnerAdapter

        binding.accountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isSpinnerInitialized) {
                    showPopupMenu(view!!, position)
                } else {
                    isSpinnerInitialized = true  // 只在初始化时跳过一次
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.account_menu, popup.menu)

        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.select_account -> {
                    val (username, password) = accountList[position]
                    binding.username.setText(username)
                    binding.password.setText(password)
                }
                R.id.delete_account -> {
                    sharedPrefs.removeAccount(accountList[position].first)
                    setupAccountSpinner()  // 重新加载列表
                }
            }
            true
        }
        popup.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return true
    }
}
