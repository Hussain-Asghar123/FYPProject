package com.example.fypproject.DTO

data class MatchDetail(
    val id: Long,
    val tournamentId: Long,
    val tournamentName: String,
    val team1Id: Long,
    val team1Name: String,
    val team2Id: Long,
    val team2Name: String,
    val status: String,
    val venue: String?,
    val date: String?,
    val time: String?
)
