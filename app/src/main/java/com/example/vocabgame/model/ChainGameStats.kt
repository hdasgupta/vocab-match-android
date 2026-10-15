package com.example.vocabgame.model

/**
 * Aggregated statistics for the "Change a Letter" game mode.
 *
 * @property totalGames Total games played in Change a Letter.
 * @property aggregatedPoints Total accumulated points earned by user (+2 per valid word).
 * @property totalChainLength Sum of all word chain lengths across all played games.
 * @property averageChainCount Average word chain count per game (totalChainLength / totalGames).
 */
data class ChainGameStats(
    val totalGames: Int,
    val aggregatedPoints: Int,
    val totalChainLength: Int,
    val averageChainCount: Double
)
