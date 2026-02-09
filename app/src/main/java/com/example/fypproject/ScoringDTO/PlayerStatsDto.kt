package com.example.fypproject.ScoringDTO

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PlayerStatsDto(
    @SerializedName("playerId") val playerId: Long? = null,
    @SerializedName("playerName") val playerName: String? = null,


    @SerializedName("runs") var runs: Int = 0,
    @SerializedName("ballsFaced") var ballsFaced: Int = 0,
    @SerializedName("fours") var fours: Int = 0,
    @SerializedName("sixes") var sixes: Int = 0,


    @SerializedName("wickets") var wickets: Int = 0,
    @SerializedName("runsConceded") var runsConceded: Int = 0,
    @SerializedName("ballsBowled") var ballsBowled: Int = 0,
    @SerializedName("economy") var economy: Double? = null,
    @SerializedName("bowlingAverage") var bowlingAverage: Double? = null,


    @SerializedName("pomCount") var pomCount: Int = 0,
    @SerializedName("compositeScore") var compositeScore: Double = 0.0
) : Serializable