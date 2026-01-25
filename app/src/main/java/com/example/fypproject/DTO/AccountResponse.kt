package com.example.fypproject.DTO

data class AccountResponse(
    val id: Long,
    val name: String,
    val username: String,
    val password: String,
    val role: String
)
