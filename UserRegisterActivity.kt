package com.example.letsgo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.letsgo.models.*

class UserRegisterActivity : AppCompatActivity() {

    private lateinit var etUserName: EditText
    private lateinit var etUserEmail: EditText
    private lateinit var etUserPhone: EditText
    private lateinit var etUserPassword: EditText
    private lateinit var btnUserRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_register)

        // Initialize input fields
        etUserName = findViewById(R.id.etUserName)
        etUserEmail = findViewById(R.id.etUserEmail)
        etUserPhone = findViewById(R.id.etUserPhone)
        etUserPassword = findViewById(R.id.etUserPassword)
        btnUserRegister = findViewById(R.id.btnUserRegister)

        // Register button click listener
        btnUserRegister.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val name = etUserName.text.toString().trim()
        val email = etUserEmail.text.toString().trim()
        val phone = etUserPhone.text.toString().trim()
        val password = etUserPassword.text.toString().trim()

        // Basic validation
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val request = UserRegisterRequest(name, email, phone, password)

        // API call
        RetrofitClient.instance.registerUser(request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@UserRegisterActivity,
                            "User registered successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // ✅ Redirect to LoginActivity after short delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            val intent = Intent(this@UserRegisterActivity, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }, 1500)

                    } else {
                        Toast.makeText(
                            this@UserRegisterActivity,
                            "Registration failed! Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(
                        this@UserRegisterActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}
