package com.example.fypproject.DTO

data class MatchResponse(
    val id: Long? = null,
    val tournamentId: Long? = null,
    val tournamentName: String? = null,
    val team1Id: Long? = null,
    val team1Name: String? = null,
    val team2Id: Long? = null,
    val team2Name: String? = null,
    val status: String? = null,
    val venue: String? = null,
    val date: String? = null,
    val time: String? = null
)
