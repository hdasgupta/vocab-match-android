package com.example.vocabgame.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.vocabgame.model.GameStats

/**
 * SQLiteOpenHelper implementation utilizing Android's built-in SQLite database engine.
 *
 * Persists each game outcome and maintains aggregated points and average guess statistics.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "vocab_game.db"
        const val DATABASE_VERSION = 1

        // Table: game_records (Individual game history)
        const val TABLE_GAMES = "game_records"
        const val COL_ID = "id"
        const val COL_SECRET_WORD = "secret_word"
        const val COL_GUESS_COUNT = "guess_count"
        const val COL_POINTS = "points"
        const val COL_TIMESTAMP = "timestamp"

        // Table: aggregated_stats (Single row persistent totals)
        const val TABLE_AGGREGATED = "aggregated_stats"
        const val COL_AGG_ID = "id"
        const val COL_TOTAL_GAMES = "total_games"
        const val COL_TOTAL_POINTS = "total_points"
        const val COL_TOTAL_GUESSES = "total_guesses"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Table 1: Individual game records
        val createGamesTable = """
            CREATE TABLE $TABLE_GAMES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SECRET_WORD TEXT NOT NULL,
                $COL_GUESS_COUNT INTEGER NOT NULL,
                $COL_POINTS REAL NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL
            );
        """.trimIndent()
        db.execSQL(createGamesTable)

        // Table 2: Dedicated aggregated totals table
        val createAggregatedTable = """
            CREATE TABLE $TABLE_AGGREGATED (
                $COL_AGG_ID INTEGER PRIMARY KEY,
                $COL_TOTAL_GAMES INTEGER DEFAULT 0,
                $COL_TOTAL_POINTS REAL DEFAULT 0.0,
                $COL_TOTAL_GUESSES INTEGER DEFAULT 0
            );
        """.trimIndent()
        db.execSQL(createAggregatedTable)

        // Initialize single summary row (id = 1)
        val initialValues = ContentValues().apply {
            put(COL_AGG_ID, 1)
            put(COL_TOTAL_GAMES, 0)
            put(COL_TOTAL_POINTS, 0.0)
            put(COL_TOTAL_GUESSES, 0)
        }
        db.insert(TABLE_AGGREGATED, null, initialValues)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GAMES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_AGGREGATED")
        onCreate(db)
    }

    /**
     * Records a completed game in the database:
     * 1. Inserts the detailed game log into game_records.
     * 2. Atomically increments and stores aggregated points, total games, and total guesses in aggregated_stats.
     */
    @Synchronized
    fun recordGame(secretWord: String, guessCount: Int, points: Double) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // 1. Insert into game_records
            val gameValues = ContentValues().apply {
                put(COL_SECRET_WORD, secretWord)
                put(COL_GUESS_COUNT, guessCount)
                put(COL_POINTS, points)
                put(COL_TIMESTAMP, System.currentTimeMillis())
            }
            db.insert(TABLE_GAMES, null, gameValues)

            // 2. Update aggregated_stats table
            val updateSql = """
                UPDATE $TABLE_AGGREGATED 
                SET $COL_TOTAL_GAMES = $COL_TOTAL_GAMES + 1,
                    $COL_TOTAL_POINTS = $COL_TOTAL_POINTS + ?,
                    $COL_TOTAL_GUESSES = $COL_TOTAL_GUESSES + ?
                WHERE $COL_AGG_ID = 1
            """.trimIndent()
            db.execSQL(updateSql, arrayOf(points, guessCount))

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Retrieves aggregated statistics, calculating the user's average guesses and total points.
     */
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

    /**
     * Resets all game records and resets aggregated statistics back to zero.
     */
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
}
