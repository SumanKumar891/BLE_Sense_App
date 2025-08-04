// Package declaration for the Android application
package com.blesense.app

// Import statements for Android Bluetooth, permissions, lifecycle, coroutines, and Firebase
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
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.blesense.app.ReflectanceUploadWorker
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
//import retrofit2.http.Query
import com.google.firebase.firestore.Query
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

// ViewModel class for handling Bluetooth scanning and sensor data processing
class BluetoothScanViewModel<T>(private val context: Context) : ViewModel() {

    // Firebase Firestore setup for storing reflectance readings
    private val firestore = Firebase.firestore
    private val reflectanceCollection = firestore.collection("reflectance_readings")

    // StateFlow to hold the list of discovered Bluetooth devices
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    // Scan callback for handling Bluetooth scan results
    private var scanCallback: ScanCallback? = null

    // StateFlow to track scanning status
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Coroutine Job for managing periodic scans
    private var scanJob: Job? = null

    // Thread-safe storage for historical sensor data
    private val deviceHistoricalData = ConcurrentHashMap<String, MutableList<HistoricalDataEntry>>()

    // StateFlow to manage the current game mode
    private val _currentGameMode = MutableStateFlow<GameMode>(GameMode.NONE)
    private val stepCounterOffsets = ConcurrentHashMap<String, Int>()
    val currentGameMode: StateFlow<GameMode> = _currentGameMode.asStateFlow()

    // Configure scan intervals
    companion object {
        private const val SCAN_PERIOD = 10000L // Duration of each scan: 10 seconds
        private const val SCAN_INTERVAL = 30000L // Interval between scans: 30 seconds
        private const val MAX_HISTORY_ENTRIES_PER_DEVICE = 1000 // Maximum historical entries per device to prevent memory issues
    }

    // Enum class to define available game modes
    enum class GameMode {
        NONE,
        HUNT_THE_HEROES,
        GUESS_THE_CHARACTER
    }

    // Function to set the current game mode and clear devices
    fun setGameMode(mode: GameMode) {
        _currentGameMode.value = mode
        // Clear devices when changing mode
        clearDevices()
    }

    // Data class to store historical sensor data with timestamps
    data class HistoricalDataEntry(
        val timestamp: Long,
        val sensorData: SensorData?
    )

    // Sealed class to represent different types of sensor data
    sealed class SensorData {
        abstract val deviceId: String

        // Data class for SHT40 (temperature and humidity) sensor data
        data class SHT40Data(
            override val deviceId: String,
            val temperature: String,
            val humidity: String
        ) : SensorData()

        // Data class for Lux (light intensity) sensor data
        data class LuxSensorData(
            override val deviceId: String,
            val lux: String,
            val rawData: String
        ) : SensorData()

        // Data class for LIS2DH (accelerometer) sensor data
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

        // Data class for speed and distance (SDT) sensor data
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
            val steps: String
        ) : SensorData()

        // Data class for ammonia sensor data
        data class AmmoniaSensorData(
            override val deviceId: String,
            val ammonia: String, // In ppm
            val rawData: String
        ) : SensorData()

        // Data class for optical sensor data
        data class OpticalSensorData(
            override val deviceId: String,
            val reflectanceValues: List<Float>, // List of 18 reflectance values (6 bytes each)
            val rawData: String // Raw data for debugging
        ) : SensorData()

