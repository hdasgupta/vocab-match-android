package com.example.vocabgame.logic

import com.example.vocabgame.data.WordMeanings
import com.example.vocabgame.model.GuessResult
import kotlin.random.Random

/**
 * Handles core game mechanics:
 * - Secret word selection.
 * - Validation of input guesses.
 * - Matching letter calculation (multiset intersection count).
 * - Exact position match calculation.
 * - Score calculation (100 / number of guesses).
 * - Meaning retrieval for each guessed word.
 */
class GameEngine(private val wordDictionary: Set<String>) {

    var secretWord: String = ""
        private set

    var guessCount: Int = 0
        private set

    var isGameOver: Boolean = false
        private set

    val guessHistory = mutableListOf<GuessResult>()

    /**
     * Initializes or restarts the game.
     * Optionally takes an explicit secret word (e.g. from WordDictionary's common secret list).
     */
    fun startNewGame(preferredSecret: String? = null): String {
        secretWord = if (!preferredSecret.isNullOrBlank()) {
            preferredSecret.trim().uppercase()
        } else {
            require(wordDictionary.isNotEmpty()) { "Dictionary cannot be empty" }
            val wordsList = wordDictionary.toList()
            wordsList[Random.nextInt(wordsList.size)].uppercase()
        }
        guessCount = 0
        isGameOver = false
        guessHistory.clear()
        return secretWord
    }

    /**
     * Restores complete game engine state across activity recreation / orientation changes.
     */
    fun restoreState(secret: String, count: Int, gameOver: Boolean, history: List<GuessResult>) {
        secretWord = secret.uppercase()
        guessCount = count
        isGameOver = gameOver
        guessHistory.clear()
        guessHistory.addAll(history)
    }

    /**
     * Specifically sets a secret word (useful for automated testing).
     */
    fun setCustomSecretWord(word: String) {
        secretWord = word.uppercase()
        guessCount = 0
        isGameOver = false
        guessHistory.clear()
    }

    /**
     * Ends the game immediately when the secret word is revealed by player forfeit.
     */
    fun forfeitGame() {
        isGameOver = true
    }

    /**
     * Verifies whether a given word is in the dictionary.
     */
    fun isValidDictionaryWord(word: String): Boolean {
        return wordDictionary.contains(word.trim().uppercase())
    }

    /**
     * Evaluates a user guess against the secret word and attaches the word's dictionary meaning.
     */
    fun evaluateGuess(guess: String): GuessResult {
        check(!isGameOver) { "Game is already over. Start a new game." }
        val upperGuess = guess.trim().uppercase()
        guessCount++

        val matchingCount = countMatchingLetters(secretWord, upperGuess)
        val exactPositions = countExactPositions(secretWord, upperGuess)
        val isCorrect = (upperGuess == secretWord)
        val meaning = WordMeanings.getMeaning(upperGuess)

        if (isCorrect) {
            isGameOver = true
        }

        val result = GuessResult(
            guessNumber = guessCount,
            guessedWord = upperGuess,
            matchingCount = matchingCount,
            exactPositions = exactPositions,
            isCorrect = isCorrect,
            meaning = meaning
        )
        guessHistory.add(result)
        return result
    }

    /**
     * Calculates the game points: 100 divided by number of guesses.
     */
    fun calculatePoints(isHintUsed: Boolean = false): Double {
        if (guessCount == 0) return 0.0
        val base = 100.0 / guessCount
        return if (isHintUsed) base / 2.0 else base
    }

    companion object {
        /**
         * Calculates the count of matching letters between secret and guess.
         * Uses multiset letter frequencies so duplicate letters are handled accurately.
         */
        fun countMatchingLetters(secret: String, guess: String): Int {
            val sFreq = IntArray(26)
            val gFreq = IntArray(26)

            for (c in secret.uppercase()) {
                if (c in 'A'..'Z') sFreq[c - 'A']++
            }
            for (c in guess.uppercase()) {
                if (c in 'A'..'Z') gFreq[c - 'A']++
            }

            var totalMatch = 0
            for (i in 0 until 26) {
                totalMatch += minOf(sFreq[i], gFreq[i])
            }
            return totalMatch
        }

        /**
         * Calculates how many letters are matching in the exact same index position.
         */
        fun countExactPositions(secret: String, guess: String): Int {
            var exact = 0
            val len = minOf(secret.length, guess.length)
            for (i in 0 until len) {
                if (secret[i].uppercaseChar() == guess[i].uppercaseChar()) {
                    exact++
                }
            }
            return exact
        }
    }
}
