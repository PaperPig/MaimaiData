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

        binding.applyAccountBtn.setOnClickListener {
            selectedAccount?.let { (username, password) ->
                binding.username.setText(username)
                binding.password.setText(password)
            } ?: Toast.makeText(this, "请选择账号", Toast.LENGTH_SHORT).show()
        }

        binding.deleteAccountBtn.setOnClickListener {
            selectedAccount?.let { user ->
                accountList.remove(user)
                sharedPrefs.removeAccount(user.first)
                updateAccountSpinner()
            } ?: Toast.makeText(this, "请选择要删除的账号", Toast.LENGTH_SHORT).show()
        }

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

    private fun updateAccountSpinner() {
        val usernames = accountList.map { it.first }
        spinnerAdapter.clear()
        spinnerAdapter.addAll(usernames)
        spinnerAdapter.notifyDataSetChanged()
    }

    private var selectedAccount: Pair<String, String>? = null

    private fun setupAccountSpinner() {
        accountList = sharedPrefs.getAccountHistory().toMutableList()
        val usernames = accountList.map { it.first }

        spinnerAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, usernames
        )

        binding.accountSpinner.adapter = spinnerAdapter

        binding.accountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAccount = accountList.getOrNull(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedAccount = null
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return true
    }
}
