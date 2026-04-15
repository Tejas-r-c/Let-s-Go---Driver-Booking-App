package com.example.letsgo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale



class RoundTripActivity : AppCompatActivity() {

    private lateinit var etFrom: EditText

    private lateinit var tvStart: TextView
    private lateinit var tvEnd: TextView
    private lateinit var tvPricing: TextView
    private lateinit var tvTotal: TextView

    private var startMillis = 0L
    private var endMillis = 0L
    private var totalPrice = 0

    private lateinit var etTo: EditText
    private lateinit var etFinalDestination: EditText

    private var startDateTime: Calendar? = null
    private var endDateTime: Calendar? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_round_trip)

        etFrom = findViewById(R.id.etFrom)
        etTo = findViewById(R.id.etTo)
        etFinalDestination = findViewById(R.id.etFinalDestination)

        tvStart = findViewById(R.id.tvStartDateTime)
        tvEnd = findViewById(R.id.tvEndDateTime)
        tvPricing = findViewById(R.id.tvPricing)
        tvTotal = findViewById(R.id.tvTotal)
        

        // ✅ Auto copy FROM → FINAL DESTINATION
        etFrom.addTextChangedListener(object : android.text.TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                etFinalDestination.setText(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        tvStart.setOnClickListener { pickDateTime(true) }
        tvEnd.setOnClickListener { pickDateTime(false) }

        findViewById<Button>(R.id.btnEstimate).setOnClickListener {
            calculateEstimate()
        }

        findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            confirmRoundTrip()
        }
    }
    private fun formatDate(cal: Calendar): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(cal.time)
    }


    private fun pickDateTime(isStart: Boolean) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, day ->
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        calendar.set(year, month, day, hour, minute)

                        if (isStart) {
                            startDateTime = calendar
                            tvStart.text = "Start: ${formatDate(calendar)}"
                        } else {
                            endDateTime = calendar
                            tvEnd.text = "End: ${formatDate(calendar)}"
                        }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }



    private fun calculateEstimate() {

        if (startDateTime == null || endDateTime == null) {
            Toast.makeText(this, "Select start and end date/time", Toast.LENGTH_SHORT).show()
            return
        }

        val startMillis = startDateTime!!.timeInMillis
        val endMillis = endDateTime!!.timeInMillis

        if (endMillis <= startMillis) {
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show()
            return
        }

        val diffMillis = endMillis - startMillis

        // ⏱ Hours (round up)
        val totalHours = Math.ceil(diffMillis / (1000.0 * 60 * 60)).toInt()

        // 📅 Days (round up)
        val totalDays = Math.ceil(diffMillis / (1000.0 * 60 * 60 * 24)).toInt()

        val hourlyCost = totalHours * 150
        val dailyCost = totalDays * 300

        val totalPrice = hourlyCost + dailyCost

        tvPricing.text = """
        Duration:
        • Hours: $totalHours × ₹150 = ₹$hourlyCost
        • Days: $totalDays × ₹300 = ₹$dailyCost
    """.trimIndent()

        tvTotal.text = "Total Price: ₹$totalPrice"
    }


    private fun confirmRoundTrip() {
        if (totalPrice == 0) {
            Toast.makeText(this, "Get estimate first", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, UserRideTrackingActivity::class.java)
        intent.putExtra("tripType", "ROUND_TRIP")
        intent.putExtra("from", etFrom.text.toString())
        intent.putExtra("startTime", startMillis)
        intent.putExtra("endTime", endMillis)
        intent.putExtra("price", totalPrice)

        startActivity(intent)
        finish()
    }
}
