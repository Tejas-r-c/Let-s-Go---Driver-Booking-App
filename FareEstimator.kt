package com.example.letsgo.utils


enum class RideType {
    QUICK_RIDE,
    TRIP_RIDE
}

object FareEstimator {
    /**
     * Simple fare estimator.
     * - rideType: keep for future use (pricing tiers)
     * - distanceKm: kilometers
     * - baseFare: per-ride fixed base
     * - perKmFare: per-km rate
     * - driverBaseFare: extra fixed driver fee (optional)
     * - taxPercent: percent (0.0 - 100.0)
     */

    fun estimateFare(
        rideType: RideType,
        distanceKm: Double,
        baseFare: Double,
        perKmFare: Double,
        driverBaseFare: Double = 0.0,
        taxPercent: Double = 0.0
    ): FareBreakdown {
        val distanceFare = perKmFare * distanceKm
        val subtotal = baseFare + distanceFare + driverBaseFare
        val taxAmount = subtotal * (taxPercent / 100.0)
        val total = subtotal + taxAmount
        return FareBreakdown(
            baseFare = baseFare,
            distanceFare = distanceFare,
            driverBaseFare = driverBaseFare,
            subtotal = subtotal,
            taxAmount = taxAmount,
            totalFare = total
        )
    }
}
