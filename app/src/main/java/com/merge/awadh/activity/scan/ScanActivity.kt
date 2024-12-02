package com.merge.awadh.activity.scan

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
//import android.widget.Spinner
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.merge.awadh.BuildConfig
import com.merge.awadh.R
import com.merge.awadh.ble.BLEManager
import com.merge.awadh.ble.ENABLE_BLUETOOTH_REQUEST_CODE
import com.merge.awadh.databinding.ActivityScanBinding
import com.merge.awadh.BLEScanService
import com.merge.awadh.MainActivity
import com.merge.awadh.activity.scan.fragment.DeviceInfoFragment
import com.merge.awadh.activity.scan.fragment.RSSIFilterFragment
import com.merge.awadh.ble.BLEManager.bAdapter
import com.merge.awadh.test2
import com.merge.awadh.test4


class ScanActivity : AppCompatActivity(), ScanAdapter.Delegate, ScanInterface {

    private lateinit var binding: ActivityScanBinding
    private var scanItem: MenuItem? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val dropdownListeners = mutableListOf<DropdownSelectionListener>()


    fun registerDropdownListener(listener: DropdownSelectionListener) {
        if (!dropdownListeners.contains(listener)) {
            dropdownListeners.add(listener)
        }
    }

    fun unregisterDropdownListener(listener: DropdownSelectionListener) {
        dropdownListeners.remove(listener)
    }

    private fun notifyDropdownItemSelected(item: String) {
        for (listener in dropdownListeners) {
            listener.onDropdownItemSelected(item)
        }
    }

//    private val REQUIRED_PERMISSIONS = arrayOf(
//        Manifest.permission.BLUETOOTH,
//        Manifest.permission.BLUETOOTH_ADMIN,
//        Manifest.permission.BLUETOOTH_SCAN,  // For API 31+
//        Manifest.permission.BLUETOOTH_CONNECT // For API 31+
//    )
//
//    private const val PERMISSION_REQUEST_CODE = 101


    @RequiresApi(Build.VERSION_CODES.R)
    private fun hideSystemUI() {
        val decorView = window.decorView
        decorView.windowInsetsController?.hide(WindowInsets.Type.statusBars())
    }
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, BLEScanService::class.java))
        binding = DataBindingUtil.setContentView(this, R.layout.activity_scan)
        sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE)
//
//        val intent = Intent(this, MainActivity::class.java)
//        intent.putExtra("IS_SCAN_ACTIVITY", true) // Pass a flag indicating the context source
//        startActivity(intent)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_menu) // Use your menu icon resource
        binding.toolbar.setNavigationOnClickListener {
            // Show menu options
            showMenuOptions()
        }
        // Inflate the custom layout for the ActionBar
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(true)
        actionBar?.setDisplayUseLogoEnabled(true)
        val customActionBar = LayoutInflater.from(this).inflate(R.layout.custom_actionbar, null)

        // Set the custom view for the ActionBar
        actionBar?.customView = customActionBar

        // Find the ImageView in the custom layout and set its image resource and dimensions
        val logoImageView = customActionBar.findViewById<ImageView>(R.id.logoImageView)
        logoImageView.setImageResource(R.drawable.awadh)
        logoImageView.layoutParams.height = resources.getDimensionPixelSize(R.dimen.logo_height) // Set the desired height

        setupRecyclerView()
//        BLEManager.scanAdapter?.updateResults(BLEManager.scanResults)
//
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



        BLEManager.scanInterface = this
        BLEManager.startScan(this)

