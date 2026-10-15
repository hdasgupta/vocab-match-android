package com.example.vocabgame.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vocabgame.R
import com.example.vocabgame.model.RearrangeRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RearrangeHistoryAdapter : RecyclerView.Adapter<RearrangeHistoryAdapter.HistoryViewHolder>() {

    private val items = mutableListOf<RearrangeRecord>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.US)

    fun submitList(list: List<RearrangeRecord>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rearrange_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position], dateFormat)
    }

    override fun getItemCount(): Int = items.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBaseWord: TextView = itemView.findViewById(R.id.tvHistBaseWord)
        private val tvPoints: TextView = itemView.findViewById(R.id.tvHistPoints)
        private val tvWordsList: TextView = itemView.findViewById(R.id.tvHistWordsList)
        private val tvDate: TextView = itemView.findViewById(R.id.tvHistDate)

        fun bind(item: RearrangeRecord, dateFormat: SimpleDateFormat) {
            tvBaseWord.text = item.baseWord
            tvPoints.text = "+${item.points} pts"
            val foundStr = if (item.wordsFoundList.isNotEmpty()) {
                item.wordsFoundList.joinToString(", ")
            } else {
                "None"
            }
            tvWordsList.text = "Words found (${item.wordsFoundCount}/${item.possibleWordsCount}): $foundStr"
            tvDate.text = dateFormat.format(Date(item.timestamp))
        }
    }
}
