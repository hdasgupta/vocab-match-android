package com.example.vocabgame.ui

import android.app.Dialog
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputFilter
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    enum class LetterState {
        UNSELECTED,
        SELECTED,
        ELIMINATED
    }

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

    // Letter Tracker views and state
    private lateinit var layoutLetterRows: LinearLayout
    private lateinit var btnClearTracker: Button
    private val letterStates = mutableMapOf<Char, LetterState>()
    private val letterViews = mutableMapOf<Char, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)
        val dictionary = loadDictionary()
        gameEngine = GameEngine(dictionary)

        initViews()
        setupLetterTracker()
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

        layoutLetterRows = findViewById(R.id.layoutLetterRows)
        btnClearTracker = findViewById(R.id.btnClearTracker)

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

    /**
     * Programmatically creates rounded stroke drawables to avoid any XML resource linking dependency.
     */
    private fun createLetterDrawable(bgColor: Int, strokeColor: Int): GradientDrawable {
        val density = resources.displayMetrics.density
        return GradientDrawable().apply {
            cornerRadius = 8f * density
            setColor(bgColor)
            setStroke((1.5f * density).toInt(), strokeColor)
        }
    }

    private fun setupLetterTracker() {
        layoutLetterRows.removeAllViews()
        letterStates.clear()
        letterViews.clear()

        // 4 alphabetical rows covering all 26 letters (A–Z)
        val rows = listOf(
            "ABCDEFG", // 7
            "HIJKLMN", // 7
            "OPQRSTU", // 7
            "VWXYZ"    // 5
        )

        val density = resources.displayMetrics.density
        val heightPx = (38 * density).toInt()
        val marginPx = (3 * density).toInt()

        for (rowStr in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (4 * density).toInt()
                }
            }

            for (ch in rowStr) {
                letterStates[ch] = LetterState.UNSELECTED
                val tv = TextView(this).apply {
                    text = ch.toString()
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    
                    val params = LinearLayout.LayoutParams(0, heightPx, 1f).apply {
                        setMargins(marginPx, 0, marginPx, 0)
                    }
                    layoutParams = params

                    setOnClickListener {
                        toggleLetterState(ch)
                    }
                }
                letterViews[ch] = tv
                updateLetterView(ch, LetterState.UNSELECTED, tv)
                rowLayout.addView(tv)
            }
            layoutLetterRows.addView(rowLayout)
        }
    }

    private fun toggleLetterState(ch: Char) {
        val current = letterStates[ch] ?: LetterState.UNSELECTED
        val next = when (current) {
            LetterState.UNSELECTED -> LetterState.SELECTED
            LetterState.SELECTED -> LetterState.ELIMINATED
            LetterState.ELIMINATED -> LetterState.UNSELECTED
        }
        letterStates[ch] = next
        updateLetterView(ch, next)
    }

    private fun updateLetterView(ch: Char, state: LetterState, view: TextView? = letterViews[ch]) {
        val targetView = view ?: return
        when (state) {
            LetterState.UNSELECTED -> {
                targetView.background = createLetterDrawable(
                    ContextCompat.getColor(this, R.color.card_background),
                    ContextCompat.getColor(this, R.color.card_stroke)
                )
                targetView.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                targetView.paintFlags = targetView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            LetterState.SELECTED -> {
                targetView.background = createLetterDrawable(
                    ContextCompat.getColor(this, R.color.primary_brand),
                    ContextCompat.getColor(this, R.color.primary_variant)
                )
                targetView.setTextColor(ContextCompat.getColor(this, R.color.white))
                targetView.paintFlags = targetView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            LetterState.ELIMINATED -> {
                targetView.background = createLetterDrawable(
                    ContextCompat.getColor(this, R.color.screen_background),
                    ContextCompat.getColor(this, R.color.card_stroke)
                )
                targetView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                targetView.paintFlags = targetView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }
        }
    }

    private fun resetLetterTracker() {
        for (ch in 'A'..'Z') {
            letterStates[ch] = LetterState.UNSELECTED
            updateLetterView(ch, LetterState.UNSELECTED)
        }
    }

    private fun setupListeners() {
        btnSubmitGuess.setOnClickListener {
            handleGuessSubmission()
        }

        btnClearTracker.setOnClickListener {
            resetLetterTracker()
            Toast.makeText(this, "Letter tracker cleared", Toast.LENGTH_SHORT).show()
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
        resetLetterTracker()
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
            set.addAll(listOf("WORD", "GAME", "PLAY", "READ", "WALK", "TALK", "TIME", "HOME", "LOVE", "STAR"))
        }
        return set
    }
}
