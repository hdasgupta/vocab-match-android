package com.example.vocabgame.logic

import com.example.vocabgame.data.WordMeanings
import com.example.vocabgame.model.RearrangeWordEntry
import java.util.Locale
import kotlin.random.Random

sealed class RearrangeSubmitResult {
    data class Success(
        val word: String,
        val pointsEarnedThisWord: Int,
        val totalPoints: Int,
        val wordsFoundCount: Int,
        val totalPossibleCount: Int,
        val isAllFound: Boolean,
        val meaning: String
    ) : RearrangeSubmitResult()

    data class Error(val message: String) : RearrangeSubmitResult()
}

/**
 * Game engine for the "Rearrange" (Anagram) vocabulary game.
 *
 * Rules:
 * - Computer chooses a 4-letter word that can be rearranged into at least 2 other valid words
 *   (total anagram set size >= 3 words).
 * - User rearranges the 4 letters to form valid words.
 * - 1st word found = 25 points.
 * - Every additional word doubles the points (2 words = 50 pts, 3 words = 100 pts, etc.).
 * - $Points = 25 \times 2^{N-1}$ for $N \ge 1$ words found.
 */
class RearrangeEngine(private val wordDictionary: Set<String>) {

    // Maps sorted letter signatures to sets of valid anagram words
    private val eligibleAnagramGroups: Map<String, List<String>>

    var baseWord: String = ""
        private set

    val allPossibleAnagrams = mutableSetOf<String>()
    val foundWords = mutableSetOf<String>()

    var isGameOver: Boolean = false
        private set

    init {
        val groups = mutableMapOf<String, MutableList<String>>()
        for (w in wordDictionary) {
            if (w.length == 4 && w.all { it in 'A'..'Z' }) {
                val sig = getSignature(w)
                groups.getOrPut(sig) { mutableListOf() }.add(w)
            }
        }
        // Retain only groups with at least 3 words (1 base word + at least 2 other rearrangements)
        eligibleAnagramGroups = groups.filterValues { it.size >= 3 }
    }

    fun startNewGame(customBaseWord: String? = null): String {
        require(eligibleAnagramGroups.isNotEmpty()) { "No eligible anagram groups found" }

        foundWords.clear()
        allPossibleAnagrams.clear()
        isGameOver = false

        if (!customBaseWord.isNullOrBlank()) {
            val upper = customBaseWord.trim().uppercase(Locale.US)
            val sig = getSignature(upper)
            val wordsInGroup = eligibleAnagramGroups[sig] ?: listOf(upper)
            baseWord = upper
            allPossibleAnagrams.addAll(wordsInGroup.filter { it != baseWord })
        } else {
            val eligibleKeys = eligibleAnagramGroups.keys.toList()
            val chosenKey = eligibleKeys[Random.nextInt(eligibleKeys.size)]
            val wordsInGroup = eligibleAnagramGroups.getValue(chosenKey)
            
            // Pick a base word from the group
            baseWord = wordsInGroup[Random.nextInt(wordsInGroup.size)]
            allPossibleAnagrams.addAll(wordsInGroup.filter { it != baseWord })
        }

        return baseWord
    }

    fun restoreState(base: String, found: Set<String>, gameOver: Boolean) {
        baseWord = base.uppercase(Locale.US)
        foundWords.clear()
        foundWords.addAll(found)
        isGameOver = gameOver

        allPossibleAnagrams.clear()
        val sig = getSignature(baseWord)
        val wordsInGroup = eligibleAnagramGroups[sig] ?: listOf()
        allPossibleAnagrams.addAll(wordsInGroup.filter { it != baseWord })
    }

    /**
     * Submits a user's rearranged word and evaluates points.
     */
    fun submitWord(candidate: String): RearrangeSubmitResult {
        if (isGameOver) {
            return RearrangeSubmitResult.Error("Game is already completed. Start a new round!")
        }

        val upper = candidate.trim().uppercase(Locale.US)

        if (upper.length != 4) {
            return RearrangeSubmitResult.Error("Word must be exactly 4 letters.")
        }

        if (!upper.all { it in 'A'..'Z' }) {
            return RearrangeSubmitResult.Error("Only alphabetic letters are permitted.")
        }

        if (upper == baseWord) {
            return RearrangeSubmitResult.Error("'$upper' is the base word! Rearrange its letters to form a different word.")
        }

        if (foundWords.contains(upper)) {
            return RearrangeSubmitResult.Error("You already found '$upper'!")
        }

        if (getSignature(upper) != getSignature(baseWord)) {
            return RearrangeSubmitResult.Error("'$upper' does not use the exact 4 letters of '$baseWord'.")
        }

        if (!allPossibleAnagrams.contains(upper) && !wordDictionary.contains(upper)) {
            return RearrangeSubmitResult.Error("'$upper' is not recognized as a valid English word.")
        }

        val previousPoints = calculatePoints()
        foundWords.add(upper)
        val newPoints = calculatePoints()
        val deltaPoints = newPoints - previousPoints

        val isAllFound = (foundWords.size == allPossibleAnagrams.size)
        if (isAllFound) {
            isGameOver = true
        }

        val meaning = WordMeanings.getMeaning(upper)

        return RearrangeSubmitResult.Success(
            word = upper,
            pointsEarnedThisWord = deltaPoints,
            totalPoints = newPoints,
            wordsFoundCount = foundWords.size,
            totalPossibleCount = allPossibleAnagrams.size,
            isAllFound = isAllFound,
            meaning = meaning
        )
    }

    fun finishGame(): Int {
        isGameOver = true
        return calculatePoints()
    }

    /**
     * Calculates points based on words found:
     * 1 word = 25 pts
     * 2 words = 50 pts
     * 3 words = 100 pts
     * N words = 25 * 2^(N - 1) pts
     */
    fun calculatePoints(): Int {
        val count = foundWords.size
        if (count == 0) return 0
        var pts = 25
        for (i in 2..count) {
            pts *= 2
        }
        return pts
    }

    /**
     * Returns list of words that were missed by the user.
     */
    fun getMissedWords(): List<RearrangeWordEntry> {
        val missed = allPossibleAnagrams.filter { !foundWords.contains(it) }.sorted()
        return missed.map { word ->
            RearrangeWordEntry(
                word = word,
                pointsAwarded = 0,
                meaning = WordMeanings.getMeaning(word),
                isFound = false
            )
        }
    }

    /**
     * Returns list of words successfully found by the user with their point values and meanings.
     */
    fun getFoundWordEntries(): List<RearrangeWordEntry> {
        val list = mutableListOf<RearrangeWordEntry>()
        val foundList = foundWords.toList()
        for (i in foundList.indices) {
            val word = foundList[i]
            // Point contribution of this word
            val pts = if (i == 0) 25 else 25 * (1 shl (i - 1))
            list.add(
                RearrangeWordEntry(
                    word = word,
                    pointsAwarded = pts,
                    meaning = WordMeanings.getMeaning(word),
                    isFound = true
                )
            )
        }
        return list
    }

    companion object {
        fun getSignature(word: String): String {
            val chars = word.trim().uppercase(Locale.US).toCharArray()
            chars.sort()
            return String(chars)
        }
    }
}
