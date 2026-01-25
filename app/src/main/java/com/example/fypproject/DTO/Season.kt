package com.example.fypproject.DTO

data class Season(
    val id: Long,
    val name: String?,
    val sportsOffered: List<Sport>?
)