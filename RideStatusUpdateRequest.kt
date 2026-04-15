package com.example.letsgo.models

data class RideStatusUpdateRequest(
    val rideId: String,
    val status: String
)
