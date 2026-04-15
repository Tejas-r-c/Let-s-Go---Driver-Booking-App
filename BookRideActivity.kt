package com.example.letsgo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgo.databinding.ActivityBookRideBinding
import com.example.letsgo.models.PlaceSuggestion
import com.example.letsgo.models.RideResponse
import com.example.letsgo.models.VehicleFareItem
import com.example.letsgo.models.VehicleType
import com.example.letsgo.utils.FareBreakdown
import com.example.letsgo.utils.FareEstimator
import com.example.letsgo.utils.RideType
import com.example.letsgo.utils.SimpleRouteHelper
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import kotlin.concurrent.thread
import com.example.letsgo.models.RideRequest

class BookRideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookRideBinding

    // Vehicles
    private lateinit var vehicleAdapter: VehicleOptionAdapter
    private val vehicleTypes = mutableListOf<VehicleType>()
    private val vehicleFareItems = mutableListOf<VehicleFareItem>()
    private var selectedVehicleFareItem: VehicleFareItem? = null

    // Suggestions + debounce
    private val handler = Handler(Looper.getMainLooper())
    private var debounceFrom: Runnable? = null
    private var debounceTo: Runnable? = null
    private val debounceMs = 500L

    private var fromPlaceList: List<PlaceSuggestion> = emptyList()
    private var toPlaceList: List<PlaceSuggestion> = emptyList()
    private lateinit var fromAdapter: ArrayAdapter<String>
    private lateinit var toAdapter: ArrayAdapter<String>

    // Views may be AutoCompleteTextView or EditText depending on XML
    private var acFrom: AutoCompleteTextView? = null
    private var editFrom: EditText? = null
    private var acTo: AutoCompleteTextView? = null

    // coords and markers
    private var selectedFromLat: Double? = null
    private var selectedFromLng: Double? = null
    private var selectedToLat: Double? = null
    private var selectedToLng: Double? = null
    private var fromMarker: Marker? = null
    private var toMarker: Marker? = null

    private var myLocationOverlay: MyLocationNewOverlay? = null
    private val LOCATION_PERMISSION_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityBookRideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fromAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())
        toAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())

        bindFromField()
        bindToField()

        setupMap()
        setupRecycler()
        fetchVehicleTypes()
        setupEstimateFareClick()
        setupConfirmClick()
    }

    private fun bindFromField() {
        val vFrom = binding.root.findViewById<View>(R.id.etFrom)
        when (vFrom) {
            is AutoCompleteTextView -> {
                acFrom = vFrom
                acFrom?.setAdapter(fromAdapter)
                attachWatcherAutoComplete(acFrom!!, true)
                acFrom?.setOnItemClickListener(AdapterView.OnItemClickListener { parent, _, position, _ ->
                    val name = parent.getItemAtPosition(position) as String
                    val suggestion = fromPlaceList.firstOrNull { it.name == name } ?: fromPlaceList.getOrNull(position)
                    suggestion?.let { onPlaceSelected(it, true) }
                })
            }
            is EditText -> {
                editFrom = vFrom
                attachWatcherEditText(editFrom!!, true)
            }
            else -> {
                // fallback to binding property
                try {
                    acFrom = binding.etFrom as? AutoCompleteTextView
                    if (acFrom != null) {
                        acFrom?.setAdapter(fromAdapter)
                        attachWatcherAutoComplete(acFrom!!, true)
                        acFrom?.setOnItemClickListener(AdapterView.OnItemClickListener { parent, _, position, _ ->
                            val name = parent.getItemAtPosition(position) as String
                            val suggestion = fromPlaceList.firstOrNull { it.name == name } ?: fromPlaceList.getOrNull(position)
                            suggestion?.let { onPlaceSelected(it, true) }
                        })
                    } else {
                        editFrom = binding.etFrom
                        attachWatcherEditText(editFrom!!, true)
                    }
                } catch (e: Exception) {
                    editFrom = binding.etFrom
                    attachWatcherEditText(editFrom!!, true)
                }
            }
        }
    }

    private fun bindToField() {
        val vTo = binding.root.findViewById<View>(R.id.etTo)
        if (vTo is AutoCompleteTextView) {
            acTo = vTo
            acTo?.setAdapter(toAdapter)
            attachWatcherAutoComplete(acTo!!, false)
            acTo?.setOnItemClickListener(AdapterView.OnItemClickListener { parent, _, position, _ ->
                val name = parent.getItemAtPosition(position) as String
                val suggestion = toPlaceList.firstOrNull { it.name == name } ?: toPlaceList.getOrNull(position)
                suggestion?.let { onPlaceSelected(it, false) }
            })
        } else {
            try {
                acTo = binding.etTo as? AutoCompleteTextView
                if (acTo != null) {
                    acTo?.setAdapter(toAdapter)
                    attachWatcherAutoComplete(acTo!!, false)
                    acTo?.setOnItemClickListener(AdapterView.OnItemClickListener { parent, _, position, _ ->
                        val name = parent.getItemAtPosition(position) as String
                        val suggestion = toPlaceList.firstOrNull { it.name == name } ?: toPlaceList.getOrNull(position)
                        suggestion?.let { onPlaceSelected(it, false) }
                    })
                } else {
                    attachWatcherEditText(binding.etTo, false)
                }
            } catch (e: Exception) {
                attachWatcherEditText(binding.etTo, false)
            }
        }
    }

    private fun attachWatcherAutoComplete(ac: AutoCompleteTextView, isFrom: Boolean) {
        ac.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim() ?: ""
                if (q.length < 2) {
                    if (isFrom) { fromAdapter.clear(); fromPlaceList = emptyList() }
                    else { toAdapter.clear(); toPlaceList = emptyList() }
                    return
                }
                if (isFrom) {
                    debounceFrom?.let { handler.removeCallbacks(it) }
                    debounceFrom = Runnable { fetchGeocoderSuggestions(q, true) }
                    handler.postDelayed(debounceFrom!!, debounceMs)
                } else {
                    debounceTo?.let { handler.removeCallbacks(it) }
                    debounceTo = Runnable { fetchGeocoderSuggestions(q, false) }
                    handler.postDelayed(debounceTo!!, debounceMs)
                }
            }
        })
    }

    private fun attachWatcherEditText(et: EditText, isFrom: Boolean) {
        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim() ?: ""
                if (q.length < 2) {
                    if (isFrom) { fromAdapter.clear(); fromPlaceList = emptyList() }
                    else { toAdapter.clear(); toPlaceList = emptyList() }
                    return
                }
                if (isFrom) {
                    debounceFrom?.let { handler.removeCallbacks(it) }
                    debounceFrom = Runnable { fetchGeocoderSuggestions(q, true) }
                    handler.postDelayed(debounceFrom!!, debounceMs)
                } else {
                    debounceTo?.let { handler.removeCallbacks(it) }
                    debounceTo = Runnable { fetchGeocoderSuggestions(q, false) }
                    handler.postDelayed(debounceTo!!, debounceMs)
                }
            }
        })
    }

    private fun fetchGeocoderSuggestions(query: String, isFrom: Boolean) {
        thread {
            try {
                val geocoder = Geocoder(this@BookRideActivity, Locale.getDefault())
                val results = geocoder.getFromLocationName(query, 6) ?: emptyList()
                val list = results.mapIndexed { idx, addr ->
                    val name = buildString {
                        if (!addr.thoroughfare.isNullOrBlank()) append(addr.thoroughfare)
                        if (!addr.subLocality.isNullOrBlank()) { if (isNotEmpty()) append(", "); append(addr.subLocality) }
                        if (!addr.locality.isNullOrBlank()) { if (isNotEmpty()) append(", "); append(addr.locality) }
                        if (isEmpty() && !addr.getAddressLine(0).isNullOrBlank()) append(addr.getAddressLine(0))
                    }.ifEmpty { addr.getAddressLine(0) ?: "Unknown" }

                    PlaceSuggestion(id = idx.toString(), name = name, lat = addr.latitude, lng = addr.longitude)
                }
                runOnUiThread { updateSuggestionUI(list, isFrom) }
            } catch (e: Exception) {
                runOnUiThread { updateSuggestionUI(emptyList(), isFrom) }
            }
        }
    }

    private fun updateSuggestionUI(list: List<PlaceSuggestion>, isFrom: Boolean) {
        val names = list.map { it.name }
        if (isFrom) {
            fromPlaceList = list
            fromAdapter.clear()
            fromAdapter.addAll(names)
            fromAdapter.notifyDataSetChanged()
            acFrom?.showDropDown()
        } else {
            toPlaceList = list
            toAdapter.clear()
            toAdapter.addAll(names)
            toAdapter.notifyDataSetChanged()
            acTo?.showDropDown()
        }
    }

    private fun onPlaceSelected(place: PlaceSuggestion, isFrom: Boolean) {
        val name = place.name
        val lat = place.lat
        val lng = place.lng

        if (isFrom) {
            acFrom?.setText(name) ?: editFrom?.setText(name)
            selectedFromLat = lat
            selectedFromLng = lng
            if (lat != null && lng != null) placeMarkerAndCenter(lat, lng, "From: $name", true)
        } else {
            acTo?.setText(name) ?: binding.etTo.setText(name)
            selectedToLat = lat
            selectedToLng = lng
            if (lat != null && lng != null) placeMarkerAndCenter(lat, lng, "To: $name", false)
        }

        if (selectedFromLat != null && selectedFromLng != null && selectedToLat != null && selectedToLng != null) {
            val start = GeoPoint(selectedFromLat!!, selectedFromLng!!)
            val end = GeoPoint(selectedToLat!!, selectedToLng!!)
            // draw a simple straight line route (visual)
            SimpleRouteHelper.drawStraightLineRoute(this, binding.mapViewRide, start, end)
            val km = SimpleRouteHelper.estimateDistanceKmPublic(start, end)
            binding.tvDistance.text = "Distance: ${"%.2f".format(km)} km"
            binding.tvInfo.text = "ETA: --"
            handleDistance(km)
        }
    }

    private fun placeMarkerAndCenter(lat: Double, lng: Double, title: String, isFrom: Boolean) {
        val gp = GeoPoint(lat, lng)
        if (isFrom) {
            if (fromMarker == null) {
                fromMarker = Marker(binding.mapViewRide).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) }
                binding.mapViewRide.overlays.add(fromMarker)
            }
            fromMarker!!.position = gp
            fromMarker!!.title = title
        } else {
            if (toMarker == null) {
                toMarker = Marker(binding.mapViewRide).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) }
                binding.mapViewRide.overlays.add(toMarker)
            }
            toMarker!!.position = gp
            toMarker!!.title = title
        }
        binding.mapViewRide.controller.setCenter(gp)
        binding.mapViewRide.controller.setZoom(14.5)
        binding.mapViewRide.invalidate()
    }

    private fun setupMap() {
        val map = binding.mapViewRide
        map.setMultiTouchControls(true)
        map.setTileSource(TileSourceFactory.MAPNIK)
        val default = GeoPoint(12.9716, 77.5946)
        map.controller.setZoom(14.0)
        map.controller.setCenter(default)
        checkLocationPermissionAndEnableMyLocation()
    }

    private fun checkLocationPermissionAndEnableMyLocation() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) enableMyLocationOverlay()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST)
    }

    private fun enableMyLocationOverlay() {
        if (myLocationOverlay != null) return
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), binding.mapViewRide).apply {
            enableMyLocation(); enableFollowLocation()
            runOnFirstFix {
                val myLoc = myLocation
                if (myLoc != null) {
                    val geo = GeoPoint(myLoc.latitude, myLoc.longitude)
                    runOnUiThread {
                        binding.mapViewRide.controller.setZoom(16.0)
                        binding.mapViewRide.controller.animateTo(geo)
                    }
                }
            }
        }
        binding.mapViewRide.overlays.add(myLocationOverlay)
        binding.mapViewRide.invalidate()
    }

    private fun setupRecycler() {
        vehicleAdapter = VehicleOptionAdapter(vehicleFareItems) { item ->
            selectedVehicleFareItem = item
            binding.tvInfo.text = "Selected: ${item.vehicle.name} • ₹${"%.0f".format(item.fare.totalFare)}"
        }
        binding.rvVehicleOptions.apply {
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(this@BookRideActivity)
            adapter = vehicleAdapter
        }
    }

    private fun fetchVehicleTypes() {
        Log.d("BookRide", "Fetching vehicle types...")
        try {
            RetrofitClient.instance.getVehicleTypes().enqueue(object : Callback<List<VehicleType>> {
                override fun onResponse(call: Call<List<VehicleType>>, response: Response<List<VehicleType>>) {
                    Log.d("BookRide", "Response ${response.code()}")
                    val list = response.body()
                    runOnUiThread {
                        vehicleTypes.clear()
                        if (!list.isNullOrEmpty()) {
                            vehicleTypes.addAll(list)
                        } else {
                            vehicleTypes.add(VehicleType("bike","Bike",20.0,6.0))
                            vehicleTypes.add(VehicleType("mini","Mini",30.0,8.0))
                            vehicleTypes.add(VehicleType("sedan","Sedan",50.0,12.0))
                        }

                        val temp = vehicleTypes.map { v ->
                            val breakdown = FareEstimator.estimateFare(
                                rideType = RideType.QUICK_RIDE,
                                distanceKm = 1.0,
                                baseFare = v.baseFare,
                                perKmFare = v.perKmFare,
                                driverBaseFare = 0.0,
                                taxPercent = 0.0
                            )
                            VehicleFareItem(v, breakdown)
                        }
                        vehicleFareItems.clear()
                        vehicleFareItems.addAll(temp)
                        vehicleAdapter.updateData(temp)
                        Log.d("BookRide", "Adapter updated with ${temp.size} items")
                    }
                }

                override fun onFailure(call: Call<List<VehicleType>>, t: Throwable) {
                    Log.e("BookRide", "getVehicleTypes failed: ${t.message}", t)
                    runOnUiThread {
                        Toast.makeText(this@BookRideActivity, "Failed to load vehicles — showing defaults", Toast.LENGTH_SHORT).show()
                        vehicleTypes.clear()
                        vehicleTypes.add(VehicleType("bike","Bike",20.0,6.0))
                        val temp = vehicleTypes.map { v ->
                            val breakdown = FareEstimator.estimateFare(
                                rideType = RideType.QUICK_RIDE,
                                distanceKm = 1.0,
                                baseFare = v.baseFare,
                                perKmFare = v.perKmFare,
                                driverBaseFare = 0.0,
                                taxPercent = 0.0
                            )
                            VehicleFareItem(v, breakdown)
                        }
                        vehicleFareItems.clear()
                        vehicleFareItems.addAll(temp)
                        vehicleAdapter.updateData(temp)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("BookRide", "fetchVehicleTypes exception: ${e.message}", e)
        }
    }

    private fun setupEstimateFareClick() {
        binding.btnEstimateFare.setOnClickListener {
            val lat1 = selectedFromLat; val lng1 = selectedFromLng
            val lat2 = selectedToLat; val lng2 = selectedToLng
            if (lat1 != null && lng1 != null && lat2 != null && lng2 != null) {
                val start = Location("").apply { latitude = lat1; longitude = lng1 }
                val end = Location("").apply { latitude = lat2; longitude = lng2 }
                val distanceKm = start.distanceTo(end) / 1000.0
                handleDistance(distanceKm)
            } else {
                val fromText = acFrom?.text?.toString()?.trim() ?: editFrom?.text?.toString()?.trim() ?: ""
                val toText = acTo?.text?.toString()?.trim() ?: binding.etTo.text?.toString()?.trim() ?: ""
                if (fromText.isEmpty() || toText.isEmpty()) { Toast.makeText(this, "Enter From and To", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                getDistanceInKm(fromText, toText) { distanceKm -> handleDistance(distanceKm) }
            }
        }
    }

    private fun handleDistance(distanceKm: Double) {
        if (distanceKm <= 0.0) {
            runOnUiThread { Toast.makeText(this, "Could not calculate distance", Toast.LENGTH_LONG).show() }
            return
        }
        runOnUiThread { binding.tvDistance.text = "Distance: ${"%.2f".format(distanceKm)} km" }
        val newList = vehicleTypes.map { v ->
            val breakdown: FareBreakdown = FareEstimator.estimateFare(
                rideType = RideType.QUICK_RIDE,
                distanceKm = distanceKm,
                baseFare = v.baseFare,
                perKmFare = v.perKmFare,
                driverBaseFare = 0.0,
                taxPercent = 0.0
            )
            VehicleFareItem(v, breakdown)
        }
        runOnUiThread {
            vehicleFareItems.clear()
            vehicleFareItems.addAll(newList)
            vehicleAdapter.updateData(newList)
            binding.tvInfo.text = "Tap a vehicle to select and see its price."
            selectedVehicleFareItem = null
        }
    }

    private fun getDistanceInKm(from: String, to: String, callback: (Double) -> Unit) {
        thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val fromList = geocoder.getFromLocationName(from, 1)
                val toList = geocoder.getFromLocationName(to, 1)
                if (fromList.isNullOrEmpty() || toList.isNullOrEmpty()) { runOnUiThread { callback(0.0) }; return@thread }
                val start = Location("").apply { latitude = fromList[0].latitude; longitude = fromList[0].longitude }
                val end = Location("").apply { latitude = toList[0].latitude; longitude = toList[0].longitude }
                val distanceKm = start.distanceTo(end) / 1000.0
                runOnUiThread { callback(distanceKm) }
            } catch (e: Exception) { e.printStackTrace(); runOnUiThread { callback(0.0) } }
        }
    }

    private fun setupConfirmClick() {
        binding.btnConfirmRide.setOnClickListener {

            // 1️⃣ Collect From & To text
            val fromText = acFrom?.text?.toString()?.trim()
                ?: editFrom?.text?.toString()?.trim()
                ?: ""

            val toText = acTo?.text?.toString()?.trim()
                ?: binding.etTo.text?.toString()?.trim()
                ?: ""

            if (fromText.isEmpty() || toText.isEmpty()) {
                Toast.makeText(this, "Please enter From and To locations", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2️⃣ Check vehicle selection
            val item = selectedVehicleFareItem
            if (item == null) {
                Toast.makeText(this, "Please select a vehicle", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3️⃣ Get user info
            val prefs = getSharedPreferences("LetsGoPrefs", MODE_PRIVATE)
            val userId = prefs.getString("userId", null)
            val userEmail = prefs.getString("email", "unknown@user.com") ?: "unknown@user.com"

            // 4️⃣ Build pickup & dropoff objects
            val pickupPlace = if (selectedFromLat != null && selectedFromLng != null) {
                com.example.letsgo.models.Place(
                    address = fromText,
                    coordinates = listOf(selectedFromLng!!, selectedFromLat!!)
                )
            } else null

            val dropoffPlace = if (selectedToLat != null && selectedToLng != null) {
                com.example.letsgo.models.Place(
                    address = toText,
                    coordinates = listOf(selectedToLng!!, selectedToLat!!)
                )
            } else null

            // 5️⃣ Build ride request
            val rideRequest = com.example.letsgo.models.RideRequest(
                userEmail = userEmail,
                from = fromText,
                to = toText,
                vehicleName = item.vehicle.name,
                estimatedFare = item.fare.totalFare,
                userId = userId,
                pickup = pickupPlace,
                dropoff = dropoffPlace,
                vehicleType = item.vehicle.name
            )

            // 6️⃣ Disable button to prevent double click
            binding.btnConfirmRide.isEnabled = false
            binding.btnConfirmRide.text = "Booking..."

            // 7️⃣ Call backend API
            RetrofitClient.instance.requestRide(rideRequest)
                .enqueue(object : Callback<RideResponse> {

                    override fun onResponse(
                        call: Call<RideResponse>,
                        response: Response<RideResponse>
                    ) {
                        binding.btnConfirmRide.isEnabled = true
                        binding.btnConfirmRide.text = "Confirm Ride"

                        val body = response.body()

                        if (response.isSuccessful &&
                            body != null &&
                            body.success &&
                            body.rideId != null
                        ) {

                            // ✅ ONLY NOW open tracking screen
                            val intent = Intent(
                                this@BookRideActivity,
                                UserRideTrackingActivity::class.java
                            ).apply {
                                putExtra("rideId", body.rideId)
                                putExtra("vehicleType", item.vehicle.name)
                                putExtra("fare", item.fare.totalFare)
                                putExtra("ridePending", true)
                            }

                            startActivity(intent)
                            finish()

                        } else {
                            Toast.makeText(
                                this@BookRideActivity,
                                body?.message ?: "Ride creation failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<RideResponse>, t: Throwable) {
                        binding.btnConfirmRide.isEnabled = true
                        binding.btnConfirmRide.text = "Confirm Ride"

                        Toast.makeText(
                            this@BookRideActivity,
                            "Network error: ${t.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
    }







    // permissions
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) enableMyLocationOverlay()
            else Toast.makeText(this, "Location permission denied. Map will be static.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() { super.onResume(); binding.mapViewRide.onResume() }
    override fun onPause() { super.onPause(); binding.mapViewRide.onPause() }
}
