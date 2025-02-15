package com.example.blegame

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.blegame.R.drawable.notfound

// Adapter class for managing and displaying BLE devices
class GameDeviceAdapter(
    private val devices: List<BLEDevice>,
    private val onClick: (BLEDevice) -> Unit
) : RecyclerView.Adapter<GameDeviceAdapter.BLEDeviceViewHolder>() {


    // List of Game Devices

    private val characterImages = mapOf(
        "Scarlet Witch" to R.drawable.scarlet_witch,
        "Black Widow" to R.drawable.black_widow,
        "Captain Marvel" to R.drawable.captain_marvel,
        "Wasp" to R.drawable.wasp,
        "Hela" to R.drawable.hela,
        "Hulk" to R.drawable.hulk3,
        "Thor" to R.drawable.thor,
        "Iron_Man" to R.drawable.iron_man1,
        "Spider Man" to R.drawable.spider_man,
        "Captain America" to R.drawable.captain_america
    )

    inner class BLEDeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.deviceNameTextView)
        private val addressTextView: TextView = itemView.findViewById(R.id.deviceMacAddressTextView)
        val rssiTextView: TextView = itemView.findViewById(R.id.deviceRssi)
        private val characterImageView: ImageView = itemView.findViewById(R.id.characterImageView)
        val foundLabel: TextView = itemView.findViewById(R.id.foundLabelTextView) // Add this line

        fun bind(device: BLEDevice) {
            nameTextView.text = device.name ?: "Unknown Device"
            addressTextView.text = device.address
            rssiTextView.text = "RSSI: ${device.rssi} dBm"

            val imageResId = characterImages[device.name] ?: notfound
            characterImageView.setImageResource(imageResId)

            // Show/hide found label based on device status
            foundLabel.visibility = if (device.isFound) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onClick(device) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BLEDeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_card, parent, false)
        return BLEDeviceViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: BLEDeviceViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            when (val payload = payloads[0]) {
                is String -> holder.rssiTextView.text = "RSSI: $payload dBm"
                is Boolean -> holder.foundLabel.visibility =
                    if (payload) View.VISIBLE else View.GONE

                else -> super.onBindViewHolder(holder, position, payloads)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: BLEDeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size
}
