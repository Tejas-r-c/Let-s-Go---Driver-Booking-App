package com.example.letsgo.models

import com.example.letsgo.utils.FareBreakdown

data class VehicleFareItem(
    val vehicle: VehicleType,
    val fare: FareBreakdown
)
