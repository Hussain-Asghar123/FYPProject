package com.example.fypproject.ScoringDTO

import java.io.Serializable

data class Ball(
    val id: Long,
    val overBall: String?,
    val event: String?,
    val runs: Int?,
    val extra: Int?,
    val extraType: String?,
    val eventType: String?,
    val bowlerName: String?,
    val batsmanName: String?,
    val nonStrikerName: String?,
    val isWicket: Boolean?,
    val isBoundary: Boolean?,
    val dismissalType: String?,
    val outPlayerName: String?,
    val fielderName: String?,
    val comment: String?
): Serializable

data class BallResponse(
    val balls: List<Ball>
)
