package com.example.fypproject.ScoringDTO

data class ChessScoreDTO(
    val team1Moves: Int?           = null,
    val team2Moves: Int?           = null,
    val team1Checks: Int?          = null,
    val team2Checks: Int?          = null,
    val totalMoves: Int?           = null,
    val status: String?            = null,
    val resultType: String?        = null,
    val isDraw: Boolean?           = null,
    val currentTurnTeamId: Long?   = null,
    val currentTurnTeamName: String? = null,
    val matchStartTime: Long?      = null,
    val comment: String?           = null,
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