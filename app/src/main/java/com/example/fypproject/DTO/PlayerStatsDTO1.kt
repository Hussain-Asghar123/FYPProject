package com.example.fypproject.DTO

data class PlayeraStatsDTO1(
    val matchesPlayed: Int? = null,

    // Cricket
    val runsScored: Int? = null,
    val average: Double? = null,
    val strikeRate: Double? = null,
    val highestScore: Int? = null,
    val hundreds: Int? = null,
    val fifties: Int? = null,
    val fours: Int? = null,
    val sixes: Int? = null,
    val wicketsTaken: Int? = null,
    val economy: Double? = null,
    val bowlingAverage: Double? = null,
    val bestBowling: String? = null,
    val catches: Int? = null,

    // Futsal / Volleyball / Badminton / TT / Ludo / Chess
    val goals: Int? = null,
    val assists: Int? = null,
    val futsalFouls: Int? = null,
    val yellowCards: Int? = null,
    val redCards: Int? = null
)
