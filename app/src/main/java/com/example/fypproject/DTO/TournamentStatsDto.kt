package com.example.fypproject.DTO

data class TournamentStatsDto(
    val tournamentId: Long,
    val tournamentName: String?,
    val manOfTournament: PlayerAwardDto?,
    val bestBatsman: PlayerAwardDto?,
    val bestBowler: PlayerAwardDto?,
    val bestFielder: PlayerAwardDto?,
    val mostSixes: SixesStatDto?,
    val topRunScorers: List<TopBatsmanDto>,
    val topBowlers: List<TopBowlerDto>
)

data class PlayerAwardDto(
    val playerId: Long,
    val playerName: String,
    val awardType: String?,
    val points: Int?,
    val reason: String?
)

data class SixesStatDto(
    val playerId: Long,
    val playerName: String,
    val sixes: Int
)

data class TopBatsmanDto(
    val playerId: Long,
    val playerName: String,
    val runs: Int,
    val ballsFaced: Int,
    val fours: Int,
    val sixes: Int,
    val strikeRate: Int,
    val playerOfMatchCount: Int
)

data class TopBowlerDto(
    val playerId: Long,
    val playerName: String,
    val wickets: Int,
    val runs: Int,
    val ballsBowled: Int,
    val economy: Double,
    val runsConceded: Int,
    val playerOfMatchCount: Int
)