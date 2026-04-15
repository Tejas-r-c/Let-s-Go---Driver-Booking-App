package com.example.letsgo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.letsgo.models.RidePaymentRequest
import com.example.letsgo.models.RidePaymentResponse
import com.example.letsgo.models.RatingRequest
import com.example.letsgo.models.RatingResponse
import com.example.letsgo.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class UserRideCompleteActivity : AppCompatActivity() {

    private lateinit var tvFinalFare: TextView
    private lateinit var tvDriverNameFinal: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var etComment: EditText
    private lateinit var btnPayNow: Button
    private lateinit var btnMarkCash: Button
    private lateinit var btnSubmitRating: Button

    private var rideId: String? = null
    private var driverId: String? = null
    private var amount: Double = 0.0
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_ride_complete)

        tvFinalFare = findViewById(R.id.tvFinalFare)
        tvDriverNameFinal = findViewById(R.id.tvDriverNameFinal)
        ratingBar = findViewById(R.id.ratingBar)
        etComment = findViewById(R.id.etRatingComment)
        btnPayNow = findViewById(R.id.btnPayNow)
        btnMarkCash = findViewById(R.id.btnMarkCash)
        btnSubmitRating = findViewById(R.id.btnSubmitRating)

        rideId = intent.getStringExtra("rideId")
        driverId = intent.getStringExtra("driverId")
        amount = intent.getDoubleExtra("fare", 0.0)

        tvFinalFare.text = "Amount: ₹${"%.2f".format(amount)}"
        tvDriverNameFinal.text = "Driver: ${intent.getStringExtra("driverName") ?: "--"}"

        // user id from prefs
        val prefs = getSharedPreferences("LetsGoPrefs", MODE_PRIVATE)
        userId = prefs.getString("userId", null) ?: prefs.getString("email", "unknown@user.com")

        btnPayNow.setOnClickListener {
            // TODO: integrate real payment gateway. Right now, we just call server to mark paid
            if (rideId == null || userId == null) {
                Toast.makeText(this, "Missing details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val req = RidePaymentRequest(rideId = rideId!!, userId = userId!!, amount = amount, paymentMethod = "CARD")
            RetrofitClient.instance.completePayment(req).enqueue(object : Callback<RidePaymentResponse> {
                override fun onResponse(call: Call<RidePaymentResponse>, response: Response<RidePaymentResponse>) {
                    val body = response.body()
                    if (body?.success == true) {
                        Toast.makeText(this@UserRideCompleteActivity, "Payment successful", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@UserRideCompleteActivity, body?.message ?: "Payment failed", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<RidePaymentResponse>, t: Throwable) {
                    Toast.makeText(this@UserRideCompleteActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        btnMarkCash.setOnClickListener {
            if (rideId == null || userId == null) {
                Toast.makeText(this, "Missing details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val req = RidePaymentRequest(rideId = rideId!!, userId = userId!!, amount = amount, paymentMethod = "CASH")
            RetrofitClient.instance.completePayment(req).enqueue(object : Callback<RidePaymentResponse> {
                override fun onResponse(call: Call<RidePaymentResponse>, response: Response<RidePaymentResponse>) {
                    val body = response.body()
                    if (body?.success == true) {
                        Toast.makeText(this@UserRideCompleteActivity, "Marked as cash", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@UserRideCompleteActivity, body?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<RidePaymentResponse>, t: Throwable) {
                    Toast.makeText(this@UserRideCompleteActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        btnSubmitRating.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val comment = etComment.text.toString().trim()

            if (rideId == null || userId == null || driverId == null) {
                Toast.makeText(this, "Missing details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val req = RatingRequest(rideId = rideId!!, userId = userId!!, driverId = driverId!!, rating = rating, comment = if (comment.isEmpty()) null else comment)
            RetrofitClient.instance.rateRide(req).enqueue(object : Callback<RatingResponse> {
                override fun onResponse(call: Call<RatingResponse>, response: Response<RatingResponse>) {
                    val body = response.body()
                    if (body?.success == true) {
                        Toast.makeText(this@UserRideCompleteActivity, "Thanks for rating!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@UserRideCompleteActivity, body?.message ?: "Failed to submit rating", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<RatingResponse>, t: Throwable) {
                    Toast.makeText(this@UserRideCompleteActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
