package com.example.blegame

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapterWrapper: BluetoothAdapterWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activitymain)

        // Initialize BluetoothAdapterWrapper
        bluetoothAdapterWrapper = BluetoothAdapterWrapper(this)

        // Check and request permissions at the start of the app
        if (!bluetoothAdapterWrapper.hasPermissions()) {
            bluetoothAdapterWrapper.requestPermissions(REQUEST_CODE_BLUETOOTH_PERMISSIONS)
        } else {
            initializeBluetooth()
        }

        // CardView for BLE App
        val cardBLEApp: CardView = findViewById(R.id.cardBLEApp)
        val btnBLE: Button = findViewById(R.id.btnBLE)
        btnBLE.setOnClickListener {
            Toast.makeText(this, "Navigating to BLE App", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, BLEAppActivity::class.java)
            startActivity(intent)
        }

        // CardView for Games Section
        val cardGames: CardView = findViewById(R.id.cardGames)
        val btnGames: Button = findViewById(R.id.btnGames)
        btnGames.setOnClickListener {
            Toast.makeText(this, "Navigating to Games Section", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, StartGameActivity::class.java)
            startActivity(intent)
        }
    }

    // Initialize Bluetooth Prompt

    private fun initializeBluetooth() {
        bluetoothAdapterWrapper.checkAndEnableBluetooth()
        Toast.makeText(this, "Bluetooth initialized", Toast.LENGTH_SHORT).show()
    }

    // Request Permissions from User

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            if (bluetoothAdapterWrapper.hasPermissions()) {
                initializeBluetooth()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted. The app may not function correctly.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_BLUETOOTH_PERMISSIONS = 1001
    }
}

