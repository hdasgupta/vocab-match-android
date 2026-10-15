package com.example.vocabgame.model

enum class ClueDirection {
    ACROSS, DOWN
}

data class CrosswordClue(
    val number: Int,
    val direction: ClueDirection,
    val word: String,
    val clueText: String,
    val startRow: Int,
    val startCol: Int
)

data class CrosswordCellState(
    val row: Int,
    val col: Int,
    val solutionChar: Char,
    var userChar: Char? = null,
    var isExposed: Boolean = false, // Exposing via hint sets readonly
    var isCorrectlyGuessedByUser: Boolean = false,
    val cellNumber: Int? = null // 1, 2, 3 or null
) {
    val isSolved: Boolean
        get() = isExposed || isCorrectlyGuessedByUser || (userChar != null && userChar == solutionChar)
}

data class CrosswordGameStats(
    val totalGames: Int,
    val totalCorrectGuesses: Int,
    val totalHintsTaken: Int,
    val totalPoints: Int,
    val averageCorrectGuessesPerGame: Double,
    val averagePointsPerGame: Double
)

data class CrosswordRecord(
    val id: Long,
    val across1: String,
    val across2: String,
    val down1: String,
    val down2: String,
    val correctGuesses: Int,
    val hintsTaken: Int,
    val points: Int,
    val isCompleted: Boolean,
    val timestamp: Long
)
