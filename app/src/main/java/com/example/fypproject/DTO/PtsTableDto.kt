package com.example.fypproject.DTO

data class PtsTableDto(
    val id: Long,
    val tournamentId: Long,
    val teamId: Long,
    val winnerId: Long,
    val loserId: Long,
    val teamName: String,
    val played: Int,
    val wins: Int,
    val losses: Int,
    val points: Int,
    val nrr: Double,

    val sport: String? = null,       // "futsal" or "cricket"
    val draws: Int? = null,
    val goalsFor: Int? = null,
    val goalsAgainst: Int? = null,
    val goalDifference: Int? = null
)