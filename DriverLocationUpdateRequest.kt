package com.example.letsgo.models

data class DriverLocationUpdateRequest(
    val rideId: String,
    val driverId: String,
    val lat: Double,
    val lng: Double
)
