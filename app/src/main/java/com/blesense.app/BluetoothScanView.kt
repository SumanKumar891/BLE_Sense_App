package com.blesense.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
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

/**
 * ViewModel for BLE scanning.
 * REMOVED: DO Sensor, Metal Detector, Step Counter
 * KEPT: SHT40, Lux, LIS2DH, Soil, SDT, Ammonia, DataLogger
 */
class BluetoothScanViewModel<T>(private val context: Context) : ViewModel() {

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _latestPacketId = MutableStateFlow(-1)
    val latestPacketId: StateFlow<Int> = _latestPacketId.asStateFlow()

    val dataLoggerPacketHistory = MutableStateFlow<List<SensorData.DataLoggerData>>(emptyList())

    private var scanJob: Job? = null
    private val deviceHistoricalData = ConcurrentHashMap<String, MutableList<HistoricalDataEntry>>()

    companion object {
        private const val SCAN_PERIOD = 10000L
        private const val SCAN_INTERVAL = 30000L
        private const val MAX_HISTORY_ENTRIES_PER_DEVICE = 1000
    }

    data class HistoricalDataEntry(
        val timestamp: Long,
        val sensorData: SensorData?
    )

    sealed class SensorData {
        abstract val deviceId: String

        // KEPT: Weather - Temperature & Humidity
        data class SHT40Data(
            override val deviceId: String,
            val temperature: String,
            val humidity: String
        ) : SensorData()

        // KEPT: Lux Sensor
        data class LuxSensorData(
            override val deviceId: String,
            val lux: String,
            val rawData: String
        ) : SensorData()

        // KEPT: Accelerometer
        data class LIS2DHData(
            override val deviceId: String,
            val x: String,
            val y: String,
            val z: String
        ) : SensorData()

        // KEPT: Soil Sensor
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

        // KEPT: Speed & Distance
        data class SDTData(
            override val deviceId: String,
            val speed: String,
            val distance: String
        ) : SensorData()

        // KEPT: Ammonia
        data class AmmoniaSensorData(
            override val deviceId: String,
            val ammonia: String,
            val rawData: String
        ) : SensorData()

        // KEPT: DataLogger (main large packet)
        data class DataLoggerData(
            override val deviceId: String,
            val rawData: String,
            val packetId: Int,
            val payloadPackets: List<List<Int>>,
            val isContinuousData: Boolean = false,
            val continuousDataPoint: List<Int>? = null
        ) : SensorData() {
            val allDataPoints: List<List<Int>>
                get() = if (isContinuousData && continuousDataPoint != null) {
                    listOf(continuousDataPoint)
                } else {
                    payloadPackets
                }

            val isValid: Boolean
                get() = packetId >= 0 && allDataPoints.isNotEmpty()

            val displayRawData: String
                get() = if (isContinuousData) {
                    "Continuous: ${continuousDataPoint?.joinToString() ?: "No data"}"
                } else {
                    "Large Packet ID: $packetId, Packets: ${payloadPackets.size}"
                }
        }
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
                delay(SCAN_PERIOD)
                stopScan()
                _isScanning.value = false
                delay(SCAN_INTERVAL)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(activity: Activity?) {
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
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setLegacy(false)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .build()

    private var scanCallback: ScanCallback? = null

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

                    val sensorData: SensorData? = when (deviceType) {
                        "Ammonia Sensor" -> {
                            val data = manufacturerData.valueAt(0)
                            parseAmmoniaSensorData(data, deviceAddress)
                        }
                        "Lux Sensor" -> {
                            var sensorData: SensorData? = null
                            for (i in 0 until manufacturerData.size()) {
                                val data = manufacturerData.valueAt(i) ?: continue
                                sensorData = parseLuxSensorData(data, deviceAddress)
                                break
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
                // Ignore
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun sendAdvertiseCommandToSensor(deviceAddress: String) {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val advertiser = bluetoothAdapter.bluetoothLeAdvertiser ?: return

            val commandBytes = byteArrayOf(0xA1.toByte(), 0x01.toByte())

            val data = AdvertiseData.Builder()
                .addManufacturerData(0x004C, commandBytes)
                .setIncludeDeviceName(false)
                .build()

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build()

            advertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    println("Command advertisement started.")
                }

                override fun onStartFailure(errorCode: Int) {
                    println("Advertisement failed: $errorCode")
                }
            })

            Handler(Looper.getMainLooper()).postDelayed({
                advertiser.stopAdvertising(object : AdvertiseCallback() {})
                println("Command advertisement stopped.")
            }, 2000)
        } catch (e: Exception) {
            println("Failed: ${e.message}")
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

    fun getHistoricalDataForDevice(deviceAddress: String): List<HistoricalDataEntry> {
        return deviceHistoricalData[deviceAddress]?.toList() ?: emptyList()
    }

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
            "Ammonia Sensor" -> parseAmmoniaSensorData(data, deviceAddress)
            "Lux Sensor" -> parseLuxSensorData(data, deviceAddress)
            "DataLogger" -> parseDataLoggerData(data, deviceAddress)
            else -> null
        }
    }

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

