package com.example.vocabgame.model

/**
 * Aggregated statistics for the "Rearrange" anagram game mode.
 */
data class RearrangeGameStats(
    val totalGames: Int,
    val totalPoints: Int,
    val totalWordsFound: Int,
    val averageWordsPerGame: Double,
    val averagePointsPerGame: Double
)
