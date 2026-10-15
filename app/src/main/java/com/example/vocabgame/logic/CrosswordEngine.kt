package com.example.vocabgame.logic

import com.example.vocabgame.data.WordMeanings
import com.example.vocabgame.model.ClueDirection
import com.example.vocabgame.model.CrosswordCellState
import com.example.vocabgame.model.CrosswordClue
import java.util.Locale
import kotlin.random.Random

sealed class CrosswordLetterResult {
    data class Correct(val newlyAwarded: Boolean, val pointsEarned: Int, val isPuzzleCompleted: Boolean) : CrosswordLetterResult()
    data class Incorrect(val char: Char) : CrosswordLetterResult()
    object CellReadOnly : CrosswordLetterResult()
}

/**
 * Game engine for the 4x4 minimal-space Crossword Puzzle.
 *
 * Layout:
 * - 4 4-letter words forming an interlocking 4x4 perimeter frame (12 active cells).
 * - Horizontal: 1 Across (Row 0, Cols 0..3) and 3 Across (Row 3, Cols 0..3).
 * - Vertical: 1 Down (Col 0, Rows 0..3) and 2 Down (Col 3, Rows 0..3).
 * - Intersections: (0,0), (0,3), (3,0), (3,3).
 *
 * Scoring:
 * - +20 points for each correct letter guessed by the user.
 * - -10 points penalty for each hint taken (revealing a blank cell as readonly).
 */
class CrosswordEngine(private val wordDictionary: Set<String>) {

    // Active 4-letter words
    private val validWords: List<String>
    private val wordsByFirstAndLast: Map<Pair<Char, Char>, List<String>>
    private val wordsByFirst: Map<Char, List<String>>
    private val curatedWords: List<String>

    val cells = mutableMapOf<Pair<Int, Int>, CrosswordCellState>()
    var clues = listOf<CrosswordClue>()
        private set

    var across1: String = ""
        private set
    var across2: String = ""
        private set
    var down1: String = ""
        private set
    var down2: String = ""
        private set

    var points: Int = 0
        private set
    var correctGuessesCount: Int = 0
        private set
    var hintsTaken: Int = 0
        private set
    var isGameOver: Boolean = false
        private set

    init {
        validWords = wordDictionary.filter { it.length == 4 && it.all { c -> c in 'A'..'Z' } }
        
        val byFl = mutableMapOf<Pair<Char, Char>, MutableList<String>>()
        val byF = mutableMapOf<Char, MutableList<String>>()

        for (w in validWords) {
            val fl = Pair(w.first(), w.last())
            byFl.getOrPut(fl) { mutableListOf() }.add(w)
            byF.getOrPut(w.first()) { mutableListOf() }.add(w)
        }

        wordsByFirstAndLast = byFl
        wordsByFirst = byF

        // Prioritize common curated words with rich definitions
        curatedWords = validWords.filter { WordMeanings.hasMeaning(it) }
    }

    fun startNewGame(customWords: List<String>? = null): Boolean {
        cells.clear()
        points = 0
        correctGuessesCount = 0
        hintsTaken = 0
        isGameOver = false

        val tuple = if (!customWords.isNullOrEmpty() && customWords.size == 4) {
            val a1 = customWords[0].uppercase(Locale.US)
            val a2 = customWords[1].uppercase(Locale.US)
            val d1 = customWords[2].uppercase(Locale.US)
            val d2 = customWords[3].uppercase(Locale.US)
            CrosswordTuple(a1, a2, d1, d2)
        } else {
            generateRandomPuzzle() ?: return false
        }

        across1 = tuple.across1
        across2 = tuple.across2
        down1 = tuple.down1
        down2 = tuple.down2

        buildCells(tuple)
        buildClues(tuple)
        return true
    }

