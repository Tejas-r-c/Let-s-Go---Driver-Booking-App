package com.example.letsgo.models

data class ApiResponse(
    val success: Boolean,
    val message: String?,
    val name: String?,
    val userId: String? = null,
    val driverId: String? = null   // ⭐ ADD THIS
)

