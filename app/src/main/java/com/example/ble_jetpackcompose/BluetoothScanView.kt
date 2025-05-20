package com.example.ble_jetpackcompose

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class BluetoothScanViewModel<T>(private val context: Context) : ViewModel() {
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private var scanCallback: ScanCallback? = null

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    private var scanJob: Job? = null

    // Historical data storage - using ConcurrentHashMap for thread safety
    private val deviceHistoricalData = ConcurrentHashMap<String, MutableList<HistoricalDataEntry>>()
    private val _currentGameMode = MutableStateFlow<GameMode>(GameMode.NONE)
    private val stepCounterOffsets = ConcurrentHashMap<String, Int>()
    val currentGameMode: StateFlow<GameMode> = _currentGameMode.asStateFlow()

    // Configure scan intervals
    companion object {
        private const val SCAN_PERIOD = 10000L // 10 seconds
        private const val SCAN_INTERVAL = 30000L // 30 seconds between scans
        private const val MAX_HISTORY_ENTRIES_PER_DEVICE = 1000 // Limit entries to prevent memory issues
    }

    enum class GameMode {
        NONE,
        HUNT_THE_HEROES,
        GUESS_THE_CHARACTER
    }

    fun setGameMode(mode: GameMode) {
        _currentGameMode.value = mode
        // Clear devices when changing mode
        clearDevices()
    }

    // Data class to store historical data with timestamps
    data class HistoricalDataEntry(
        val timestamp: Long,
        val sensorData: SensorData?
    )

    // Region: Data Classes and Sealed Classes
    sealed class SensorData {
        abstract val deviceId: String

        data class SHT40Data(
            override val deviceId: String,
            val temperature: String,
            val humidity: String
        ) : SensorData()

        data class LuxData(
            override val deviceId: String,
            val calculatedLux: Float
        ) : SensorData()

        data class LIS2DHData(
            override val deviceId: String,
            val x: String,
            val y: String,
            val z: String
        ) : SensorData()

        data class SoilSensorData(
            override val deviceId: String,
            val nitrogen: String,
            val phosphorus: String,
            val potassium: String,
            val moisture: String,
            val temperature: String,
            val ec: String,
            val pH: String
        ) : SensorData()

        data class SDTData(
            override val deviceId: String,
            val speed: String,
            val distance: String
        ) : SensorData()

        data class ObjectDetectorData(
            override val deviceId: String,
            val detection: Boolean
        ) : SensorData()

        data class StepCounterData(
            override val deviceId: String,
            val steps: String
        ) : SensorData()

        // New Ammonia Sensor data class
        data class AmmoniaSensorData(
            override val deviceId: String,
            val ammonia: String, // In ppm
            val rawData: String
        ) : SensorData()
    }

    data class BluetoothDevice(
        val name: String,
        val rssi: String,
        val address: String,
        val deviceId: String,
        val sensorData: SensorData? = null
    )


    fun startPeriodicScan(activity: Activity) {
        if (_isScanning.value) return

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            while (isActive) {
                _isScanning.value = true
                startScan(activity)
                delay(SCAN_PERIOD) // 10 seconds
                stopScan()
                _isScanning.value = false
                delay(SCAN_INTERVAL) // 30 seconds between scans
            }
        }
    }

    // Region: Scanner Management
    @SuppressLint("MissingPermission")
    fun startScan(activity: Activity) {
        getBluetoothScanner()?.let { scanner ->
            val scanSettings = createScanSettings()
            scanCallback = createScanCallback()
            scanner.startScan(null, scanSettings, scanCallback)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        getBluetoothScanner()?.let { scanner ->
            scanCallback?.let { callback ->
                scanner.stopScan(callback)
                scanCallback = null
            }
        }
    }

    private fun getBluetoothScanner(): BluetoothLeScanner? =
        BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner

    private fun createScanSettings(): ScanSettings =
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setLegacy(false)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .build()

    private fun createScanCallback(): ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                // Check required permissions first
                if (!hasRequiredPermissions()) {
                    return
                }

                result.device?.let { device ->
                    try {
                        // Check if we can access device properties
                        val deviceName = device.name
                        val deviceAddress = device.address

                        if (deviceAddress.isNullOrEmpty() || deviceName.isNullOrEmpty()) return

                        val deviceType = determineDeviceType(deviceName)

                        // For step counter and ammonia sensor, we need to pass the device address
                        val sensorData = when (deviceType) {
                            "Step Counter" -> parseStepCounterData(result.scanRecord?.manufacturerSpecificData?.valueAt(0), deviceAddress)
                            "Ammonia Sensor" -> parseAmmoniaSensorData(result.scanRecord?.manufacturerSpecificData?.valueAt(0), deviceAddress)
                            else -> parseAdvertisingData(result, deviceType)
                        }

                        val bluetoothDevice = BluetoothDevice(
                            name = deviceName,
                            address = deviceAddress,
                            rssi = result.rssi.toString(),
                            deviceId = sensorData?.deviceId ?: "Unknown",
                            sensorData = sensorData
                        )

                        updateDevice(bluetoothDevice)

                        // Store data in history with timestamp
                        sensorData?.let {
                            storeHistoricalData(deviceAddress, it)
                        }
                    } catch (e: SecurityException) {
                        // Handle security exception
                    }
                }
            } catch (e: SecurityException) {
                // Handle security exception
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun storeHistoricalData(deviceAddress: String, sensorData: SensorData) {
        // Get or create the device's history list
        val deviceHistory = deviceHistoricalData.getOrPut(deviceAddress) {
            ArrayList()
        }

        // Add new entry with current timestamp
        deviceHistory.add(
            HistoricalDataEntry(
                timestamp = System.currentTimeMillis(),
                sensorData = sensorData
            )
        )

        // Trim if exceeds maximum size
        if (deviceHistory.size > MAX_HISTORY_ENTRIES_PER_DEVICE) {
            // Remove oldest entries
            val excessEntries = deviceHistory.size - MAX_HISTORY_ENTRIES_PER_DEVICE
            repeat(excessEntries) {
                deviceHistory.removeAt(0)
            }
        }
    }

    fun getHistoricalDataForDevice(deviceAddress: String): List<HistoricalDataEntry> {
        return deviceHistoricalData[deviceAddress]?.toList() ?: emptyList()
    }

    // Region: Data Parsing
    fun parseAdvertisingData(result: ScanResult, deviceType: String?): SensorData? {
        val manufacturerData = result.scanRecord?.manufacturerSpecificData ?: return null
        if (manufacturerData.size() == 0) return null

        val data = manufacturerData.valueAt(0) ?: return null

        // Get the device address for step counter and ammonia sensor devices
        val deviceAddress = result.device?.address ?: return null

        return when (deviceType) {
            "SHT40" -> parseSHT40Data(data)
            "Lux Sensor" -> parseLuxSensorData(data)
            "LIS2DH" -> parseLIS2DHData(data)
            "Soil Sensor" -> parseSoilSensorData(data)
            "SPEED_DISTANCE" -> parseSDTData(data)
            "Metal Detector" -> parseMetalDetectorData(data)
            "Step Counter" -> parseStepCounterData(data, deviceAddress)
            "Ammonia Sensor" -> parseAmmoniaSensorData(data, deviceAddress)
            else -> null
        }
    }

    private fun parseSHT40Data(data: ByteArray): SensorData? {
        if (data.size < 5) return null
        return SensorData.SHT40Data(
            deviceId = data[0].toUByte().toString(),
            temperature = "${data[1].toUByte()}.${data[2].toUByte()}",
            humidity = "${data[3].toUByte()}.${data[4].toUByte()}"
        )
    }

    private fun parseLuxSensorData(data: ByteArray): SensorData? {
        if (data.size < 11) return null
        val deviceId = data[0].toUByte().toString()
        val digits = data.sliceArray(5..10).map { it.toUByte().toInt() }
        val luxValueStr = digits.joinToString("") { it.toString() }.trimStart('0')
        val luxValue = if (luxValueStr.isEmpty()) 0f else luxValueStr.toFloat()
        return SensorData.LuxData(
            deviceId = deviceId,
            calculatedLux = luxValue
        )
    }

    private fun parseLIS2DHData(data: ByteArray): SensorData? {
        if (data.size < 7) return null
        return SensorData.LIS2DHData(
            deviceId = data[0].toUByte().toString(),
            x = "${data[1].toInt()}.${data[2].toUByte()}",
            y = "${data[3].toInt()}.${data[4].toUByte()}",
            z = "${data[5].toInt()}.${data[6].toUByte()}"
        )
    }

    private fun parseSoilSensorData(data: ByteArray): SensorData? {
        if (data.size < 11) return null
        return SensorData.SoilSensorData(
            deviceId = data[0].toUByte().toString(),
            nitrogen = data[1].toUByte().toString(),
            phosphorus = data[2].toUByte().toString(),
            potassium = data[3].toUByte().toString(),
            moisture = data[4].toUByte().toString(),
            temperature = "${data[5].toUByte()}.${data[6].toUByte()}",
            ec = "${data[7].toUByte()}.${data[8].toUByte()}",
            pH = "${data[9].toUByte()}.${data[10].toUByte()}"
        )
    }

    private fun parseSDTData(data: ByteArray): SensorData? {
        if (data.size < 6) return null
        return SensorData.SDTData(
            deviceId = data[0].toUByte().toString(),
            speed = "${data[1].toUByte()}.${data[2].toUByte()}",
            distance = "${data[4].toUByte()}.${data[5].toUByte()}"
        )
    }

    private fun parseMetalDetectorData(data: ByteArray): SensorData? {
        if (data.size < 2) return null
        return SensorData.ObjectDetectorData(
            deviceId = data[0].toUByte().toString(),
            detection = data[1].toInt() != 0
        )
    }

    private fun parseStepCounterData(data: ByteArray?, deviceAddress: String): SensorData? {
        if (data == null || data.size < 5) return null
        val deviceId = data[0].toUByte().toString()
        val rawStepCount = (data[3].toUByte().toInt() shl 8) or data[4].toUByte().toInt()
        val offset = stepCounterOffsets[deviceAddress] ?: 0
        val adjustedStepCount = (rawStepCount - offset).coerceAtLeast(0)
        return SensorData.StepCounterData(
            deviceId = deviceId,
            steps = adjustedStepCount.toString()
        )
    }

    private fun parseAmmoniaSensorData(data: ByteArray?, deviceAddress: String): SensorData? {
        if (data == null) {
            Log.w("AmmoniaParser", "Null data received")
            return null
        }

        // Convert raw data to hex string
        val rawDataString = data.joinToString(" ") { byte -> String.format("%02X", byte) }
        Log.d("AmmoniaParser", "Raw BLE Data: $rawDataString")

        if (data.size < 6) {
            Log.w("AmmoniaParser", "Invalid Data: Data size (${data.size}) is too short")
            return null
        }

        val deviceId = data[0].toUByte().toString()
        val ammoniaPpm = try {
            data[5].toUByte().toFloat()
        } catch (e: Exception) {
            Log.e("AmmoniaParser", "Error parsing ammonia value", e)
            return null
        }

        val ammoniaValue = String.format(Locale.US, "%.1f", ammoniaPpm)

        return SensorData.AmmoniaSensorData(
            deviceId = deviceId,
            ammonia = "$ammoniaValue ppm",
            rawData = rawDataString // Include raw data
        )
    }
    fun resetStepCounter(deviceAddress: String) {
        viewModelScope.launch {
            val devices = _devices.value
            val device = devices.find { it.address == deviceAddress }

            if (device != null && device.sensorData is SensorData.StepCounterData) {
                val currentSteps = (device.sensorData as SensorData.StepCounterData).steps.toIntOrNull() ?: 0
                val currentOffset = stepCounterOffsets[deviceAddress] ?: 0
                val rawStepCount = currentSteps + currentOffset
                stepCounterOffsets[deviceAddress] = rawStepCount
                val updatedDevice = device.copy(
                    sensorData = SensorData.StepCounterData(
                        deviceId = device.deviceId,
                        steps = "0"
                    )
                )
                _devices.update { currentDevices ->
                    currentDevices.map {
                        if (it.address == deviceAddress) updatedDevice else it
                    }
                }
            }
        }
    }

    // Region: Utility Functions
    private fun determineDeviceType(name: String?): String? = when {
        name?.contains("SHT", ignoreCase = true) == true -> "SHT40"
        name?.contains("Data", ignoreCase = true) == true -> "Lux Sensor"
        name?.contains("Activity", ignoreCase = true) == true -> "LIS2DH"
        name?.contains("SOIL", ignoreCase = true) == true -> "Soil Sensor"
        name?.contains("Speed", ignoreCase = true) == true -> "SPEED_DISTANCE"
        name?.contains("Object", ignoreCase = true) == true -> "Metal Detector"
        name?.contains("Step", ignoreCase = true) == true -> "Step Counter"
        name?.contains("NH", ignoreCase = true) == true -> "Ammonia Sensor"
        else -> null
    }

    private fun updateDevice(newDevice: BluetoothDevice, sensorData: SensorData? = null) {
        _devices.update { devices ->
            val existingDeviceIndex = devices.indexOfFirst { it.address == newDevice.address }
            if (existingDeviceIndex >= 0) {
                devices.toMutableList().apply {
                    val updatedDevice = if (sensorData != null) {
                        this[existingDeviceIndex].copy(sensorData = sensorData)
                    } else {
                        newDevice
                    }
                    this[existingDeviceIndex] = updatedDevice
                }
            } else {
                devices + if (sensorData != null) {
                    newDevice.copy(sensorData = sensorData)
                } else {
                    newDevice
                }
            }
        }
    }

    fun clearDevices() {
        _devices.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        stopScan()
    }
}
