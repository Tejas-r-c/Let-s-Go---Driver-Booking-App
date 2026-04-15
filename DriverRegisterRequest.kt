package com.example.letsgo.models

data class DriverRegisterRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val license: String,
    val vehicleType: String,
    val vehicleNumber: String
)
