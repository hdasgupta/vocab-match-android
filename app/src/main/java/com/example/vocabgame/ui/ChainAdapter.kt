package com.example.vocabgame.ui

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.vocabgame.R
import com.example.vocabgame.model.ChainWordEntry
import com.example.vocabgame.model.PlayerType

class ChainAdapter : RecyclerView.Adapter<ChainAdapter.ChainViewHolder>() {

    private val items = mutableListOf<ChainWordEntry>()

    fun submitList(list: List<ChainWordEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChainViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chain_word, parent, false)
        return ChainViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChainViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ChainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStep: TextView = itemView.findViewById(R.id.tvChainStep)
        private val tvWord: TextView = itemView.findViewById(R.id.tvChainWord)
        private val tvPoints: TextView = itemView.findViewById(R.id.tvChainPoints)
        private val tvPlayer: TextView = itemView.findViewById(R.id.tvChainPlayer)

        fun bind(item: ChainWordEntry) {
            val context = itemView.context
            val density = context.resources.displayMetrics.density

            tvStep.text = "#${item.stepNumber}"
            tvWord.text = item.word

            if (item.player == PlayerType.USER) {
                tvPlayer.text = "You"
                val userBg = GradientDrawable().apply {
                    cornerRadius = 6f * density
                    setColor(ContextCompat.getColor(context, R.color.success_green))
                }
                tvPlayer.background = userBg
                tvPlayer.setTextColor(ContextCompat.getColor(context, R.color.white))

                tvPoints.visibility = View.VISIBLE
                tvPoints.text = "+${item.pointsAwarded} pts"
            } else {
                tvPlayer.text = "Computer"
                val compBg = GradientDrawable().apply {
                    cornerRadius = 6f * density
                    setColor(ContextCompat.getColor(context, R.color.primary_brand))
                }
                tvPlayer.background = compBg
                tvPlayer.setTextColor(ContextCompat.getColor(context, R.color.white))

                tvPoints.visibility = View.GONE
            }
        }
    }
}
