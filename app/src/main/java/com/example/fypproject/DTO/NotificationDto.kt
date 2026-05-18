package com.example.fypproject.DTO

import com.google.gson.annotations.SerializedName

data class NotificationDto(
    val id: Long,
    val title: String?,
    val message: String?,
    @SerializedName("isRead") // ← Bug 2 fix bhi yahan hai
    val isRead: Boolean = false,
    val type: String?,
    val createdAt: String?  // ← String rakho, LocalDateTime nahi
)

