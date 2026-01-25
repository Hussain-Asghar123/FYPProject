package com.example.fypproject.DTO

data class PlayerDto(
    val id: Long,
    val name: String,
    val playerRole: String? = null,
    val username: String? = null,
    val playerRequests: List<Any> = emptyList()
)
