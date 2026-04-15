package com.example.letsgo.models

data class AcceptRideRequest(
    val rideId: String,
    val driverId: String,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val pickupLat: Double? = null,
    val pickupLng: Double? = null
)
