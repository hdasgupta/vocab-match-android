package com.example.vocabgame

import com.example.vocabgame.data.WordDictionary
import com.example.vocabgame.data.WordMeanings
import com.example.vocabgame.logic.GameEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineTest {

    private lateinit var dictionary: Set<String>
    private lateinit var engine: GameEngine

    @Before
    fun setUp() {
        dictionary = setOf("BEAR", "BIRD", "BOOK", "MOON", "WORD", "WOOD", "GAME")
        engine = GameEngine(dictionary)
    }

    @Test
    fun testMatchingLetterCount_DistinctLetters() {
        // BEAR vs BIRD -> 'B' and 'R' match (2 letters)
        val match = GameEngine.countMatchingLetters("BEAR", "BIRD")
        assertEquals(2, match)

        val exact = GameEngine.countExactPositions("BEAR", "BIRD")
        assertEquals(1, exact) // 'B' at pos 0
    }

    @Test
    fun testMatchingLetterCount_DuplicateLetters() {
        // BOOK vs MOON:
        // 'B': 0, 'O': 2 (in both), 'K': 0 -> total 2 matching
        val match = GameEngine.countMatchingLetters("BOOK", "MOON")
        assertEquals(2, match)
    }

    @Test
    fun testScoreCalculation_Rule100DividedByGuesses() {
        engine.setCustomSecretWord("WORD")

        // Guess 1: incorrect
        val r1 = engine.evaluateGuess("GAME")
        assertFalse(r1.isCorrect)
        assertFalse(engine.isGameOver)

        // Guess 2: correct
        val r2 = engine.evaluateGuess("WORD")
        assertTrue(r2.isCorrect)
        assertTrue(engine.isGameOver)

        // Points = 100 / 2 = 50.0
        val points = engine.calculatePoints()
        assertEquals(50.0, points, 0.001)
    }

    @Test
    fun testScoreCalculation_WithHintPenaltyHalved() {
        engine.setCustomSecretWord("WORD")

        engine.evaluateGuess("GAME") // Guess 1
        engine.evaluateGuess("WORD") // Guess 2 (win)

        // Standard points = 100 / 2 = 50.0
        // With hint penalty (-50%) = 25.0
        val pointsWithHint = engine.calculatePoints(isHintUsed = true)
        assertEquals(25.0, pointsWithHint, 0.001)
    }

    @Test
    fun testScoreCalculation_FirstGuessWin() {
        engine.setCustomSecretWord("WORD")
        engine.evaluateGuess("WORD")
        assertEquals(100.0, engine.calculatePoints(), 0.001)
    }

    @Test
    fun testScoreCalculation_FourGuessesWin() {
        engine.setCustomSecretWord("WORD")
        engine.evaluateGuess("BEAR")
        engine.evaluateGuess("BIRD")
        engine.evaluateGuess("BOOK")
        engine.evaluateGuess("WORD")
        assertEquals(4, engine.guessCount)
        assertEquals(25.0, engine.calculatePoints(), 0.001)
    }

    @Test
    fun testGameForfeit() {
        engine.setCustomSecretWord("BIRD")
        engine.evaluateGuess("BEAR")
        assertFalse(engine.isGameOver)

        engine.forfeitGame()
        assertTrue(engine.isGameOver)
    }

    @Test
    fun testWordDictionaryAcceptsCommonEverydayWords() {
        val testWords = listOf("CATS", "DOGS", "BOYS", "CARS", "PENS", "DAYS", "EGGS", 
                               "TREE", "BIRD", "FISH", "COME", "WENT", "SAID", "LOOK", 
                               "BOOK", "COOK", "TAKE", "GIVE", "FIND", "HELP", "GOOD")
        for (w in testWords) {
            assertTrue("Word $w should be valid in WordDictionary", WordDictionary.isValidWord(w))
        }
    }

    @Test
    fun testWordMeanings_DoesNotRevealWord() {
        val sampleWords = listOf("BIRD", "TREE", "BOOK", "FISH", "MOON", "STAR", "TIME", "HOME", "LOVE")
        for (w in sampleWords) {
            val meaning = WordMeanings.getMeaning(w)
            assertFalse("Clue for $w must not mention the word itself", meaning.contains(w, ignoreCase = true))
            assertTrue("Clue for $w should be descriptive and long", meaning.length > 30)
        }
    }
}
