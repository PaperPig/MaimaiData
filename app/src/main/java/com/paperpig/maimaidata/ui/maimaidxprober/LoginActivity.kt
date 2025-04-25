package com.paperpig.maimaidata.ui.maimaidxprober

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityLoginBinding
import com.paperpig.maimaidata.model.ResponseErrorBody
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.utils.SpUtil

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
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

        binding.username.setText(SpUtil.getUserName())
        binding.password.setText(SpUtil.getPassword())

        setupAccountSpinner()

        binding.applyAccountBtn.setOnClickListener {
            if (accountList.isEmpty()) {
                Toast.makeText(this, R.string.select_account_hint, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            selectedAccount?.let { (username, password) ->
                binding.username.setText(username)
                binding.password.setText(password)
            } ?: Toast.makeText(this, R.string.select_account_hint, Toast.LENGTH_SHORT).show()
        }

        binding.deleteAccountBtn.setOnClickListener {
            if (accountList.isEmpty()) {
                Toast.makeText(this, R.string.select_account_hint, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            selectedAccount?.let { user ->
                accountList.remove(user)
                SpUtil.removeAccount(user.first)
                updateAccountSpinner()
            } ?: Toast.makeText(this, R.string.select_account_hint, Toast.LENGTH_SHORT).show()
        }

        binding.loginBtn.setOnClickListener {
            val username = binding.username.text.toString()
            val password = binding.password.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.type_in_account_pwd_hint, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.loading.visibility = View.VISIBLE
            MaimaiDataRequests.login(username, password)
                .subscribe({
                    binding.loading.visibility = View.GONE

                    if (it.code() == 200) {
                        val cookie = it.headers()["set-cookie"] ?: ""
                        if (cookie.isNotBlank()) {
                            SpUtil.putLoginInfo(username, password, cookie)
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
        val usernames = if (accountList.isEmpty()) {
            listOf("无保存的账号")
        } else {
            accountList.map { it.first }
        }

        spinnerAdapter.clear()
        spinnerAdapter.addAll(usernames)
        spinnerAdapter.notifyDataSetChanged()
    }

    private var selectedAccount: Pair<String, String>? = null

    private fun setupAccountSpinner() {
        accountList = SpUtil.getAccountHistory().toMutableList()

        val usernames = if (accountList.isEmpty()) {
            listOf("无保存的账号")
        } else {
            accountList.map { it.first }
        }

        spinnerAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, usernames
        )

        binding.accountSpinner.adapter = spinnerAdapter

        binding.accountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAccount = if (accountList.isEmpty()) null else accountList.getOrNull(position)
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
