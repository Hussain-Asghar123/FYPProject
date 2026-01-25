package com.example.fypproject.DTO

data class TournamentStatsDto(
    val tournamentId: Long,
    val manOfTournamentId: Long?,
    val manOfTournamentName: String?,
    val highestScorerId: Long?,
    val highestScorerName: String?,
    val highestRuns: Int?,
    val bestBatsmanId: Long?,
    val bestBatsmanName: String?,
    val bestBatsmanRuns: Int?,
    val bestBowlerId: Long?,
    val bestBowlerName: String?,
    val bestBowlerWickets: Int?,
    val topBatsmen: List<PlayerPerformanceDto>,
    val topBowlers: List<PlayerPerformanceDto>
)
data class PlayerPerformanceDto(
    val playerId: Long,
    val playerName: String,
    val runs: Int,
    val ballsFaced: Int,
    val fours: Int,
    val sixes: Int,
    val wickets: Int,
    val runsConceded: Int,
    val ballsBowled: Int,
    val economy: Double?,
    val bowlingAverage: Double?,
    val pomCount: Int,
    val compositeScore: Int
)