    private fun generateRandomPuzzle(): CrosswordTuple? {
        // Try up to 30 times with curated words first for best vocabulary learning
        val pool = if (curatedWords.size >= 10) curatedWords.shuffled() else validWords.shuffled()

        for (a1 in pool.take(40)) {
            val d1Candidates = wordsByFirst[a1[0]]?.filter { it != a1 }?.shuffled() ?: continue
            val d2Candidates = wordsByFirst[a1[3]]?.filter { it != a1 }?.shuffled() ?: continue

            for (d1 in d1Candidates.take(15)) {
                for (d2 in d2Candidates.take(15)) {
                    if (d2 == d1) continue
                    val neededFl = Pair(d1[3], d2[3])
                    val a2Candidates = wordsByFirstAndLast[neededFl]?.filter { it != a1 && it != d1 && it != d2 }
                    if (!a2Candidates.isNullOrEmpty()) {
                        val a2 = a2Candidates.random()
                        return CrosswordTuple(across1 = a1, across2 = a2, down1 = d1, down2 = d2)
                    }
                }
            }
        }

        // Fallback: search across all valid words
        for (a1 in validWords.shuffled().take(30)) {
            val d1Candidates = wordsByFirst[a1[0]]?.filter { it != a1 }?.shuffled() ?: continue
            val d2Candidates = wordsByFirst[a1[3]]?.filter { it != a1 }?.shuffled() ?: continue

            for (d1 in d1Candidates.take(10)) {
                for (d2 in d2Candidates.take(10)) {
                    if (d2 == d1) continue
                    val neededFl = Pair(d1[3], d2[3])
                    val a2Candidates = wordsByFirstAndLast[neededFl]?.filter { it != a1 && it != d1 && it != d2 }
                    if (!a2Candidates.isNullOrEmpty()) {
                        val a2 = a2Candidates.random()
                        return CrosswordTuple(across1 = a1, across2 = a2, down1 = d1, down2 = d2)
                    }
                }
            }
        }
        return null
    }

    private fun buildCells(tuple: CrosswordTuple) {
        // Row 0: Across 1 (4 cells)
        cells[Pair(0, 0)] = CrosswordCellState(0, 0, tuple.across1[0], cellNumber = 1)
        cells[Pair(0, 1)] = CrosswordCellState(0, 1, tuple.across1[1], cellNumber = null)
        cells[Pair(0, 2)] = CrosswordCellState(0, 2, tuple.across1[2], cellNumber = null)
        cells[Pair(0, 3)] = CrosswordCellState(0, 3, tuple.across1[3], cellNumber = 2)

        // Row 1: Down 1 and Down 2
        cells[Pair(1, 0)] = CrosswordCellState(1, 0, tuple.down1[1], cellNumber = null)
        cells[Pair(1, 3)] = CrosswordCellState(1, 3, tuple.down2[1], cellNumber = null)

        // Row 2: Down 1 and Down 2
        cells[Pair(2, 0)] = CrosswordCellState(2, 0, tuple.down1[2], cellNumber = null)
        cells[Pair(2, 3)] = CrosswordCellState(2, 3, tuple.down2[2], cellNumber = null)

        // Row 3: Across 2 (4 cells)
        cells[Pair(3, 0)] = CrosswordCellState(3, 0, tuple.across2[0], cellNumber = 3)
        cells[Pair(3, 1)] = CrosswordCellState(3, 1, tuple.across2[1], cellNumber = null)
        cells[Pair(3, 2)] = CrosswordCellState(3, 2, tuple.across2[2], cellNumber = null)
        cells[Pair(3, 3)] = CrosswordCellState(3, 3, tuple.across2[3], cellNumber = null)
    }

    private fun buildClues(tuple: CrosswordTuple) {
        clues = listOf(
            CrosswordClue(
                number = 1,
                direction = ClueDirection.ACROSS,
                word = tuple.across1,
                clueText = WordMeanings.getMeaning(tuple.across1),
                startRow = 0,
                startCol = 0
            ),
            CrosswordClue(
                number = 3,
                direction = ClueDirection.ACROSS,
                word = tuple.across2,
                clueText = WordMeanings.getMeaning(tuple.across2),
                startRow = 3,
                startCol = 0
            ),
            CrosswordClue(
                number = 1,
                direction = ClueDirection.DOWN,
                word = tuple.down1,
                clueText = WordMeanings.getMeaning(tuple.down1),
                startRow = 0,
                startCol = 0
            ),
            CrosswordClue(
                number = 2,
                direction = ClueDirection.DOWN,
                word = tuple.down2,
                clueText = WordMeanings.getMeaning(tuple.down2),
                startRow = 0,
                startCol = 3
            )
        )
    }

