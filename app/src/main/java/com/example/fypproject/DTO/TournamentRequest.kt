package com.example.fypproject.DTO

data class TournamentRequest(
    val name: String,
    val seasonId: Long,
    val sportsId: Long,
    val username: String,
    val startDate: String,
    val endDate: String,
    val playerType: String,
    val tournamentType: String,
    val tournamentStage: String
)
