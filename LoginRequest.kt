package com.example.letsgo.models

data class LoginRequest(
    val email: String,
    val password: String,
    val role: String // e.g. "USER" or "DRIVER"
)
