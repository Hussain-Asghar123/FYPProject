package com.example.fypproject.DTO

data class PlayerStatsDto(
    val playerId: Long = -1L,
    val playerName: String? = null,
    val sport: String? = null,

    // Shared
    val matchesPlayed: Int = 0,
    val pomCount: Int = 0,

    // Cricket
    val totalRuns: Int = 0,
    val highest: Int = 0,
    val ballsFaced: Int = 0,
    val ballsBowled: Int = 0,
    val runsConceded: Int = 0,
    val strikeRate: Double = 0.0,
    val economy: Double = 0.0,
    val battingAvg: Double = 0.0,
    val bowlingAverage: Double = 0.0,
    val notOuts: Int = 0,
    val wickets: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val cricketMatchesPlayed: Int = 0,

    // Futsal
    val futsalMatchesPlayed: Int = 0,
    val goals: Int = 0,
    val assists: Int = 0,
    val futsalFouls: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0
)

