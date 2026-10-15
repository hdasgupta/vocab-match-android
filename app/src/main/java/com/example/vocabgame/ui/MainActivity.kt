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
import com.example.vocabgame.data.WordDictionary
import com.example.vocabgame.data.WordMeanings
import com.example.vocabgame.logic.GameEngine
import com.example.vocabgame.model.GameStats
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val KEY_SECRET_WORD = "key_secret_word"
        private const val KEY_GUESS_COUNT = "key_guess_count"
        private const val KEY_IS_GAME_OVER = "key_is_game_over"
        private const val KEY_IS_HINT_USED = "key_is_hint_used"
        private const val KEY_HINT_TEXT = "key_hint_text"
        private const val KEY_IS_HINT_VISIBLE = "key_is_hint_visible"
        private const val KEY_STATUS_HINT = "key_status_hint"
        private const val KEY_GUESS_HISTORY = "key_guess_history"
        private const val KEY_LETTER_STATES = "key_letter_states"
    }

    enum class LetterState {
        UNSELECTED,
        SELECTED,
        AMBER,
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

    // Hint & Show Word UI elements
    private lateinit var cardHintBanner: MaterialCardView
    private lateinit var tvHintText: TextView
    private lateinit var btnHint: Button
    private lateinit var btnShowWord: Button
    private var isHintUsed: Boolean = false

    // Letter Tracker views and state
    private lateinit var layoutLetterRows: LinearLayout
    private lateinit var btnClearTracker: Button
    private val letterStates = mutableMapOf<Char, LetterState>()
    private val letterViews = mutableMapOf<Char, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)
        
        // Initialize comprehensive 5,600+ word dictionary
        WordDictionary.init(this)
        gameEngine = GameEngine(WordDictionary.getAllWords())

        initViews()
        setupLetterTracker()
        setupListeners()
        refreshStats()

        if (savedInstanceState != null) {
            restoreGameState(savedInstanceState)
        } else {
            startNewGame()
        }
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

        cardHintBanner = findViewById(R.id.cardHintBanner)
        tvHintText = findViewById(R.id.tvHintText)
        btnHint = findViewById(R.id.btnHint)
        btnShowWord = findViewById(R.id.btnShowWord)

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
            LetterState.SELECTED -> LetterState.AMBER
            LetterState.AMBER -> LetterState.ELIMINATED
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
                // Selected letters' background is GREEN
                targetView.background = createLetterDrawable(
                    ContextCompat.getColor(this, R.color.tracker_selected_green),
                    ContextCompat.getColor(this, R.color.tracker_selected_green_stroke)
                )
                targetView.setTextColor(ContextCompat.getColor(this, R.color.white))
                targetView.paintFlags = targetView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            LetterState.AMBER -> {
                // Amber state
                targetView.background = createLetterDrawable(
                    ContextCompat.getColor(this, R.color.tracker_amber),
                    ContextCompat.getColor(this, R.color.tracker_amber_stroke)
                )
                targetView.setTextColor(ContextCompat.getColor(this, R.color.white))
                targetView.paintFlags = targetView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            LetterState.ELIMINATED -> {
                // Eliminated letters' background is RED
                targetView.background = createLetterDrawable(
                    ContextCompat.getColor(this, R.color.tracker_eliminated_red),
                    ContextCompat.getColor(this, R.color.tracker_eliminated_red_stroke)
                )
                targetView.setTextColor(ContextCompat.getColor(this, R.color.white))
                targetView.paintFlags = targetView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
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

        // Hint with descriptive meaning (-50% points penalty)
        btnHint.setOnClickListener {
            if (gameEngine.isGameOver) return@setOnClickListener

            if (isHintUsed) {
                cardHintBanner.visibility = View.VISIBLE
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle(R.string.confirm_hint_title)
                .setMessage(R.string.confirm_hint_msg)
                .setPositiveButton(R.string.yes) { _, _ ->
                    isHintUsed = true
                    val clue = WordMeanings.getMeaning(gameEngine.secretWord)
                    tvHintText.text = clue
                    cardHintBanner.visibility = View.VISIBLE
                    btnHint.text = getString(R.string.hint_active_badge)
                    btnHint.isEnabled = false
                    Toast.makeText(this, "Hint active: Points earned will be halved.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        // Show Word option (Ends game with 0 points)
        btnShowWord.setOnClickListener {
            if (gameEngine.isGameOver) return@setOnClickListener

            AlertDialog.Builder(this)
                .setTitle(R.string.confirm_show_word_title)
                .setMessage(R.string.confirm_show_word_msg)
                .setPositiveButton(R.string.reveal) { _, _ ->
                    onGameForfeited()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
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
        val secret = WordDictionary.getRandomSecretWord()
        gameEngine.startNewGame(secret)

        isHintUsed = false
        cardHintBanner.visibility = View.GONE
        btnHint.isEnabled = true
        btnHint.text = getString(R.string.btn_hint)
        btnShowWord.isEnabled = true

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

        if (!WordDictionary.isValidWord(input)) {
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
        btnHint.isEnabled = false
        btnShowWord.isEnabled = false

        val guesses = gameEngine.guessCount
        val points = gameEngine.calculatePoints(isHintUsed)
        val secret = gameEngine.secretWord

        // Store into built-in SQLite database
        dbHelper.recordGame(secret, guesses, points)

        // Refresh stats card
        val updatedStats = dbHelper.getAggregatedStats()
        displayStats(updatedStats)

        tvStatusHint.text = getString(R.string.status_game_won, secret, guesses, points)

        showVictoryDialog(secret, guesses, points, updatedStats)
    }

    private fun onGameForfeited() {
        gameEngine.forfeitGame()
        etWordInput.isEnabled = false
        btnSubmitGuess.isEnabled = false
        btnHint.isEnabled = false
        btnShowWord.isEnabled = false

        val secret = gameEngine.secretWord
        val meaning = WordMeanings.getMeaning(secret)

        tvStatusHint.text = getString(R.string.status_word_revealed, secret)

        showRevealDialog(secret, meaning)
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

        val pointsFormat = if (isHintUsed) {
            String.format(Locale.US, "%.1f pts (100 / %d / 2 [Hint])", points, guesses)
        } else {
            String.format(Locale.US, "%.1f pts (100 / %d)", points, guesses)
        }
        tvPointsEarned.text = pointsFormat

        tvAvgGuesses.text = String.format(Locale.US, "%.2f guesses/game", stats.averageGuesses)
        tvAggPoints.text = String.format(Locale.US, "%.1f pts", stats.aggregatedPoints)

        btnPlayAgain.setOnClickListener {
            dialog.dismiss()
            startNewGame()
        }

        dialog.setCancelable(false)
        dialog.show()
    }

    /**
     * Programmatic reveal dialog: 100% self-contained with ZERO external XML layout dependencies.
     */
    private fun showRevealDialog(secret: String, meaning: String) {
        val dialog = Dialog(this)
        val density = resources.displayMetrics.density
        val padding = (24 * density).toInt()

        val card = MaterialCardView(this).apply {
            radius = 20f * density
            cardElevation = 6f * density
            setStrokeColor(ContextCompat.getColor(this@MainActivity, R.color.danger_red))
            strokeWidth = (2 * density).toInt()
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.card_background))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
        }

        val icon = TextView(this).apply {
            text = "👁️"
            textSize = 44f
            gravity = Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = getString(R.string.dialog_revealed_title)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.danger_red))
            gravity = Gravity.CENTER
            setPadding(0, (8 * density).toInt(), 0, 0)
        }

        val wordView = TextView(this).apply {
            text = secret
            textSize = 32f
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = 0.2f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
            gravity = Gravity.CENTER
            setPadding(0, (6 * density).toInt(), 0, 0)
        }

        val meaningView = TextView(this).apply {
            text = meaning
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            gravity = Gravity.CENTER
            setPadding(0, (8 * density).toInt(), 0, (12 * density).toInt())
        }

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (1 * density).toInt())
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.card_stroke))
        }

        val notice = TextView(this).apply {
            text = "Game ended with 0.0 points.\nNo points added to your statistics."
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            gravity = Gravity.CENTER
            setPadding(0, (12 * density).toInt(), 0, (16 * density).toInt())
        }

        val btnPlayNext = com.google.android.material.button.MaterialButton(this).apply {
            text = "Play Next Word"
            textSize = 15f
            cornerRadius = (12 * density).toInt()
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.primary_brand))
            setOnClickListener {
                dialog.dismiss()
                startNewGame()
            }
        }

        layout.addView(icon)
        layout.addView(title)
        layout.addView(wordView)
        layout.addView(meaningView)
        layout.addView(divider)
        layout.addView(notice)
        layout.addView(btnPlayNext)
        card.addView(layout)

        dialog.setContentView(card)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
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


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SECRET_WORD, gameEngine.secretWord)
        outState.putInt(KEY_GUESS_COUNT, gameEngine.guessCount)
        outState.putBoolean(KEY_IS_GAME_OVER, gameEngine.isGameOver)
        outState.putBoolean(KEY_IS_HINT_USED, isHintUsed)
        outState.putString(KEY_HINT_TEXT, tvHintText.text.toString())
        outState.putBoolean(KEY_IS_HINT_VISIBLE, cardHintBanner.visibility == View.VISIBLE)
        outState.putString(KEY_STATUS_HINT, tvStatusHint.text.toString())

        // Save guess history
        val historyList = ArrayList<String>()
        for (item in gameEngine.guessHistory) {
            historyList.add("${item.guessNumber}|${item.guessedWord}|${item.matchingCount}|${item.exactPositions}|${item.isCorrect}")
        }
        outState.putStringArrayList(KEY_GUESS_HISTORY, historyList)

        // Save letter tracker states
        val trackerList = ArrayList<String>()
        for ((ch, state) in letterStates) {
            trackerList.add("$ch:${state.name}")
        }
        outState.putStringArrayList(KEY_LETTER_STATES, trackerList)
    }

    private fun restoreGameState(bundle: Bundle) {
        val secret = bundle.getString(KEY_SECRET_WORD) ?: ""
        val count = bundle.getInt(KEY_GUESS_COUNT, 0)
        val isOver = bundle.getBoolean(KEY_IS_GAME_OVER, false)

        // Restore guess history
        val historyStrings = bundle.getStringArrayList(KEY_GUESS_HISTORY) ?: arrayListOf()
        val restoredHistory = mutableListOf<com.example.vocabgame.model.GuessResult>()
        for (raw in historyStrings) {
            val parts = raw.split("|")
            if (parts.size == 5) {
                restoredHistory.add(
                    com.example.vocabgame.model.GuessResult(
                        guessNumber = parts[0].toIntOrNull() ?: 1,
                        guessedWord = parts[1],
                        matchingCount = parts[2].toIntOrNull() ?: 0,
                        exactPositions = parts[3].toIntOrNull() ?: 0,
                        isCorrect = parts[4].toBoolean()
                    )
                )
            }
        }
        gameEngine.restoreState(secret, count, isOver, restoredHistory)
        guessAdapter.submitList(gameEngine.guessHistory.toList())
        updateHistoryVisibility()

        // Restore hint banner and penalty state
        isHintUsed = bundle.getBoolean(KEY_IS_HINT_USED, false)
        val hintText = bundle.getString(KEY_HINT_TEXT) ?: ""
        val isHintVisible = bundle.getBoolean(KEY_IS_HINT_VISIBLE, false)
        if (isHintVisible && hintText.isNotEmpty()) {
            tvHintText.text = hintText
            cardHintBanner.visibility = View.VISIBLE
            btnHint.text = getString(R.string.hint_active_badge)
            btnHint.isEnabled = false
        } else {
            cardHintBanner.visibility = View.GONE
            btnHint.isEnabled = !isOver
            btnHint.text = getString(R.string.btn_hint)
        }

        // Restore status text
        tvStatusHint.text = bundle.getString(KEY_STATUS_HINT) ?: getString(R.string.status_secret_chosen)

        // Restore UI enabled states
        if (isOver) {
            etWordInput.isEnabled = false
            btnSubmitGuess.isEnabled = false
            btnHint.isEnabled = false
            btnShowWord.isEnabled = false
        } else {
            etWordInput.isEnabled = true
            btnSubmitGuess.isEnabled = true
            btnShowWord.isEnabled = true
        }

        // Restore letter tracker selection / colors
        val trackerStrings = bundle.getStringArrayList(KEY_LETTER_STATES) ?: arrayListOf()
        for (entry in trackerStrings) {
            val parts = entry.split(":")
            if (parts.size == 2) {
                val ch = parts[0].firstOrNull() ?: continue
                val stateName = parts[1]
                val state = try {
                    LetterState.valueOf(stateName)
                } catch (e: Exception) {
                    LetterState.UNSELECTED
                }
                letterStates[ch] = state
                updateLetterView(ch, state)
            }
        }
    }

}
