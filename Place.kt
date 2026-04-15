package com.example.letsgo.models

data class Place(
    val address: String,
    val coordinates: List<Double>? = null // [lng, lat]
)