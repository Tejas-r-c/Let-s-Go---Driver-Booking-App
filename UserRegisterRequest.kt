package com.example.letsgo.models

data class UserRegisterRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String
)
