package com.example.fypproject.DTO

data class SubstituteRequest(
    val inningsId: Long?,
    val outPlayerId: Long,
    val inPlayerId: Long,
    val teamId: Long
)