package com.merge.awadh

import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.merge.awadh.activity.scan.ScanAdapter
import com.merge.awadh.ble.BLEManager
import com.merge.awadh.ble.BLEManager.scanAdapter
import com.merge.awadh.databinding.ActivityTest4Binding

class test4: AppCompatActivity(), ScanAdapter.Delegate {
    private lateinit var binding: ActivityTest4Binding
    private val selectedDevices = mutableSetOf<ScanResult>()
    private lateinit var gestureDetector: GestureDetector
    private lateinit var selectedDevicesButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, BLEScanService::class.java))
        binding = DataBindingUtil.setContentView(this, R.layout.activity_test4)

////        val rainbowBorder = findViewById<View>(R.id.rainbowBorderView)
//        val animation = rainbowBorder.background as AnimationDrawable
//        animation.start()

        setupRecyclerView()

        val searchView = findViewById<SearchView>(R.id.searchBar)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Optional: Handle the search button submit action if needed
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filter the devices in the RecyclerView
                filterDevices(newText ?: "")
                return true
            }
        })
        selectedDevicesButton = findViewById(R.id.selectedDevicesButton)
        selectedDevicesButton.setOnClickListener {
            handleSelectedDevices()
        }
    }


    private fun handleSelectedDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_BLUETOOTH_CONNECT
            )
            return
        }

        val selectedDeviceNames = ArrayList<String>()
        val selectedDeviceRSSIs = ArrayList<Int>()
        val selectedDeviceAddress = ArrayList<String>()

        selectedDevices.forEach { scanResult ->
            val deviceName = scanResult.device.name
            val deviceRssi = scanResult.rssi
            val deviceAddress = scanResult.device.address
            if (!deviceName.isNullOrEmpty()) {
                selectedDeviceNames.add(deviceName)
                selectedDeviceRSSIs.add(deviceRssi)
                selectedDeviceAddress.add(deviceAddress)
            }
        }

        if (selectedDeviceNames.isNotEmpty()) {
            val intent = Intent(this, test5::class.java).apply {
                putStringArrayListExtra("selectedDevices", selectedDeviceNames)
                putStringArrayListExtra("selectedAddress", selectedDeviceAddress)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "No devices selected", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted
                Toast.makeText(this, "Bluetooth permission granted. Please try again.", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied
                Toast.makeText(this, "Bluetooth permission is required to get device names.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterDevices(query: String) {
        val filteredList = if (query.isEmpty()) {
            BLEManager.scanResults // Show all devices when the query is empty
        } else {
            BLEManager.scanResults.filter { scanResult ->
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@filter false
                }

                val deviceName = scanResult.device.name
                deviceName?.contains(query, ignoreCase = true) == true
            }
        }

        BLEManager.scanAdapter?.updateResults(filteredList)

        val deviceNotFound = findViewById<TextView>(R.id.deviceNotFound)
        val recyclerView = binding.scanResultsRecyclerView
        if (query.isEmpty()) {
            // When query is empty, reset to show all devices and hide "Device not found"
            deviceNotFound.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        } else if (filteredList.isEmpty()) {
            // When query is non-empty and no matches are found, show "Device not found"
            deviceNotFound.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            // When query is non-empty and matches are found, show the RecyclerView
            deviceNotFound.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        // Create and set Adapter
        BLEManager.scanAdapter = ScanAdapter(BLEManager.scanResults, BLEManager.delegate)

        binding.scanResultsRecyclerView.apply {
            adapter = BLEManager.scanAdapter
            layoutManager = LinearLayoutManager(this@test4, RecyclerView.VERTICAL, false)
            isNestedScrollingEnabled = false
        }

        BLEManager.scanAdapter?.updateResults(BLEManager.scanResults)

        // Turn off update animation
        val animator = binding.scanResultsRecyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

        // Handle item clicks using OnItemTouchListener
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                handleItemClick(e)
                return super.onSingleTapUp(e)
            }
        })

        // Attach touch listener
        binding.scanResultsRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return gestureDetector.onTouchEvent(e) // Pass touch events to the GestureDetector
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun handleItemClick(e: MotionEvent) {
        val childView = binding.scanResultsRecyclerView.findChildViewUnder(e.x, e.y)
        childView?.let {
            val position = binding.scanResultsRecyclerView.getChildAdapterPosition(it)
            if (position != RecyclerView.NO_POSITION) {
                val scanResult = BLEManager.scanResults[position]

                // Toggle selection
                if (selectedDevices.contains(scanResult)) {
                    selectedDevices.remove(scanResult)
                    it.setBackgroundResource(R.drawable.shape_bg1) // Reset background color
                } else {
                    selectedDevices.add(scanResult)
                    it.setBackgroundColor(Color.parseColor("#8B0000")) // Dark red color
                }

                BLEManager.scanAdapter?.notifyItemChanged(position)
            }
        }
    }

    override fun onItemClick(dialog: DialogFragment) {
        dialog.show(supportFragmentManager, "AdvertisingDataDialog")
    }
    companion object {
        const val REQUEST_BLUETOOTH_CONNECT = 1001
    }

}
