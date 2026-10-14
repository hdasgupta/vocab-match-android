package com.example.vocabgame.model

/**
 * Aggregated statistics retrieved from the SQLite database.
 *
 * @property totalGames Total completed games count.
 * @property aggregatedPoints Total accumulated points across all games.
 * @property totalGuesses Total guesses made across all completed games.
 * @property averageGuesses Average number of guesses per game (totalGuesses / totalGames).
 * @property averagePoints Average points earned per game (aggregatedPoints / totalGames).
 */
data class GameStats(
    val totalGames: Int,
    val aggregatedPoints: Double,
    val totalGuesses: Int,
    val averageGuesses: Double,
    val averagePoints: Double
)
