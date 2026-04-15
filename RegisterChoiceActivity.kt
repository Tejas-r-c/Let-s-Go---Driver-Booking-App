package com.example.letsgo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class RegisterChoiceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_choice)

        val btnUser = findViewById<Button>(R.id.btnUserRegister)
        val btnDriver = findViewById<Button>(R.id.btnDriverRegister)

        btnUser.setOnClickListener {
            startActivity(Intent(this, UserRegisterActivity::class.java))
        }

        btnDriver.setOnClickListener {
            startActivity(Intent(this, DriverRegisterActivity::class.java))
        }
    }
}
