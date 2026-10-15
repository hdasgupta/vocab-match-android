package com.example.vocabgame

import com.example.vocabgame.logic.RearrangeEngine
import com.example.vocabgame.logic.RearrangeSubmitResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RearrangeEngineTest {

    private lateinit var dictionary: Set<String>
    private lateinit var engine: RearrangeEngine

    @Before
    fun setUp() {
        // Dictionary containing anagram group with 4 words: STOP, POST, POTS, TOPS
        // plus non-anagram words
        dictionary = setOf(
            "STOP", "POST", "POTS", "TOPS",
            "CARE", "RACE", "ACRE",
            "COLD", "WARM", "TREE"
        )
        engine = RearrangeEngine(dictionary)
    }

    @Test
    fun testSignatureGenerator() {
        assertEquals("OPST", RearrangeEngine.getSignature("STOP"))
        assertEquals("OPST", RearrangeEngine.getSignature("POST"))
        assertEquals("ACER", RearrangeEngine.getSignature("CARE"))
    }

    @Test
    fun testBaseWordGuaranteesAtLeastTwoOtherAnagrams() {
        val baseWord = engine.startNewGame()
        assertTrue("Base word must have at least 2 other anagrams", engine.allPossibleAnagrams.size >= 2)
        assertFalse("Possible anagrams must not include the base word itself", engine.allPossibleAnagrams.contains(baseWord))
    }

    @Test
    fun testScoringDoublingRule_TwentyFiveFiftyOneHundred() {
        engine.startNewGame("STOP")
        assertEquals("STOP", engine.baseWord)
        assertEquals(3, engine.allPossibleAnagrams.size) // POST, POTS, TOPS

        // 1st word: 25 points
        val r1 = engine.submitWord("POST")
        assertTrue(r1 is RearrangeSubmitResult.Success)
        val s1 = r1 as RearrangeSubmitResult.Success
        assertEquals(25, s1.pointsEarnedThisWord)
        assertEquals(25, s1.totalPoints)
        assertEquals(25, engine.calculatePoints())
        assertFalse(s1.isAllFound)

        // 2nd word: doubles to 50 points total (+25 pts delta)
        val r2 = engine.submitWord("POTS")
        assertTrue(r2 is RearrangeSubmitResult.Success)
        val s2 = r2 as RearrangeSubmitResult.Success
        assertEquals(25, s2.pointsEarnedThisWord) // 50 - 25 = 25
        assertEquals(50, s2.totalPoints)
        assertEquals(50, engine.calculatePoints())
        assertFalse(s2.isAllFound)

        // 3rd word: doubles to 100 points total (+50 pts delta)
        val r3 = engine.submitWord("TOPS")
        assertTrue(r3 is RearrangeSubmitResult.Success)
        val s3 = r3 as RearrangeSubmitResult.Success
        assertEquals(50, s3.pointsEarnedThisWord) // 100 - 50 = 50
        assertEquals(100, s3.totalPoints)
        assertEquals(100, engine.calculatePoints())
        assertTrue("All 3 words found should complete the game", s3.isAllFound)
        assertTrue(engine.isGameOver)
    }

    @Test
    fun testSubmitWord_RejectsBaseWordItself() {
        engine.startNewGame("STOP")
        val result = engine.submitWord("STOP")
        assertTrue(result is RearrangeSubmitResult.Error)
        val error = result as RearrangeSubmitResult.Error
        assertTrue(error.message.contains("is the base word"))
    }

    @Test
    fun testSubmitWord_RejectsDuplicateSubmissions() {
        engine.startNewGame("STOP")
        engine.submitWord("POST")

        val duplicate = engine.submitWord("POST")
        assertTrue(duplicate is RearrangeSubmitResult.Error)
        val error = duplicate as RearrangeSubmitResult.Error
        assertTrue(error.message.contains("already found"))
    }

    @Test
    fun testSubmitWord_RejectsNonAnagram() {
        engine.startNewGame("STOP")
        val result = engine.submitWord("COLD")
        assertTrue(result is RearrangeSubmitResult.Error)
        val error = result as RearrangeSubmitResult.Error
        assertTrue(error.message.contains("does not use the exact 4 letters"))
    }
}
