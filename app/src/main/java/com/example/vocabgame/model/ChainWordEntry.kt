package com.example.vocabgame.model

enum class PlayerType {
    USER,
    COMPUTER
}

/**
 * Represents a single word played in the "Change a Letter" word chain.
 */
data class ChainWordEntry(
    val stepNumber: Int,
    val word: String,
    val player: PlayerType,
    val pointsAwarded: Int
)
