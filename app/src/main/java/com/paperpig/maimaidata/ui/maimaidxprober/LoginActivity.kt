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

        binding.showAccountListButton.setOnClickListener {
            val accountList = SpUtil.getAccountHistory().toMutableList()
            val accounts = if (accountList.isEmpty()) {
                listOf("无保存的账号")
            } else {
                accountList.map { it.first }
            }

            val bottomSheet = AccountListBottomSheet(
                accounts,
                onAccountSelected = { selected ->
                    selectedAccount = accountList.find { x -> x.first == selected }

                    selectedAccount?.let { (username, password) ->
                        binding.username.setText(username)
                        binding.password.setText(password)
                    } ?: Toast.makeText(this, R.string.select_account_hint, Toast.LENGTH_SHORT).show()
                },
                onAccountDeleted = { deletedAccount ->
                    SpUtil.removeAccount(deletedAccount)
                    Toast.makeText(this, "已删除账号：$deletedAccount", Toast.LENGTH_SHORT).show()
                }
            )

            bottomSheet.show(supportFragmentManager, "AccountList")
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

    private var selectedAccount: Pair<String, String>? = null


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return true
    }
}
