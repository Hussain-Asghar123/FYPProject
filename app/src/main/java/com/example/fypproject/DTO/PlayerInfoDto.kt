package com.example.fypproject.DTO

data class PlayerInfoDto(
    val playerName: String,
    val jerseyNumber: Int?,
    val profilePhotoUrl: String?,
    val sports: List<String>?,
    val teams: List<PlayerTeamInfo>?,
    val tournaments: List<PlayerTournamentInfo>?,
    val totalMatchesPlayed: Int
)

data class PlayerTeamInfo(
    val teamName: String,
    val tournamentName: String,
    val sport: String
)

data class PlayerTournamentInfo(
    val name: String,
    val status: String   // e.g. "ONGOING", "COMPLETED", "UPCOMING"
)