package com.example.letsgo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.letsgo.models.BasicResponse
import com.example.letsgo.models.DriverLocationUpdateRequest
import com.example.letsgo.models.RideStatusUpdateRequest
import com.example.letsgo.utils.RouteHelper
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.letsgo.utils.SimpleRouteHelper



class DriverOngoingRideActivity : AppCompatActivity(), LocationListener {

    private lateinit var mapView: MapView
    private lateinit var locationManager: LocationManager
    private var driverMarker: Marker? = null

    private lateinit var tvRideInfo: TextView
    private lateinit var tvEta: TextView
    private lateinit var btnOnTheWay: Button
    private lateinit var btnArrived: Button
    private lateinit var btnStartTrip: Button
    private lateinit var btnCompleteTrip: Button

    private var lastRouteRequestMs = 0L
    private val routeRequestIntervalMs = 10_000L // 10 seconds throttle

    // pickup coordinates (may be null if not provided)
    private var pickupLat: Double? = null
    private var pickupLng: Double? = null

    private val LOCATION_PERMISSION_REQUEST = 201

    private var rideId: String? = null
    private var from: String? = null
    private var to: String? = null
    private var fare: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_driver_ongoing_ride)

        // --- view bindings ---
        tvRideInfo = findViewById(R.id.tvRideInfo)
        tvEta = findViewById(R.id.tvEta)
        mapView = findViewById(R.id.mapViewOngoing)
        btnOnTheWay = findViewById(R.id.btnOnTheWay)
        btnArrived = findViewById(R.id.btnArrived)
        btnStartTrip = findViewById(R.id.btnStartTrip)
        btnCompleteTrip = findViewById(R.id.btnCompleteTrip)

        // --- read intent extras safely ---
        rideId = intent.getStringExtra("rideId")
        from = intent.getStringExtra("from")
        to = intent.getStringExtra("to")
        fare = intent.getDoubleExtra("fare", 0.0)

        // pickup coords if provided via intent
        val pLat = intent.getDoubleExtra("pickupLat", Double.NaN)
        val pLng = intent.getDoubleExtra("pickupLng", Double.NaN)
        pickupLat = pLat.takeIf { !it.isNaN() }
        pickupLng = pLng.takeIf { !it.isNaN() }

        tvRideInfo.text = "From: $from\nTo: $to\nFare: ₹${"%.0f".format(fare)}"

        setupMap()
        requestLocationPermission()
        setupButtons()
    }

    private fun setupMap() {
        mapView.setMultiTouchControls(true)
        mapView.setUseDataConnection(true)
        mapView.setTileSource(TileSourceFactory.MAPNIK)

        val defaultPoint = GeoPoint(12.9716, 77.5946)
        val controller = mapView.controller
        controller.setZoom(16.0)
        controller.setCenter(defaultPoint)
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                3000,
                5f,
                this
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Location error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onLocationChanged(location: Location) {
        val point = GeoPoint(location.latitude, location.longitude)

        // add marker if not present
        if (driverMarker == null) {
            driverMarker = Marker(mapView).apply {
                icon = ContextCompat.getDrawable(this@DriverOngoingRideActivity, R.drawable.ic_car_pin)
                title = "You"
            }
            mapView.overlays.add(driverMarker)
        }

        driverMarker!!.position = point
        mapView.controller.setCenter(point)
        mapView.invalidate()

        // send to backend
        sendLocationToServer(location.latitude, location.longitude)

        // request route to pickup (throttled)
        requestRouteToPickup(location.latitude, location.longitude)
    }

    private fun sendLocationToServer(lat: Double, lng: Double) {
        val id = rideId ?: return

        val prefs = getSharedPreferences("LetsGoPrefs", MODE_PRIVATE)
        val driverId = prefs.getString("driverId", null) ?: return

        val req = DriverLocationUpdateRequest(
            rideId = id,
            driverId = driverId,
            lat = lat,
            lng = lng
        )

        RetrofitClient.instance.updateDriverLocation(req)
            .enqueue(object : Callback<BasicResponse> {
                override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                    // no UI action required
                }

                override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                    // optional: logging
                }
            })
    }

    private fun setupButtons() {
        btnOnTheWay.setOnClickListener {
            updateStatus("ON_THE_WAY")
        }
        btnArrived.setOnClickListener {
            updateStatus("ARRIVED")
        }
        btnStartTrip.setOnClickListener {
            updateStatus("STARTED")
        }
        btnCompleteTrip.setOnClickListener {
            updateStatus("COMPLETED")
            Toast.makeText(this, "Trip completed", Toast.LENGTH_LONG).show()
            // clear route overlay when done
            RouteHelper.clearRoute(mapView)
            finish()
        }
    }

    private fun updateStatus(status: String) {
        val id = rideId ?: return
        val req = RideStatusUpdateRequest(
            rideId = id,
            status = status
        )

        RetrofitClient.instance.updateRideStatus(req)
            .enqueue(object : Callback<BasicResponse> {
                override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                    val body = response.body()
                    if (body?.success != true) {
                        Toast.makeText(
                            this@DriverOngoingRideActivity,
                            body?.message ?: "Failed to update status",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                    Toast.makeText(
                        this@DriverOngoingRideActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun requestRouteToPickup(driverLat: Double, driverLng: Double) {
        // throttle route requests
        val now = System.currentTimeMillis()
        if (now - lastRouteRequestMs < routeRequestIntervalMs) return
        lastRouteRequestMs = now

        val pLat = pickupLat ?: return
        val pLng = pickupLng ?: return

        val start = GeoPoint(driverLat, driverLng)
        val end = GeoPoint(pLat, pLng)

        RouteHelper.drawRoute(this, mapView, start, end) { _, _ ->
            // We do not have a Road object for straight-line; compute straight-line distance instead
            val km = SimpleRouteHelper.estimateDistanceKmPublic(start, end)
            val etaLabel = "--" // straight-line preview does not provide ETA
            val distLabel = RouteHelper.roadDistanceLabelKm(km)
            tvEta.text = "ETA: $etaLabel • $distLabel"
        }


    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
    }
}
