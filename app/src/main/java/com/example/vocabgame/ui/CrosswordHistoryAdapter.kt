package com.example.vocabgame.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vocabgame.R
import com.example.vocabgame.model.CrosswordRecord
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
