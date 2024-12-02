package com.merge.awadh.activity.scan

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.merge.awadh.R
import com.merge.awadh.activity.scan.fragment.AdvertisingDataFragment
import com.merge.awadh.ble.BLEManager
import com.merge.awadh.databinding.RowScanResultBinding
import kotlin.collections.ArrayList

@SuppressLint("NotifyDataSetChanged", "MissingPermission")
class adapter1(
    private var items: MutableList<ScanResult>,
    private val delegate: ScanActivity
) : RecyclerView.Adapter<adapter1.ViewHolder>() {

    private val itemsCopy: MutableList<ScanResult> = ArrayList()

    init {
        itemsCopy.addAll(items) // Copy all items to the backup list
    }

    interface Delegate {
        fun onItemClick(dialog: DialogFragment)
    }

    inner class ViewHolder(val binding: RowScanResultBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val result = items[bindingAdapterPosition]
                val dialog = AdvertisingDataFragment.newInstance(result.device.address)
                delegate.onItemClick(dialog)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<RowScanResultBinding>(
            inflater,
            R.layout.row_scan_result,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = items[position]
        with(holder.binding) {
            deviceName.text = result.device.name ?: "Unnamed"
            macAddress.text = result.device.address
            signalStrength.text = "${result.rssi} dBm"
        }
    }

    override fun getItemCount(): Int = items.size

    // Update results and refresh RecyclerView
    fun updateResults(newResults: List<ScanResult>) {
        items = newResults.toMutableList()
        itemsCopy.clear()
        itemsCopy.addAll(items)
        notifyDataSetChanged()
    }


    // Filter RecyclerView based on input value and type
    fun filter(value: String, type:String) {
        if (value.isNotEmpty()) {
            itemsCopy.clear()
            itemsCopy.addAll(items)

            when (type) {
                "name" -> BLEManager.deviceNameFilter = value
                "rssi" -> BLEManager.deviceRSSIFilter = value
            }
            BLEManager.scanResults.clear()

            for (item in itemsCopy) {
                if (filterCompare(item, value, type)) {
                    BLEManager.scanResults.add(item)
                }
            }


            notifyDataSetChanged()
        }
    }

    // Compare filter conditions
    fun filterCompare(item: ScanResult, value: String, type: String): Boolean {
        if (value.isEmpty()) return true

        return if (type == "name") {
            item.device.name?.contains(value, ignoreCase = true) == true
        } else {
            item.rssi >= value.toInt()
        }
    }
}