//        setupSpinner()
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




    private fun showMenuOptions() {
        // Create a popup menu anchored to the toolbar
        val popupMenu = PopupMenu(this, findViewById<Toolbar>(R.id.toolbar))

        // Add only the two menu items dynamically
        popupMenu.menu.add(0, R.id.allDevicesItem, 0, "All Devices")
        popupMenu.menu.add(0, R.id.gamingOptionsItem, 1, "Gaming Options")
        popupMenu.menu.add(0, R.id.switchThemeItem, 2, "Switch Theme")

        // Handle item selection
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.allDevicesItem -> {
                    // Handle "All Devices" action
                    findViewById<TextView>(R.id.devicesdetail).text = "All Devices:"
                    true
                }
                R.id.gamingOptionsItem -> {
                    // Navigate to Test2 activity
                    val intent = Intent(this, test2::class.java)
                    startActivity(intent)
                    true
                }
                R.id.switchThemeItem -> {
                    // Toggle the theme
                    toggleTheme()
                    true
                }
                else -> false
            }
        }

        // Show the menu
        popupMenu.show()
    }

    private fun toggleTheme() {
        // Access shared preferences
        val sharedPreferences = getSharedPreferences("appPreferences", Context.MODE_PRIVATE)
        val isDarkTheme = sharedPreferences.getBoolean("isDarkTheme", false)

        // Toggle the theme
        val newTheme = !isDarkTheme
        if (newTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Save the new theme preference
        with(sharedPreferences.edit()) {
            putBoolean("isDarkTheme", newTheme)
            apply()
        }

        // Optional: Update the theme switch state
//        findViewById<SwitchCompat>(R.id.themeSwitch).isChecked = newTheme
    }


    override fun onResume() {
        super.onResume()


//        binding.themeSwitch.isChecked = sharedPreferences.getBoolean("isDarkTheme", false)
        if (!bAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }
//    private fun setupSpinner() {
//        val options = listOf("SHT40", "LIS2DH", "WindSpeed","StepCount", "Speed Distance", "Object Finding")
//        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        binding.byteSpinner.adapter = adapter
//
//        // Set "Temperature-Humidity" as the default selected item
//        val defaultPosition = options.indexOf("SHT40")
//        if (defaultPosition != -1) {
//            binding.byteSpinner.setSelection(defaultPosition)
//        }
//
//        binding.byteSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
//                val selectedOption = options[position]
//                Toast.makeText(this@ScanActivity, "Selected: $selectedOption", Toast.LENGTH_SHORT).show()
//                onDropdownItemSelected(selectedOption)
//            }
//
//
//            override fun onNothingSelected(parent: AdapterView<*>) {
//                // No action needed here
//            }
//        }
//    }


//    override fun onDropdownItemSelected(item: String) {
//        // Logic to update the RecyclerView based on the selection
//        when (item) {
//            "SHT40" -> {
//                // Update the list for Temperature-Humidity
//            }
//            "LIS2DH" -> {
//                // Update the list for Accelerometer
//            }
//            "WindSpeed" -> {
//                // Update the list for Accelerometer
//            }
//            "StepCount" -> {
//                // Update the list for Accelerometer
//            }
//            "Speed Distance" ->{
//                // Update the list for Accelerometer
//            }
//            "Object Finding" ->{
////                filterDevicesForObjectFinding()
//
//            }
//        }
//        notifyDropdownItemSelected(item)
//    }

    override fun onStop() {
        super.onStop()
    }

    // Prompt to Enable BT
    override fun promptEnableBluetooth() {
        if (!bAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            ActivityCompat.startActivityForResult(
                this, enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE, null
            )
        }
    }



    // Request Runtime Permissions (Based on Android Version)
    @SuppressLint("ObsoleteSdkInt")
    override fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ))
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    // Request Permissions if Not Given by User (Limit 2)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (BLEManager.hasPermissions(this)) {
            BLEManager.startScan(this)
        } else {
            requestPermissions()
        }
    }

    /** Toolbar Menu */

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)

        // Get Current App Version
        menu.findItem(R.id.appVersionItem).apply {
            title = "$title ${BuildConfig.VERSION_NAME}"
        }

        scanItem = menu.findItem(R.id.scanItem)

        return true
    }

    // Item on Toolbar Selected
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.scanItem -> {
                if (BLEManager.isScanning) {
                    BLEManager.stopScan()
                    item.setIcon(R.drawable.ic_play)
                } else {
                    BLEManager.startScan(this)
                    item.setIcon(R.drawable.ic_pause)
                }
            }
            R.id.rssiFilterItem -> {
                RSSIFilterFragment().show(supportFragmentManager, "rssiFilterFragment")
            }
            R.id.deviceInfoItem -> {
                DeviceInfoFragment().show(supportFragmentManager, "deviceInfoFragment")
            }
        }


        return false
    }

    /** Recycler View */

    // Sets Up the Recycler View for BLE Scan List
    private fun setupRecyclerView() {
        // Create & Set Adapter
        BLEManager.scanAdapter = ScanAdapter(BLEManager.scanResults,  this)

        binding.scanResultsRecyclerView.apply {
            adapter = BLEManager.scanAdapter
            layoutManager = LinearLayoutManager(
                this@ScanActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }
                BLEManager.scanAdapter?.updateResults(BLEManager.scanResults)


        // Turns Off Update Animation
        val animator = binding.scanResultsRecyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }



    // Connect Button Clicked



    // Item Clicked (Show Advertising Data)
    override fun onItemClick(dialog: DialogFragment) {

        dialog.show(supportFragmentManager, "advertisingDataFragment")
    }



    /** Helper Functions */

    // Go to ConnectionInterface Activity
    override fun startIntent() {
    }

    override fun startToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }


}