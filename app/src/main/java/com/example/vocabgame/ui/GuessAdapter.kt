package com.example.vocabgame.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.vocabgame.R
import com.example.vocabgame.model.GuessResult
import com.google.android.material.card.MaterialCardView

class GuessAdapter : RecyclerView.Adapter<GuessAdapter.GuessViewHolder>() {

    private val items = mutableListOf<GuessResult>()

    fun submitList(list: List<GuessResult>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuessViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_guess, parent, false)
        return GuessViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuessViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class GuessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.cardGuess)
        private val tvGuessNumber: TextView = itemView.findViewById(R.id.tvGuessNumber)
        private val tvGuessedWord: TextView = itemView.findViewById(R.id.tvGuessedWord)
        private val tvMatchFeedback: TextView = itemView.findViewById(R.id.tvMatchFeedback)
        private val tvPositionsFeedback: TextView = itemView.findViewById(R.id.tvPositionsFeedback)
        private val tvGuessMeaning: TextView = itemView.findViewById(R.id.tvGuessMeaning)

        fun bind(item: GuessResult) {
            val context = itemView.context
            tvGuessNumber.text = "#${item.guessNumber}"
            tvGuessedWord.text = item.guessedWord

            if (item.meaning.isNotEmpty()) {
                tvGuessMeaning.visibility = View.VISIBLE
                tvGuessMeaning.text = item.meaning
            } else {
                tvGuessMeaning.visibility = View.GONE
            }

            if (item.isCorrect) {
                tvMatchFeedback.text = context.getString(R.string.feedback_exact_word)
                tvPositionsFeedback.text = context.getString(R.string.feedback_all_matched)
                card.strokeColor = ContextCompat.getColor(context, R.color.success_green)
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.success_light))
            } else {
                tvMatchFeedback.text = context.resources.getQuantityString(
                    R.plurals.letters_matching_format,
                    item.matchingCount,
                    item.matchingCount
                )
                tvPositionsFeedback.text = context.resources.getQuantityString(
                    R.plurals.positions_exact_format,
                    item.exactPositions,
                    item.exactPositions
                )
                card.strokeColor = ContextCompat.getColor(context, R.color.card_stroke)
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
            }
        }
    }
}
