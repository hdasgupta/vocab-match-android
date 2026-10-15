package com.example.vocabgame

import com.example.vocabgame.logic.CrosswordEngine
import com.example.vocabgame.logic.CrosswordLetterResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CrosswordEngineTest {

    private lateinit var dictionary: Set<String>
    private lateinit var engine: CrosswordEngine

    @Before
    fun setUp() {
        // Sample dictionary including known valid crossword set:
        // Across 1: BACK, Across 2: LEAP, Down 1: BALL, Down 2: KEEP
        dictionary = setOf(
            "BACK", "LEAP", "BALL", "KEEP",
            "BAKE", "EYES", "KEYS", "EARS",
            "STOP", "POST", "POTS", "TOPS",
            "COLD", "WARM", "TREE"
        )
        engine = CrosswordEngine(dictionary)
    }

    @Test
    fun testCrosswordPuzzleGenerationWithCustomTuple() {
        val success = engine.startNewGame(listOf("BACK", "LEAP", "BALL", "KEEP"))
        assertTrue("Game should initialize successfully", success)

        assertEquals("BACK", engine.across1)
        assertEquals("LEAP", engine.across2)
        assertEquals("BALL", engine.down1)
        assertEquals("KEEP", engine.down2)

        // 12 active cells
        assertEquals(12, engine.cells.size)

        // Intersections
        assertEquals('B', engine.cells[Pair(0, 0)]?.solutionChar)
        assertEquals('K', engine.cells[Pair(0, 3)]?.solutionChar)
        assertEquals('L', engine.cells[Pair(3, 0)]?.solutionChar)
        assertEquals('P', engine.cells[Pair(3, 3)]?.solutionChar)

        // Cell numbers
        assertEquals(1, engine.cells[Pair(0, 0)]?.cellNumber)
        assertEquals(2, engine.cells[Pair(0, 3)]?.cellNumber)
        assertEquals(3, engine.cells[Pair(3, 0)]?.cellNumber)
        assertEquals(null, engine.cells[Pair(1, 0)]?.cellNumber)

        // 4 clues: 1 Across, 3 Across, 1 Down, 2 Down
        assertEquals(4, engine.clues.size)
    }

    @Test
    fun testScoringTwentyPointsPerCorrectGuess() {
        engine.startNewGame(listOf("BACK", "LEAP", "BALL", "KEEP"))
        assertEquals(0, engine.points)
        assertEquals(0, engine.correctGuessesCount)

        // Enter correct letter in (0, 1) -> 'A'
        val result = engine.enterLetter(0, 1, 'A')
        assertTrue(result is CrosswordLetterResult.Correct)
        val correct = result as CrosswordLetterResult.Correct
        assertTrue(correct.newlyAwarded)
        assertEquals(20, correct.pointsEarned)
        assertEquals(20, engine.points)
        assertEquals(1, engine.correctGuessesCount)

        // Typing same letter again does not re-award points
        val repeatResult = engine.enterLetter(0, 1, 'A')
        assertTrue(repeatResult is CrosswordLetterResult.Correct)
        assertFalse((repeatResult as CrosswordLetterResult.Correct).newlyAwarded)
        assertEquals(20, engine.points)
        assertEquals(1, engine.correctGuessesCount)
    }

    @Test
    fun testIncorrectGuessDoesNotAwardPoints() {
        engine.startNewGame(listOf("BACK", "LEAP", "BALL", "KEEP"))

        val result = engine.enterLetter(0, 1, 'Z')
        assertTrue(result is CrosswordLetterResult.Incorrect)
        assertEquals(0, engine.points)
        assertEquals(0, engine.correctGuessesCount)
    }

    @Test
    fun testHintDeductsTenPointsAndMakesCellReadOnly() {
        engine.startNewGame(listOf("BACK", "LEAP", "BALL", "KEEP"))
        assertEquals(0, engine.points)
        assertEquals(0, engine.hintsTaken)

        val exposedCell = engine.exposeRandomHint()
        assertNotNull(exposedCell)
        assertTrue(exposedCell!!.isExposed)
        assertEquals(exposedCell.solutionChar, exposedCell.userChar)
        assertEquals(-10, engine.points)
        assertEquals(1, engine.hintsTaken)

        // Attempting to overwrite exposed cell should be rejected as ReadOnly
        val editResult = engine.enterLetter(exposedCell.row, exposedCell.col, 'X')
        assertTrue(editResult is CrosswordLetterResult.CellReadOnly)
        assertEquals(exposedCell.solutionChar, exposedCell.userChar) // uncorrupted
    }

    @Test
    fun testPuzzleCompletion() {
        engine.startNewGame(listOf("BACK", "LEAP", "BALL", "KEEP"))
        assertFalse(engine.isGameOver)

        // Solve all 12 cells
        for (cell in engine.cells.values) {
            engine.enterLetter(cell.row, cell.col, cell.solutionChar)
        }

        assertTrue(engine.isPuzzleCompleted())
        assertTrue(engine.isGameOver)
        assertEquals(12, engine.correctGuessesCount)
        assertEquals(240, engine.points) // 12 * 20 = 240
    }

    @Test
    fun testRandomGenerationValidPuzzle() {
        val success = engine.startNewGame()
        assertTrue("Random puzzle generation should succeed", success)
        assertEquals(12, engine.cells.size)
        assertTrue(engine.across1.length == 4)
        assertTrue(engine.across2.length == 4)
        assertTrue(engine.down1.length == 4)
        assertTrue(engine.down2.length == 4)

        // Test intersection integrity
        assertEquals(engine.across1[0], engine.down1[0])
        assertEquals(engine.across1[3], engine.down2[0])
        assertEquals(engine.down1[3], engine.across2[0])
        assertEquals(engine.across2[3], engine.down2[3])
    }
}
