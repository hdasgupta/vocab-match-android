package com.example.vocabgame.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.vocabgame.model.ChainGameStats
import com.example.vocabgame.model.GameStats

/**
 * SQLiteOpenHelper implementation utilizing Android's built-in SQLite database engine.
 *
 * Persists each game outcome and maintains aggregated statistics for both:
 * 1. "Guess Word" (Deduction vocabulary game)
 * 2. "Change a Letter" (Word chain game)
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "vocab_game.db"
        const val DATABASE_VERSION = 2

        // Table: game_records (Guess Word game history)
        const val TABLE_GAMES = "game_records"
        const val COL_ID = "id"
        const val COL_SECRET_WORD = "secret_word"
        const val COL_GUESS_COUNT = "guess_count"
        const val COL_POINTS = "points"
        const val COL_TIMESTAMP = "timestamp"

        // Table: aggregated_stats (Guess Word persistent totals)
        const val TABLE_AGGREGATED = "aggregated_stats"
        const val COL_AGG_ID = "id"
        const val COL_TOTAL_GAMES = "total_games"
        const val COL_TOTAL_POINTS = "total_points"
        const val COL_TOTAL_GUESSES = "total_guesses"

        // Table: chain_game_records (Change a Letter game history)
        const val TABLE_CHAIN_GAMES = "chain_game_records"
        const val COL_CHAIN_ID = "id"
        const val COL_CHAIN_LENGTH = "chain_length"
        const val COL_CHAIN_USER_WORDS = "user_words"
        const val COL_CHAIN_POINTS = "points"
        const val COL_CHAIN_WINNER = "winner"
        const val COL_CHAIN_TIMESTAMP = "timestamp"

        // Table: chain_aggregated_stats (Change a Letter persistent totals)
        const val TABLE_CHAIN_AGGREGATED = "chain_aggregated_stats"
        const val COL_CHAIN_AGG_ID = "id"
        const val COL_TOTAL_CHAIN_GAMES = "total_games"
        const val COL_TOTAL_CHAIN_LENGTH = "total_chain_length"
        const val COL_TOTAL_CHAIN_USER_WORDS = "total_user_words"
        const val COL_TOTAL_CHAIN_POINTS = "total_points"
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

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createChainTables(db)
        }
    }

    // ==========================================
    // Guess Word Methods
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
    // Change a Letter Methods
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
}
