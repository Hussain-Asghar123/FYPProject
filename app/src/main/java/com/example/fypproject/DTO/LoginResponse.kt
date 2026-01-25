package com.example.fypproject.DTO

data class LoginResponse(
    val id: Long,
    val name: String?,
    val username: String,
    val password: String,
    val role: String,
    val playerId: Long,
    val seasonsCreated: List<Any>?,
    val tournamentsCreated: List<Any>?,
    val scoredMatches: List<Any>?
)

