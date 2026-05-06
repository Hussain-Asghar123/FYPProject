package com.example.fypproject.ScoringDTO

data class ChessScoreDTO(
    val status: String?          = null,
    val resultType: String?      = null,
    val isDraw: Boolean?         = null,
    val winnerTeamId: Long?      = null,
    val matchStartTime: Long?    = null,
    val comment: String?         = null,
    val chessEvents: List<ChessEvent>? = null
)

data class ChessEvent(
    val id: Long?              = null,
    val eventType: String?     = null,
    val teamName: String?      = null,
    val playerName: String?    = null,
    val moveNotation: String?  = null,
    val moveNumber: Int?       = null,
    val eventTimeSeconds: Int? = null
)