    private fun parseDataLoggerData(data: ByteArray?, deviceAddress: String): SensorData.DataLoggerData? {
        if (data == null) return null

        val rawData = data.joinToString(" ") { "%02X".format(it) }
        val requiredSize = 234
        val fixedData = when {
            data.size < requiredSize -> data + ByteArray(requiredSize - data.size) { 0 }
            data.size > requiredSize -> data.copyOf(requiredSize)
            else -> data
        }

        val deviceId = fixedData[231].toInt() and 0xFF
        _latestPacketId.value = deviceId

        val payloadEndIndex = 231
        val payloadPackets = mutableListOf<List<Int>>()
        var index = 0

        while (index + 2 < payloadEndIndex) {
            val x = fixedData[index++].toInt() and 0xFF
            val y = fixedData[index++].toInt() and 0xFF
            val z = fixedData[index++].toInt() and 0xFF
            payloadPackets.add(listOf(x, y, z))
        }

        val dataLoggerDataObj = SensorData.DataLoggerData(
            deviceId = "Data optionalLogger_Large_$deviceId",
            rawData = rawData,
            packetId = deviceId,
            payloadPackets = payloadPackets,
            isContinuousData = false
        )

        if (dataLoggerDataObj.isValid) {
            val newList = dataLoggerPacketHistory.value.toMutableList().apply { add(dataLoggerDataObj) }
            dataLoggerPacketHistory.value = newList
        }

        return dataLoggerDataObj
    }

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

    private fun parseAmmoniaSensorData(data: ByteArray?, deviceAddress: String): SensorData? {
        if (data == null || data.size < 6) return null
        val rawDataString = data.joinToString(" ") { String.format("%02X", it) }
        val deviceId = data[0].toUByte().toString()
        val ammoniaPpm = try { data[5].toUByte().toFloat() } catch (e: Exception) { return null }
        val ammoniaValue = String.format(Locale.US, "%.1f", ammoniaPpm)
        return SensorData.AmmoniaSensorData(
            deviceId = deviceId,
            ammonia = "$ammoniaValue ppm",
            rawData = rawDataString
        )
    }

    private fun determineDeviceType(name: String?): String = when {
        name?.contains("SHT", ignoreCase = true) == true -> "SHT40"
        name?.contains("Lux_Data", ignoreCase = true) == true -> "Lux Sensor"
        name?.contains("SOIL", ignoreCase = true) == true -> "Soil Sensor"
        name?.contains("Activity", ignoreCase = true) == true -> "LIS2DH"
        name?.contains("Speed", ignoreCase = true) == true -> "SPEED_DISTANCE"
        name?.contains("NH", ignoreCase = true) == true -> "Ammonia Sensor"
        name?.contains("DataLogger", ignoreCase = true) == true -> "DataLogger"
        name?.contains("Data Logger", ignoreCase = true) == true -> "DataLogger"
        name?.contains("DLOG", ignoreCase = true) == true -> "DataLogger"
        else -> "Unknown Device"
    }

    private fun updateDevice(newDevice: BluetoothDevice, sensorData: SensorData? = null) {
        _devices.update { devices ->
            val existingIndex = devices.indexOfFirst { it.address == newDevice.address }
            if (existingIndex >= 0) {
                val updatedList = devices.toMutableList()
                val updatedDevice = if (sensorData != null) {
                    updatedList[existingIndex].copy(sensorData = sensorData)
                } else {
                    newDevice
                }
                updatedList[existingIndex] = updatedDevice
                updatedList
            } else {
                devices + if (sensorData != null) newDevice.copy(sensorData = sensorData) else newDevice
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