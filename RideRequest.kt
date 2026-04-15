package com.example.letsgo.models


data class RideRequest(
    val userEmail: String,
    val from: String,
    val to: String,
    val vehicleName: String,
    val estimatedFare: Double,


// new optional fields
    val userId: String? = null,
    val pickup: Place? = null,
    val dropoff: Place? = null,
    val vehicleType: String? = null
)

