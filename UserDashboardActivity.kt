package com.example.letsgo
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.letsgo.databinding.ActivityUserDashboardBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.Marker
// (optional) for routes:
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.views.overlay.Polyline
import android.content.Intent



class UserDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDashboardBinding
    private var myLocationOverlay: MyLocationNewOverlay? = null

    private val locationPermsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grant ->
            val ok = (grant[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    || grant[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
            if (ok) enableMyLocation()
            else Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSMDroid must be configured before setContentView
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(this)
        )
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityUserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)

        // --- Map basic setup ---
        val map = binding.mapView
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.setUseDataConnection(true)
        map.controller.setZoom(16.0)
        map.controller.setCenter(GeoPoint(12.9716, 77.5946)) // Bengaluru fallback

        // location permission & overlay
        checkLocationPerms()

        // Your existing buttons & bottom nav remain:
        binding.btnQuickRide.setOnClickListener {
            Toast.makeText(this, "Quick Ride flow", Toast.LENGTH_SHORT).show()
        }
        binding.btnQuickRide.setOnClickListener {
            val i = Intent(this, BookRideActivity::class.java)
            startActivity(i)
        }

        binding.btnBookDriver.setOnClickListener {
            Toast.makeText(this, "Driver-for-a-trip flow", Toast.LENGTH_SHORT).show()
        }
        binding.btnBookDriver.setOnClickListener {
            startActivity(
                Intent(this@UserDashboardActivity, RoundTripActivity::class.java)
            )
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_history -> { Toast.makeText(this,"History",Toast.LENGTH_SHORT).show(); true }
                R.id.nav_profile -> { Toast.makeText(this,"Profile",Toast.LENGTH_SHORT).show(); true }
                else -> false
            }
        }
    }

    private fun checkLocationPerms() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        } else {
            locationPermsLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun enableMyLocation() {
        val map = binding.mapView
        val provider = GpsMyLocationProvider(this)
        myLocationOverlay = MyLocationNewOverlay(provider, map).apply {
            enableMyLocation()
            enableFollowLocation()
            // optional: change person/accuracy icons here
        }
        map.overlays.add(myLocationOverlay)

        // Move camera when first fix arrives and drop a marker
        myLocationOverlay?.runOnFirstFix {
            val p = myLocationOverlay?.myLocation ?: return@runOnFirstFix
            runOnUiThread {
                map.controller.animateTo(p)
                addMarker(p.latitude, p.longitude, "You are here")
            }
        }
        map.invalidate()
    }

    private fun addMarker(lat: Double, lon: Double, title: String) {
        val map = binding.mapView
        val marker = Marker(map).apply {
            position = GeoPoint(lat, lon)
            this.title = title
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(marker)
        map.invalidate()
    }

    // ---------- Optional: draw a route using OSRM (no key) ----------
    private fun drawRoute(start: GeoPoint, end: GeoPoint) {
        val map = binding.mapView
        val roadManager: RoadManager = OSRMRoadManager(this, packageName)
        // OSRM is free, no API key; be mindful of fair-use
        Thread {
            val road: Road = roadManager.getRoad(arrayListOf(start, end))
            runOnUiThread {
                if (road.mStatus == Road.STATUS_OK) {
                    val polyline: Polyline = RoadManager.buildRoadOverlay(road)
                    map.overlays.add(polyline)
                    map.invalidate()
                } else {
                    Toast.makeText(this, "Route error: ${road.mStatus}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}
