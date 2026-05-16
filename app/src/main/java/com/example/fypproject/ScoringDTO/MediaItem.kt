package com.example.fypproject.ScoringDTO

import java.io.Serializable

data class MediaItem(
    val id: Long = 0,
    val url: String = "",
    val fileType: String? = null,   // "image/jpeg", "video/mp4", etc.
    val comment: String? = null,
    val matchId: Long? = null,
    val ballId: Long? = null
) : Serializable
