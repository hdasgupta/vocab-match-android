package com.example.vocabgame.logic

import com.example.vocabgame.model.ChainWordEntry
import com.example.vocabgame.model.PlayerType
import kotlin.random.Random

sealed class UserPlayResult {
    data class Success(val playedWord: String, val pointsAwarded: Int, val totalPoints: Int) : UserPlayResult()
    data class Error(val message: String) : UserPlayResult()
}

sealed class ComputerPlayResult {
    data class Played(val computerWord: String, val chainLength: Int) : ComputerPlayResult()
    data class Surrendered(val reason: String, val finalChainLength: Int, val userPoints: Int) : ComputerPlayResult()
}

/**
 * Game engine for the "Change a Letter" word chain game.
 * Rules:
 * - Computer starts with a meaningful 4-letter word.
 * - Player and computer alternate changing exactly 1 letter to form another valid word.
 * - Words cannot repeat in the chain.
 * - Player earns +2 points for each valid word created.
 * - Game ends when either player or computer surrenders because no words can be formed.
 */
class ChangeLetterEngine(private val wordDictionary: Set<String>) {

    var currentWord: String = ""
        private set

    var userPoints: Int = 0
        private set

    var isGameOver: Boolean = false
        private set

    val chainHistory = mutableListOf<ChainWordEntry>()
    val usedWords = mutableSetOf<String>()

    fun startNewGame(startingWord: String? = null): String {
        require(wordDictionary.isNotEmpty()) { "Dictionary cannot be empty" }
        chainHistory.clear()
        usedWords.clear()
        userPoints = 0
        isGameOver = false

        val initial = if (!startingWord.isNullOrBlank()) {
            startingWord.trim().uppercase()
        } else {
            val list = wordDictionary.toList()
            list[Random.nextInt(list.size)].uppercase()
        }

        currentWord = initial
        usedWords.add(initial)
        chainHistory.add(ChainWordEntry(stepNumber = 1, word = initial, player = PlayerType.COMPUTER, pointsAwarded = 0))

        return currentWord
    }

    fun restoreState(
        currWord: String,
        points: Int,
        gameOver: Boolean,
        history: List<ChainWordEntry>,
        used: Set<String>
    ) {
        currentWord = currWord.uppercase()
        userPoints = points
        isGameOver = gameOver
        chainHistory.clear()
        chainHistory.addAll(history)
        usedWords.clear()
        usedWords.addAll(used)
    }

    /**
     * User plays a word changing exactly 1 letter from currentWord.
     */
    fun playUserWord(candidate: String): UserPlayResult {
        if (isGameOver) {
            return UserPlayResult.Error("Game is already over. Start a new game.")
        }

        val upper = candidate.trim().uppercase()

        if (upper.length != 4) {
            return UserPlayResult.Error("Word must be exactly 4 letters.")
        }

        if (!upper.all { it in 'A'..'Z' }) {
            return UserPlayResult.Error("Word must contain alphabetic letters only.")
        }

        if (usedWords.contains(upper)) {
            return UserPlayResult.Error("'$upper' has already been used in this chain!")
        }

        if (!isOneLetterDifferent(currentWord, upper)) {
            return UserPlayResult.Error("You must change exactly one letter from '$currentWord'.")
        }

        if (!wordDictionary.contains(upper)) {
            return UserPlayResult.Error("'$upper' is not recognized as a valid English word.")
        }

        // Valid user play! +2 points
        userPoints += 2
        usedWords.add(upper)
        currentWord = upper
        chainHistory.add(ChainWordEntry(stepNumber = chainHistory.size + 1, word = upper, player = PlayerType.USER, pointsAwarded = 2))

        return UserPlayResult.Success(upper, 2, userPoints)
    }

    /**
     * Finds candidate words differing by 1 letter and plays a move, or surrenders if none found.
     */
    fun playComputerTurn(): ComputerPlayResult {
        if (isGameOver) {
            return ComputerPlayResult.Surrendered("Game is already over.", chainHistory.size, userPoints)
        }

        val candidates = findCandidates(currentWord)

        if (candidates.isEmpty()) {
            isGameOver = true
            return ComputerPlayResult.Surrendered(
                "Computer cannot make any word from '$currentWord' and surrenders! You win!",
                chainHistory.size,
                userPoints
            )
        }

        val chosen = candidates[Random.nextInt(candidates.size)]
        usedWords.add(chosen)
        currentWord = chosen
        chainHistory.add(ChainWordEntry(stepNumber = chainHistory.size + 1, word = chosen, player = PlayerType.COMPUTER, pointsAwarded = 0))

        return ComputerPlayResult.Played(chosen, chainHistory.size)
    }

    fun surrenderUser(): Int {
        isGameOver = true
        return userPoints
    }

    fun findCandidates(baseWord: String): List<String> {
        val candidates = mutableListOf<String>()
        val base = baseWord.uppercase()
        for (i in 0 until 4) {
            for (c in 'A'..'Z') {
                if (c != base[i]) {
                    val candidate = buildString {
                        append(base.substring(0, i))
                        append(c)
                        append(base.substring(i + 1))
                    }
                    if (wordDictionary.contains(candidate) && !usedWords.contains(candidate)) {
                        candidates.add(candidate)
                    }
                }
            }
        }
        return candidates
    }

    companion object {
        fun isOneLetterDifferent(w1: String, w2: String): Boolean {
            if (w1.length != 4 || w2.length != 4) return false
            var diff = 0
            for (i in 0 until 4) {
                if (w1[i].uppercaseChar() != w2[i].uppercaseChar()) {
                    diff++
                }
            }
            return diff == 1
        }
    }
}
