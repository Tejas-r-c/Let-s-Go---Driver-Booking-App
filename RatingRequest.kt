package com.example.letsgo.models

data class RatingRequest(
    val rideId: String,
    val userId: String,
    val driverId: String,
    val rating: Int,     // 1..5
    val comment: String? = null
)
