package com.example.fypproject.DTO

data class TeamResponse(
    val teamId: Long,
    val teamStatus: String,
    val teamName: String,
    val players: List<Player>
)
data class Player(
    val name: String,
    val id: Int,
    val status: String
)

data class CreateTeamRequestDto(
    val name: String
)
data class CreateTeamResponseDto(
    val message: String,
    val teamId: Long,
    val name: String,
    val tournamentId: Long
)

data class PlayerRequest(
    val playerId: Long,
    val teamId: Long,
    val tournamentId: Long
)
data class TeamRequest(
    val teamId: Long,
    val playerId: Long
)

data class PlayerResponse(
    val playerId: Long,
    val username: String,
    val name: String,
)












