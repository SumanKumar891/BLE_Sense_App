package com.blesense.app

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

// ViewModel for managing Bluetooth Low Energy (BLE) scanning and device data
class BluetoothScanViewModel<T>(private val context: Context) : ViewModel() {

    // StateFlow to hold the list of discovered Bluetooth devices
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    // Callback for BLE scan results
    private var scanCallback: ScanCallback? = null

    // StateFlow to track scanning status
    private val _isScanning = MutableStateFlow(false)

    // StateFlow to track the latest packet ID for UI observation
    private val _latestPacketId = MutableStateFlow(-1)
    val latestPacketId: StateFlow<Int> = _latestPacketId.asStateFlow()

    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    val dataLoggerPacketHistory = MutableStateFlow<List<BluetoothScanViewModel.SensorData.DataLoggerData>>(emptyList())


    // Coroutine job for periodic scanning
    private var scanJob: Job? = null

    // Thread-safe storage for historical sensor data per device
    private val deviceHistoricalData = ConcurrentHashMap<String, MutableList<HistoricalDataEntry>>()

    // StateFlow to track the current game mode
    private val _currentGameMode = MutableStateFlow<GameMode>(GameMode.NONE)
    private val stepCounterOffsets = ConcurrentHashMap<String, Int>()
    val currentGameMode: StateFlow<GameMode> = _currentGameMode.asStateFlow()

    // Companion object for constants
    companion object {
        private const val SCAN_PERIOD = 10000L // Duration of each scan in milliseconds
        private const val SCAN_INTERVAL = 30000L // Interval between scans in milliseconds
        private const val MAX_HISTORY_ENTRIES_PER_DEVICE = 1000 // Maximum historical entries per device
    }

    // Enum to represent different game modes
    enum class GameMode {
        NONE,
        HUNT_THE_HEROES,
        GUESS_THE_CHARACTER
    }

    // Sets the current game mode and clears the device list
    fun setGameMode(mode: GameMode) {
        _currentGameMode.value = mode
        clearDevices()
    }

    // Data class to store historical sensor data with timestamp
    data class HistoricalDataEntry(
        val timestamp: Long,
        val sensorData: SensorData?
    )

    // Sealed class for different types of sensor data
    sealed class SensorData {
        abstract val deviceId: String

        // Data class for SHT40 sensor data (temperature and humidity)
        data class SHT40Data(
            override val deviceId: String,
            val temperature: String,
            val humidity: String
        ) : SensorData()

        // Data class for Lux sensor data
        data class LuxSensorData(
            override val deviceId: String,
            val lux: String,
            val rawData: String
        ) : SensorData()

        // Data class for LIS2DH accelerometer data
        data class LIS2DHData(
            override val deviceId: String,
            val x: String,
            val y: String,
            val z: String
        ) : SensorData()

        // Data class for soil sensor data
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

        // Data class for speed and distance sensor data
        data class SDTData(
            override val deviceId: String,
            val speed: String,
            val distance: String
        ) : SensorData()

        // Data class for object detection sensor data
        data class ObjectDetectorData(
            override val deviceId: String,
            val detection: Boolean
        ) : SensorData()

        // Data class for step counter sensor data
        data class StepCounterData(
            override val deviceId: String,
            val steps: String,
            val rawData: ByteArray? // Added raw data field
        ) : SensorData()

        // Data class for ammonia sensor data
        data class AmmoniaSensorData(
            override val deviceId: String,
            val ammonia: String,
            val rawData: String
        ) : SensorData()

        // Data class for data logger sensor data
        data class DataLoggerData(
            override val deviceId: String,
            val rawData: String,
            val packetId: Int,
            val payloadPackets: List<List<Int>>,
            val isContinuousData: Boolean = false,
            val continuousDataPoint: List<Int>? = null
        ) : SensorData() {
            // Returns all data points, either continuous or packet-based
            val allDataPoints: List<List<Int>>
                get() = if (isContinuousData && continuousDataPoint != null) {
                    listOf(continuousDataPoint)
                } else {
                    payloadPackets
                }

            // Validates the data
            val isValid: Boolean
                get() = packetId >= 0 && allDataPoints.isNotEmpty()

            // Formats raw data for display
            val displayRawData: String
                get() = if (isContinuousData) {
                    "Continuous: ${continuousDataPoint?.joinToString() ?: "No data"}"
                } else {
                    "Large Packet ID: $packetId, Packets: ${payloadPackets.size}"
                }
        }

