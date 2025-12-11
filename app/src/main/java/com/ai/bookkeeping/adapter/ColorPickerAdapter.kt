package com.ai.bookkeeping.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.ai.bookkeeping.R

class ColorPickerAdapter(
    private val colors: List<String>,
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorPickerAdapter.ViewHolder>() {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val color = colors[position]
        holder.bind(color, position == selectedPosition)
    }

    override fun getItemCount() = colors.size

    fun setSelectedColor(color: String) {
        val index = colors.indexOfFirst { it.equals(color, ignoreCase = true) }
        if (index >= 0) {
            val oldPosition = selectedPosition
            selectedPosition = index
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
        }
    }

    fun getSelectedColor(): String = colors[selectedPosition]

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewColor: View = itemView.findViewById(R.id.viewColor)
        private val ivCheck: ImageView = itemView.findViewById(R.id.ivCheck)

        fun bind(color: String, isSelected: Boolean) {
            val drawable = viewColor.background as? GradientDrawable
                ?: GradientDrawable().also { viewColor.background = it }
            drawable.shape = GradientDrawable.OVAL
            try {
                drawable.setColor(Color.parseColor(color))
            } catch (e: Exception) {
                drawable.setColor(Color.GRAY)
            }

            ivCheck.visibility = if (isSelected) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onColorSelected(color)
            }
        }
    }

    companion object {
        fun getDefaultColors(): List<String> {
            return listOf(
                "#FF6B6B",  // Red
                "#FF8E53",  // Orange
                "#FFD93D",  // Yellow
                "#6BCB77",  // Green
                "#4D96FF",  // Blue
                "#6C63FF",  // Purple
                "#C850C0",  // Pink
                "#00D9FF",  // Cyan
                "#A66CFF",  // Violet
                "#9D4EDD",  // Deep Purple
                "#2EC4B6",  // Teal
                "#E07A5F"   // Terracotta
            )
        }
    }
}
