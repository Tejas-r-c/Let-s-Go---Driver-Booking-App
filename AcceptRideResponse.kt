package com.example.letsgo.models

data class AcceptRideResponse(
    val success: Boolean,
    val rideId: String? = null,
    val message: String? = null
)

