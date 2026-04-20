package com.example.fypproject.ScoringDTO

data class LudoScoreDTO(
    val team1HomeRuns: Int?    = null,
    val team2HomeRuns: Int?    = null,
    val team1Captures: Int?    = null,
    val team2Captures: Int?    = null,
    val status: String?        = null,
    val comment: String?       = null,
    val matchStartTime: Long?  = null,
    val ludoEvents: List<LudoEvent>? = null
)

data class LudoEvent(
    val id: Long?              = null,
    val eventType: String?     = null,
    val teamName: String?      = null,
    val playerName: String?    = null,
    val eventTimeSeconds: Int? = null
)