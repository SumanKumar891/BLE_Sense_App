package com.merge.awadh

import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.merge.awadh.ble.BLEManager

class test5 : AppCompatActivity() {

    private val deviceRssiMap = mutableMapOf<String, Int>()
    private var selectedDevices: List<String>? = null
    private var selectedAddress: List<String>? = null
    private lateinit var statusTextView: TextView

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var isUpdating = false
    private var selectedDeviceIndex: Int? = null // Variable to track selected device

    // Map to store the tick status for each device (true if tick should be shown)
    private val deviceTickStatus = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test5)
        statusTextView = findViewById(R.id.statusTextView)

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
        Log.d("DeviceScan", "Selected Devices: $selectedDevices")
        Log.d("DeviceScan", "Selected Devices: $selectedAddress")

        val deviceListView = findViewById<ListView>(R.id.deviceListView)

        // Adapter for the ListView
        val adapter = object : ArrayAdapter<String>(this, R.layout.list_item_device, R.id.deviceName, selectedDevices!!) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)

                val scanResults: List<ScanResult> = BLEManager.scanResults // Assuming BLEManager holds the current list of ScanResults
                val matchingDevice = scanResults.find { it.device.address == selectedAddress?.get(position) }

                if (matchingDevice != null) {
                    val deviceName = view.findViewById<TextView>(R.id.deviceName)
//                    val rssiValue = view.findViewById<TextView>(R.id.rssiValue)
                    val tickSymbol = view.findViewById<TextView>(R.id.tickSymbol) // TextView for tick symbol

                    val nameOrAddress = selectedDevices?.get(position) ?: "Unknown"
                    deviceName.text = nameOrAddress
                    val rssi = matchingDevice.rssi
//                    rssiValue.text = "RSSI: $rssi dBm"

                    // Check if the tick should be shown based on the stored tick status
                    if (deviceTickStatus.getOrDefault(nameOrAddress, false)) {
                        tickSymbol.text = "✔" // Show the tick symbol
                        tickSymbol.visibility = View.VISIBLE
                    } else {
                        tickSymbol.text = "" // Hide the tick symbol
                    }

                    val status = when {
                        rssi < -70 -> "Your Device is far"
                        rssi in -70..-40 -> "Your Device is near"
                        rssi > -40 -> "Your Device is found"
                        else -> "RSSI not available"
                    }

                    // Update the tick symbol only when the user clicks on the device
                    if (status == "Object is found" && !deviceTickStatus.getOrDefault(nameOrAddress, false)) {
                        // Set tick symbol to true once object is found
                        deviceTickStatus[nameOrAddress] = true
                    }
                }

                return view
            }
        }

        deviceListView.adapter = adapter

        // Click listener for ListView items
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            selectedDeviceIndex = position // Track the selected device index

            // Get the corresponding device and its RSSI value
            val deviceName = selectedDevices?.get(position)
            val matchingDevice = BLEManager.scanResults.find { it.device.address == selectedAddress?.get(position) }

            matchingDevice?.let { device ->
                val rssi = device.rssi
                val status = when {
                    rssi < -70 -> "Your Device is far"
                    rssi in -70..-40 -> "Your Device is near"
                    rssi > -40 -> "Your Device is found"
                    else -> "RSSI not available"
                }

                // Display the status in the TextView
                statusTextView.text = "$status"

                // Optionally, display the RSSI value in a Toast message
                Toast.makeText(this, "Device: $deviceName, RSSI: $rssi dBm", Toast.LENGTH_SHORT)
                    .show()

                // If the device's status is "Object is found", mark the tick symbol
                if (status == "Your Device is found") {
                    deviceTickStatus[deviceName ?: "Unknown"] = true
                    adapter.notifyDataSetChanged() // Refresh the ListView
                }
            }
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

                // Update the statusTextView with the selected device's RSSI if a device is selected
                selectedDeviceIndex?.let { index ->
                    val selectedDeviceName = selectedDevices?.get(index) ?: "Unknown"
                    val selectedDeviceAddress = selectedAddress?.get(index)
                    val matchingDevice = scanResults.find { it.device.address == selectedDeviceAddress }

                    matchingDevice?.let { device ->
                        val rssi = device.rssi
                        val status = when {
                            rssi < -70 -> "Your Device is far"
                            rssi in -70..-40 -> "Your Device is near"
                            rssi > -40 -> "Your Device is found"
                            else -> "RSSI not available"
                        }

                        statusTextView.text = "$status"
                    }
                }

                // Notify adapter of the changes
                adapter.notifyDataSetChanged()

                // Repeat the process every 50 ms
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