    /**
     * Handles user typing a letter in cell (row, col).
     * Awards +20 points when a correct letter is guessed for that cell by the user.
     */
    fun enterLetter(row: Int, col: Int, char: Char): CrosswordLetterResult {
        val cell = cells[Pair(row, col)] ?: return CrosswordLetterResult.Incorrect(char)
        if (cell.isExposed) {
            return CrosswordLetterResult.CellReadOnly
        }

        val upper = char.uppercaseChar()
        cell.userChar = upper

        val isCorrect = (upper == cell.solutionChar)
        if (isCorrect) {
            val newlyAwarded = !cell.isCorrectlyGuessedByUser
            if (newlyAwarded) {
                cell.isCorrectlyGuessedByUser = true
                correctGuessesCount++
                points += 20
            }
            val completed = isPuzzleCompleted()
            if (completed) {
                isGameOver = true
            }
            return CrosswordLetterResult.Correct(
                newlyAwarded = newlyAwarded,
                pointsEarned = if (newlyAwarded) 20 else 0,
                isPuzzleCompleted = completed
            )
        } else {
            return CrosswordLetterResult.Incorrect(upper)
        }
    }

    fun clearLetter(row: Int, col: Int): Boolean {
        val cell = cells[Pair(row, col)] ?: return false
        if (cell.isExposed) return false
        cell.userChar = null
        return true
    }

    /**
     * Exposes one random blank/unsolved cell as readonly.
     * Deducts 10 points for taking the hint.
     */
    fun exposeRandomHint(): CrosswordCellState? {
        if (isGameOver) return null

        // Blank/unsolved cells: cells where isExposed is false and userChar != solutionChar
        val unsolvedCells = cells.values.filter { !it.isExposed && it.userChar != it.solutionChar }
        val targetCell = if (unsolvedCells.isNotEmpty()) {
            unsolvedCells.random()
        } else {
            // If user filled letters but without hints, find any non-exposed cell
            cells.values.filter { !it.isExposed }.let { if (it.isNotEmpty()) it.random() else null }
        } ?: return null

        targetCell.userChar = targetCell.solutionChar
        targetCell.isExposed = true
        hintsTaken++
        points -= 10

        if (isPuzzleCompleted()) {
            isGameOver = true
        }

        return targetCell
    }

    fun isPuzzleCompleted(): Boolean {
        if (cells.isEmpty()) return false
        return cells.values.all { it.isSolved }
    }

    fun getRemainingBlankCount(): Int {
        return cells.values.count { !it.isSolved }
    }

    fun restoreState(
        a1: String,
        a2: String,
        d1: String,
        d2: String,
        cellData: List<CrosswordCellState>,
        pts: Int,
        correctCount: Int,
        hints: Int,
        gameOver: Boolean
    ) {
        across1 = a1
        across2 = a2
        down1 = d1
        down2 = d2
        points = pts
        correctGuessesCount = correctCount
        hintsTaken = hints
        isGameOver = gameOver

        cells.clear()
        for (c in cellData) {
            cells[Pair(c.row, c.col)] = c
        }
        buildClues(CrosswordTuple(a1, a2, d1, d2))
    }

    companion object {
        fun isActiveCell(row: Int, col: Int): Boolean {
            return (row in 0..3 && col in 0..3) && (row == 0 || row == 3 || col == 0 || col == 3)
        }
    }

    private data class CrosswordTuple(
        val across1: String,
        val across2: String,
        val down1: String,
        val down2: String
    )
}
