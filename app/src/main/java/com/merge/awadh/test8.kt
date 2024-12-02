package com.merge.awadh

import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.merge.awadh.ble.BLEManager

class test8 : AppCompatActivity() {
    private val deviceRssiMap = mutableMapOf<String, Int>()
    private var selectedDevices: List<String>? = null
    private var selectedAddress: List<String>? = null

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var isUpdating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test8)

        // Check Bluetooth permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }

        // Get selected devices from the intent and convert to uppercase for case-insensitive comparison
        selectedDevices = intent.getStringArrayListExtra("selectedDevices")?.map { it.toUpperCase() }
        selectedAddress = intent.getStringArrayListExtra("selectedAddress")

        val deviceListView = findViewById<ListView>(R.id.deviceListView1)

        // Adapter for the ListView
        val adapter = object : ArrayAdapter<String>(this, R.layout.list_item_game2, R.id.deviceName1, selectedDevices!!) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)

                val scanResults: List<ScanResult> = BLEManager.scanResults // Assuming BLEManager holds the current list of ScanResults
                val matchingDevice = scanResults.find { it.device.address == selectedAddress?.get(position) }

                if (matchingDevice != null) {
                    val deviceName = view.findViewById<TextView>(R.id.deviceName1)
                    val rssiValue = view.findViewById<TextView>(R.id.rssiValue1)
                    val nameOrAddress = selectedDevices?.get(position) ?: "Unknown"
                    deviceName.text = nameOrAddress

                    val rssi = matchingDevice.rssi
                    rssiValue.text = "RSSI: $rssi dBm"
                }

                return view
            }
        }

        deviceListView.adapter = adapter

        // Click listener for ListView items
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val deviceName = selectedDevices?.get(position)
            val rssi = deviceRssiMap[deviceName?.toUpperCase()]
            Toast.makeText(this, "Device: $deviceName, RSSI: $rssi dBm", Toast.LENGTH_SHORT).show()
        }

        // Initialize Handler
        handler = Handler(Looper.getMainLooper())

        // Start periodic RSSI updates
        startRssiUpdates(adapter)
    }

    private fun startRssiUpdates(adapter: ArrayAdapter<String>) {
        if (isUpdating) return

        isUpdating = true
        runnable = object : Runnable {
            override fun run() {
                val scanResults: List<ScanResult> = BLEManager.scanResults
                // Update RSSI for each selected device
                selectedAddress?.forEachIndexed { index, address ->
                    val matchingDevice = scanResults.find { it.device.address == address }
                    if (matchingDevice != null) {
                        val deviceName = selectedDevices?.get(index) ?: "Unknown"
                        val rssi = matchingDevice.rssi
                        deviceRssiMap[deviceName.toUpperCase()] = rssi
                    }
                }

                // Notify adapter of the changes
                adapter.notifyDataSetChanged()

                // Repeat the process every 1 second (1000 ms)
                handler.postDelayed(this, 50)
            }
        }

        // Start the periodic updates
        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the periodic updates when the activity is destroyed
        handler.removeCallbacksAndMessages(null)
        isUpdating = false
    }
}