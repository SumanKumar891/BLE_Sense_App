package com.example.blegame

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class SoilSensorAdapter : RecyclerView.Adapter<SoilSensorAdapter.ViewHolder>() {
    private var items: List<SoilSensorDataItem> = emptyList()

    fun updateData(newItems: List<SoilSensorDataItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_soil_sensor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleText: TextView = view.findViewById(R.id.titleText)
        private val valueText: TextView = view.findViewById(R.id.valueText)
        private val unitText: TextView = view.findViewById(R.id.unitText)

        fun bind(item: SoilSensorDataItem) {
            titleText.text = item.title
            valueText.text = item.value
            unitText.text = item.unit
        }
    }
}