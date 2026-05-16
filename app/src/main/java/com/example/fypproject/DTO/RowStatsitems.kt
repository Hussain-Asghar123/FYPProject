package com.example.fypproject.DTO

data class StatRowItem(
    val label: String,
    val val1: String,   // Formatted display value
    val val2: String,
    val p1Wins: Boolean,   // Green highlight
    val p2Wins: Boolean,
    val isTie: Boolean
)