        // Data class for dissolved oxygen (DO) sensor data
        data class DOSensorData(
            override val deviceId: String,
            val temperature: String,         // e.g., "24.78 °C"
            val doPercentage: String,        // e.g., "90.45 %"
            val doValue: String,             // e.g., "7.49 mg/L"
            val temperatureFloat: Float,     // raw float value
            val doPercentageFloat: Float,
            val doValueFloat: Float
        ) : SensorData()
    }

    // Data class to represent a Bluetooth device
    data class BluetoothDevice(
        val name: String,
        val rssi: String,
        val address: String,
        val deviceId: String,
        val sensorData: SensorData? = null
    )

    // Start periodic Bluetooth scanning
    fun startPeriodicScan(activity: Activity) {
        if (_isScanning.value) return

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            while (isActive) {
                _isScanning.value = true
                startScan(activity)
                delay(SCAN_PERIOD) // Scan for 10 seconds
                stopScan()
                _isScanning.value = false
                delay(SCAN_INTERVAL) // Wait 30 seconds before next scan
            }
        }
    }

    // Region: Scanner Management
    // Start a single Bluetooth scan
    @SuppressLint("MissingPermission")
    fun startScan(activity: Activity) {
        getBluetoothScanner()?.let { scanner ->
            val scanSettings = createScanSettings()
            scanCallback = createScanCallback()
            scanner.startScan(null, scanSettings, scanCallback)
        }
    }

    // Stop the current Bluetooth scan
    @SuppressLint("MissingPermission")
    fun stopScan() {
        getBluetoothScanner()?.let { scanner ->
            scanCallback?.let { callback ->
                scanner.stopScan(callback)
                scanCallback = null
            }
        }
    }

    // Get the Bluetooth LE scanner
    private fun getBluetoothScanner(): BluetoothLeScanner? =
        BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner

    // Create scan settings for Bluetooth LE scanning
    private fun createScanSettings(): ScanSettings =
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setLegacy(false)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .build()

    // Debug function to log optical sensor data
    fun debugOpticalSensorData(deviceAddress: String) {
        val device = _devices.value.find { it.address == deviceAddress }
        if (device?.sensorData is SensorData.OpticalSensorData) {
            val opticalData = device.sensorData as SensorData.OpticalSensorData
            Log.d("OpticalDebug", "Device: ${device.name}")
            Log.d("OpticalDebug", "Address: ${device.address}")
            Log.d("OpticalDebug", "Device ID: ${opticalData.deviceId}")
            Log.d("OpticalDebug", "Raw Data: ${opticalData.rawData}")
            Log.d("OpticalDebug", "Reflectance Values (${opticalData.reflectanceValues.size}): ${opticalData.reflectanceValues}")
        } else {
            Log.d("OpticalDebug", "No optical sensor data found for device: $deviceAddress")
        }
    }

    // Create a callback for handling Bluetooth scan results
    private fun createScanCallback(): ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                if (!hasRequiredPermissions()) return

                result.device?.let { device ->
                    val deviceName = device.name ?: return
                    val deviceAddress = device.address ?: return
                    Log.d("Scan", "Found device: $deviceName at $deviceAddress")
                    val deviceType = determineDeviceType(deviceName)

                    // Parse sensor data based on device type
                    val sensorData: BluetoothScanViewModel.SensorData? = when (deviceType) {
                        "Step Counter" -> {
                            val data = result.scanRecord?.manufacturerSpecificData?.valueAt(0)
                            parseStepCounterData(data, deviceAddress)
                        }

                        "Ammonia Sensor" -> {
                            val data = result.scanRecord?.manufacturerSpecificData?.valueAt(0)
                            parseAmmoniaSensorData(data, deviceAddress)
                        }
                        "Lux Sensor" -> {
                            val manufacturerData = result.scanRecord?.manufacturerSpecificData
                            var sensorData: SensorData? = null

                            manufacturerData?.let {
                                for (i in 0 until it.size()) {
                                    val key = it.keyAt(i)
                                    val data = it.valueAt(i)
                                    Log.d("LuxSensor", "Found manufacturer data with ID: $key, data: ${data?.joinToString { b -> "%02X".format(b) }}")
                                    if (data != null) {
                                        sensorData = parseLuxSensorData(data, deviceAddress)
                                        break // found data, stop
                                    }
                                }
                                if (sensorData == null) {
                                    Log.w("LuxSensor", "No manufacturer data available in list")
                                }
                            } ?: run {
                                Log.w("LuxSensor", "manufacturerSpecificData is null for $deviceAddress")
                            }

                            sensorData
                        }

                        else -> {
                            // Includes Optical Soil Sensor
                            parseAdvertisingData(result, deviceType)
                        }
                    }

                    val bluetoothDevice = BluetoothDevice(
                        name = deviceName,
                        address = deviceAddress,
                        rssi = result.rssi.toString(),
                        deviceId = sensorData?.deviceId ?: "Unknown",
                        sensorData = sensorData
                    )

                    updateDevice(bluetoothDevice)

                    sensorData?.let {
                        storeHistoricalData(deviceAddress, it)
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    // Check if required Bluetooth permissions are granted
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

    // Store historical sensor data in memory and Firestore (for optical sensor data)
    private fun storeHistoricalData(deviceAddress: String, sensorData: SensorData) {
        // Add to in-memory storage
        val deviceHistory = deviceHistoricalData.getOrPut(deviceAddress) { ArrayList() }
        deviceHistory.add(HistoricalDataEntry(System.currentTimeMillis(), sensorData))

        // Handle optical sensor data for Firestore upload
        if (sensorData is SensorData.OpticalSensorData) {
            // Immediate Firestore upload in the foreground
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    reflectanceCollection.add(
                        hashMapOf(
                            "timestamp" to System.currentTimeMillis(),
                            "deviceId" to sensorData.deviceId,
                            "deviceAddress" to deviceAddress,
                            "reflectanceValues" to sensorData.reflectanceValues,
                            "rawData" to sensorData.rawData,
                            "userId" to Firebase.auth.currentUser?.uid,
                            "uploadSource" to "Foreground" // For debugging
                        )
                    ).await()
                    Log.d("Firestore", "Immediate upload succeeded")
                } catch (e: Exception) {
                    Log.e("Firestore", "Immediate upload failed, enqueuing worker", e)
                    // Fallback to WorkManager if immediate upload fails
                    enqueueReflectanceWorker(deviceAddress, sensorData)
                }
            }

            // Always enqueue worker for background persistence
            enqueueReflectanceWorker(deviceAddress, sensorData)
        }
    }

    // Enqueue a WorkManager task to upload optical sensor data
    private fun enqueueReflectanceWorker(deviceAddress: String, sensorData: SensorData.OpticalSensorData) {
        val workData = workDataOf(
            "deviceAddress" to deviceAddress,
            "deviceId" to sensorData.deviceId,
            "reflectanceValues" to sensorData.reflectanceValues.toFloatArray(),
            "rawData" to sensorData.rawData
        )

        val workRequest = OneTimeWorkRequestBuilder<ReflectanceUploadWorker>()
            .setInputData(workData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10_000L, // Initial backoff of 10 seconds
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reflectance_upload_${System.currentTimeMillis()}",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    // Load persisted data from Firestore for a specific device
    fun loadPersistedData(deviceAddress: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val querySnapshot = reflectanceCollection
                    .whereEqualTo("deviceAddress", deviceAddress)
                    .whereEqualTo("userId", Firebase.auth.currentUser?.uid) // User-specific data
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                querySnapshot.documents.forEach { doc ->
                    val data = doc.toObject(ReflectanceData::class.java)
                    data?.let {
                        deviceHistoricalData.getOrPut(deviceAddress) { ArrayList() }.add(
                            HistoricalDataEntry(
                                it.timestamp,
                                SensorData.OpticalSensorData(
                                    deviceId = it.deviceId,
                                    reflectanceValues = it.reflectanceValues,
                                    rawData = it.rawData
                                )
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("Firestore", "Load failed", e)
            }
        }
    }

    // Data class for Firestore reflectance data
    private data class ReflectanceData(
        val timestamp: Long = 0,
        val deviceId: String = "",
        val deviceAddress: String = "",
        val reflectanceValues: List<Float> = emptyList(),
        val rawData: String = "",
        val userId: String? = null
    )

    // Retrieve historical data for a specific device
    fun getHistoricalDataForDevice(deviceAddress: String): List<HistoricalDataEntry> {
        return deviceHistoricalData[deviceAddress]?.toList() ?: emptyList()
    }

    // Get the latest reflectance values for a specific device
    fun getLatestReflectanceValuesForSelectedDevice(address: String): List<Float>? {
        return (deviceHistoricalData[address]
            ?.lastOrNull()
            ?.sensorData as? SensorData.OpticalSensorData
                )?.reflectanceValues
    }

    // Region: Data Parsing
    // Parse advertising data based on device type
    fun parseAdvertisingData(result: ScanResult, deviceType: String?): SensorData? {
        val manufacturerData = result.scanRecord?.manufacturerSpecificData ?: return null
        if (manufacturerData.size() == 0) return null

        val data = manufacturerData.valueAt(0) ?: return null

        // Get the device address for step counter and ammonia sensor devices
        val deviceAddress = result.device?.address ?: return null

        return when (deviceType) {
            "SHT40" -> parseSHT40Data(data)
            "LIS2DH" -> parseLIS2DHData(data)
            "Soil Sensor" -> parseSoilSensorData(data)
            "SPEED_DISTANCE" -> parseSDTData(data)
            "Metal Detector" -> parseMetalDetectorData(data)
            "Step Counter" -> parseStepCounterData(data, deviceAddress)
            "Ammonia Sensor" -> parseAmmoniaSensorData(data, deviceAddress)
            "Optical Sensor" -> parseOpticalSensorData(data, deviceAddress)
            "Lux Sensor" -> parseLuxSensorData(data, deviceAddress)
            "DO Sensor" -> parseDOSensorData(data, deviceAddress)
            else -> null
        }
    }

    // Parse Lux sensor data
    private fun parseLuxSensorData(data: ByteArray?, deviceAddress: String): SensorData? {
        if (data == null) {
            Log.d("parseLuxSensorData", "Null data array")
            return null
        }

        if (data.isEmpty()) {
            Log.d("parseLuxSensorData", "Empty data array")
            return null
        }

        val rawDataString = data.joinToString(" ") { "%02X".format(it) }
        Log.d("parseLuxSensorData", "Raw data: $rawDataString")

        // Check if we have at least 5 bytes
        if (data.size < 5) {
            Log.d("parseLuxSensorData", "Insufficient data length: ${data.size}, need at least 3 bytes")
            return null
        }

        // Extract device ID from first byte
        val deviceId = data[0].toInt() and 0xFF

        // Extract lux values from second and third bytes
        val highLux = data[1].toInt() and 0xFF
        val lowLux = data[2].toInt() and 0xFF

        // Calculate lux value
        val luxValue = (highLux * 256) + lowLux

        Log.d("parseLuxSensorData", "Device ID: $deviceId")
        Log.d("parseLuxSensorData", "High Lux: $highLux, Low Lux: $lowLux")
        Log.d("parseLuxSensorData", "Calculated Lux: $luxValue")

        return SensorData.LuxSensorData(
            deviceId = deviceId.toString(),
            lux = luxValue.toString(),
            rawData = rawDataString
        )
    }

    // Parse SHT40 (temperature and humidity) sensor data
    private fun parseSHT40Data(data: ByteArray): SensorData? {
        if (data.size < 5) return null
        return SensorData.SHT40Data(
            deviceId = data[0].toUByte().toString(),
            temperature = "${data[1].toUByte()}.${data[2].toUByte()}",
            humidity = "${data[3].toUByte()}.${data[4].toUByte()}"
        )
    }

    // Parse LIS2DH (accelerometer) sensor data
    private fun parseLIS2DHData(data: ByteArray): SensorData? {
        if (data.size < 7) return null
        return SensorData.LIS2DHData(
            deviceId = data[0].toUByte().toString(),
            x = "${data[1].toInt()}.${data[2].toUByte()}",
            y = "${data[3].toInt()}.${data[4].toUByte()}",
            z = "${data[5].toInt()}.${data[6].toUByte()}"
        )
    }

    // Parse soil sensor data
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

    // Parse speed and distance (SDT) sensor data
    private fun parseSDTData(data: ByteArray): SensorData? {
        if (data.size < 6) return null
        return SensorData.SDTData(
            deviceId = data[0].toUByte().toString(),
            speed = "${data[1].toUByte()}.${data[2].toUByte()}",
            distance = "${data[4].toUByte()}.${data[5].toUByte()}"
        )
    }

    // Parse metal detector sensor data
    private fun parseMetalDetectorData(data: ByteArray): SensorData? {
        if (data.size < 2) return null
        return SensorData.ObjectDetectorData(
            deviceId = data[0].toUByte().toString(),
            detection = data[1].toInt() != 0
        )
    }

    // Parse step counter sensor data
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

    // Parse dissolved oxygen (DO) sensor data
    private fun parseDOSensorData(data: ByteArray, deviceAddress: String): SensorData? {
        Log.d("DOSensorParser", "parseDOSensorData called")
        Log.d("DOSensorParser", "Data size: ${data.size}")
        Log.d("DOSensorParser", "Raw data: ${data.joinToString(", ") { it.toUByte().toString() }}")

        if (data.size < 8) {
            Log.d("DOSensorParser", "Data size too small (<8). Returning null.")
            return null
        }

        val deviceId = data[0].toUByte().toString() // 133

        // Parse temperature
        val tempInt = data[1].toUByte().toInt()
        val tempFrac = data[2].toUByte().toInt()
        val temperature = tempInt + tempFrac / 100f

        // Parse saturation percentage
        val satInt = data[3].toUByte().toInt()
        val satFrac = data[4].toUByte().toInt()
        val doPercentage = satInt + satFrac / 100f

        // Parse DO concentration
        val doValInt = data[5].toUByte().toInt()
        val doValFrac = data[6].toUByte().toInt()
        val doValue = doValInt + doValFrac / 100f

        Log.d(
            "DOSensorParser",
            "Parsed: deviceId=$deviceId, temp=$temperature°C, doPercentage=$doPercentage%, doValue=$doValue mg/L"
        )

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

    // Parse ammonia sensor data
    private fun parseAmmoniaSensorData(data: ByteArray?, deviceAddress: String): SensorData? {
        if (data == null) {
            Log.w("AmmoniaParser", "Null data received")
            return null
        }

        // Convert raw data to hex string for logging
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

    // Parse optical sensor data
    private fun parseOpticalSensorData(
        data: ByteArray?,
        deviceAddress: String
    ): SensorData.OpticalSensorData? {
        if (data == null) {
            Log.w("OpticalParser", "Null data from $deviceAddress")
            return null
        }

        val limitedData = data.take(108).toByteArray()
        val rawDataString = limitedData.joinToString(" ") { "%02X".format(it) }
        Log.d("OpticalParser", "Raw BLE Data (first 108 bytes): $rawDataString")

        if (limitedData.size < 108) {
            Log.w("OpticalParser", "Expected at least 108 bytes, got ${limitedData.size}")
            return SensorData.OpticalSensorData(
                deviceId = deviceAddress,
                reflectanceValues = List(18) { 0f },
                rawData = rawDataString
            )
        }

        return try {
            val reflectanceValues = (0 until 18).map { i ->
                val startIndex = i * 6
                val bytes = limitedData.sliceArray(startIndex until startIndex + 6)

                var number = 0L
                for (b in bytes) {
                    number = (number shl 8) or b.toUByte().toLong()
                }

                var reflectance = number / 1_000_000f

                if (number == 0L) {
                    reflectance = if (i == 0 || i == 8) 0.05f else 0.01f
                    Log.d("", "Original number==0 → set reflectance=$reflectance at index=$i")
                }

                reflectance
            }

            Log.d("OpticalParser", "Parsed reflectance values: $reflectanceValues")

            val max = reflectanceValues.maxOrNull()?.takeIf { it > 0f } ?: 1f
            Log.d("OpticalParser", "Max reflectance before normalization: $max")

            val normalizedReflectance = reflectanceValues.mapIndexed { i, value ->
                var normalized = value / max
                if (normalized <= 0f) {
                    normalized = 0.01f
                    Log.d("OpticalParser", "Normalized reflectance[$i] was 0/negative → set to 0.01f")
                }
                if (normalized < 0.000001f) normalized = 0.000001f
                normalized
            }

            Log.d("OpticalParser", "Final normalized reflectance: $normalizedReflectance")

            SensorData.OpticalSensorData(
                deviceId = deviceAddress,
                reflectanceValues = normalizedReflectance,
                rawData = rawDataString
            )
        } catch (e: Exception) {
            Log.e("OpticalParser", "Parse error", e)
            SensorData.OpticalSensorData(
                deviceId = deviceAddress,
                reflectanceValues = List(18) { 0f },
                rawData = "ERROR: ${e.message} | Original: $rawDataString"
            )
        }
    }

    // Reset the step counter for a specific device
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
    // Determine device type based on BLE device name
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
        name?.contains("Optical_Sensor", ignoreCase = true) == true -> "Optical Sensor"
        else -> "Unknown Device"
    }

    // Update device information in the StateFlow
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

    // Clear all devices from the StateFlow
    fun clearDevices() {
        _devices.value = emptyList()
    }

    // Clean up resources when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        stopScan()
    }

    // Format reflectance values for display
    fun formatReflectanceValues(values: List<Float>): List<String> {
        return values.map { String.format(Locale.US, "%.6f", it) }
    }
}