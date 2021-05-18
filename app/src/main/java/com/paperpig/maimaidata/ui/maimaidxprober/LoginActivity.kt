package com.paperpig.maimaidata.ui.maimaidxprober

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.model.ResponseErrorBody
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)
        username.setText( SharePreferencesUtils(this).getUserName())
        password.setText( SharePreferencesUtils(this).getPassword())


        loginBtn.setOnClickListener {
            val username = username.text.toString()
            val password = password.text.toString()
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loading.visibility = View.VISIBLE
            MaimaiDataRequests.login(username, password)
                .subscribe({
                    loading.visibility = View.GONE

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
                    loading.visibility = View.GONE
                    it.printStackTrace()
                })
        }

    }

}
