package com.ai.bookkeeping.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.ai.bookkeeping.R

class IconPickerAdapter(
    private val icons: List<IconItem>,
    private val onIconSelected: (IconItem) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.ViewHolder>() {

    data class IconItem(val name: String, val resId: Int)

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val icon = icons[position]
        holder.bind(icon, position == selectedPosition)
    }

    override fun getItemCount() = icons.size

    fun setSelectedIcon(iconName: String) {
        val index = icons.indexOfFirst { it.name == iconName }
        if (index >= 0) {
            val oldPosition = selectedPosition
            selectedPosition = index
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
        }
    }

    fun getSelectedIcon(): IconItem = icons[selectedPosition]

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val frameIcon: FrameLayout = itemView.findViewById(R.id.frameIcon)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)

        fun bind(icon: IconItem, isSelected: Boolean) {
            ivIcon.setImageResource(icon.resId)
            frameIcon.isSelected = isSelected

            itemView.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onIconSelected(icon)
            }
        }
    }

    companion object {
        fun getDefaultIcons(): List<IconItem> {
            return listOf(
                IconItem("ic_food", R.drawable.ic_food),
                IconItem("ic_transport", R.drawable.ic_transport),
                IconItem("ic_shopping", R.drawable.ic_shopping),
                IconItem("ic_entertainment", R.drawable.ic_entertainment),
                IconItem("ic_medical", R.drawable.ic_medical),
                IconItem("ic_education", R.drawable.ic_education),
                IconItem("ic_housing", R.drawable.ic_housing),
                IconItem("ic_communication", R.drawable.ic_communication),
                IconItem("ic_clothing", R.drawable.ic_clothing),
                IconItem("ic_salary", R.drawable.ic_salary),
                IconItem("ic_bonus", R.drawable.ic_bonus),
                IconItem("ic_investment", R.drawable.ic_investment),
                IconItem("ic_parttime", R.drawable.ic_parttime),
                IconItem("ic_redpacket", R.drawable.ic_redpacket),
                IconItem("ic_other", R.drawable.ic_other)
            )
        }
    }
}
