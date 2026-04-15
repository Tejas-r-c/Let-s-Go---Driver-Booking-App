package com.example.letsgo.utils

import android.content.Context
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker
import android.util.Log
import kotlin.math.*

object SimpleRouteHelper {

    private var currentPolyline: Polyline? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null

    /**
     * Draw a simple straight line between start and end on the map.
     * This is NOT a turn-by-turn route — just a visual line + markers.
     */
    fun drawStraightLineRoute(context: Context, mapView: MapView, start: GeoPoint, end: GeoPoint) {
        try {
            // remove previous
            clearRoute(mapView)

            // create polyline consisting of only start and end
            val poly = Polyline(mapView).apply {
                // two point line (you can add intermediate points if you want curved look)
                setPoints(listOf(start, end))
                outlinePaint.strokeWidth = 6f
                outlinePaint.isAntiAlias = true
            }

            // create markers
            startMarker = Marker(mapView).apply {
                position = start
                title = "From"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            endMarker = Marker(mapView).apply {
                position = end
                title = "To"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }

            // add overlays and refresh map on UI thread
            mapView.post {
                mapView.overlays.add(poly)
                mapView.overlays.add(startMarker)
                mapView.overlays.add(endMarker)
                currentPolyline = poly

                // center and zoom heuristics
                try {
                    val midLat = (start.latitude + end.latitude) / 2.0
                    val midLon = (start.longitude + end.longitude) / 2.0
                    val mid = GeoPoint(midLat, midLon)
                    mapView.controller.setCenter(mid)

                    val distanceKm = estimateDistanceKm(start, end)
                    val zoom = when {
                        distanceKm < 0.5 -> 16.5
                        distanceKm < 2.0 -> 15.0
                        distanceKm < 10.0 -> 13.5
                        distanceKm < 30.0 -> 11.5
                        else -> 8.0
                    }
                    mapView.controller.setZoom(zoom)
                } catch (e: Exception) {
                    Log.w("SimpleRouteHelper", "zoom/center failed: ${e.message}")
                }

                mapView.invalidate()
            }
        } catch (e: Exception) {
            Log.w("SimpleRouteHelper", "drawStraightLineRoute failed: ${e.message}", e)
        }
    }

    /** Remove the line and markers from the map. */
    fun clearRoute(mapView: MapView) {
        try {
            currentPolyline?.let { mapView.overlays.remove(it) }
            startMarker?.let { mapView.overlays.remove(it) }
            endMarker?.let { mapView.overlays.remove(it) }
            currentPolyline = null
            startMarker = null
            endMarker = null
            mapView.post { mapView.invalidate() }
        } catch (e: Exception) {
            Log.w("SimpleRouteHelper", "clearRoute failed: ${e.message}", e)
        }
    }

    // Haversine distance approximation in kilometers
    private fun estimateDistanceKm(a: GeoPoint, b: GeoPoint): Double {
        val R = 6371.0 // Earth radius km
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val sinDLat = sin(dLat / 2.0)
        val sinDLon = sin(dLon / 2.0)
        val aa = sinDLat * sinDLat + cos(lat1) * cos(lat2) * sinDLon * sinDLon
        val c = 2.0 * atan2(sqrt(aa), sqrt(1.0 - aa))
        return R * c
    }

    // Expose a helper if needed by activity (returns km)
    fun estimateDistanceKmPublic(a: GeoPoint, b: GeoPoint): Double = estimateDistanceKm(a, b)
}
