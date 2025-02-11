package com.example.blegame

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter class for displaying BLE devices in a RecyclerView
class BLEDeviceAdapter(
    private val devices: List<BLEDevice>, // List of BLE devices to display
    private val onClick: (BLEDevice) -> Unit // Callback function for item click events
) : RecyclerView.Adapter<BLEDeviceAdapter.BLEDeviceViewHolder>() {

    // View type constants for different types of RecyclerView items (if needed)
    private val TYPE_HEADER = 0
    private val TYPE_DEVICE = 1

    // Mapping of device names to their corresponding character image resources
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

    // ViewHolder class to represent each item in the RecyclerView
    inner class BLEDeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView =
            itemView.findViewById(R.id.deviceNameTextView) // Device name TextView
        val addressTextView: TextView =
            itemView.findViewById(R.id.deviceMacAddressTextView) // MAC address TextView
        val rssiTextView: TextView = itemView.findViewById(R.id.deviceRssi) // RSSI value TextView
        val characterImageView: ImageView =
            itemView.findViewById(R.id.characterImageView) // Character image view

        // Bind device data to the UI components
        fun bind(device: BLEDevice) {
            // Display the device name or a default placeholder if null
            nameTextView.text = device.name ?: "Unknown Device"
            // Display the device's MAC address
            addressTextView.text = device.address
            // Display the device's RSSI value
            rssiTextView.text = "RSSI: ${device.rssi} dBm"

            // Set the corresponding character image or a default image if the device name is not in the map
            val imageResId = characterImages[device.name] ?: R.drawable.default_character
            characterImageView.setImageResource(imageResId)

            // Set click listener for the item to trigger the onClick callback
            itemView.setOnClickListener { onClick(device) }
        }
    }

    // Create and return a ViewHolder for a new item view
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BLEDeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_card, parent, false) // Inflate item layout
        return BLEDeviceViewHolder(view)
    }

    // Bind data to the ViewHolder; supports partial updates via payloads
    override fun onBindViewHolder(
        holder: BLEDeviceViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            // Partial update: Update only the RSSI value
            val updatedRSSI = payloads[0] as? String
            holder.rssiTextView.text = "RSSI: $updatedRSSI dBm"
        } else {
            // Full update: Bind all device data
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    // Bind data to the ViewHolder (full update fallback method)
    override fun onBindViewHolder(holder: BLEDeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    // Return the total number of items in the dataset
    override fun getItemCount(): Int = devices.size
}
