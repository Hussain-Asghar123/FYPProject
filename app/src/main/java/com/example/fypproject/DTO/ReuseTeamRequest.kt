package com.example.fypproject.DTO

data class ReuseTeamRequest(
    val sourceTeamId: Long,
    val targetTournamentId: Long,
    val creatorPlayerId: Long
)

data class ReuseTeamResponse(
    val invitesSent: Int
)

data class TeamHistoryDto(
    val teamId: Long,
    val teamName: String,
    val tournamentName: String,
    val sport: String,
    val playerCount: Int
)