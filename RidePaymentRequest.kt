package com.example.letsgo.models

data class RidePaymentRequest(
    val rideId: String,
    val userId: String,
    val amount: Double,
    val paymentMethod: String // e.g. "CASH", "CARD", "UPI"
)
