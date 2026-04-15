package com.example.letsgo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.letsgo.models.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var rbUser: RadioButton
    private lateinit var rbDriver: RadioButton
    private lateinit var btnLogin: Button
    private lateinit var chkRemember: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        rbUser = findViewById(R.id.rbUser)
        rbDriver = findViewById(R.id.rbDriver)
        btnLogin = findViewById(R.id.btnLogin)
        chkRemember = findViewById(R.id.chkRemember)

        val prefs = getSharedPreferences("LetsGoPrefs", Context.MODE_PRIVATE)

        // 🔁 Auto-login if already logged in
        val savedRole = prefs.getString("role", null)
        val savedName = prefs.getString("name", null)
        val savedDriverId = prefs.getString("driverId", null)

        if (savedRole != null && savedName != null) {
            if (savedRole == "user") {
                startActivity(
                    Intent(this, UserDashboardActivity::class.java)
                        .putExtra("userName", savedName)
                )
            } else if (savedRole == "driver" && !savedDriverId.isNullOrEmpty()) {
                startActivity(
                    Intent(this, DriverDashboardActivity::class.java)
                        .putExtra("driverName", savedName)
                )
            }
            finish()
            return
        }

        // Prefill email if stored
        prefs.getString("email", null)?.let { etEmail.setText(it) }

        btnLogin.setOnClickListener {
            loginUser(prefs)
        }
    }

    private fun loginUser(prefs: android.content.SharedPreferences) {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val role = if (rbUser.isChecked) "user" else "driver"

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val request = LoginRequest(email, password, role)

        RetrofitClient.instance.loginUser(request)
            .enqueue(object : Callback<ApiResponse> {

                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.success == true) {

                        val nameFromApi =
                            body.name?.takeIf { it.isNotBlank() }
                                ?: email.substringBefore("@")

                        // 🔥 SAVE LOGIN DATA (CRITICAL FIX)
                        val editor = prefs.edit()
                        editor.putString("role", role)
                        editor.putString("name", nameFromApi)

                        // Save email only if Remember Me checked
                        if (chkRemember.isChecked) {
                            editor.putString("email", email)
                        }

                        // ⭐⭐⭐ MOST IMPORTANT FIX ⭐⭐⭐
                        if (role == "driver") {
                            editor.putString("driverId", body.driverId)
                        }

                        editor.apply()

                        Toast.makeText(
                            this@LoginActivity,
                            "Login successful",
                            Toast.LENGTH_SHORT
                        ).show()

                        // 🚀 Navigate to dashboard
                        if (role == "user") {
                            startActivity(
                                Intent(
                                    this@LoginActivity,
                                    UserDashboardActivity::class.java
                                ).putExtra("userName", nameFromApi)
                            )
                        } else {
                            startActivity(
                                Intent(
                                    this@LoginActivity,
                                    DriverDashboardActivity::class.java
                                ).putExtra("driverName", nameFromApi)
                            )
                        }

                        finish()

                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            body?.message ?: "Invalid credentials",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}
