package com.merge.awadh.activity.scan

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.merge.awadh.R
import com.merge.awadh.activity.scan.fragment.AdvertisingDataFragment
import com.merge.awadh.ble.BLEManager
import com.merge.awadh.databinding.RowScanResultBinding
import com.merge.awadh.test4
import com.merge.awadh.test5
import kotlin.collections.ArrayList

@SuppressLint("NotifyDataSetChanged", "MissingPermission")
class ScanAdapter(
    private val items: List<ScanResult>,
    private val delegate: ScanAdapter.Delegate?
) : RecyclerView.Adapter<ScanAdapter.ViewHolder>() {

    private val itemsCopy: ArrayList<ScanResult> = arrayListOf()
    private val selectedDevices = mutableSetOf<ScanResult>()
    private var currentFilterType: String = "SHT40"

    interface Delegate {
        fun onItemClick(dialog: DialogFragment)
    }



    inner class ViewHolder(val binding: RowScanResultBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val result = items[bindingAdapterPosition]
                val dialog = AdvertisingDataFragment.newInstance(result.device.address)
                delegate?.onItemClick(dialog)
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
            signalStrength.text = "RSSI value: ${result.rssi} dBm"

        }
    }

    override fun getItemCount() = items.size


    // Filter Recycler View by given text
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

    fun filterCompare(item: ScanResult, value: String, type: String): Boolean {
        if (value.isEmpty()) return true

        return if (type == "name") {
            item.device.name != null && item.device.name.uppercase().contains(value.uppercase())
        } else {
            item.rssi >= value.toInt()
        }
    }

    fun updateResults(filteredList: List<ScanResult>) {
        // Replace the current dataset with the filtered results
        itemsCopy.clear() // Clear the existing itemsCopy
        itemsCopy.addAll(filteredList)
        (items as ArrayList).clear()
        (items as ArrayList).addAll(filteredList)
        notifyDataSetChanged() // Notify the adapter of changes
    }
    fun updateFilter(type: String) {
        currentFilterType = type
        notifyDataSetChanged()
    }



}


//val context = itemView.context
//val intent = Intent(context, test5::class.java)
// Pass the device's RSSI value and other details if needed
//intent.putExtra("device_name", result.device.name)
//intent.putExtra("rssi", result.rssi)
//context.startActivity(intent)

