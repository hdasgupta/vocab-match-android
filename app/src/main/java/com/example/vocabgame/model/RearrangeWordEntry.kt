package com.example.vocabgame.model

/**
 * Represents an individual anagram word entry in the "Rearrange" list.
 */
data class RearrangeWordEntry(
    val word: String,
    val pointsAwarded: Int,
    val meaning: String,
    val isFound: Boolean = true
)
