package com.example.letsgo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.example.letsgo.databinding.ActivityDriverDashboardBinding
import com.example.letsgo.models.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DriverDashboardActivity : AppCompatActivity(), LocationListener {

    private lateinit var binding: ActivityDriverDashboardBinding
    private lateinit var locationManager: LocationManager
    private var driverMarker: Marker? = null

    private val handler = Handler(Looper.getMainLooper())
    private val pollIntervalMs = 4000L

    private var isOnline = false
    private var isRideDialogVisible = false

    private var lastLat = 0.0
    private var lastLng = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityDriverDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadDriverName()
        setupMap()
        requestLocationPermission()
        setupClicks()
    }

    private fun loadDriverName() {
        val prefs = getSharedPreferences("LetsGoPrefs", Context.MODE_PRIVATE)
        binding.tvDriverName.text = prefs.getString("name", "Driver")
    }

    private fun setupMap() {
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.controller.setZoom(16.0)
        binding.mapView.controller.setCenter(GeoPoint(12.9716, 77.5946))
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            3000L,
            5f,
            this
        )
    }

    override fun onLocationChanged(location: Location) {
        lastLat = location.latitude
        lastLng = location.longitude

        val point = GeoPoint(lastLat, lastLng)
        if (driverMarker == null) {
            driverMarker = Marker(binding.mapView)
            binding.mapView.overlays.add(driverMarker)
        }
        driverMarker!!.position = point
        binding.mapView.controller.setCenter(point)
        binding.mapView.invalidate()
    }

    private fun setupClicks() {
        binding.switchOnline.setOnCheckedChangeListener { _, online ->
            isOnline = online
            if (online) {
                markDriverOnline()
                startPolling()
            } else {
                markDriverOffline()
                stopPolling()
            }
        }
    }

    /* =========================
       DRIVER ONLINE / OFFLINE
    ========================== */
    private fun markDriverOnline() {
        val prefs = getSharedPreferences("LetsGoPrefs", Context.MODE_PRIVATE)
        val driverId = prefs.getString("driverId", null)

        if (driverId.isNullOrEmpty()) {
            Toast.makeText(this, "Driver ID missing. Login again.", Toast.LENGTH_LONG).show()
            return
        }

        RetrofitClient.instance.driverOnline(
            DriverOnlineRequest(driverId, lastLat, lastLng)
        ).enqueue(SimpleCallback())
    }

    private fun markDriverOffline() {
        val prefs = getSharedPreferences("LetsGoPrefs", Context.MODE_PRIVATE)
        val driverId = prefs.getString("driverId", null) ?: return
        RetrofitClient.instance.driverOffline(
            DriverOfflineRequest(driverId)
        )

    }

    /* =========================
       POLLING
    ========================== */
    private fun startPolling() {
        handler.post(pollRunnable)
    }

    private fun stopPolling() {
        handler.removeCallbacks(pollRunnable)
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (isOnline && !isRideDialogVisible) {
                fetchPendingRide()
                handler.postDelayed(this, pollIntervalMs)
                Log.d("DriverDash", "⏱ polling tick, isOnline=$isOnline")

            }
        }
    }

    private fun fetchPendingRide() {
        val prefs = getSharedPreferences("LetsGoPrefs", Context.MODE_PRIVATE)
        val driverId = prefs.getString("driverId", null) ?: run {
            Log.e("DriverDash", "❌ driverId is NULL — cannot poll rides")
            return
        }

        RetrofitClient.instance.getPendingRides(driverId)
            .enqueue(object : Callback<List<RideItem>> {
                override fun onResponse(
                    call: Call<List<RideItem>>,
                    response: Response<List<RideItem>>
                ) {
                    val rides = response.body() ?: emptyList()
                    Log.d("DriverDash", "🚨 rides received = ${rides.size}")

                    if (rides.isNotEmpty() && !isRideDialogVisible) {
                        showIncomingRideDialog(rides[0])
                    }
                }

                override fun onFailure(call: Call<List<RideItem>>, t: Throwable) {
                    Log.e("DriverDash", "Pending rides error", t)
                }
            })

    }


    /* =========================
       RIDE ACCEPT FLOW
    ========================== */
    private fun showIncomingRideDialog(ride: RideItem) {
        isRideDialogVisible = true

        AlertDialog.Builder(this)
            .setTitle("New Ride Request")
            .setMessage("From: ${ride.from}\nTo: ${ride.to}\nFare: ₹${ride.estimatedFare}")
            .setCancelable(false)
            .setPositiveButton("Accept") { d, _ ->
                d.dismiss()
                isRideDialogVisible = false
                acceptRide(ride)
            }
            .setNegativeButton("Ignore") { d, _ ->
                d.dismiss()
                isRideDialogVisible = false
            }
            .show()
    }

    private fun acceptRide(ride: RideItem) {
        val prefs = getSharedPreferences("LetsGoPrefs", Context.MODE_PRIVATE)
        val driverId = prefs.getString("driverId", null) ?: return

        RetrofitClient.instance.acceptRide(
            AcceptRideRequest(ride.rideId!!, driverId)
        ).enqueue(object : Callback<AcceptRideResponse> {

            override fun onResponse(
                call: Call<AcceptRideResponse>,
                response: Response<AcceptRideResponse>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {

                    val intent = Intent(
                        this@DriverDashboardActivity,
                        DriverOngoingRideActivity::class.java
                    ).apply {
                        putExtra("rideId", response.body()!!.rideId)

                        // 🔥 THIS IS THE CRITICAL FIX
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                        )
                    }

                    startActivity(intent)
                    finish() // kill DriverDashboardActivity
                }
            }

            override fun onFailure(call: Call<AcceptRideResponse>, t: Throwable) {
                Toast.makeText(
                    this@DriverDashboardActivity,
                    "Accept failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
    }

    /* =========================
       SIMPLE CALLBACK
    ========================== */
    class SimpleCallback : Callback<BasicResponse> {
        override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {}
        override fun onFailure(call: Call<BasicResponse>, t: Throwable) {}
    }
}
