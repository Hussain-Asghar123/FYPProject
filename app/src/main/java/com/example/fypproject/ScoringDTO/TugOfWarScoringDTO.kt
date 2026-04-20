package com.example.fypproject.ScoringDTO

data class TugOfWarScoreDTO(
    val team1Rounds: Int?     = null,
    val team2Rounds: Int?     = null,
    val currentRound: Int?    = null,
    val roundsToWin: Int?     = null,
    val totalRounds: Int?     = null,
    val status: String?       = null,
    val comment: String?      = null,
    val roundStartTime: Long? = null,
    val tugOfWarEvents: List<TugOfWarEvent>? = null
)

data class TugOfWarEvent(
    val id: Long?                  = null,
    val eventType: String?         = null,
    val winnerTeamName: String?    = null,
    val roundNumber: Int?          = null,
    val roundDurationSeconds: Int? = null
)