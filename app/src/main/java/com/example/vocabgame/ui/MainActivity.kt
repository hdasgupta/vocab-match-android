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
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vocabgame.R
import com.example.vocabgame.data.DatabaseHelper
import com.example.vocabgame.data.WordDictionary
import com.example.vocabgame.data.WordMeanings
import com.example.vocabgame.logic.RearrangeEngine
import com.example.vocabgame.logic.RearrangeSubmitResult
import com.example.vocabgame.model.RearrangeGameStats
import com.example.vocabgame.model.RearrangeRecord
import com.example.vocabgame.model.RearrangeWordEntry
import com.example.vocabgame.logic.ChangeLetterEngine
import com.example.vocabgame.logic.ComputerPlayResult
import com.example.vocabgame.logic.GameEngine
import com.example.vocabgame.logic.UserPlayResult
import com.example.vocabgame.model.ChainGameStats
import com.example.vocabgame.model.ChainWordEntry
import com.example.vocabgame.model.GameStats
import com.example.vocabgame.model.PlayerType
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val KEY_ACTIVE_GAME = "key_active_game"
        private const val GAME_GUESS_WORD = "game_guess_word"
        private const val GAME_CHANGE_LETTER = "game_change_letter"
        private const val GAME_REARRANGE = "game_rearrange"

        // Rearrange state keys
        private const val KEY_REARRANGE_BASE_WORD = "key_rearrange_base_word"
        private const val KEY_REARRANGE_FOUND_WORDS = "key_rearrange_found_words"
        private const val KEY_REARRANGE_IS_GAME_OVER = "key_rearrange_is_game_over"
        private const val KEY_REARRANGE_STATUS = "key_rearrange_status"

        // Guess Word state keys
        private const val KEY_SECRET_WORD = "key_secret_word"
        private const val KEY_GUESS_COUNT = "key_guess_count"
        private const val KEY_IS_GAME_OVER = "key_is_game_over"
        private const val KEY_IS_HINT_USED = "key_is_hint_used"
        private const val KEY_HINT_TEXT = "key_hint_text"
        private const val KEY_IS_HINT_VISIBLE = "key_is_hint_visible"
        private const val KEY_STATUS_HINT = "key_status_hint"
        private const val KEY_GUESS_HISTORY = "key_guess_history"
        private const val KEY_LETTER_STATES = "key_letter_states"

        // Change a Letter state keys
        private const val KEY_CHAIN_CURRENT_WORD = "key_chain_curr_word"
        private const val KEY_CHAIN_POINTS = "key_chain_points"
        private const val KEY_CHAIN_IS_GAME_OVER = "key_chain_is_game_over"
        private const val KEY_CHAIN_HISTORY = "key_chain_history"
        private const val KEY_CHAIN_USED_WORDS = "key_chain_used_words"
        private const val KEY_CHAIN_STATUS = "key_chain_status"
    }

    enum class LetterState {
        UNSELECTED,
        SELECTED,
        AMBER,
        ELIMINATED
    }

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var gameEngine: GameEngine
    private lateinit var chainEngine: ChangeLetterEngine
    private lateinit var rearrangeEngine: RearrangeEngine

    private lateinit var layoutRearrangeContainer: LinearLayout

    // Rearrange Views
    private lateinit var rearrangeWordAdapter: RearrangeWordAdapter
    private lateinit var tvRearrangeTotalPoints: TextView
    private lateinit var tvRearrangeAvgWords: TextView
    private lateinit var tvRearrangeTotalGames: TextView
    private lateinit var tvRearrangeAvgPoints: TextView
    private lateinit var btnViewRearrangeHistory: Button
    private lateinit var btnResetRearrangeStats: Button

    private lateinit var tvRearrangeBaseWord: TextView
    private lateinit var tvRearrangeProgress: TextView
    private lateinit var tvRearrangeCurrentPoints: TextView
    private lateinit var tvRearrangeStatus: TextView
    private lateinit var etRearrangeInput: EditText
    private lateinit var btnSubmitRearrangeWord: Button
    private lateinit var btnFinishRearrangeRound: Button
    private lateinit var btnNewRearrangeGame: Button
    private lateinit var rvRearrangeWords: RecyclerView
    private lateinit var tvEmptyRearrangeWords: TextView

    // Navigation & Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var layoutGuessWordContainer: LinearLayout
    private lateinit var layoutChangeLetterContainer: LinearLayout
    private var activeGameMode: String = GAME_GUESS_WORD

    // ==========================================
    // Guess Word Views
    // ==========================================
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

    private lateinit var cardHintBanner: MaterialCardView
    private lateinit var tvHintText: TextView
    private lateinit var btnHint: Button
    private lateinit var btnShowWord: Button
    private var isHintUsed: Boolean = false

    private lateinit var layoutLetterRows: LinearLayout
    private lateinit var btnClearTracker: Button
    private val letterStates = mutableMapOf<Char, LetterState>()
    private val letterViews = mutableMapOf<Char, TextView>()

    // ==========================================
    // Change a Letter Views
    // ==========================================
    private lateinit var chainAdapter: ChainAdapter
    private lateinit var tvChainAvgCount: TextView
    private lateinit var tvChainTotalPoints: TextView
    private lateinit var tvChainTotalGames: TextView
    private lateinit var btnResetChainStats: Button

    private lateinit var tvChainCurrentWord: TextView
    private lateinit var tvChainCurrentMeaning: TextView
    private lateinit var tvChainCurrentLength: TextView
    private lateinit var tvChainGamePoints: TextView
    private lateinit var tvChainStatus: TextView
    private lateinit var etChainInput: EditText
    private lateinit var btnChainPlayWord: Button
    private lateinit var btnChainSurrender: Button
    private lateinit var btnNewChainGame: Button
    private lateinit var rvChainHistory: RecyclerView
    private lateinit var tvEmptyChainHistory: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)
        
        // Initialize 5,600+ word dictionary
        WordDictionary.init(this)
        gameEngine = GameEngine(WordDictionary.getAllWords())
        chainEngine = ChangeLetterEngine(WordDictionary.getAllWords())
        rearrangeEngine = RearrangeEngine(WordDictionary.getAllWords())

        initViews()
        setupNavigationDrawer()
        setupLetterTracker()
        setupListeners()
        refreshStats()
        refreshChainStats()
        refreshRearrangeStats()

        if (savedInstanceState != null) {
            restoreAllState(savedInstanceState)
        } else {
            startNewGame()
            startNewChainGame()
            startNewRearrangeGame()
            switchToGame(GAME_GUESS_WORD)
        }
    }

    private fun initViews() {
        // Navigation & Containers
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        toolbar = findViewById(R.id.toolbar)
        layoutGuessWordContainer = findViewById(R.id.layoutGuessWordContainer)
        layoutChangeLetterContainer = findViewById(R.id.layoutChangeLetterContainer)
        layoutRearrangeContainer = findViewById(R.id.layoutRearrangeContainer)

        // Rearrange Views Binding
        tvRearrangeTotalPoints = findViewById(R.id.tvRearrangeTotalPoints)
        tvRearrangeAvgWords = findViewById(R.id.tvRearrangeAvgWords)
        tvRearrangeTotalGames = findViewById(R.id.tvRearrangeTotalGames)
        tvRearrangeAvgPoints = findViewById(R.id.tvRearrangeAvgPoints)
        btnViewRearrangeHistory = findViewById(R.id.btnViewRearrangeHistory)
        btnResetRearrangeStats = findViewById(R.id.btnResetRearrangeStats)

        tvRearrangeBaseWord = findViewById(R.id.tvRearrangeBaseWord)
        tvRearrangeProgress = findViewById(R.id.tvRearrangeProgress)
        tvRearrangeCurrentPoints = findViewById(R.id.tvRearrangeCurrentPoints)
        tvRearrangeStatus = findViewById(R.id.tvRearrangeStatus)
        etRearrangeInput = findViewById(R.id.etRearrangeInput)
        btnSubmitRearrangeWord = findViewById(R.id.btnSubmitRearrangeWord)
        btnFinishRearrangeRound = findViewById(R.id.btnFinishRearrangeRound)
        btnNewRearrangeGame = findViewById(R.id.btnNewRearrangeGame)
        rvRearrangeWords = findViewById(R.id.rvRearrangeWords)
        tvEmptyRearrangeWords = findViewById(R.id.tvEmptyRearrangeWords)

        etRearrangeInput.filters = arrayOf(InputFilter.LengthFilter(4), InputFilter.AllCaps())
        rearrangeWordAdapter = RearrangeWordAdapter()
        val rearrangeLayoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }
        rvRearrangeWords.layoutManager = rearrangeLayoutManager
        rvRearrangeWords.adapter = rearrangeWordAdapter

        // Guess Word Views
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

        etWordInput.filters = arrayOf(InputFilter.LengthFilter(4), InputFilter.AllCaps())
        guessAdapter = GuessAdapter()
        val guessLayoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }
        rvGuesses.layoutManager = guessLayoutManager
        rvGuesses.adapter = guessAdapter

        // Change a Letter Views
        tvChainAvgCount = findViewById(R.id.tvChainAvgCount)
        tvChainTotalPoints = findViewById(R.id.tvChainTotalPoints)
        tvChainTotalGames = findViewById(R.id.tvChainTotalGames)
        btnResetChainStats = findViewById(R.id.btnResetChainStats)

        tvChainCurrentWord = findViewById(R.id.tvChainCurrentWord)
        tvChainCurrentMeaning = findViewById(R.id.tvChainCurrentMeaning)
        tvChainCurrentLength = findViewById(R.id.tvChainCurrentLength)
        tvChainGamePoints = findViewById(R.id.tvChainGamePoints)
        tvChainStatus = findViewById(R.id.tvChainStatus)
        etChainInput = findViewById(R.id.etChainInput)
        btnChainPlayWord = findViewById(R.id.btnChainPlayWord)
        btnChainSurrender = findViewById(R.id.btnChainSurrender)
        btnNewChainGame = findViewById(R.id.btnNewChainGame)
        rvChainHistory = findViewById(R.id.rvChainHistory)
        tvEmptyChainHistory = findViewById(R.id.tvEmptyChainHistory)

        etChainInput.filters = arrayOf(InputFilter.LengthFilter(4), InputFilter.AllCaps())
        chainAdapter = ChainAdapter()
        val chainLayoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }
        rvChainHistory.layoutManager = chainLayoutManager
        rvChainHistory.adapter = chainAdapter
    }

    private fun setupNavigationDrawer() {
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_guess_word -> {
                    switchToGame(GAME_GUESS_WORD)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_change_letter -> {
                    switchToGame(GAME_CHANGE_LETTER)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_rearrange -> {
                    switchToGame(GAME_REARRANGE)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchToGame(gameMode: String) {
        activeGameMode = gameMode
        when (gameMode) {
            GAME_GUESS_WORD -> {
                layoutGuessWordContainer.visibility = View.VISIBLE
                layoutChangeLetterContainer.visibility = View.GONE
                layoutRearrangeContainer.visibility = View.GONE
                toolbar.title = getString(R.string.title_guess_word)
                toolbar.subtitle = getString(R.string.subtitle_guess_word)
                navView.setCheckedItem(R.id.nav_guess_word)
            }
            GAME_CHANGE_LETTER -> {
                layoutGuessWordContainer.visibility = View.GONE
                layoutChangeLetterContainer.visibility = View.VISIBLE
                layoutRearrangeContainer.visibility = View.GONE
                toolbar.title = getString(R.string.title_change_letter)
                toolbar.subtitle = getString(R.string.subtitle_change_letter)
                navView.setCheckedItem(R.id.nav_change_letter)
            }
            GAME_REARRANGE -> {
                layoutGuessWordContainer.visibility = View.GONE
                layoutChangeLetterContainer.visibility = View.GONE
                layoutRearrangeContainer.visibility = View.VISIBLE
                toolbar.title = getString(R.string.title_rearrange)
                toolbar.subtitle = getString(R.string.subtitle_rearrange)
                navView.setCheckedItem(R.id.nav_rearrange)
            }
        }
    }

    // ==========================================
    // Guess Word Mechanics
    // ==========================================

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

        val rows = listOf("ABCDEFG", "HIJKLMN", "OPQRSTU", "VWXYZ")
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
                targetView.background = createLetterDrawable(
                    ContextCompat.getColor(this, R.color.tracker_selected_green),
                    ContextCompat.getColor(this, R.color.tracker_selected_green_stroke)
                )
                targetView.setTextColor(ContextCompat.getColor(this, R.color.white))
                targetView.paintFlags = targetView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            LetterState.AMBER -> {
                targetView.background = createLetterDrawable(
                    ContextCompat.getColor(this, R.color.tracker_amber),
                    ContextCompat.getColor(this, R.color.tracker_amber_stroke)
                )
                targetView.setTextColor(ContextCompat.getColor(this, R.color.white))
                targetView.paintFlags = targetView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            LetterState.ELIMINATED -> {
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
        // Guess Word Listeners
        btnSubmitGuess.setOnClickListener { handleGuessSubmission() }
        btnClearTracker.setOnClickListener {
            resetLetterTracker()
            Toast.makeText(this, "Letter tracker cleared", Toast.LENGTH_SHORT).show()
        }
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
        btnShowWord.setOnClickListener {
            if (gameEngine.isGameOver) return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle(R.string.confirm_show_word_title)
                .setMessage(R.string.confirm_show_word_msg)
                .setPositiveButton(R.string.reveal) { _, _ -> onGameForfeited() }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        etWordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleGuessSubmission()
                true
            } else false
        }
        btnNewGame.setOnClickListener {
            if (!gameEngine.isGameOver && gameEngine.guessCount > 0) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_new_game_title)
                    .setMessage(R.string.confirm_new_game_msg)
                    .setPositiveButton(R.string.yes) { _, _ -> startNewGame() }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            } else startNewGame()
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

        // Change a Letter Listeners
        btnChainPlayWord.setOnClickListener { handleUserChainWord() }
        etChainInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleUserChainWord()
                true
            } else false
        }
        btnChainSurrender.setOnClickListener { handleUserChainSurrender() }
        btnNewChainGame.setOnClickListener { startNewChainGame() }
        btnResetChainStats.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.confirm_reset_chain_stats_title)
                .setMessage(R.string.confirm_reset_chain_stats_msg)
                .setPositiveButton(R.string.reset) { _, _ ->
                    dbHelper.resetChainStats()
                    refreshChainStats()
                    Toast.makeText(this, "Change a Letter stats reset.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        // Rearrange Listeners
        btnSubmitRearrangeWord.setOnClickListener { handleRearrangeWordSubmission() }
        etRearrangeInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleRearrangeWordSubmission()
                true
            } else false
        }
        btnFinishRearrangeRound.setOnClickListener { handleFinishRearrangeRound() }
        btnNewRearrangeGame.setOnClickListener { startNewRearrangeGame() }
        btnViewRearrangeHistory.setOnClickListener { showRearrangeHistoryDialog() }
        btnResetRearrangeStats.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.confirm_reset_rearrange_stats_title)
                .setMessage(R.string.confirm_reset_rearrange_stats_msg)
                .setPositiveButton(R.string.reset) { _, _ ->
                    dbHelper.resetRearrangeStats()
                    refreshRearrangeStats()
                    Toast.makeText(this, "Rearrange statistics reset.", Toast.LENGTH_SHORT).show()
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
            tvStatusHint.text = getString(R.string.status_feedback_format, result.guessedWord, result.matchingCount)
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

        dbHelper.recordGame(secret, guesses, points)
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

    // ==========================================
    // Change a Letter Mechanics
    // ==========================================

    private fun startNewChainGame() {
        val startWord = WordDictionary.getRandomSecretWord()
        chainEngine.startNewGame(startWord)

        etChainInput.text.clear()
        etChainInput.isEnabled = true
        btnChainPlayWord.isEnabled = true
        btnChainSurrender.isEnabled = true

        chainAdapter.submitList(chainEngine.chainHistory.toList())
        updateChainViews()
        tvChainStatus.text = getString(R.string.chain_status_initial, startWord)
    }

    private fun handleUserChainWord() {
        val input = etChainInput.text.toString().trim().uppercase(Locale.US)
        when (val result = chainEngine.playUserWord(input)) {
            is UserPlayResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
            is UserPlayResult.Success -> {
                etChainInput.text.clear()
                chainAdapter.submitList(chainEngine.chainHistory.toList())
                updateChainViews()
                rvChainHistory.smoothScrollToPosition(chainAdapter.itemCount - 1)

                tvChainStatus.text = getString(R.string.chain_status_turn_comp, result.playedWord)
                
                // Trigger computer turn
                rvChainHistory.postDelayed({
                    handleComputerChainTurn()
                }, 400)
            }
        }
    }

    private fun handleComputerChainTurn() {
        when (val compResult = chainEngine.playComputerTurn()) {
            is ComputerPlayResult.Played -> {
                chainAdapter.submitList(chainEngine.chainHistory.toList())
                updateChainViews()
                rvChainHistory.smoothScrollToPosition(chainAdapter.itemCount - 1)
                tvChainStatus.text = getString(R.string.chain_status_turn_user, compResult.computerWord)
            }
            is ComputerPlayResult.Surrendered -> {
                // Computer surrendered! User won!
                etChainInput.isEnabled = false
                btnChainPlayWord.isEnabled = false
                btnChainSurrender.isEnabled = false

                val finalChain = compResult.finalChainLength
                val earnedPoints = compResult.userPoints
                val userWords = chainEngine.chainHistory.count { it.player == PlayerType.USER }

                dbHelper.recordChainGame(finalChain, userWords, earnedPoints, "USER")
                refreshChainStats()

                tvChainStatus.text = getString(R.string.chain_status_comp_surrender, finalChain, earnedPoints)
                showChainOutcomeDialog(
                    title = "🎉 Computer Surrendered!",
                    message = "The computer is unable to make any further word from '${chainEngine.currentWord}'.\n\nYou won the round!",
                    chainLength = finalChain,
                    points = earnedPoints
                )
            }
        }
    }

    private fun handleUserChainSurrender() {
        if (chainEngine.isGameOver) return

        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_surrender_title)
            .setMessage(getString(R.string.confirm_surrender_msg, chainEngine.currentWord))
            .setPositiveButton("Surrender") { _, _ ->
                val points = chainEngine.surrenderUser()
                etChainInput.isEnabled = false
                btnChainPlayWord.isEnabled = false
                btnChainSurrender.isEnabled = false

                val finalChain = chainEngine.chainHistory.size
                val userWords = chainEngine.chainHistory.count { it.player == PlayerType.USER }

                dbHelper.recordChainGame(finalChain, userWords, points, "COMPUTER")
                refreshChainStats()

                tvChainStatus.text = getString(R.string.chain_status_user_surrender, finalChain, points)
                showChainOutcomeDialog(
                    title = "🏳️ Game Over",
                    message = "You surrendered this word chain. Great effort maintaining the sequence!",
                    chainLength = finalChain,
                    points = points
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateChainViews() {
        tvChainCurrentWord.text = chainEngine.currentWord
        val meaning = WordMeanings.getMeaning(chainEngine.currentWord)
        tvChainCurrentMeaning.text = "Definition: $meaning"

        val count = chainEngine.chainHistory.size
        tvChainCurrentLength.text = "Chain: $count word${if (count != 1) "s" else ""}"
        tvChainGamePoints.text = "This Game: ${chainEngine.userPoints} pts"

        if (chainAdapter.itemCount == 0) {
            tvEmptyChainHistory.visibility = View.VISIBLE
            rvChainHistory.visibility = View.GONE
        } else {
            tvEmptyChainHistory.visibility = View.GONE
            rvChainHistory.visibility = View.VISIBLE
        }
    }

    private fun refreshChainStats() {
        val stats = dbHelper.getChainAggregatedStats()
        tvChainAvgCount.text = String.format(Locale.US, "%.1f", stats.averageChainCount)
        tvChainTotalPoints.text = "${stats.aggregatedPoints} pts"
        tvChainTotalGames.text = stats.totalGames.toString()
    }

    private fun showChainOutcomeDialog(title: String, message: String, chainLength: Int, points: Int) {
        val dialog = Dialog(this)
        val density = resources.displayMetrics.density
        val padding = (24 * density).toInt()

        val card = MaterialCardView(this).apply {
            radius = 20f * density
            cardElevation = 6f * density
            setStrokeColor(ContextCompat.getColor(this@MainActivity, R.color.primary_brand))
            strokeWidth = (2 * density).toInt()
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.card_background))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
            gravity = Gravity.CENTER
        }

        val msgView = TextView(this).apply {
            text = message
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            gravity = Gravity.CENTER
            setPadding(0, (8 * density).toInt(), 0, (14 * density).toInt())
        }

        val statsSummary = TextView(this).apply {
            text = "Total Chain Words: $chainLength\nPoints Earned: +$points pts (+2 per word)"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.success_green))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (18 * density).toInt())
        }

        val btnPlayNext = com.google.android.material.button.MaterialButton(this).apply {
            text = "Start Next Chain"
            textSize = 15f
            cornerRadius = (12 * density).toInt()
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.primary_brand))
            setOnClickListener {
                dialog.dismiss()
                startNewChainGame()
            }
        }

        layout.addView(titleView)
        layout.addView(msgView)
        layout.addView(statsSummary)
        layout.addView(btnPlayNext)
        card.addView(layout)

        dialog.setContentView(card)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)
        dialog.show()
    }

    // ==========================================
    // Unified Lifecycle State Preservation
    // ==========================================

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_ACTIVE_GAME, activeGameMode)

        // Save Guess Word state
        outState.putString(KEY_SECRET_WORD, gameEngine.secretWord)
        outState.putInt(KEY_GUESS_COUNT, gameEngine.guessCount)
        outState.putBoolean(KEY_IS_GAME_OVER, gameEngine.isGameOver)
        outState.putBoolean(KEY_IS_HINT_USED, isHintUsed)
        outState.putString(KEY_HINT_TEXT, tvHintText.text.toString())
        outState.putBoolean(KEY_IS_HINT_VISIBLE, cardHintBanner.visibility == View.VISIBLE)
        outState.putString(KEY_STATUS_HINT, tvStatusHint.text.toString())

        val historyList = ArrayList<String>()
        for (item in gameEngine.guessHistory) {
            historyList.add("${item.guessNumber}|${item.guessedWord}|${item.matchingCount}|${item.exactPositions}|${item.isCorrect}|${item.meaning}")
        }
        outState.putStringArrayList(KEY_GUESS_HISTORY, historyList)

        val trackerList = ArrayList<String>()
        for ((ch, state) in letterStates) {
            trackerList.add("$ch:${state.name}")
        }
        outState.putStringArrayList(KEY_LETTER_STATES, trackerList)

        // Save Change a Letter state
        outState.putString(KEY_CHAIN_CURRENT_WORD, chainEngine.currentWord)
        outState.putInt(KEY_CHAIN_POINTS, chainEngine.userPoints)
        outState.putBoolean(KEY_CHAIN_IS_GAME_OVER, chainEngine.isGameOver)
        outState.putString(KEY_CHAIN_STATUS, tvChainStatus.text.toString())

        val chainHistoryList = ArrayList<String>()
        for (item in chainEngine.chainHistory) {
            chainHistoryList.add("${item.stepNumber}|${item.word}|${item.player.name}|${item.pointsAwarded}|${item.meaning}")
        }
        outState.putStringArrayList(KEY_CHAIN_HISTORY, chainHistoryList)

        val chainUsedList = ArrayList<String>(chainEngine.usedWords)
        outState.putStringArrayList(KEY_CHAIN_USED_WORDS, chainUsedList)

        // Save Rearrange state
        outState.putString(KEY_REARRANGE_BASE_WORD, rearrangeEngine.baseWord)
        outState.putBoolean(KEY_REARRANGE_IS_GAME_OVER, rearrangeEngine.isGameOver)
        outState.putStringArrayList(KEY_REARRANGE_FOUND_WORDS, ArrayList(rearrangeEngine.foundWords))
        outState.putString(KEY_REARRANGE_STATUS, tvRearrangeStatus.text.toString())
    }

    private fun restoreAllState(bundle: Bundle) {
        // Restore Guess Word
        val secret = bundle.getString(KEY_SECRET_WORD) ?: ""
        val count = bundle.getInt(KEY_GUESS_COUNT, 0)
        val isOver = bundle.getBoolean(KEY_IS_GAME_OVER, false)

        val historyStrings = bundle.getStringArrayList(KEY_GUESS_HISTORY) ?: arrayListOf()
        val restoredHistory = mutableListOf<com.example.vocabgame.model.GuessResult>()
        for (raw in historyStrings) {
            val parts = raw.split("|")
            if (parts.size >= 5) {
                val word = parts[1]
                val meaning = if (parts.size >= 6 && parts[5].isNotEmpty()) parts[5] else WordMeanings.getMeaning(word)
                restoredHistory.add(
                    com.example.vocabgame.model.GuessResult(
                        guessNumber = parts[0].toIntOrNull() ?: 1,
                        guessedWord = word,
                        matchingCount = parts[2].toIntOrNull() ?: 0,
                        exactPositions = parts[3].toIntOrNull() ?: 0,
                        isCorrect = parts[4].toBoolean(),
                        meaning = meaning
                    )
                )
            }
        }
        gameEngine.restoreState(secret, count, isOver, restoredHistory)
        guessAdapter.submitList(gameEngine.guessHistory.toList())
        updateHistoryVisibility()

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

        tvStatusHint.text = bundle.getString(KEY_STATUS_HINT) ?: getString(R.string.status_secret_chosen)
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

        val trackerStrings = bundle.getStringArrayList(KEY_LETTER_STATES) ?: arrayListOf()
        for (entry in trackerStrings) {
            val parts = entry.split(":")
            if (parts.size == 2) {
                val ch = parts[0].firstOrNull() ?: continue
                val state = try {
                    LetterState.valueOf(parts[1])
                } catch (e: Exception) {
                    LetterState.UNSELECTED
                }
                letterStates[ch] = state
                updateLetterView(ch, state)
            }
        }

        // Restore Change a Letter
        val chainWord = bundle.getString(KEY_CHAIN_CURRENT_WORD) ?: ""
        val chainPoints = bundle.getInt(KEY_CHAIN_POINTS, 0)
        val chainIsOver = bundle.getBoolean(KEY_CHAIN_IS_GAME_OVER, false)

        val rawChainHistory = bundle.getStringArrayList(KEY_CHAIN_HISTORY) ?: arrayListOf()
        val restoredChainHistory = mutableListOf<ChainWordEntry>()
        for (raw in rawChainHistory) {
            val parts = raw.split("|")
            if (parts.size >= 4) {
                val word = parts[1]
                val meaning = if (parts.size >= 5 && parts[4].isNotEmpty()) parts[4] else WordMeanings.getMeaning(word)
                restoredChainHistory.add(
                    ChainWordEntry(
                        stepNumber = parts[0].toIntOrNull() ?: 1,
                        word = word,
                        player = if (parts[2] == "USER") PlayerType.USER else PlayerType.COMPUTER,
                        pointsAwarded = parts[3].toIntOrNull() ?: 0,
                        meaning = meaning
                    )
                )
            }
        }

        val rawUsedWords = bundle.getStringArrayList(KEY_CHAIN_USED_WORDS) ?: arrayListOf()
        chainEngine.restoreState(chainWord, chainPoints, chainIsOver, restoredChainHistory, rawUsedWords.toSet())
        chainAdapter.submitList(chainEngine.chainHistory.toList())
        updateChainViews()

        tvChainStatus.text = bundle.getString(KEY_CHAIN_STATUS) ?: "Your turn: change 1 letter (+2 pts)!"
        if (chainIsOver) {
            etChainInput.isEnabled = false
            btnChainPlayWord.isEnabled = false
            btnChainSurrender.isEnabled = false
        } else {
            etChainInput.isEnabled = true
            btnChainPlayWord.isEnabled = true
            btnChainSurrender.isEnabled = true
        }

        // Restore Rearrange
        val rearrangeBase = bundle.getString(KEY_REARRANGE_BASE_WORD) ?: ""
        val rearrangeGameOver = bundle.getBoolean(KEY_REARRANGE_IS_GAME_OVER, false)
        val rearrangeFound = bundle.getStringArrayList(KEY_REARRANGE_FOUND_WORDS)?.toSet() ?: emptySet()
        rearrangeEngine.restoreState(rearrangeBase, rearrangeFound, rearrangeGameOver)

        if (rearrangeGameOver) {
            val combined = mutableListOf<RearrangeWordEntry>()
            combined.addAll(rearrangeEngine.getFoundWordEntries())
            combined.addAll(rearrangeEngine.getMissedWords())
            rearrangeWordAdapter.submitList(combined)
            etRearrangeInput.isEnabled = false
            btnSubmitRearrangeWord.isEnabled = false
            btnFinishRearrangeRound.isEnabled = false
        } else {
            rearrangeWordAdapter.submitList(rearrangeEngine.getFoundWordEntries())
            etRearrangeInput.isEnabled = true
            btnSubmitRearrangeWord.isEnabled = true
            btnFinishRearrangeRound.isEnabled = true
        }
        updateRearrangeViews()
        tvRearrangeStatus.text = bundle.getString(KEY_REARRANGE_STATUS) ?: "Rearrange into other valid words!"

        // Restore active game mode
        val activeGame = bundle.getString(KEY_ACTIVE_GAME) ?: GAME_GUESS_WORD
        switchToGame(activeGame)
    }

    // ==========================================
    // Rearrange Game Mechanics
    // ==========================================

    private fun startNewRearrangeGame() {
        val base = rearrangeEngine.startNewGame()
        etRearrangeInput.text.clear()
        etRearrangeInput.isEnabled = true
        btnSubmitRearrangeWord.isEnabled = true
        btnFinishRearrangeRound.isEnabled = true

        rearrangeWordAdapter.submitList(emptyList())
        updateRearrangeViews()
        tvRearrangeStatus.text = getString(R.string.rearrange_status_initial, base)
    }

    private fun handleRearrangeWordSubmission() {
        val input = etRearrangeInput.text.toString().trim().uppercase(Locale.US)
        when (val result = rearrangeEngine.submitWord(input)) {
            is RearrangeSubmitResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
            is RearrangeSubmitResult.Success -> {
                etRearrangeInput.text.clear()
                rearrangeWordAdapter.submitList(rearrangeEngine.getFoundWordEntries())
                updateRearrangeViews()
                rvRearrangeWords.smoothScrollToPosition(rearrangeWordAdapter.itemCount - 1)

                Toast.makeText(this, "+${result.pointsEarnedThisWord} points! (${result.totalPoints} total)", Toast.LENGTH_SHORT).show()

                if (result.isAllFound) {
                    onRearrangeAllWordsFound()
                } else {
                    tvRearrangeStatus.text = getString(
                        R.string.rearrange_status_progress,
                        result.wordsFoundCount,
                        result.totalPossibleCount,
                        result.totalPoints
                    )
                }
            }
        }
    }

    private fun onRearrangeAllWordsFound() {
        etRearrangeInput.isEnabled = false
        btnSubmitRearrangeWord.isEnabled = false
        btnFinishRearrangeRound.isEnabled = false

        val points = rearrangeEngine.calculatePoints()
        val foundList = rearrangeEngine.foundWords.toList()
        val base = rearrangeEngine.baseWord
        val possibleCount = rearrangeEngine.allPossibleAnagrams.size

        // Record in SQLite database
        dbHelper.recordRearrangeGame(base, foundList, possibleCount, points)
        refreshRearrangeStats()

        showRearrangeOutcomeDialog(
            title = "🏆 All Anagrams Discovered!",
            message = "Brilliant! You found all $possibleCount words that can be rearranged from '$base'.",
            wordsFoundCount = foundList.size,
            possibleCount = possibleCount,
            points = points
        )
    }

    private fun handleFinishRearrangeRound() {
        if (rearrangeEngine.isGameOver) return

        val currentPoints = rearrangeEngine.calculatePoints()
        val base = rearrangeEngine.baseWord
        val foundList = rearrangeEngine.foundWords.toList()
        val possibleCount = rearrangeEngine.allPossibleAnagrams.size

        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_finish_rearrange_title)
            .setMessage(getString(R.string.confirm_finish_rearrange_msg, currentPoints))
            .setPositiveButton("Finish & Reveal") { _, _ ->
                val finalPoints = rearrangeEngine.finishGame()
                etRearrangeInput.isEnabled = false
                btnSubmitRearrangeWord.isEnabled = false
                btnFinishRearrangeRound.isEnabled = false

                // Record in SQLite database
                dbHelper.recordRearrangeGame(base, foundList, possibleCount, finalPoints)
                refreshRearrangeStats()

                // Display all words (found + missed) in the adapter
                val combined = mutableListOf<RearrangeWordEntry>()
                combined.addAll(rearrangeEngine.getFoundWordEntries())
                combined.addAll(rearrangeEngine.getMissedWords())
                rearrangeWordAdapter.submitList(combined)
                updateRearrangeViews()

                showRearrangeOutcomeDialog(
                    title = "Round Finished",
                    message = "You discovered ${foundList.size} of $possibleCount possible words from '$base'.\nCheck the list below to study the missed words and definitions!",
                    wordsFoundCount = foundList.size,
                    possibleCount = possibleCount,
                    points = finalPoints
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateRearrangeViews() {
        tvRearrangeBaseWord.text = rearrangeEngine.baseWord
        val found = rearrangeEngine.foundWords.size
        val totalPossible = rearrangeEngine.allPossibleAnagrams.size
        val currentPts = rearrangeEngine.calculatePoints()

        tvRearrangeProgress.text = "Found: $found / $totalPossible words"
        tvRearrangeCurrentPoints.text = "Score: $currentPts pts"

        if (rearrangeWordAdapter.itemCount == 0) {
            tvEmptyRearrangeWords.visibility = View.VISIBLE
            rvRearrangeWords.visibility = View.GONE
        } else {
            tvEmptyRearrangeWords.visibility = View.GONE
            rvRearrangeWords.visibility = View.VISIBLE
        }
    }

    private fun refreshRearrangeStats() {
        val stats = dbHelper.getRearrangeAggregatedStats()
        tvRearrangeTotalPoints.text = "${stats.totalPoints} pts"
        tvRearrangeAvgWords.text = String.format(Locale.US, "%.1f", stats.averageWordsPerGame)
        tvRearrangeTotalGames.text = stats.totalGames.toString()
        tvRearrangeAvgPoints.text = String.format(Locale.US, "%.1f", stats.averagePointsPerGame)
    }

    private fun showRearrangeOutcomeDialog(title: String, message: String, wordsFoundCount: Int, possibleCount: Int, points: Int) {
        val dialog = Dialog(this)
        val density = resources.displayMetrics.density
        val padding = (24 * density).toInt()

        val card = MaterialCardView(this).apply {
            radius = 20f * density
            cardElevation = 6f * density
            setStrokeColor(ContextCompat.getColor(this@MainActivity, R.color.primary_brand))
            strokeWidth = (2 * density).toInt()
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.card_background))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
            gravity = Gravity.CENTER
        }

        val msgView = TextView(this).apply {
            text = message
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            gravity = Gravity.CENTER
            setPadding(0, (8 * density).toInt(), 0, (14 * density).toInt())
            setLineSpacing(2f * density, 1f)
        }

        val scoreBadge = TextView(this).apply {
            text = "Total Points Earned: +$points pts\n(Stored in SQLite Database)"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.success_green))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (16 * density).toInt())
        }

        val btnPlayNext = com.google.android.material.button.MaterialButton(this).apply {
            text = "Play Next Word"
            textSize = 15f
            cornerRadius = (12 * density).toInt()
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.primary_brand))
            setOnClickListener {
                dialog.dismiss()
                startNewRearrangeGame()
            }
        }

        layout.addView(titleView)
        layout.addView(msgView)
        layout.addView(scoreBadge)
        layout.addView(btnPlayNext)
        card.addView(layout)

        dialog.setContentView(card)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun showRearrangeHistoryDialog() {
        val dialog = Dialog(this)
        val density = resources.displayMetrics.density
        val padding = (20 * density).toInt()

        val card = MaterialCardView(this).apply {
            radius = 18f * density
            cardElevation = 6f * density
            setStrokeColor(ContextCompat.getColor(this@MainActivity, R.color.card_stroke))
            strokeWidth = (1.5f * density).toInt()
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.card_background))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val titleView = TextView(this).apply {
            text = getString(R.string.rearrange_dialog_history_title)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
        }

        val subtitleView = TextView(this).apply {
            text = "Historical records persisted in SQLite database"
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            setPadding(0, (2 * density).toInt(), 0, (12 * density).toInt())
        }

        val historyList = dbHelper.getRearrangeHistory(50)
        val rvHistory = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (320 * density).toInt()
            )
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        val adapter = RearrangeHistoryAdapter()
        rvHistory.adapter = adapter
        adapter.submitList(historyList)

        val emptyView = TextView(this).apply {
            text = getString(R.string.rearrange_history_empty)
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            gravity = Gravity.CENTER
            setPadding(0, (40 * density).toInt(), 0, (40 * density).toInt())
            visibility = if (historyList.isEmpty()) View.VISIBLE else View.GONE
        }

        val btnClose = com.google.android.material.button.MaterialButton(this).apply {
            text = "Close"
            textSize = 14f
            cornerRadius = (10 * density).toInt()
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.primary_brand))
            setOnClickListener { dialog.dismiss() }
        }

        layout.addView(titleView)
        layout.addView(subtitleView)
        if (historyList.isEmpty()) {
            layout.addView(emptyView)
        } else {
            layout.addView(rvHistory)
        }
        layout.addView(btnClose)
        card.addView(layout)

        dialog.setContentView(card)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

}
