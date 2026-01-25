package com.example.fypproject.DTO

data class TournamentUpdateRequest(
    var name: String? = null,
    var username: String? = null,
    var playerType: String? = null,
    var tournamentType: String? = null,
    var tournamentStage: String? = null,
    var startDate: String? = null,
    var endDate: String? = null,
    var seasonId: Long? = null,
    var sportsId: Long? = null
)
