package com.example.letsgo.models

data class RideResponse(
    val success: Boolean,
    val rideId: String? = null,
    val message: String? = null
)
