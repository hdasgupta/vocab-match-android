package com.example.vocabgame.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.vocabgame.R
import com.example.vocabgame.model.RearrangeWordEntry
import com.google.android.material.card.MaterialCardView

class RearrangeWordAdapter : RecyclerView.Adapter<RearrangeWordAdapter.WordViewHolder>() {

    private val items = mutableListOf<RearrangeWordEntry>()

    fun submitList(list: List<RearrangeWordEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rearrange_word, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.cardRearrangeWord)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvRearrangeStatusBadge)
        private val tvWord: TextView = itemView.findViewById(R.id.tvRearrangeWord)
        private val tvPoints: TextView = itemView.findViewById(R.id.tvRearrangePoints)
        private val tvMeaning: TextView = itemView.findViewById(R.id.tvRearrangeMeaning)

        fun bind(item: RearrangeWordEntry) {
            val context = itemView.context
            tvWord.text = item.word
            tvMeaning.text = item.meaning

            if (item.isFound) {
                tvBadge.text = "✓"
                tvBadge.setTextColor(ContextCompat.getColor(context, R.color.success_green))
                tvPoints.visibility = View.VISIBLE
                tvPoints.text = "+${item.pointsAwarded} pts"
                tvPoints.setTextColor(ContextCompat.getColor(context, R.color.success_green))
                card.strokeColor = ContextCompat.getColor(context, R.color.card_stroke)
            } else {
                tvBadge.text = "✕"
                tvBadge.setTextColor(ContextCompat.getColor(context, R.color.danger_red))
                tvPoints.visibility = View.VISIBLE
                tvPoints.text = "Missed (0 pts)"
                tvPoints.setTextColor(ContextCompat.getColor(context, R.color.danger_red))
                card.strokeColor = ContextCompat.getColor(context, R.color.danger_red)
            }
        }
    }
}
