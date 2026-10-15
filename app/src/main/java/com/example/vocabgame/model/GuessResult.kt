package com.example.vocabgame.model

/**
 * Represents the outcome of a single user guess.
 *
 * @property guessNumber The sequential number of the guess in the current game.
 * @property guessedWord The 4-letter word submitted by the user.
 * @property matchingCount The count of letters in the guess matching the secret word (multiset intersection).
 * @property exactPositions The count of letters in the exact correct position.
 * @property isCorrect True if the guess matches the secret word completely.
 * @property meaning The dictionary definition / meaning of the guessed word.
 */
data class GuessResult(
    val guessNumber: Int,
    val guessedWord: String,
    val matchingCount: Int,
    val exactPositions: Int,
    val isCorrect: Boolean,
    val meaning: String = ""
)
