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
import com.example.letsgo.ApiService
import com.example.letsgo.models.ApiResponse
import com.example.letsgo.models.DriverRegisterRequest


class DriverRegisterActivity : AppCompatActivity() {

    private lateinit var etDriverName: EditText
    private lateinit var etDriverEmail: EditText
    private lateinit var etDriverPhone: EditText
    private lateinit var etDriverPassword: EditText
    private lateinit var etLicenseNumber: EditText
    private lateinit var etVehicleType: EditText
    private lateinit var etVehicleNumber: EditText
    private lateinit var btnDriverRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_register)

        // Initialize UI elements
        etDriverName = findViewById(R.id.etDriverName)
        etDriverEmail = findViewById(R.id.etDriverEmail)
        etDriverPhone = findViewById(R.id.etDriverPhone)
        etDriverPassword = findViewById(R.id.etDriverPassword)
        etLicenseNumber = findViewById(R.id.etLicenseNumber)
        etVehicleType = findViewById(R.id.etVehicleType)
        etVehicleNumber = findViewById(R.id.etVehicleNumber)
        btnDriverRegister = findViewById(R.id.btnDriverRegister)

        btnDriverRegister.setOnClickListener {
            registerDriver()
        }
    }

    private fun registerDriver() {
        val name = etDriverName.text.toString().trim()
        val email = etDriverEmail.text.toString().trim()
        val phone = etDriverPhone.text.toString().trim()
        val password = etDriverPassword.text.toString().trim()
        val license = etLicenseNumber.text.toString().trim()
        val vehicleType = etVehicleType.text.toString().trim()
        val vehicleNumber = etVehicleNumber.text.toString().trim()

        // Basic validation
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()
            || license.isEmpty() || vehicleType.isEmpty() || vehicleNumber.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val request = DriverRegisterRequest(name, email, phone, password, license, vehicleType, vehicleNumber)

        // Call backend
        RetrofitClient.instance.registerDriver(request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@DriverRegisterActivity,
                            "Driver registered successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // ✅ Redirect to LoginActivity after short delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            val intent = Intent(this@DriverRegisterActivity, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }, 1500)

                    } else {
                        Toast.makeText(
                            this@DriverRegisterActivity,
                            "Registration failed! Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(
                        this@DriverRegisterActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}
