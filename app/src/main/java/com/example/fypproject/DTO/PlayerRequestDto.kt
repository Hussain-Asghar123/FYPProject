package com.example.fypproject.DTO

data class PlayerRequestDto(
    val teamName: String,
    val teamCreatorName: String,
    val requestId: Long,
    val status: String
)

data class TeamRequestDto(
    val teamName: String,
    val tournamentName: String,
    val requestId: Long,
    val players: List<PlayerInfo>,
    val CaptainName: String,
    val status: String
)

data class PlayerInfo(
    val name: String
)