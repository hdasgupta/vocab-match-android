package com.example.vocabgame.ui

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.vocabgame.R
import com.example.vocabgame.data.WordMeanings
import com.example.vocabgame.model.ChainGameRecord
import com.example.vocabgame.model.CrosswordRecord
import com.example.vocabgame.model.GameType
import com.example.vocabgame.model.GuessWordRecord
import com.example.vocabgame.model.UnifiedGameHistoryRecord
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrosswordHistoryAdapter(
    private val onItemClick: ((CrosswordRecord) -> Unit)? = null
) : RecyclerView.Adapter<CrosswordHistoryAdapter.HistoryViewHolder>() {

    private val items = mutableListOf<CrosswordRecord>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.US)

    fun submitList(list: List<CrosswordRecord>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_crossword_history, parent, false)
        return HistoryViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position], dateFormat)
    }

    override fun getItemCount(): Int = items.size

    class HistoryViewHolder(
        itemView: View,
        private val onItemClick: ((CrosswordRecord) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvWordsSummary: TextView = itemView.findViewById(R.id.tvCwHistWordsSummary)
        private val tvPoints: TextView = itemView.findViewById(R.id.tvCwHistPoints)
        private val tvDownSummary: TextView = itemView.findViewById(R.id.tvCwHistDownSummary)
        private val tvGuesses: TextView = itemView.findViewById(R.id.tvCwHistGuesses)
        private val tvHints: TextView = itemView.findViewById(R.id.tvCwHistHints)
        private val tvDate: TextView = itemView.findViewById(R.id.tvCwHistDate)

        fun bind(item: CrosswordRecord, dateFormat: SimpleDateFormat) {
            tvWordsSummary.text = "1A: ${item.across1} • 3A: ${item.across2}"
            tvDownSummary.text = "1D: ${item.down1} • 2D: ${item.down2}"
            tvPoints.text = "${if (item.points >= 0) "+" else ""}${item.points} pts"
            tvGuesses.text = "Correct Guesses: ${item.correctGuesses}/12"
            tvHints.text = "Hints: ${item.hintsTaken} (-${item.hintsTaken * 10} pts)"
            tvDate.text = dateFormat.format(Date(item.timestamp))

            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}

class GuessWordHistoryAdapter(
    private val onItemClick: ((GuessWordRecord) -> Unit)? = null
) : RecyclerView.Adapter<GuessWordHistoryAdapter.GuessViewHolder>() {

    private val items = mutableListOf<GuessWordRecord>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.US)

    fun submitList(list: List<GuessWordRecord>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuessViewHolder {
        val context = parent.context
        val density = context.resources.displayMetrics.density

        val card = MaterialCardView(context).apply {
            radius = 10f * density
            cardElevation = 1f * density
            strokeWidth = (1f * density).toInt()
            setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke))
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
            val lp = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, (4 * density).toInt(), 0, (4 * density).toInt())
            layoutParams = lp
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
        }

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvWord = TextView(context).apply {
            textSize = 18f
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.primary_brand))
        }

        val space = Space(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvPoints = TextView(context).apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, R.color.success_green))
            setBackgroundResource(R.drawable.bg_stat_cell)
            setPadding((8 * density).toInt(), (3 * density).toInt(), (8 * density).toInt(), (3 * density).toInt())
        }

        topRow.addView(tvWord)
        topRow.addView(space)
        topRow.addView(tvPoints)

        val tvGuesses = TextView(context).apply {
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(0, (4 * density).toInt(), 0, 0)
        }

        val tvMeaning = TextView(context).apply {
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, (3 * density).toInt(), 0, 0)
        }

        val tvDate = TextView(context).apply {
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, (4 * density).toInt(), 0, 0)
        }

        layout.addView(topRow)
        layout.addView(tvGuesses)
        layout.addView(tvMeaning)
        layout.addView(tvDate)
        card.addView(layout)

        return GuessViewHolder(card, tvWord, tvPoints, tvGuesses, tvMeaning, tvDate, onItemClick)
    }

    override fun onBindViewHolder(holder: GuessViewHolder, position: Int) {
        holder.bind(items[position], dateFormat)
    }

    override fun getItemCount(): Int = items.size

    class GuessViewHolder(
        itemView: View,
        private val tvWord: TextView,
        private val tvPoints: TextView,
        private val tvGuesses: TextView,
        private val tvMeaning: TextView,
        private val tvDate: TextView,
        private val onItemClick: ((GuessWordRecord) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: GuessWordRecord, dateFormat: SimpleDateFormat) {
            tvWord.text = item.secretWord
            tvPoints.text = "+${String.format(Locale.US, "%.1f", item.points)} pts"
            tvGuesses.text = "Solved in ${item.guessCount} ${if (item.guessCount == 1) "guess" else "guesses"}"
            tvMeaning.text = WordMeanings.getMeaning(item.secretWord)
            tvDate.text = dateFormat.format(Date(item.timestamp))

            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}

class ChainGameHistoryAdapter(
    private val onItemClick: ((ChainGameRecord) -> Unit)? = null
) : RecyclerView.Adapter<ChainGameHistoryAdapter.ChainViewHolder>() {

    private val items = mutableListOf<ChainGameRecord>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.US)

    fun submitList(list: List<ChainGameRecord>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChainViewHolder {
        val context = parent.context
        val density = context.resources.displayMetrics.density

        val card = MaterialCardView(context).apply {
            radius = 10f * density
            cardElevation = 1f * density
            strokeWidth = (1f * density).toInt()
            setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke))
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
            val lp = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, (4 * density).toInt(), 0, (4 * density).toInt())
            layoutParams = lp
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
        }

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvWinner = TextView(context).apply {
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, R.color.primary_brand))
        }

        val space = Space(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvPoints = TextView(context).apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, R.color.success_green))
            setBackgroundResource(R.drawable.bg_stat_cell)
            setPadding((8 * density).toInt(), (3 * density).toInt(), (8 * density).toInt(), (3 * density).toInt())
        }

        topRow.addView(tvWinner)
        topRow.addView(space)
        topRow.addView(tvPoints)

        val tvSummary = TextView(context).apply {
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(0, (4 * density).toInt(), 0, 0)
        }

        val tvDate = TextView(context).apply {
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, (4 * density).toInt(), 0, 0)
        }

        layout.addView(topRow)
        layout.addView(tvSummary)
        layout.addView(tvDate)
        card.addView(layout)

        return ChainViewHolder(card, tvWinner, tvPoints, tvSummary, tvDate, onItemClick)
    }

    override fun onBindViewHolder(holder: ChainViewHolder, position: Int) {
        holder.bind(items[position], dateFormat)
    }

    override fun getItemCount(): Int = items.size

    class ChainViewHolder(
        itemView: View,
        private val tvWinner: TextView,
        private val tvPoints: TextView,
        private val tvSummary: TextView,
        private val tvDate: TextView,
        private val onItemClick: ((ChainGameRecord) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: ChainGameRecord, dateFormat: SimpleDateFormat) {
            val context = itemView.context
            val isUserWin = item.winner.equals("USER", ignoreCase = true)

            if (isUserWin) {
                tvWinner.text = "Player Victory 🏆"
                tvWinner.setTextColor(ContextCompat.getColor(context, R.color.primary_brand))
            } else {
                tvWinner.text = "Surrendered to Computer 🏳️"
                tvWinner.setTextColor(ContextCompat.getColor(context, R.color.tracker_amber))
            }

            tvPoints.text = "+${item.points} pts"
            tvSummary.text = "Chain Length: ${item.chainLength} words • You played: ${item.userWords} words"
            tvDate.text = dateFormat.format(Date(item.timestamp))

            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}

class UnifiedGameHistoryAdapter(
    private val onItemClick: ((UnifiedGameHistoryRecord) -> Unit)? = null
) : RecyclerView.Adapter<UnifiedGameHistoryAdapter.UnifiedViewHolder>() {

    private val allItems = mutableListOf<UnifiedGameHistoryRecord>()
    private val displayedItems = mutableListOf<UnifiedGameHistoryRecord>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.US)
    private var currentFilter: GameType? = null

    fun submitList(list: List<UnifiedGameHistoryRecord>) {
        allItems.clear()
        allItems.addAll(list)
        applyFilter(currentFilter)
    }

    fun filterByGameType(gameType: GameType?) {
        currentFilter = gameType
        applyFilter(gameType)
    }

    private fun applyFilter(gameType: GameType?) {
        displayedItems.clear()
        if (gameType == null) {
            displayedItems.addAll(allItems)
        } else {
            displayedItems.addAll(allItems.filter { it.gameType == gameType })
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnifiedViewHolder {
        val context = parent.context
        val density = context.resources.displayMetrics.density

        val card = MaterialCardView(context).apply {
            radius = 10f * density
            cardElevation = 1f * density
            strokeWidth = (1f * density).toInt()
            setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke))
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
            val lp = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, (4 * density).toInt(), 0, (4 * density).toInt())
            layoutParams = lp
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
        }

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvGameType = TextView(context).apply {
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setPadding((8 * density).toInt(), (2 * density).toInt(), (8 * density).toInt(), (2 * density).toInt())
        }

        val space = Space(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvPoints = TextView(context).apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_stat_cell)
            setPadding((8 * density).toInt(), (3 * density).toInt(), (8 * density).toInt(), (3 * density).toInt())
        }

        topRow.addView(tvGameType)
        topRow.addView(space)
        topRow.addView(tvPoints)

        val tvTitle = TextView(context).apply {
            textSize = 16f
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.primary_brand))
            setPadding(0, (6 * density).toInt(), 0, 0)
        }

        val tvSubtitle = TextView(context).apply {
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(0, (2 * density).toInt(), 0, 0)
        }

        val tvDate = TextView(context).apply {
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, (4 * density).toInt(), 0, 0)
        }

        layout.addView(topRow)
        layout.addView(tvTitle)
        layout.addView(tvSubtitle)
        layout.addView(tvDate)
        card.addView(layout)

        return UnifiedViewHolder(card, tvGameType, tvPoints, tvTitle, tvSubtitle, tvDate, onItemClick)
    }

    override fun onBindViewHolder(holder: UnifiedViewHolder, position: Int) {
        holder.bind(displayedItems[position], dateFormat)
    }

    override fun getItemCount(): Int = displayedItems.size

    class UnifiedViewHolder(
        itemView: View,
        private val tvGameType: TextView,
        private val tvPoints: TextView,
        private val tvTitle: TextView,
        private val tvSubtitle: TextView,
        private val tvDate: TextView,
        private val onItemClick: ((UnifiedGameHistoryRecord) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: UnifiedGameHistoryRecord, dateFormat: SimpleDateFormat) {
            val context = itemView.context
            tvGameType.text = item.gameType.displayName

            val badgeColor = when (item.gameType) {
                GameType.GUESS_WORD -> ContextCompat.getColor(context, R.color.primary_brand)
                GameType.CHANGE_LETTER -> ContextCompat.getColor(context, R.color.accent_purple)
                GameType.REARRANGE -> ContextCompat.getColor(context, R.color.success_green)
                GameType.CROSSWORD -> ContextCompat.getColor(context, R.color.tracker_amber)
            }
            val density = context.resources.displayMetrics.density
            val badgeDrawable = GradientDrawable().apply {
                setColor(badgeColor)
                cornerRadius = 6f * density
            }
            tvGameType.background = badgeDrawable

            tvTitle.text = item.title
            tvSubtitle.text = item.subtitle
            tvPoints.text = item.pointsText

            if (item.isSuccess) {
                tvPoints.setTextColor(ContextCompat.getColor(context, R.color.success_green))
            } else {
                tvPoints.setTextColor(ContextCompat.getColor(context, R.color.danger_red))
            }

            tvDate.text = dateFormat.format(Date(item.timestamp))

            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}
