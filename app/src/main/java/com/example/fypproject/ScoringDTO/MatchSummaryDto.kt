package com.example.fypproject.ScoringDTO

import java.io.Serializable

data class MatchSummaryDto(
    val result: String?,
    val team1Name: String?,
    val team1Runs: Int,
    val team1Wickets: Int,
    val team1Overs: String?,
    val team2Name: String?,
    val team2Runs: Int,
    val team2Wickets: Int,
    val team2Overs: String?,
    val manOfTheMatch: String?,
    val topBatsmen1: List<BatsmanPerformer>?,
    val topBowlers1: List<BowlerPerformer>?,
    val topBatsmen2: List<BatsmanPerformer>?,
    val topBowlers2: List<BowlerPerformer>?
) : Serializable
data class BatsmanPerformer(
    val playerName: String?,
    val runs: Int
) : Serializable

data class BowlerPerformer(
    val playerName: String?,
    val wickets: Int,
    val runsConceded: Int
) : Serializable