        // Data class for dissolved oxygen (DO) sensor data
        data class DOSensorData(
            override val deviceId: String,
            val temperature: String,
            val doPercentage: String,
            val doValue: String,
            val temperatureFloat: Float,
            val doPercentageFloat: Float,
            val doValueFloat: Float
        ) : SensorData()
    }

    // Data class for representing a Bluetooth device
    data class BluetoothDevice(
        val name: String,
        val rssi: String,
        val address: String,
        val deviceId: String,
        val sensorData: SensorData? = null
    )

    // Starts periodic BLE scanning
    fun startPeriodicScan(activity: Activity) {
        if (_isScanning.value) return

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            while (isActive) {
                _isScanning.value = true
                startScan(activity)
                delay(SCAN_PERIOD)
                stopScan()
                _isScanning.value = false
                delay(SCAN_INTERVAL)
            }
        }
    }

    // Starts a single BLE scan
    @SuppressLint("MissingPermission")
    fun startScan(activity: Activity?) {
        getBluetoothScanner()?.let { scanner ->
            val scanSettings = createScanSettings()
            scanCallback = createScanCallback()
            scanner.startScan(null, scanSettings, scanCallback)
        }
    }

    // Stops the current BLE scan
    @SuppressLint("MissingPermission")
    fun stopScan() {
        getBluetoothScanner()?.let { scanner ->
            scanCallback?.let { callback ->
                scanner.stopScan(callback)
                scanCallback = null
            }
        }
    }

    // Retrieves the Bluetooth LE scanner
    private fun getBluetoothScanner(): BluetoothLeScanner? =
        BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner

    // Creates scan settings for BLE scanning
    private fun createScanSettings(): ScanSettings =
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setLegacy(false)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .build()

    // Creates a callback for handling BLE scan results
    private fun createScanCallback(): ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                if (!hasRequiredPermissions()) return

                result.device?.let { device ->
                    val deviceName = device.name ?: return
                    val deviceAddress = device.address ?: return
                    val deviceType = determineDeviceType(deviceName)
                    val manufacturerData = result.scanRecord?.manufacturerSpecificData ?: return
                    if (manufacturerData.size() == 0) return

                    // Parse sensor data based on device type
                    val sensorData: SensorData? = when (deviceType) {
                        "Step Counter" -> {
                            val data = manufacturerData.valueAt(0)
                            parseStepCounterData(data, deviceAddress)
                        }
                        "Ammonia Sensor" -> {
                            val data = manufacturerData.valueAt(0)
                            parseAmmoniaSensorData(data, deviceAddress)
                        }
                        "Lux Sensor" -> {
                            var sensorData: SensorData? = null
                            for (i in 0 until manufacturerData.size()) {
                                val key = manufacturerData.keyAt(i)
                                val data = manufacturerData.valueAt(i)
                                if (data != null) {
                                    sensorData = parseLuxSensorData(data, deviceAddress)
                                    break
                                }
                            }
                            sensorData
                        }
                        "DataLogger" -> {
                            val data = manufacturerData.valueAt(0)
                            parseDataLoggerData(data, deviceAddress)
                        }
                        else -> {
                            parseAdvertisingData(result, deviceType)
                        }
                    }

                    // Create a Bluetooth device object
                    val bluetoothDevice = BluetoothDevice(
                        name = deviceName,
                        address = deviceAddress,
                        rssi = result.rssi.toString(),
                        deviceId = sensorData?.deviceId ?: "Unknown",
                        sensorData = sensorData
                    )

                    updateDevice(bluetoothDevice)
                }
            } catch (e: SecurityException) {
                // Handle security exception silently
            }
        }
    }
    // Checks if the required permissions are granted
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

    // Retrieves historical data for a specific device
    fun getHistoricalDataForDevice(deviceAddress: String): List<HistoricalDataEntry> {
        return deviceHistoricalData[deviceAddress]?.toList() ?: emptyList()
    }

    // Parses advertising data based on device type
    fun parseAdvertisingData(result: ScanResult, deviceType: String?): SensorData? {
        val manufacturerData = result.scanRecord?.manufacturerSpecificData ?: return null
        if (manufacturerData.size() == 0) return null
        val data = manufacturerData.valueAt(0) ?: return null
        val deviceAddress = result.device?.address ?: return null

        return when (deviceType) {
            "SHT40" -> parseSHT40Data(data)
            "LIS2DH" -> parseLIS2DHData(data)
            "Soil Sensor" -> parseSoilSensorData(data)
            "SPEED_DISTANCE" -> parseSDTData(data)
            "Metal Detector" -> parseMetalDetectorData(data)
            "Step Counter" -> parseStepCounterData(data, deviceAddress)
            "Ammonia Sensor" -> parseAmmoniaSensorData(data, deviceAddress)
            "Lux Sensor" -> parseLuxSensorData(data, deviceAddress)
            "DO Sensor" -> parseDOSensorData(data, deviceAddress)
            "DataLogger" -> parseDataLoggerData(data, deviceAddress)
            else -> null
        }
    }

    // Parses Lux sensor data from advertisement
    private fun parseLuxSensorData(data: ByteArray?, deviceAddress: String): SensorData? {
        if (data == null || data.isEmpty()) return null
        val rawDataString = data.joinToString(" ") { "%02X".format(it) }
        if (data.size < 5) return null
        val deviceId = data[0].toInt() and 0xFF
        val highLux = data[1].toInt() and 0xFF
        val lowLux = data[2].toInt() and 0xFF
        val luxValue = (highLux * 256) + lowLux
        return SensorData.LuxSensorData(
            deviceId = deviceId.toString(),
            lux = luxValue.toString(),
            rawData = rawDataString
        )
    }

    // Parses DataLogger sensor data from advertisement
    private fun parseDataLoggerData(data: ByteArray?, deviceAddress: String): SensorData.DataLoggerData? {
        if (data == null) return null

        val rawData = data.joinToString(" ") { byte -> "%02X".format(byte) }

        // Required payload size (without manufacturer ID)
        val requiredSize = 234
        val fixedData = when {
            data.size < requiredSize -> data + ByteArray(requiredSize - data.size) { 0 }
            data.size > requiredSize -> data.copyOf(requiredSize)
            else -> data
        }

        // Structure: [0-230: payload data] [231: device id] [232-233: footer]
        val deviceId = fixedData[231].toInt() and 0xFF
        val footer1 = fixedData[232].toInt() and 0xFF
        val footer2 = fixedData[233].toInt() and 0xFF

        _latestPacketId.value = deviceId

        // Parse payload data (bytes 0 to 230, total 231 bytes)
        val payloadEndIndex = 231
        val payloadPackets = mutableListOf<List<Int>>()
        var index = 0

        // Parse triplets from payload section
        while (index + 2 < payloadEndIndex) {
            val x = fixedData[index++].toInt() and 0xFF
            val y = fixedData[index++].toInt() and 0xFF
            val z = fixedData[index++].toInt() and 0xFF
            payloadPackets.add(listOf(x, y, z))
        }

        // Handle remaining bytes in payload
        if (index < payloadEndIndex) {
            val remainingBytes = mutableListOf<Int>()
            while (index < payloadEndIndex) {
                remainingBytes.add(fixedData[index++].toInt() and 0xFF)
            }
        }
        val dataLoggerDataObj = SensorData.DataLoggerData(
            deviceId =  "DataLogger_Large_$deviceId",
            rawData = rawData,
            packetId = deviceId,
            payloadPackets = payloadPackets,
            isContinuousData = false
        )

        if (dataLoggerDataObj.packetId >= 0 && dataLoggerDataObj.payloadPackets.isNotEmpty()) {
            val newList = dataLoggerPacketHistory.value.toMutableList().apply { add(dataLoggerDataObj) }
            dataLoggerPacketHistory.value = newList
        }

        return dataLoggerDataObj
    }

    // Parses SHT40 sensor data
    private fun parseSHT40Data(data: ByteArray): SensorData? {
        if (data.size < 5) return null
        val tempInt = data[1].toInt()
        val tempFrac = data[2].toUByte().toInt()
        val humInt = data[3].toInt()
        val humFrac = data[4].toUByte().toInt()
        val temperature = tempInt + tempFrac / 10000.0
        val humidity = humInt + humFrac / 10000.0
        return SensorData.SHT40Data(
            deviceId = data[0].toUByte().toString(),
            temperature = String.format("%.2f", temperature),
            humidity = String.format("%.2f", humidity)
        )
    }

    // Parses LIS2DH accelerometer data
    private fun parseLIS2DHData(data: ByteArray): SensorData? {
        if (data.size < 7) return null
        return SensorData.LIS2DHData(
            deviceId = data[0].toUByte().toString(),
            x = "${data[1].toInt()}.${data[2].toUByte()}",
            y = "${data[3].toInt()}.${data[4].toUByte()}",
            z = "${data[5].toInt()}.${data[6].toUByte()}"
        )
    }

    // Parses soil sensor data
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

    // Parses speed and distance sensor data
    private fun parseSDTData(data: ByteArray): SensorData? {
        if (data.size < 6) return null
        return SensorData.SDTData(
            deviceId = data[0].toUByte().toString(),
            speed = "${data[1].toUByte()}.${data[2].toUByte()}",
            distance = "${data[4].toUByte()}.${data[5].toUByte()}"
        )
    }

    // Parses metal detector data
    private fun parseMetalDetectorData(data: ByteArray): SensorData? {
        if (data.size < 2) return null
        return SensorData.ObjectDetectorData(
            deviceId = data[0].toUByte().toString(),
            detection = data[1].toInt() != 0
        )
    }

    // Parses step counter data
    private fun parseStepCounterData(data: ByteArray?, deviceAddress: String): SensorData? {
        // Validate data
        if (data == null) {
            return null
        }

        // Extract device ID (first byte, if available)
        val deviceId = if (data.isNotEmpty()) {
            data[0].toUByte().toString()
        } else {
            "Unknown"
        }

        // Calculate step count (bytes 3 and 4, if available)
        val rawStepCount = if (data.size >= 5) {
            (data[3].toUByte().toInt() shl 8) or data[4].toUByte().toInt()
        } else {
            0 // Default to 0 if not enough bytes
        }

        // Apply offset for step count adjustment
        val offset = stepCounterOffsets[deviceAddress] ?: 0
        val adjustedStepCount = (rawStepCount - offset).coerceAtLeast(0)

        // Create and return StepCounterData object with full raw data
        return SensorData.StepCounterData(
            deviceId = deviceId,
            steps = adjustedStepCount.toString(),
            rawData = data.copyOf() // Store a copy of the full raw byte array
        )
    }

    // Parses dissolved oxygen (DO) sensor data
    private fun parseDOSensorData(data: ByteArray, deviceAddress: String): SensorData? {
        if (data.size < 8) return null

        val deviceId = data[0].toUByte().toString()
        val tempInt = data[1].toUByte().toInt()
        val tempFrac = data[2].toUByte().toInt()
        val temperature = tempInt + tempFrac / 100f
        val satInt = data[3].toUByte().toInt()
        val satFrac = data[4].toUByte().toInt()
        val doPercentage = satInt + satFrac / 100f
        val doValInt = data[5].toUByte().toInt()
        val doValFrac = data[6].toUByte().toInt()
        val doValue = doValInt + doValFrac / 100f

        return SensorData.DOSensorData(
            deviceId = deviceId,
            temperature = "%.2f °C".format(temperature),
            doPercentage = "%.2f %%".format(doPercentage),
            doValue = "%.2f mg/L".format(doValue),
            temperatureFloat = temperature,
            doPercentageFloat = doPercentage,
            doValueFloat = doValue
        )
    }

    // Parses ammonia sensor data
    private fun parseAmmoniaSensorData(data: ByteArray?, deviceAddress: String): SensorData? {
        if (data == null || data.size < 6) return null
        val rawDataString = data.joinToString(" ") { byte -> String.format("%02X", byte) }
        val deviceId = data[0].toUByte().toString()
        val ammoniaPpm = try {
            data[5].toUByte().toFloat()
        } catch (e: Exception) {
            return null
        }
        val ammoniaValue = String.format(Locale.US, "%.1f", ammoniaPpm)
        return SensorData.AmmoniaSensorData(
            deviceId = deviceId,
            ammonia = "$ammoniaValue ppm",
            rawData = rawDataString
        )
    }


    // Reset the step counter for a specific device
    fun resetStepCounter(deviceAddress: String) {
        viewModelScope.launch {
            // Find the device in the current device list
            val devices = _devices.value
            val device = devices.find { it.address == deviceAddress }

            // Reset step counter if the device is a step counter
            if (device != null && device.sensorData is SensorData.StepCounterData) {
                val stepData = device.sensorData as SensorData.StepCounterData
                val currentSteps = stepData.steps.toIntOrNull() ?: 0
                val currentOffset = stepCounterOffsets[deviceAddress] ?: 0

                // Calculate new offset to reset steps to zero
                val rawStepCount = currentSteps + currentOffset
                stepCounterOffsets[deviceAddress] = rawStepCount

                // Update device with reset step count while preserving raw data
                val updatedDevice = device.copy(
                    sensorData = SensorData.StepCounterData(
                        deviceId = device.deviceId,
                        steps = "0",
                        rawData = stepData.rawData // Preserve existing raw data
                    )
                )

                // Update the device list
                _devices.update { currentDevices ->
                    currentDevices.map {
                        if (it.address == deviceAddress) updatedDevice else it
                    }
                }
            }
        }
    }

    // Determines the device type based on its name
    private fun determineDeviceType(name: String?): String = when {
        name?.contains("SHT", ignoreCase = true) == true -> "SHT40"
        name?.contains("Lux_Data", ignoreCase = true) == true -> "Lux Sensor"
        name?.contains("SOIL", ignoreCase = true) == true -> "Soil Sensor"
        name?.contains("Activity", ignoreCase = true) == true -> "LIS2DH"
        name?.contains("Speed", ignoreCase = true) == true -> "SPEED_DISTANCE"
        name?.contains("Object", ignoreCase = true) == true -> "Metal Detector"
        name?.contains("Step", ignoreCase = true) == true -> "Step Counter"
        name?.contains("NH", ignoreCase = true) == true -> "Ammonia Sensor"
        name?.contains("DO_Sensor", ignoreCase = true) == true -> "DO Sensor"
        name?.contains("Dissolved", ignoreCase = true) == true -> "DO Sensor"
        name?.contains("DataLogger", ignoreCase = true) == true -> "DataLogger"
        name?.contains("Data Logger", ignoreCase = true) == true -> "DataLogger"
        name?.contains("DLOG", ignoreCase = true) == true -> "DataLogger"
        else -> "Unknown Device"
    }

    // Updates the device list with new or updated device data
    private fun updateDevice(newDevice: BluetoothDevice, sensorData: SensorData? = null) {
        _devices.update { devices ->
            val existingDeviceIndex = devices.indexOfFirst { it.address == newDevice.address }
            if (existingDeviceIndex >= 0) {
                val updatedList = devices.toMutableList()
                val updatedDevice = if (sensorData != null) {
                    updatedList[existingDeviceIndex].copy(sensorData = sensorData)
                } else {
                    newDevice
                }
                updatedList[existingDeviceIndex] = updatedDevice
                updatedList
            } else {
                devices + if (sensorData != null) {
                    newDevice.copy(sensorData = sensorData)
                } else {
                    newDevice
                }
            }
        }
    }

    // Clears the list of discovered devices
    fun clearDevices() {
        _devices.value = emptyList()
    }

    // Cleans up resources when the ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        stopScan()
    }
}