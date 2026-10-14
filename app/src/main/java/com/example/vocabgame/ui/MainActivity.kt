package com.example.vocabgame.ui

import android.app.Dialog
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vocabgame.R
import com.example.vocabgame.data.DatabaseHelper
import com.example.vocabgame.logic.GameEngine
import com.example.vocabgame.model.GameStats
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var gameEngine: GameEngine
    private lateinit var guessAdapter: GuessAdapter

    private lateinit var tvTotalGames: TextView
    private lateinit var tvAggregatedPoints: TextView
    private lateinit var tvAverageGuesses: TextView
    private lateinit var tvAveragePoints: TextView
    private lateinit var etWordInput: EditText
    private lateinit var btnSubmitGuess: Button
    private lateinit var btnNewGame: Button
    private lateinit var btnResetStats: Button
    private lateinit var rvGuesses: RecyclerView
    private lateinit var tvEmptyHistory: TextView
    private lateinit var tvStatusHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)
        val dictionary = loadDictionary()
        gameEngine = GameEngine(dictionary)

        initViews()
        setupListeners()
        refreshStats()
        startNewGame()
    }

    private fun initViews() {
        tvTotalGames = findViewById(R.id.tvTotalGames)
        tvAggregatedPoints = findViewById(R.id.tvAggregatedPoints)
        tvAverageGuesses = findViewById(R.id.tvAverageGuesses)
        tvAveragePoints = findViewById(R.id.tvAveragePoints)
        etWordInput = findViewById(R.id.etWordInput)
        btnSubmitGuess = findViewById(R.id.btnSubmitGuess)
        btnNewGame = findViewById(R.id.btnNewGame)
        btnResetStats = findViewById(R.id.btnResetStats)
        rvGuesses = findViewById(R.id.rvGuesses)
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory)
        tvStatusHint = findViewById(R.id.tvStatusHint)

        // Force uppercase input and max 4 letters
        etWordInput.filters = arrayOf(
            InputFilter.LengthFilter(4),
            InputFilter.AllCaps()
        )

        guessAdapter = GuessAdapter()
        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        rvGuesses.layoutManager = layoutManager
        rvGuesses.adapter = guessAdapter
    }

    private fun setupListeners() {
        btnSubmitGuess.setOnClickListener {
            handleGuessSubmission()
        }

        etWordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleGuessSubmission()
                true
            } else {
                false
            }
        }

        btnNewGame.setOnClickListener {
            if (!gameEngine.isGameOver && gameEngine.guessCount > 0) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_new_game_title)
                    .setMessage(R.string.confirm_new_game_msg)
                    .setPositiveButton(R.string.yes) { _, _ -> startNewGame() }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            } else {
                startNewGame()
            }
        }

        btnResetStats.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.reset_stats_title)
                .setMessage(R.string.reset_stats_msg)
                .setPositiveButton(R.string.reset) { _, _ ->
                    dbHelper.resetAllStats()
                    refreshStats()
                    Toast.makeText(this, R.string.stats_reset_success, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun startNewGame() {
        gameEngine.startNewGame()
        etWordInput.text.clear()
        etWordInput.isEnabled = true
        btnSubmitGuess.isEnabled = true
        guessAdapter.submitList(emptyList())
        updateHistoryVisibility()
        tvStatusHint.text = getString(R.string.status_secret_chosen)
    }

    private fun handleGuessSubmission() {
        val input = etWordInput.text.toString().trim().uppercase(Locale.US)

        if (input.length != 4) {
            Toast.makeText(this, R.string.err_enter_4_letters, Toast.LENGTH_SHORT).show()
            return
        }

        if (!input.all { it in 'A'..'Z' }) {
            Toast.makeText(this, R.string.err_only_letters, Toast.LENGTH_SHORT).show()
            return
        }

        if (!gameEngine.isValidDictionaryWord(input)) {
            Toast.makeText(this, getString(R.string.err_not_meaningful_word, input), Toast.LENGTH_SHORT).show()
            return
        }

        val result = gameEngine.evaluateGuess(input)
        guessAdapter.submitList(gameEngine.guessHistory.toList())
        updateHistoryVisibility()
        rvGuesses.smoothScrollToPosition(guessAdapter.itemCount - 1)
        etWordInput.text.clear()

        if (result.isCorrect) {
            onGameWon()
        } else {
            tvStatusHint.text = getString(
                R.string.status_feedback_format,
                result.guessedWord,
                result.matchingCount
            )
        }
    }

    private fun onGameWon() {
        etWordInput.isEnabled = false
        btnSubmitGuess.isEnabled = false

        val guesses = gameEngine.guessCount
        val points = gameEngine.calculatePoints()
        val secret = gameEngine.secretWord

        // Store into built-in SQLite database
        dbHelper.recordGame(secret, guesses, points)

        // Refresh stats card
        val updatedStats = dbHelper.getAggregatedStats()
        displayStats(updatedStats)

        tvStatusHint.text = getString(R.string.status_game_won, secret, guesses, points)

        showVictoryDialog(secret, guesses, points, updatedStats)
    }

    private fun showVictoryDialog(secret: String, guesses: Int, points: Double, stats: GameStats) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_victory)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvSecretWord: TextView = dialog.findViewById(R.id.tvDialogSecretWord)
        val tvGuessesCount: TextView = dialog.findViewById(R.id.tvDialogGuessesCount)
        val tvPointsEarned: TextView = dialog.findViewById(R.id.tvDialogPointsEarned)
        val tvAvgGuesses: TextView = dialog.findViewById(R.id.tvDialogAvgGuesses)
        val tvAggPoints: TextView = dialog.findViewById(R.id.tvDialogAggPoints)
        val btnPlayAgain: Button = dialog.findViewById(R.id.btnDialogPlayAgain)

        tvSecretWord.text = secret
        tvGuessesCount.text = guesses.toString()
        tvPointsEarned.text = String.format(Locale.US, "%.1f pts (100 / %d)", points, guesses)
        tvAvgGuesses.text = String.format(Locale.US, "%.2f guesses/game", stats.averageGuesses)
        tvAggPoints.text = String.format(Locale.US, "%.1f pts", stats.aggregatedPoints)

        btnPlayAgain.setOnClickListener {
            dialog.dismiss()
            startNewGame()
        }

        dialog.setCancelable(false)
        dialog.show()
    }

    private fun refreshStats() {
        val stats = dbHelper.getAggregatedStats()
        displayStats(stats)
    }

    private fun displayStats(stats: GameStats) {
        tvTotalGames.text = stats.totalGames.toString()
        tvAggregatedPoints.text = String.format(Locale.US, "%.1f", stats.aggregatedPoints)
        tvAverageGuesses.text = String.format(Locale.US, "%.2f", stats.averageGuesses)
        tvAveragePoints.text = String.format(Locale.US, "%.1f", stats.averagePoints)
    }

    private fun updateHistoryVisibility() {
        if (guessAdapter.itemCount == 0) {
            tvEmptyHistory.visibility = View.VISIBLE
            rvGuesses.visibility = View.GONE
        } else {
            tvEmptyHistory.visibility = View.GONE
            rvGuesses.visibility = View.VISIBLE
        }
    }

    private fun loadDictionary(): Set<String> {
        val set = mutableSetOf<String>()
        try {
            assets.open("words.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        val trimmed = line.trim().uppercase(Locale.US)
                        if (trimmed.length == 4) {
                            set.add(trimmed)
                        }
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            // Safe fallback if asset read fails
            set.addAll(listOf("WORD", "GAME", "PLAY", "READ", "WALK", "TALK", "TIME", "HOME", "LOVE", "STAR"))
        }
        return set
    }
}
