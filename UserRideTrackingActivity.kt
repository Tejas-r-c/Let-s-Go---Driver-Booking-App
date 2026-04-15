package com.example.letsgo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.letsgo.models.RideDetailsResponse
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



class UserRideTrackingActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var tvRideStatus: TextView
    private lateinit var tvDriverInfo: TextView
    private lateinit var tvFromTo: TextView
    private lateinit var tvEtaUser: TextView

    private var driverMarker: Marker? = null

    private val handler = Handler(Looper.getMainLooper())
    private val pollIntervalMs = 5000L

    // route throttling
    private var lastRouteRequestMs = 0L
    private val routeRequestIntervalMs = 10_000L

    // coordinates (may come from ride details)
    private var pickupLat: Double? = null
    private var pickupLng: Double? = null
    private var dropLat: Double? = null
    private var dropLng: Double? = null

    private var rideId: String? = null
    private var isActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_user_ride_tracking)

        // view bindings
        tvRideStatus = findViewById(R.id.tvRideStatus)
        tvDriverInfo = findViewById(R.id.tvDriverInfo)
        tvFromTo = findViewById(R.id.tvFromTo)
        mapView = findViewById(R.id.mapViewUserRide)
        tvEtaUser = findViewById(R.id.tvEtaUser)

        // read rideId
        rideId = intent.getStringExtra("rideId")

        setupMap()

        if (rideId == null) {
            Toast.makeText(this, "Ride ID missing", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        startPollingRideDetails()
    }

    private fun setupMap() {
        mapView.setMultiTouchControls(true)
        mapView.setUseDataConnection(true)
        mapView.setTileSource(TileSourceFactory.MAPNIK)

        val defaultPoint = GeoPoint(12.9716, 77.5946)
        mapView.controller.setZoom(14.0)
        mapView.controller.setCenter(defaultPoint)
    }

    // ------------------ POLLING -------------------------
    private fun startPollingRideDetails() {
        handler.post(pollRunnable)
    }

    private fun stopPollingRideDetails() {
        handler.removeCallbacks(pollRunnable)
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!isActive || rideId == null) return
            fetchRideDetails(rideId!!)
            handler.postDelayed(this, pollIntervalMs)
        }
    }

    private fun fetchRideDetails(id: String) {
        RetrofitClient.instance.getRideDetails(id)
                .enqueue(object : Callback<RideDetailsResponse> {
                override fun onResponse(
                    call: Call<RideDetailsResponse>,
                    response: Response<RideDetailsResponse>
                ) {
                    val details = response.body() ?: return

                    if (details.success != true) {
                        // backend returned failure — you may want to show a message
                        return
                    }

                    // update local UI and state
                    updateUIWithRideDetails(details)

                    // route & ETA logic (throttled)
                    val driverLat = details.driverLat
                    val driverLng = details.driverLng

                    // populate pickup/drop coords if provided by backend
                    // prefer explicit pickup/drop fields, otherwise leave null
                    pickupLat = details.pickupLat ?: pickupLat ?: details.driverLat
                    pickupLng = details.pickupLng ?: pickupLng ?: details.driverLng
                    dropLat = details.dropLat ?: dropLat
                    dropLng = details.dropLng ?: dropLng

                    if (driverLat != null && driverLng != null) {
                        val now = System.currentTimeMillis()
                        if (now - lastRouteRequestMs >= routeRequestIntervalMs) {
                            lastRouteRequestMs = now

                            // show route driver -> pickup for ON_THE_WAY / ACCEPTED
                            if (details.status == "ACCEPTED" || details.status == "ON_THE_WAY") {
                                if (pickupLat != null && pickupLng != null) {
                                    val start = GeoPoint(driverLat, driverLng)
                                    val end = GeoPoint(pickupLat!!, pickupLng!!)
                                    RouteHelper.drawRoute(this@UserRideTrackingActivity, mapView, start, end) { _, _ ->
                                        val km = SimpleRouteHelper.estimateDistanceKmPublic(start, end)
                                        val eta = "--"
                                        val distLabel = RouteHelper.roadDistanceLabelKm(km)
                                        tvEtaUser.text = "ETA: $eta • $distLabel"
                                    }

                                }
                            } else if (details.status == "STARTED") {
                                // driver is en-route to drop
                                if (dropLat != null && dropLng != null) {
                                    val start = GeoPoint(driverLat, driverLng)
                                    val end = GeoPoint(dropLat!!, dropLng!!)
                                    RouteHelper.drawRoute(this@UserRideTrackingActivity, mapView, start, end) { _, _ ->
                                        val km = SimpleRouteHelper.estimateDistanceKmPublic(start, end)
                                        val eta = "--"
                                        val distLabel = RouteHelper.roadDistanceLabelKm(km)
                                        tvEtaUser.text = "ETA: $eta • $distLabel"
                                    }

                                }
                            }
                            // for other statuses we don't change the route
                        }
                    }
                }

                override fun onFailure(call: Call<RideDetailsResponse>, t: Throwable) {
                    // optional: small toast or logging
                }
            })
    }

    private fun updateUIWithRideDetails(details: RideDetailsResponse) {
        val statusText = when (details.status) {
            "PENDING" -> "Looking for driver…"
            "ACCEPTED" -> "Driver accepted your ride"
            "ON_THE_WAY" -> "Driver is on the way"
            "ARRIVED" -> "Driver has arrived"
            "STARTED" -> "Trip in progress"
            "COMPLETED" -> "Trip completed"
            else -> details.status ?: "Status unknown"
        }

        tvRideStatus.text = statusText

        tvDriverInfo.text = "Driver: ${details.driverName ?: "--"}  ${details.driverPhone ?: ""}"
        tvFromTo.text = "From: ${details.from ?: "--"}   To: ${details.to ?: "--"}"

        val lat = details.driverLat
        val lng = details.driverLng

        if (lat != null && lng != null) {
            updateDriverMarker(lat, lng)
        }

        // handle completion → open complete screen
        if (details.status == "COMPLETED") {
            RouteHelper.clearRoute(mapView)
            tvEtaUser.text = "ETA: --"

            val rId = details.rideId
            val dId = details.driverId
            val dName = details.driverName
            val fare = details.rideFare ?: 0.0

            if (!rId.isNullOrEmpty()) {
                val intent = Intent(this, UserRideCompleteActivity::class.java).apply {
                    putExtra("rideId", rId)
                    putExtra("driverId", dId)         // may be null
                    putExtra("driverName", dName)     // may be null
                    putExtra("fare", fare)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Trip completed", Toast.LENGTH_LONG).show()
            }

            isActive = false
            stopPollingRideDetails()
        }
    }

    private fun updateDriverMarker(lat: Double, lng: Double) {
        val point = GeoPoint(lat, lng)

        if (driverMarker == null) {
            driverMarker = Marker(mapView).apply {
                icon = resources.getDrawable(R.drawable.ic_car_pin, null)
                title = "Driver"
            }
            mapView.overlays.add(driverMarker)
        }

        driverMarker!!.position = point
        mapView.controller.setCenter(point)
        mapView.invalidate()
    }

    // ------------------ LIFECYCLE -------------------------
    override fun onResume() {
        super.onResume()
        mapView.onResume()
        isActive = true
        startPollingRideDetails()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        isActive = false
        stopPollingRideDetails()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPollingRideDetails()
    }
}
