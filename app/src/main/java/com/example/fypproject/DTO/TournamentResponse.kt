package com.example.fypproject.DTO

data class TournamentResponse(
    val id: Long,
    val name: String,
    val startDate: String?,
    val endDate: String?,
    val playerType: String?,
    val tournamentType: String?,
    val tournamentStage: String?,
    val username: String?,
    val sportsId: Long? = null,
    val seasonId: Long? = null
)
