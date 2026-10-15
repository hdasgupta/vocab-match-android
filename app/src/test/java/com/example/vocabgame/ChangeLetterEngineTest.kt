package com.example.vocabgame

import com.example.vocabgame.logic.ChangeLetterEngine
import com.example.vocabgame.logic.ComputerPlayResult
import com.example.vocabgame.logic.UserPlayResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChangeLetterEngineTest {

    private lateinit var dictionary: Set<String>
    private lateinit var engine: ChangeLetterEngine

    @Before
    fun setUp() {
        dictionary = setOf(
            "COLD", "CORD", "CARD", "WARD", "WARM", "WORM", 
            "WORD", "WOOD", "GOOD", "FOOD", "FOLD", "GOLD"
        )
        engine = ChangeLetterEngine(dictionary)
    }

    @Test
    fun testOneLetterDifferent() {
        assertTrue(ChangeLetterEngine.isOneLetterDifferent("COLD", "CORD")) // L -> R
        assertTrue(ChangeLetterEngine.isOneLetterDifferent("COLD", "GOLD")) // C -> G
        assertTrue(ChangeLetterEngine.isOneLetterDifferent("COLD", "COAT") == false) // L, D -> A, T (2 diffs)
        assertTrue(ChangeLetterEngine.isOneLetterDifferent("COLD", "COLD") == false) // 0 diffs
    }

    @Test
    fun testUserPlayWord_SuccessAwardsTwoPointsAndAttachesMeaning() {
        engine.startNewGame("COLD")
        assertEquals("COLD", engine.currentWord)
        assertEquals(0, engine.userPoints)
        assertEquals(1, engine.chainHistory.size)
        assertTrue("Starting word must have a meaning", engine.chainHistory[0].meaning.isNotEmpty())

        // User plays CORD (differs by 1 letter)
        val result = engine.playUserWord("CORD")
        assertTrue(result is UserPlayResult.Success)
        val success = result as UserPlayResult.Success
        assertEquals(2, success.pointsAwarded) // +2 points per word
        assertEquals(2, success.totalPoints)
        assertEquals("CORD", engine.currentWord)
        assertEquals(2, engine.chainHistory.size)
        assertTrue("User played word must have a meaning", engine.chainHistory[1].meaning.isNotEmpty())
        assertEquals(engine.chainHistory[1].meaning, success.meaning)
    }

    @Test
    fun testUserPlayWord_RejectsDuplicateWord() {
        engine.startNewGame("COLD")
        engine.playUserWord("CORD") // success

        // Attempt to play COLD again
        val duplicateResult = engine.playUserWord("COLD")
        assertTrue(duplicateResult is UserPlayResult.Error)
        val error = duplicateResult as UserPlayResult.Error
        assertTrue(error.message.contains("already been used"))
    }

    @Test
    fun testUserPlayWord_RejectsTwoLetterDifference() {
        engine.startNewGame("COLD")
        // CARD differs by 2 letters (O->A, L->R)
        val result = engine.playUserWord("CARD")
        assertTrue(result is UserPlayResult.Error)
        val error = result as UserPlayResult.Error
        assertTrue(error.message.contains("exactly one letter"))
    }

    @Test
    fun testComputerTurn_FindsValidMoveAndAttachesMeaning() {
        engine.startNewGame("COLD")
        engine.playUserWord("CORD") // user turn

        val compResult = engine.playComputerTurn()
        assertTrue(compResult is ComputerPlayResult.Played)
        val played = compResult as ComputerPlayResult.Played
        assertTrue(played.computerWord in listOf("CARD", "WORD"))
        assertEquals(played.computerWord, engine.currentWord)
        assertTrue("Computer played word must have a meaning", played.meaning.isNotEmpty())
    }

    @Test
    fun testComputerSurrender_WhenNoValidWordsRemain() {
        // Restricted dictionary where start word has zero 1-letter neighbours
        val isolatedDict = setOf("COLD", "WARM", "TREE")
        val isolatedEngine = ChangeLetterEngine(isolatedDict)
        isolatedEngine.startNewGame("COLD")

        val result = isolatedEngine.playComputerTurn()
        assertTrue(result is ComputerPlayResult.Surrendered)
        assertTrue(isolatedEngine.isGameOver)
    }
}
