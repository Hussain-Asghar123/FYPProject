package com.example.fypproject.ScoringDTO

data class BadmintionScoreDTO(
    val team1Points: Int?     = null,
    val team2Points: Int?     = null,
    val team1Games: Int?     = null,
    val team2Games: Int?     = null,
    val currentGame: Int?    = null,
    val gamesToWin: Int?     = null,
    val pointsPerGame: Int?  = null,
    val maxPoints: Int?      = null,
    val pointsToWin: Int?    = null,
    val status: String?      = null,
    val comment: String?     = null,
    val gameStartTime: Long? = null,
    val badmintonEvents: List<BadmintonEvent>? = null
)

data class BadmintonEvent(
    val id: Long?              = null,
    val eventType: String?     = null,
    val teamName: String?      = null,
    val playerName: String?    = null,
    val eventTimeSeconds: Int? = null,
    val gameNumber: Int?       = null,
    val scoreSnapshot: String? = null
)