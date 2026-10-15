package com.example.vocabgame.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.vocabgame.model.ChainGameStats
import com.example.vocabgame.model.GameStats
import com.example.vocabgame.model.CrosswordGameStats
import com.example.vocabgame.model.CrosswordRecord
import com.example.vocabgame.model.RearrangeGameStats
import com.example.vocabgame.model.RearrangeRecord

/**
 * SQLiteOpenHelper implementation utilizing Android's built-in SQLite database engine.
 *
 * Persists each game outcome and maintains aggregated statistics for all three game modes:
 * 1. "Guess Word" (Deduction vocabulary game)
 * 2. "Change a Letter" (Word chain game)
 * 3. "Rearrange" (Anagram discovery game)
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "vocab_game.db"
        const val DATABASE_VERSION = 5

        // ==========================================
        // 4. Crossword Tables
        // ==========================================
        const val TABLE_CROSSWORD_GAMES = "crossword_game_records"
        const val COL_CROSSWORD_ID = "id"
        const val COL_CROSSWORD_ACROSS1 = "across1"
        const val COL_CROSSWORD_ACROSS2 = "across2"
        const val COL_CROSSWORD_DOWN1 = "down1"
        const val COL_CROSSWORD_DOWN2 = "down2"
        const val COL_CROSSWORD_CORRECT_GUESSES = "correct_guesses"
        const val COL_CROSSWORD_HINTS_TAKEN = "hints_taken"
        const val COL_CROSSWORD_POINTS = "points"
        const val COL_CROSSWORD_COMPLETED = "is_completed"
        const val COL_CROSSWORD_TIMESTAMP = "timestamp"

        const val TABLE_CROSSWORD_AGGREGATED = "crossword_aggregated_stats"
        const val COL_CROSSWORD_AGG_ID = "id"
        const val COL_TOTAL_CROSSWORD_GAMES = "total_games"
        const val COL_TOTAL_CROSSWORD_CORRECT_GUESSES = "total_correct_guesses"
        const val COL_TOTAL_CROSSWORD_HINTS_TAKEN = "total_hints_taken"
        const val COL_TOTAL_CROSSWORD_POINTS = "total_points"

        // ==========================================
        // 1. Guess Word Tables
        // ==========================================
        const val TABLE_GAMES = "game_records"
        const val COL_ID = "id"
        const val COL_SECRET_WORD = "secret_word"
        const val COL_GUESS_COUNT = "guess_count"
        const val COL_POINTS = "points"
        const val COL_TIMESTAMP = "timestamp"

        const val TABLE_AGGREGATED = "aggregated_stats"
        const val COL_AGG_ID = "id"
        const val COL_TOTAL_GAMES = "total_games"
        const val COL_TOTAL_POINTS = "total_points"
        const val COL_TOTAL_GUESSES = "total_guesses"

        // ==========================================
        // 2. Change a Letter Tables
        // ==========================================
        const val TABLE_CHAIN_GAMES = "chain_game_records"
        const val COL_CHAIN_ID = "id"
        const val COL_CHAIN_LENGTH = "chain_length"
        const val COL_CHAIN_USER_WORDS = "user_words"
        const val COL_CHAIN_POINTS = "points"
        const val COL_CHAIN_WINNER = "winner"
        const val COL_CHAIN_TIMESTAMP = "timestamp"

        const val TABLE_CHAIN_AGGREGATED = "chain_aggregated_stats"
        const val COL_CHAIN_AGG_ID = "id"
        const val COL_TOTAL_CHAIN_GAMES = "total_games"
        const val COL_TOTAL_CHAIN_LENGTH = "total_chain_length"
        const val COL_TOTAL_CHAIN_USER_WORDS = "total_user_words"
        const val COL_TOTAL_CHAIN_POINTS = "total_points"

        // ==========================================
        // 3. Rearrange Tables
        // ==========================================
        const val TABLE_REARRANGE_GAMES = "rearrange_game_records"
        const val COL_REARRANGE_ID = "id"
        const val COL_REARRANGE_BASE_WORD = "base_word"
        const val COL_REARRANGE_WORDS_FOUND = "words_found_count"
        const val COL_REARRANGE_WORDS_CSV = "words_found_csv"
        const val COL_REARRANGE_WORDS_MISSED_CSV = "words_missed_csv"
        const val COL_REARRANGE_POSSIBLE_WORDS = "possible_words_count"
        const val COL_REARRANGE_POINTS = "points"
        const val COL_REARRANGE_TIMESTAMP = "timestamp"

        const val TABLE_REARRANGE_AGGREGATED = "rearrange_aggregated_stats"
        const val COL_REARRANGE_AGG_ID = "id"
        const val COL_TOTAL_REARRANGE_GAMES = "total_games"
        const val COL_TOTAL_REARRANGE_WORDS = "total_words_found"
        const val COL_TOTAL_REARRANGE_POINTS = "total_points"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Table 1: Guess Word game records
        db.execSQL("""
            CREATE TABLE $TABLE_GAMES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SECRET_WORD TEXT NOT NULL,
                $COL_GUESS_COUNT INTEGER NOT NULL,
                $COL_POINTS REAL NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL
            );
        """.trimIndent())

        // Table 2: Guess Word aggregated stats
        db.execSQL("""
            CREATE TABLE $TABLE_AGGREGATED (
                $COL_AGG_ID INTEGER PRIMARY KEY,
                $COL_TOTAL_GAMES INTEGER DEFAULT 0,
                $COL_TOTAL_POINTS REAL DEFAULT 0.0,
                $COL_TOTAL_GUESSES INTEGER DEFAULT 0
            );
        """.trimIndent())

        val initialValues = ContentValues().apply {
            put(COL_AGG_ID, 1)
            put(COL_TOTAL_GAMES, 0)
            put(COL_TOTAL_POINTS, 0.0)
            put(COL_TOTAL_GUESSES, 0)
        }
        db.insert(TABLE_AGGREGATED, null, initialValues)

        createChainTables(db)
        createRearrangeTables(db)
        createCrosswordTables(db)
    }

    private fun createChainTables(db: SQLiteDatabase) {
        // Table 3: Change a Letter individual game history
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CHAIN_GAMES (
                $COL_CHAIN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CHAIN_LENGTH INTEGER NOT NULL,
                $COL_CHAIN_USER_WORDS INTEGER NOT NULL,
                $COL_CHAIN_POINTS INTEGER NOT NULL,
                $COL_CHAIN_WINNER TEXT NOT NULL,
                $COL_CHAIN_TIMESTAMP INTEGER NOT NULL
            );
        """.trimIndent())

        // Table 4: Change a Letter aggregated totals
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CHAIN_AGGREGATED (
                $COL_CHAIN_AGG_ID INTEGER PRIMARY KEY,
                $COL_TOTAL_CHAIN_GAMES INTEGER DEFAULT 0,
                $COL_TOTAL_CHAIN_LENGTH INTEGER DEFAULT 0,
                $COL_TOTAL_CHAIN_USER_WORDS INTEGER DEFAULT 0,
                $COL_TOTAL_CHAIN_POINTS INTEGER DEFAULT 0
            );
        """.trimIndent())

        val chainInitial = ContentValues().apply {
            put(COL_CHAIN_AGG_ID, 1)
            put(COL_TOTAL_CHAIN_GAMES, 0)
            put(COL_TOTAL_CHAIN_LENGTH, 0)
            put(COL_TOTAL_CHAIN_USER_WORDS, 0)
            put(COL_TOTAL_CHAIN_POINTS, 0)
        }
        db.insertWithOnConflict(TABLE_CHAIN_AGGREGATED, null, chainInitial, SQLiteDatabase.CONFLICT_IGNORE)
    }

    private fun createRearrangeTables(db: SQLiteDatabase) {
        // Table 5: Rearrange historical game records
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_REARRANGE_GAMES (
                $COL_REARRANGE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_REARRANGE_BASE_WORD TEXT NOT NULL,
                $COL_REARRANGE_WORDS_FOUND INTEGER NOT NULL,
                $COL_REARRANGE_WORDS_CSV TEXT NOT NULL,
                $COL_REARRANGE_WORDS_MISSED_CSV TEXT NOT NULL DEFAULT '',
                $COL_REARRANGE_POSSIBLE_WORDS INTEGER NOT NULL,
                $COL_REARRANGE_POINTS INTEGER NOT NULL,
                $COL_REARRANGE_TIMESTAMP INTEGER NOT NULL
            );
        """.trimIndent())

        // Table 6: Rearrange aggregated totals
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_REARRANGE_AGGREGATED (
                $COL_REARRANGE_AGG_ID INTEGER PRIMARY KEY,
                $COL_TOTAL_REARRANGE_GAMES INTEGER DEFAULT 0,
                $COL_TOTAL_REARRANGE_WORDS INTEGER DEFAULT 0,
                $COL_TOTAL_REARRANGE_POINTS INTEGER DEFAULT 0
            );
        """.trimIndent())

        val rearrangeInitial = ContentValues().apply {
            put(COL_REARRANGE_AGG_ID, 1)
            put(COL_TOTAL_REARRANGE_GAMES, 0)
            put(COL_TOTAL_REARRANGE_WORDS, 0)
            put(COL_TOTAL_REARRANGE_POINTS, 0)
        }
        db.insertWithOnConflict(TABLE_REARRANGE_AGGREGATED, null, rearrangeInitial, SQLiteDatabase.CONFLICT_IGNORE)
    }

    private fun createCrosswordTables(db: SQLiteDatabase) {
        // Table 7: Crossword historical game records
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CROSSWORD_GAMES (
                $COL_CROSSWORD_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CROSSWORD_ACROSS1 TEXT NOT NULL,
                $COL_CROSSWORD_ACROSS2 TEXT NOT NULL,
                $COL_CROSSWORD_DOWN1 TEXT NOT NULL,
                $COL_CROSSWORD_DOWN2 TEXT NOT NULL,
                $COL_CROSSWORD_CORRECT_GUESSES INTEGER NOT NULL,
                $COL_CROSSWORD_HINTS_TAKEN INTEGER NOT NULL,
                $COL_CROSSWORD_POINTS INTEGER NOT NULL,
                $COL_CROSSWORD_COMPLETED INTEGER NOT NULL,
                $COL_CROSSWORD_TIMESTAMP INTEGER NOT NULL
            );
        """.trimIndent())

        // Table 8: Crossword aggregated totals
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CROSSWORD_AGGREGATED (
                $COL_CROSSWORD_AGG_ID INTEGER PRIMARY KEY,
                $COL_TOTAL_CROSSWORD_GAMES INTEGER DEFAULT 0,
                $COL_TOTAL_CROSSWORD_CORRECT_GUESSES INTEGER DEFAULT 0,
                $COL_TOTAL_CROSSWORD_HINTS_TAKEN INTEGER DEFAULT 0,
                $COL_TOTAL_CROSSWORD_POINTS INTEGER DEFAULT 0
            );
        """.trimIndent())

        val crosswordInitial = ContentValues().apply {
            put(COL_CROSSWORD_AGG_ID, 1)
            put(COL_TOTAL_CROSSWORD_GAMES, 0)
            put(COL_TOTAL_CROSSWORD_CORRECT_GUESSES, 0)
            put(COL_TOTAL_CROSSWORD_HINTS_TAKEN, 0)
            put(COL_TOTAL_CROSSWORD_POINTS, 0)
        }
        db.insertWithOnConflict(TABLE_CROSSWORD_AGGREGATED, null, crosswordInitial, SQLiteDatabase.CONFLICT_IGNORE)
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createChainTables(db)
        }
        if (oldVersion < 3) {
            createRearrangeTables(db)
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE $TABLE_REARRANGE_GAMES ADD COLUMN $COL_REARRANGE_WORDS_MISSED_CSV TEXT NOT NULL DEFAULT ''")
            } catch (e: Exception) {
                // Column might already exist
            }
        }
        if (oldVersion < 5) {
            createCrosswordTables(db)
        }
    }

    // ==========================================
    // 1. Guess Word Methods
    // ==========================================

    @Synchronized
    fun recordGame(secretWord: String, guessCount: Int, points: Double) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val gameValues = ContentValues().apply {
                put(COL_SECRET_WORD, secretWord)
                put(COL_GUESS_COUNT, guessCount)
                put(COL_POINTS, points)
                put(COL_TIMESTAMP, System.currentTimeMillis())
            }
            db.insert(TABLE_GAMES, null, gameValues)

            db.execSQL("""
                UPDATE $TABLE_AGGREGATED 
                SET $COL_TOTAL_GAMES = $COL_TOTAL_GAMES + 1,
                    $COL_TOTAL_POINTS = $COL_TOTAL_POINTS + ?,
                    $COL_TOTAL_GUESSES = $COL_TOTAL_GUESSES + ?
                WHERE $COL_AGG_ID = 1
            """.trimIndent(), arrayOf(points, guessCount))

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAggregatedStats(): GameStats {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_TOTAL_GAMES, $COL_TOTAL_POINTS, $COL_TOTAL_GUESSES FROM $TABLE_AGGREGATED WHERE $COL_AGG_ID = 1",
            null
        )

        var totalGames = 0
        var totalPoints = 0.0
        var totalGuesses = 0

        cursor.use {
            if (it.moveToFirst()) {
                totalGames = it.getInt(0)
                totalPoints = it.getDouble(1)
                totalGuesses = it.getInt(2)
            }
        }

        val averageGuesses = if (totalGames > 0) totalGuesses.toDouble() / totalGames else 0.0
        val averagePoints = if (totalGames > 0) totalPoints / totalGames else 0.0

        return GameStats(
            totalGames = totalGames,
            aggregatedPoints = totalPoints,
            totalGuesses = totalGuesses,
            averageGuesses = averageGuesses,
            averagePoints = averagePoints
        )
    }

    @Synchronized
    fun resetAllStats() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_GAMES, null, null)
            val resetValues = ContentValues().apply {
                put(COL_TOTAL_GAMES, 0)
                put(COL_TOTAL_POINTS, 0.0)
                put(COL_TOTAL_GUESSES, 0)
            }
            db.update(TABLE_AGGREGATED, resetValues, "$COL_AGG_ID = ?", arrayOf("1"))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ==========================================
    // 2. Change a Letter Methods
    // ==========================================

    @Synchronized
    fun recordChainGame(chainLength: Int, userWords: Int, points: Int, winner: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val recordValues = ContentValues().apply {
                put(COL_CHAIN_LENGTH, chainLength)
                put(COL_CHAIN_USER_WORDS, userWords)
                put(COL_CHAIN_POINTS, points)
                put(COL_CHAIN_WINNER, winner)
                put(COL_CHAIN_TIMESTAMP, System.currentTimeMillis())
            }
            db.insert(TABLE_CHAIN_GAMES, null, recordValues)

            db.execSQL("""
                UPDATE $TABLE_CHAIN_AGGREGATED
                SET $COL_TOTAL_CHAIN_GAMES = $COL_TOTAL_CHAIN_GAMES + 1,
                    $COL_TOTAL_CHAIN_LENGTH = $COL_TOTAL_CHAIN_LENGTH + ?,
                    $COL_TOTAL_CHAIN_USER_WORDS = $COL_TOTAL_CHAIN_USER_WORDS + ?,
                    $COL_TOTAL_CHAIN_POINTS = $COL_TOTAL_CHAIN_POINTS + ?
                WHERE $COL_CHAIN_AGG_ID = 1
            """.trimIndent(), arrayOf(chainLength, userWords, points))

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getChainAggregatedStats(): ChainGameStats {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_TOTAL_CHAIN_GAMES, $COL_TOTAL_CHAIN_POINTS, $COL_TOTAL_CHAIN_LENGTH FROM $TABLE_CHAIN_AGGREGATED WHERE $COL_CHAIN_AGG_ID = 1",
            null
        )

        var totalGames = 0
        var totalPoints = 0
        var totalChainLength = 0

        cursor.use {
            if (it.moveToFirst()) {
                totalGames = it.getInt(0)
                totalPoints = it.getInt(1)
                totalChainLength = it.getInt(2)
            }
        }

        val averageChainCount = if (totalGames > 0) totalChainLength.toDouble() / totalGames else 0.0

        return ChainGameStats(
            totalGames = totalGames,
            aggregatedPoints = totalPoints,
            totalChainLength = totalChainLength,
            averageChainCount = averageChainCount
        )
    }

    @Synchronized
    fun resetChainStats() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_CHAIN_GAMES, null, null)
            val resetValues = ContentValues().apply {
                put(COL_TOTAL_CHAIN_GAMES, 0)
                put(COL_TOTAL_CHAIN_LENGTH, 0)
                put(COL_TOTAL_CHAIN_USER_WORDS, 0)
                put(COL_TOTAL_CHAIN_POINTS, 0)
            }
            db.update(TABLE_CHAIN_AGGREGATED, resetValues, "$COL_CHAIN_AGG_ID = ?", arrayOf("1"))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ==========================================
    // 3. Rearrange Methods
    // ==========================================

    @Synchronized
    fun recordRearrangeGame(
        baseWord: String,
        wordsFound: List<String>,
        wordsMissed: List<String>,
        possibleWordsCount: Int,
        points: Int
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val foundCsv = wordsFound.joinToString(",")
            val missedCsv = wordsMissed.joinToString(",")
            val recordValues = ContentValues().apply {
                put(COL_REARRANGE_BASE_WORD, baseWord)
                put(COL_REARRANGE_WORDS_FOUND, wordsFound.size)
                put(COL_REARRANGE_WORDS_CSV, foundCsv)
                put(COL_REARRANGE_WORDS_MISSED_CSV, missedCsv)
                put(COL_REARRANGE_POSSIBLE_WORDS, possibleWordsCount)
                put(COL_REARRANGE_POINTS, points)
                put(COL_REARRANGE_TIMESTAMP, System.currentTimeMillis())
            }
            db.insert(TABLE_REARRANGE_GAMES, null, recordValues)

            db.execSQL("""
                UPDATE $TABLE_REARRANGE_AGGREGATED
                SET $COL_TOTAL_REARRANGE_GAMES = $COL_TOTAL_REARRANGE_GAMES + 1,
                    $COL_TOTAL_REARRANGE_WORDS = $COL_TOTAL_REARRANGE_WORDS + ?,
                    $COL_TOTAL_REARRANGE_POINTS = $COL_TOTAL_REARRANGE_POINTS + ?
                WHERE $COL_REARRANGE_AGG_ID = 1
            """.trimIndent(), arrayOf(wordsFound.size, points))

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getRearrangeAggregatedStats(): RearrangeGameStats {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_TOTAL_REARRANGE_GAMES, $COL_TOTAL_REARRANGE_WORDS, $COL_TOTAL_REARRANGE_POINTS FROM $TABLE_REARRANGE_AGGREGATED WHERE $COL_REARRANGE_AGG_ID = 1",
            null
        )

        var totalGames = 0
        var totalWords = 0
        var totalPoints = 0

        cursor.use {
            if (it.moveToFirst()) {
                totalGames = it.getInt(0)
                totalWords = it.getInt(1)
                totalPoints = it.getInt(2)
            }
        }

        val avgWords = if (totalGames > 0) totalWords.toDouble() / totalGames else 0.0
        val avgPoints = if (totalGames > 0) totalPoints.toDouble() / totalGames else 0.0

        return RearrangeGameStats(
            totalGames = totalGames,
            totalPoints = totalPoints,
            totalWordsFound = totalWords,
            averageWordsPerGame = avgWords,
            averagePointsPerGame = avgPoints
        )
    }

    fun getRearrangeHistory(limit: Int = 50): List<RearrangeRecord> {
        val list = mutableListOf<RearrangeRecord>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_REARRANGE_ID, $COL_REARRANGE_BASE_WORD, $COL_REARRANGE_WORDS_FOUND, $COL_REARRANGE_WORDS_CSV, $COL_REARRANGE_WORDS_MISSED_CSV, $COL_REARRANGE_POSSIBLE_WORDS, $COL_REARRANGE_POINTS, $COL_REARRANGE_TIMESTAMP FROM $TABLE_REARRANGE_GAMES ORDER BY $COL_REARRANGE_TIMESTAMP DESC LIMIT ?",
            arrayOf(limit.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val base = it.getString(1)
                val foundCount = it.getInt(2)
                val foundCsv = it.getString(3)
                val missedCsv = it.getString(4)
                val possible = it.getInt(5)
                val pts = it.getInt(6)
                val ts = it.getLong(7)

                val wordsFoundList = if (foundCsv.isNotBlank()) foundCsv.split(",") else emptyList()
                val wordsMissedList = if (missedCsv.isNotBlank()) missedCsv.split(",") else emptyList()
                list.add(
                    RearrangeRecord(
                        id = id,
                        baseWord = base,
                        wordsFoundCount = foundCount,
                        wordsFoundList = wordsFoundList,
                        wordsMissedList = wordsMissedList,
                        possibleWordsCount = possible,
                        points = pts,
                        timestamp = ts
                    )
                )
            }
        }
        return list
    }

    @Synchronized
    fun resetRearrangeStats() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_REARRANGE_GAMES, null, null)
            val resetValues = ContentValues().apply {
                put(COL_TOTAL_REARRANGE_GAMES, 0)
                put(COL_TOTAL_REARRANGE_WORDS, 0)
                put(COL_TOTAL_REARRANGE_POINTS, 0)
            }
            db.update(TABLE_REARRANGE_AGGREGATED, resetValues, "$COL_REARRANGE_AGG_ID = ?", arrayOf("1"))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ==========================================
    // 4. Crossword Methods
    // ==========================================

    @Synchronized
    fun recordCrosswordGame(
        across1: String,
        across2: String,
        down1: String,
        down2: String,
        correctGuesses: Int,
        hintsTaken: Int,
        points: Int,
        isCompleted: Boolean
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val recordValues = ContentValues().apply {
                put(COL_CROSSWORD_ACROSS1, across1)
                put(COL_CROSSWORD_ACROSS2, across2)
                put(COL_CROSSWORD_DOWN1, down1)
                put(COL_CROSSWORD_DOWN2, down2)
                put(COL_CROSSWORD_CORRECT_GUESSES, correctGuesses)
                put(COL_CROSSWORD_HINTS_TAKEN, hintsTaken)
                put(COL_CROSSWORD_POINTS, points)
                put(COL_CROSSWORD_COMPLETED, if (isCompleted) 1 else 0)
                put(COL_CROSSWORD_TIMESTAMP, System.currentTimeMillis())
            }
            db.insert(TABLE_CROSSWORD_GAMES, null, recordValues)

            db.execSQL("""
                UPDATE $TABLE_CROSSWORD_AGGREGATED
                SET $COL_TOTAL_CROSSWORD_GAMES = $COL_TOTAL_CROSSWORD_GAMES + 1,
                    $COL_TOTAL_CROSSWORD_CORRECT_GUESSES = $COL_TOTAL_CROSSWORD_CORRECT_GUESSES + ?,
                    $COL_TOTAL_CROSSWORD_HINTS_TAKEN = $COL_TOTAL_CROSSWORD_HINTS_TAKEN + ?,
                    $COL_TOTAL_CROSSWORD_POINTS = $COL_TOTAL_CROSSWORD_POINTS + ?
                WHERE $COL_CROSSWORD_AGG_ID = 1
            """.trimIndent(), arrayOf(correctGuesses, hintsTaken, points))

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getCrosswordAggregatedStats(): CrosswordGameStats {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_TOTAL_CROSSWORD_GAMES, $COL_TOTAL_CROSSWORD_CORRECT_GUESSES, $COL_TOTAL_CROSSWORD_HINTS_TAKEN, $COL_TOTAL_CROSSWORD_POINTS FROM $TABLE_CROSSWORD_AGGREGATED WHERE $COL_CROSSWORD_AGG_ID = 1",
            null
        )

        var totalGames = 0
        var totalCorrect = 0
        var totalHints = 0
        var totalPoints = 0

        cursor.use {
            if (it.moveToFirst()) {
                totalGames = it.getInt(0)
                totalCorrect = it.getInt(1)
                totalHints = it.getInt(2)
                totalPoints = it.getInt(3)
            }
        }

        val avgCorrect = if (totalGames > 0) totalCorrect.toDouble() / totalGames else 0.0
        val avgPoints = if (totalGames > 0) totalPoints.toDouble() / totalGames else 0.0

        return CrosswordGameStats(
            totalGames = totalGames,
            totalCorrectGuesses = totalCorrect,
            totalHintsTaken = totalHints,
            totalPoints = totalPoints,
            averageCorrectGuessesPerGame = avgCorrect,
            averagePointsPerGame = avgPoints
        )
    }

    fun getCrosswordHistory(limit: Int = 50): List<CrosswordRecord> {
        val list = mutableListOf<CrosswordRecord>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_CROSSWORD_ID, $COL_CROSSWORD_ACROSS1, $COL_CROSSWORD_ACROSS2, $COL_CROSSWORD_DOWN1, $COL_CROSSWORD_DOWN2, $COL_CROSSWORD_CORRECT_GUESSES, $COL_CROSSWORD_HINTS_TAKEN, $COL_CROSSWORD_POINTS, $COL_CROSSWORD_COMPLETED, $COL_CROSSWORD_TIMESTAMP FROM $TABLE_CROSSWORD_GAMES ORDER BY $COL_CROSSWORD_TIMESTAMP DESC LIMIT ?",
            arrayOf(limit.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    CrosswordRecord(
                        id = it.getLong(0),
                        across1 = it.getString(1),
                        across2 = it.getString(2),
                        down1 = it.getString(3),
                        down2 = it.getString(4),
                        correctGuesses = it.getInt(5),
                        hintsTaken = it.getInt(6),
                        points = it.getInt(7),
                        isCompleted = it.getInt(8) == 1,
                        timestamp = it.getLong(9)
                    )
                )
            }
        }
        return list
    }

    @Synchronized
    fun resetCrosswordStats() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_CROSSWORD_GAMES, null, null)
            val resetValues = ContentValues().apply {
                put(COL_TOTAL_CROSSWORD_GAMES, 0)
                put(COL_TOTAL_CROSSWORD_CORRECT_GUESSES, 0)
                put(COL_TOTAL_CROSSWORD_HINTS_TAKEN, 0)
                put(COL_TOTAL_CROSSWORD_POINTS, 0)
            }
            db.update(TABLE_CROSSWORD_AGGREGATED, resetValues, "$COL_CROSSWORD_AGG_ID = ?", arrayOf("1"))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
