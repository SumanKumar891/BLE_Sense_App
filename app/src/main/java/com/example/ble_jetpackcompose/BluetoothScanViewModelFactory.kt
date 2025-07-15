package com.example.ble_jetpackcompose

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// Factory class for creating instances of BluetoothScanViewModel
class BluetoothScanViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    // Creates a ViewModel instance
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel class is BluetoothScanViewModel
        if (modelClass.isAssignableFrom(BluetoothScanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Instantiate BluetoothScanViewModel with the provided context and cast to T
            return BluetoothScanViewModel<Any>(context) as T  // ✅ Correct instantiation
        }
        // Throw an exception for unknown ViewModel classes
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}