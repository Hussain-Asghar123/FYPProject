package com.example.fypproject.DTO

data class UpdateAccountRequest(
    val name: String,
    val username: String,
    val password: String?,
    val role: String?
)