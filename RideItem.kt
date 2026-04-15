package com.example.letsgo.models

import com.google.gson.annotations.SerializedName

data class RideItem(
    @SerializedName("_id")
    val rideId: String,

    val from: String,
    val to: String,
    val vehicleName: String?,
    val estimatedFare: Double?,
    val status: String,
    val driverId: String
)
