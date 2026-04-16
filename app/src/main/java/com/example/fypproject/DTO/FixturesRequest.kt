package com.example.fypproject.DTO

data class FixturesRequest(
    val tournamentId: Long,
    val scorerId: String?,
    val team1Id: Long,
    val team2Id: Long,
    val venue: String,
    val date: String,
    val time: String,
    val overs: Int
)