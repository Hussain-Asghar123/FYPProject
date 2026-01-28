package com.example.fypproject.DTO

data class MatchDTO(
    val id: Long? = null,
    val tournamentId: Long? = null,
    val team1Id: Long? = null,
    val team2Id: Long? = null,
    val team1Name: String? = null,
    val team2Name: String? = null,
    val scorerId: String? = null,
    val status: String? = null,
    val venue: String? = null,
    val date: String? = null,
    val time: String? = null,
    var tossWinnerId: Long? = null,
    var decision: String? = null,
    val sportId: Long? = null,
    val overs: Int? = null
)
