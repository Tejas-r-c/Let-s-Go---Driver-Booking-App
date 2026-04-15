package com.example.letsgo.utils

import android.content.Context
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * Tiny wrapper so other code can call RouteHelper.drawStraightLineRoute(...) consistently.
 * Currently delegates to SimpleRouteHelper (straight-line). Swap implementation here later.
 */
object RouteHelper {
    fun drawRoute(context: Context, mapView: MapView, start: GeoPoint, end: GeoPoint, callback: ((Any?, Any?)->Unit)? = null) {
        SimpleRouteHelper.drawStraightLineRoute(context, mapView, start, end)
        // callback isn't providing extra info for straight line; keep signature to match previous usage
        callback?.invoke(null, null)
    }

    fun clearRoute(mapView: MapView) = SimpleRouteHelper.clearRoute(mapView)

    fun secondsToEtaLabel(durationSeconds: Double?): String = if (durationSeconds == null || durationSeconds <= 0.0) "--" else {
        val secs = durationSeconds.toLong()
        val hours = secs / 3600
        val minutes = (secs % 3600) / 60
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun roadDistanceLabelKm(km: Double): String = if (km >= 1.0) String.format("%.2f km", km) else String.format("%.0f m", km * 1000)
}
