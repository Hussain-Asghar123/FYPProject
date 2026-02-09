package com.example.fypproject.ScoringDTO

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ScoreDTO(
    @SerializedName("matchId") var matchId: Long? = null,
    @SerializedName("inningsId") var inningsId: Long? = null,
    @SerializedName("teamId") var teamId: Long? = null,
    @SerializedName("batsmanId") var batsmanId: Long? = null,
    @SerializedName("nonStrikerId") var nonStrikerId: Long? = null,
    @SerializedName("fielderId") var fielderId: Long? = null,
    @SerializedName("bowlerId") var bowlerId: Long? = null,
    @SerializedName("outPlayerId") var outPlayerId: Long? = null,
    @SerializedName("newPlayerId") var newPlayerId: Long? = null,
    @SerializedName("mediaId") var mediaId: Long? = null,

    @SerializedName("runs") var runs: Int = 0,
    @SerializedName("overs") var overs: Int = 0,
    @SerializedName("balls") var balls: Int = 0,
    @SerializedName("wickets") var wickets: Int = 0,
    @SerializedName("target") var target: Int = 0,
    @SerializedName("status") var status: String = "LIVE",
    @SerializedName("crr") var crr: Double = 0.0,
    @SerializedName("rrr") var rrr: Double = 0.0,

    @SerializedName("extraType") var extraType: String? = null,
    @SerializedName("runsOnThisBall") var runsOnThisBall: Int = 0,
    @SerializedName("extrasThisBall") var extrasThisBall: Int = 0,
    @SerializedName("extra") var extra: Int = 0,

    @SerializedName("event") var event: String = "",
    @SerializedName("eventType") var eventType: String? = null,
    @SerializedName("dismissalType") var dismissalType: String? = null,
    @SerializedName("comment") var comment: String? = null,

    @SerializedName("isLegal") var isLegal: Boolean = true,
    @SerializedName("undo") var undo: Boolean = false,


    @SerializedName("four") var four: Boolean=false,
    @SerializedName("six") var six: Boolean=false,



    @SerializedName("firstInnings") var firstInnings: Boolean = true,

    @SerializedName("batsman1Stats") var batsman1Stats: PlayerStatsDto? = null,
    @SerializedName("batsman2Stats") var batsman2Stats: PlayerStatsDto? = null,
    @SerializedName("bowlerStats") var bowlerStats: PlayerStatsDto? = null
) : Serializable