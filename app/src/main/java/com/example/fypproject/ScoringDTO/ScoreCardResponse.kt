package com.example.fypproject.ScoringDTO

import java.io.Serializable


data class ScorecardResponse(
    val batsmanScores: List<BatsmanScore> = emptyList(),
    val bowlerScores: List<BowlerScore> = emptyList(),
    val extras: Int = 0,
    val totalRuns: Int = 0,
    val overs: Int = 0,
    val balls: Int = 0,
    val fallOfWickets: List<FallOfWicket>? = null,
    val partnerships:  List<Partnership>?  = null
) :Serializable

data class BatsmanScore(
    val name: String = "",
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val strikeRate: Double = 0.0
): Serializable

data class BowlerScore(
    val name: String = "",
    val overs: Int = 0,
    val economy: Double = 0.0,
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val ballsBowled: Int = 0
) : Serializable

data class FallOfWicket(
    val score:        Int,
    val wicketNumber: Int,
    val playerName:   String,
    val over:         String
)

data class Partnership(
    val batter1:  String,
    val batter2:  String,
    val runs:     Int,
    val balls:    Int,
    val isNotOut: Boolean = false
)
