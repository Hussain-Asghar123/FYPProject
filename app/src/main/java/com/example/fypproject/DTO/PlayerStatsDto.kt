package com.example.fypproject.DTO

data class PlayerStatsDto(
    val playerId: Long,
    val playerName: String,
    val totalRuns: Int,
    val highest: Int,
    val ballsFaced: Int,
    val ballsBowled: Int,
    val runsConceded: Int,
    val strikeRate: Double,
    val economy: Double,
    val battingAvg: Double,
    val bowlingAverage: Double,
    val notOuts: Int,
    val matchesPlayed: Int,
    val wickets: Int,
    val fours: Int,
    val sixes: Int,
    val pomCount: Int
)

