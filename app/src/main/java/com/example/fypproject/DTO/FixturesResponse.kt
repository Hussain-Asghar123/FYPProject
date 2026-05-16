package com.example.fypproject.DTO

data class FixturesResponse(
    val id: Long,
    val tournamentId: Long,
    val tournamentName: String,
    val team1Id: Long,
    val team1Name: String,
    val team2Id: Long,
    val team2Name: String,
    val venue: String,
    val date: String,
    val time: String,
    val sportId: Long,
    val overs: Int,
    val status: String,
    val scorerId: String,
    val mediaScorerUsername: String,

    val winnerTeamId: Long? = null,   // Winner highlight karne ke liye
    val groupName: String? = null,     // Group stage badge ke liye
    val roundNumber: Int? = null       // Bracket round ke liye
)
