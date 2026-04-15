package com.example.letsgo.models

data class RideDetailsResponse(
    val success: Boolean,
    val message: String?,
    val rideId: String?,
    val from: String?,
    val to: String?,
    val status: String?,
    val driverId: String?,
    val driverName: String?,
    val driverPhone: String?,
    val driverLat: Double?,
    val driverLng: Double?,

    // 🔥 ADD THESE FOR ROUTING
    val pickupLat: Double?,
    val pickupLng: Double?,
    val dropLat: Double?,
    val dropLng: Double?,

    val rideFare: Double?
)
