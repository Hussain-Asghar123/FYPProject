package com.example.fypproject.DTO

data class TournamentOverviewResponse(
    val teams: Int,
    val playerType: String,
    val startDate: String,
    val top: List<PointsTableItem>
)
data class PointsTableItem(
    val name: String,
    val points: Int
)
