package com.example.fypproject.DTO

data class MilestoneDto(
    val title: String,
    val subtitle: String?,
    val emoji: String,
    val color: String    // "gold" | "red" | "blue" | "green"
)