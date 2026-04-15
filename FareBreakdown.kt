package com.example.letsgo.utils

data class FareBreakdown(
    val baseFare: Double,
    val distanceFare: Double,
    val driverBaseFare: Double,
    val subtotal: Double,
    val taxAmount: Double,
    val totalFare: Double
)
