package com.example.fypproject.DTO

data class SeasonSportsRequest(
    val seasonId: Long,
    val sportsIds: List<Long>
)