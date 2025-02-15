package com.example.blegame

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdvertisingDataActivity : AppCompatActivity() {
    private lateinit var bleAdapter: BluetoothAdapterWrapper
    private lateinit var deviceInfo: TextView
    private lateinit var advertisingDataDetails: TextView
    private lateinit var temperatureMeterView: TemperatureViewMeter
    private lateinit var luxViewBulb: ImageView
    private val sht40Readings = mutableListOf<SHT40Reading>()
    private val luxReadings = mutableListOf<LuxSensorReading>()
    private val lis2dhReadings = mutableListOf<LIS2DHReading>()
    private val soilReadings = mutableListOf<SoilSensorReading>()
    private val weatherReadings = mutableListOf<WeatherReading>()

//    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    //    private lateinit var luxChart: LineChart
//    private lateinit var sht40Chart: LineChart

    private var scanCallback: ScanCallback? = null


    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advertising_data)

//        luxChart = findViewById(R.id.luxChart)
//        sht40Chart = findViewById(R.id.sht40Chart)
        initializeViews()
//        setupChart(luxChart)
//        setupChart(sht40Chart)




        val selectedDeviceName = intent.getStringExtra("DEVICE_NAME")
        val selectedDeviceAddress = intent.getStringExtra("DEVICE_ADDRESS")
//        val deviceType = intent.getStringExtra("DEVICE_TYPE")
//        val deviceType = intent.getStringExtra("DEVICE_TYPE")
        val deviceType = intent.getStringExtra("DEVICE_TYPE")
        val downloadButton = Button(this).apply {
            text = "Download Data"
            visibility = View.VISIBLE
            setOnClickListener {
                when (deviceType) {
                    "SHT40" -> exportDataToCSV()  // existing function
                    "Lux Sensor" -> exportSensorData("LUX")
                    "LIS2DH" -> exportSensorData("LIS2DH")
                    "Soil Sensor" -> exportSensorData("SOIL")
                    "Weather" -> exportSensorData("WEATHER")

                }
            }
        }
        findViewById<LinearLayout>(R.id.root_layout).addView(downloadButton)
        luxViewBulb = findViewById(R.id.bulbImageView)
        luxViewBulb.visibility = View.GONE // Default to hidden
//        downloadButton = Button(this).apply {
//            text = "Download Data"
//            visibility = View.GONE
//            setOnClickListener { exportDataToCSV() }
//        }
//         Add the download button to your layout
//        findViewById<LinearLayout>(R.id.root_layout).addView(downloadButton)

        advertisingDataDetails.setOnClickListener {
            if (checkPermissions()) {
                // Restart scanning when clicked
                startRealTimeScan(
                    selectedDeviceAddress ?: return@setOnClickListener,
                    selectedDeviceName ?: return@setOnClickListener,
                    deviceType
                )
                showToast("Restarting scan...")
            } else {
                requestPermissions()
            }
        }



        // Show download button only for SHT40 device
        if (intent.getStringExtra("DEVICE_TYPE") == "SHT40") {
            downloadButton.visibility = View.VISIBLE
        }



        if (deviceType == "Lux Sensor") {
            luxViewBulb.visibility = View.VISIBLE
        }
//
//        if (deviceType == "Speed Distance") {
//            speedDistanceMeter.visibility = View.VISIBLE
//        }



        if (selectedDeviceName == null || selectedDeviceAddress == null) {
            showToast("Device details not found. Returning to previous screen.")
            finish()
            return
        }
        deviceInfo.text = "Device: $selectedDeviceName ($selectedDeviceAddress)"
        temperatureMeterView.visibility = if (deviceType == "SHT40" || deviceType == "SPEED_DISTANCE") View.VISIBLE else View.GONE
//        speedDistanceMeter.visibility = if (deviceType == "Speed Distance" ) View.VISIBLE else View.GONE
        if (checkPermissions()) {
            startRealTimeScan(selectedDeviceAddress, selectedDeviceName, deviceType)
        } else {
            requestPermissions()
        }
    }


    private var lastReadingTime: Long = 0
    private val READ_INTERVAL = 30 * 60 * 1000 // 30 minutes in milliseconds
    private val MAX_READINGS = 1000 // Maximum number of readings to store

    private fun shouldStoreReading(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastReadingTime >= READ_INTERVAL) {
            lastReadingTime = currentTime
            return true
        }
        return false
    }

    private fun addReading(reading: Any) {
        if (!shouldStoreReading()) {
            return // Skip if it's not time for a new reading
        }

        when (reading) {
            is LuxSensorReading -> {
                synchronized(luxReadings) {
                    if (luxReadings.size >= MAX_READINGS) {
                        luxReadings.removeAt(0)
                    }
                    luxReadings.add(reading)
                }
            }
            is LIS2DHReading -> {
                synchronized(lis2dhReadings) {
                    if (lis2dhReadings.size >= MAX_READINGS) {
                        lis2dhReadings.removeAt(0)
                    }
                    lis2dhReadings.add(reading)
                }
            }
            is SoilSensorReading -> {
                synchronized(soilReadings) {
                    if (soilReadings.size >= MAX_READINGS) {
                        soilReadings.removeAt(0)
                    }
                    soilReadings.add(reading)
                }
            }
            is WeatherReading -> {
                synchronized(weatherReadings) {
                    if (weatherReadings.size >= MAX_READINGS) {
                        weatherReadings.removeAt(0)
                    }
                    weatherReadings.add(reading)
                }
            }
            is SHT40Reading -> {
                synchronized(sht40Readings) {
                    if (sht40Readings.size >= MAX_READINGS) {
                        sht40Readings.removeAt(0)
                    }
                    sht40Readings.add(reading)
                }
            }
        }
    }


    private fun initializeViews() {
        bleAdapter = BluetoothAdapterWrapper(this)
        deviceInfo = findViewById(R.id.deviceInfo)
        advertisingDataDetails = findViewById(R.id.advertisingDataDetails)
        temperatureMeterView = findViewById(R.id.temperatureMeter)

        temperatureMeterView.visibility = View.GONE
        temperatureMeterView.visibility = View.GONE
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
    }

    @SuppressLint("InlinedApi")
    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("InlinedApi")
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_CODE_PERMISSIONS
        )
    }

    @SuppressLint("MissingPermission", "NewApi")
    private fun startRealTimeScan(
        selectedDeviceAddress: String,
        selectedDeviceName: String,
        deviceType: String?
    ) {
        val bluetoothLeScanner = bleAdapter.bluetoothAdapter?.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            showToast("BLE Scanner not available.")
            return
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setLegacy(false)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .build()
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (result.device.name == selectedDeviceName && result.device.address == selectedDeviceAddress) {
                    val parsedData = parseAdvertisingData(result, deviceType)
                    updateUIWithScanData(parsedData)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                showToast("Scan failed. Error code: $errorCode")
            }
        }
        bluetoothLeScanner.startScan(null, scanSettings, scanCallback)

        Handler(mainLooper).postDelayed({
            bluetoothLeScanner.stopScan(scanCallback)
            showToast("Data Fetched.")
        }, 20000) // 10 seconds delay


    }



    private fun exportDataToCSV() {
        activityScope.launch(Dispatchers.IO) {
            try {
                val fileName = "SHT40_Data_${System.currentTimeMillis()}.csv"
                val file = File(getExternalFilesDir(null), fileName)

                file.bufferedWriter().use { writer ->
                    // Write CSV header
                    writer.write("Timestamp,Device ID,Temperature (°C),Humidity (%)\n")

                    // Write data
                    synchronized(sht40Readings) {  // Add synchronization for thread safety
                        sht40Readings.forEach { reading ->
                            writer.write(
                                "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                                    Date(reading.timestamp)
                                )}," +
                                        "${reading.deviceId}," +
                                        "${reading.temperature}," +
                                        "${reading.humidity}\n"
                            )
                        }
                    }
                }

                // Show success message on main thread
                withContext(Dispatchers.Main) {
                    showToast("Data exported to: ${file.absolutePath}")

                    // Create file URI and start sharing intent
                    val uri = FileProvider.getUriForFile(
                        this@AdvertisingDataActivity,
                        "${packageName}.provider",
                        file
                    )

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    startActivity(Intent.createChooser(intent, "Share CSV File"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error exporting data: ${e.message}")
                }
            }
        }
    }



//    private fun exportSensorData(sensorType: String) {
//        activityScope.launch(Dispatchers.IO) {
//            try {
//                val fileName = "${sensorType}_Data_${System.currentTimeMillis()}.csv"
//                val file = File(getExternalFilesDir(null), fileName)
//
//                file.bufferedWriter().use { writer ->
//                    when (sensorType) {
//                        "LUX" -> {
//                            writer.write("Timestamp,Device ID,Lux\n")
//                            synchronized(luxReadings) {
//                                luxReadings.forEach { reading ->
//                                    writer.write(
//                                        "${formatTimestamp(reading.timestamp)}," +
//                                                "${reading.deviceId}," +
//                                                "${reading.lux}\n"
//                                    )
//                                }
//                            }
//                        }
//                        "LIS2DH" -> {
//                            writer.write("Timestamp,Device ID,X,Y,Z\n")
//                            synchronized(lis2dhReadings) {
//                                lis2dhReadings.forEach { reading ->
//                                    writer.write(
//                                        "${formatTimestamp(reading.timestamp)}," +
//                                                "${reading.deviceId}," +
//                                                "${reading.x}," +
//                                                "${reading.y}," +
//                                                "${reading.z}\n"
//                                    )
//                                }
//                            }
//                        }
//                        "SOIL" -> {
//                            writer.write("Timestamp,Device ID,Nitrogen (mg/kg),Phosphorus (mg/kg)," +
//                                    "Potassium (mg/kg),Moisture (%),Temperature (°C)," +
//                                    "Electrical Conductivity (µS/cm),pH\n")
//                            synchronized(soilReadings) {
//                                soilReadings.forEach { reading ->
//                                    writer.write(
//                                        "${formatTimestamp(reading.timestamp)}," +
//                                                "${reading.deviceId}," +
//                                                "${reading.nitrogen}," +
//                                                "${reading.phosphorus}," +
//                                                "${reading.potassium}," +
//                                                "${reading.moisture}," +
//                                                "${reading.temperature}," +
//                                                "${reading.electricalConductivity}," +
//                                                "${reading.pH}\n"
//                                    )
//                                }
//                            }
//                        }
//                        "WEATHER" -> {
//                            writer.write("Timestamp,Device ID 1,Temperature 1 (°C),Humidity 1 (%)," +
//                                    "Pressure 1 (hPa),Dew Point Temperature 1 (°C)," +
//                                    "Device ID 2,Temperature 2 (°C),Humidity 2 (%)," +
//                                    "Pressure 2 (hPa),Dew Point Temperature 2 (°C)\n")
//                            synchronized(weatherReadings) {
//                                weatherReadings.forEach { reading ->
//                                    writer.write(
//                                        "${formatTimestamp(reading.timestamp)}," +
//                                                "${reading.deviceId1}," +
//                                                "${reading.temperature1}," +
//                                                "${reading.humidity1}," +
//                                                "${reading.pressure1}," +
//                                                "${reading.dewPointTemperature1}," +
//                                                "${reading.deviceId2}," +
//                                                "${reading.temperature2}," +
//                                                "${reading.humidity2}," +
//                                                "${reading.pressure2}," +
//                                                "${reading.dewPointTemperature2}\n"
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//
//                withContext(Dispatchers.Main) {
//                    showToast("Data exported to: ${file.absolutePath}")
//                    shareFile(file)
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    showToast("Error exporting data: ${e.message}")
//                }
//            }
//        }
//    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    private fun parseAdvertisingData(result: ScanResult, deviceType: String?): String {
        val gapDetails = StringBuilder()
        val scanRecord = result.scanRecord
        scanRecord?.manufacturerSpecificData?.let { manufacturerData ->
            for (i in 0 until manufacturerData.size()) {
                val data = manufacturerData.valueAt(i)
                if (data != null) {
                    when (deviceType) {
                        "SHT40" -> gapDetails.append(parseSHT40Data(data))
                        "Lux Sensor" -> gapDetails.append(parseLuxSensorData(data))
                        else -> gapDetails.append(parseManufacturerData(data, deviceType))
                    }
                    gapDetails.append("\n\n")
                }
            }
        }
        return gapDetails.toString()
    }

    //    private fun setupChart(chart: LineChart) {
//        chart.description.isEnabled = false
//        chart.setDrawGridBackground(false)
//        chart.isDragEnabled = true
//        chart.setScaleEnabled(true)
//        chart.setTouchEnabled(true)
//    }
    private fun parseLuxSensorData(data: ByteArray): String {
        val builder = StringBuilder()
        builder.append("Raw BLE Data: ")
        builder.append(data.joinToString(" ") { byte -> String.format("%02X", byte) })
            .append("\n\n")

        return if (data.size >= 5) {
            val deviceId = data[0].toUByte().toString()
            val temperatureBeforeDecimal = data[1].toUByte().toString()
            val temperatureAfterDecimal = data[2].toUByte().toString()
            val temperature = "$temperatureBeforeDecimal.$temperatureAfterDecimal".toFloat()
            val calculatedLux = temperature * 60

            // Update UI in real-time
            Handler(Looper.getMainLooper()).post {
                temperatureMeterView.visibility = View.GONE

//
                luxViewBulb.visibility = View.VISIBLE
                val bulbResource = when {
                    calculatedLux < 1700 -> R.drawable.bulb_off
                    calculatedLux in 1700.0..2000.0 -> R.drawable.bulb_25
                    calculatedLux in 2000.0..2500.0 -> R.drawable.bulb_50
                    calculatedLux in 2500.0..3000.0 -> R.drawable.bulb_75
                    else -> R.drawable.bulb_glow
                }
                luxViewBulb.setImageResource(bulbResource)
            }

            // Store reading at intervals
            addReading(
                LuxSensorReading(
                    timestamp = System.currentTimeMillis(),
                    deviceId = deviceId,
                    lux = calculatedLux
                )
            )

            builder.append("Device ID: $deviceId\n")
            builder.append("Calculated Lux: $calculatedLux lux\n")
            builder.toString()
        } else {
            builder.append("Manufacturer Data is too short for Lux Sensor\n")
            builder.toString()
        }
    }


    private fun parseSHT40Data(data: ByteArray): String {
        val builder = StringBuilder()
        if (data.size >= 5) {
            val deviceId = data[0].toUByte().toString()
            val temperatureBeforeDecimal = data[1].toUByte().toString()
            val temperatureAfterDecimal = data[2].toUByte().toString()
            val humidityBeforeDecimal = data[3].toUByte().toString()
            val humidityAfterDecimal = data[4].toUByte().toString()
            val temperature = "$temperatureBeforeDecimal.$temperatureAfterDecimal"
            val humidity = "$humidityBeforeDecimal.$humidityAfterDecimal"

            // Update UI in real-time
            Handler(Looper.getMainLooper()).post {
                temperatureMeterView.visibility = View.VISIBLE
                temperatureMeterView.updateSensorTypeByDevice("SHT40")
                temperatureMeterView.setTemperature(temperature.toFloat())
            }

            // Store reading at intervals
            addReading(
                SHT40Reading(
                    timestamp = System.currentTimeMillis(),
                    deviceId = deviceId,
                    temperature = temperature,
                    humidity = humidity
                )
            )

            builder.append("Device ID: $deviceId\n")
            builder.append("Temperature: $temperature°C\n")
            builder.append("Humidity: $humidity%\n")
        } else {
            builder.append("Manufacturer Data is too short for SHT40\n")
        }
        return builder.toString()
    }

    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    private fun exportLuxData(writer: BufferedWriter) {
        // Write CSV header
        writer.write("Timestamp,Device ID,Lux Value\n")

        // Safely export all lux readings
        synchronized(luxReadings) {
            luxReadings.forEach { reading ->
                writer.write(
                    "${formatTimestamp(reading.timestamp)}," +
                            "${reading.deviceId}," +
                            "${reading.lux}\n"
                )
            }
        }
    }

    private fun exportLIS2DHData(writer: BufferedWriter) {
        // Write CSV header
        writer.write("Timestamp,Device ID,X Axis,Y Axis,Z Axis\n")

        // Safely export all accelerometer readings
        synchronized(lis2dhReadings) {
            lis2dhReadings.forEach { reading ->
                writer.write(
                    "${formatTimestamp(reading.timestamp)}," +
                            "${reading.deviceId}," +
                            "${reading.x}," +
                            "${reading.y}," +
                            "${reading.z}\n"
                )
            }
        }
    }

    private fun exportSoilData(writer: BufferedWriter) {
        // Write CSV header
        writer.write("Timestamp,Device ID,Nitrogen (mg/kg),Phosphorus (mg/kg)," +
                "Potassium (mg/kg),Moisture (%),Temperature (°C)," +
                "Electrical Conductivity (µS/cm),pH\n")

        // Safely export all soil sensor readings
        synchronized(soilReadings) {
            soilReadings.forEach { reading ->
                writer.write(
                    "${formatTimestamp(reading.timestamp)}," +
                            "${reading.deviceId}," +
                            "${reading.nitrogen}," +
                            "${reading.phosphorus}," +
                            "${reading.potassium}," +
                            "${reading.moisture}," +
                            "${reading.temperature}," +
                            "${reading.electricalConductivity}," +
                            "${reading.pH}\n"
                )
            }
        }
    }

    private fun exportWeatherData(writer: BufferedWriter) {
        // Write CSV header
        writer.write("Timestamp,Device ID 1,Temperature 1 (°C),Humidity 1 (%)," +
                "Pressure 1 (hPa),Dew Point 1 (°C)," +
                "Device ID 2,Temperature 2 (°C),Humidity 2 (%)," +
                "Pressure 2 (hPa),Dew Point 2 (°C)\n")

        // Safely export all weather readings
        synchronized(weatherReadings) {
            weatherReadings.forEach { reading ->
                writer.write(
                    "${formatTimestamp(reading.timestamp)}," +
                            "${reading.deviceId1}," +
                            "${reading.temperature1}," +
                            "${reading.humidity1}," +
                            "${reading.pressure1}," +
                            "${reading.dewPointTemperature1}," +
                            "${reading.deviceId2}," +
                            "${reading.temperature2}," +
                            "${reading.humidity2}," +
                            "${reading.pressure2}," +
                            "${reading.dewPointTemperature2}\n"
                )
            }
        }
    }

    // Helper function for timestamp formatting
//    private fun formatTimestamp(timestamp: Long): String {
//        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//            .format(Date(timestamp))
//    }
//


    private fun exportSensorData(sensorType: String) {
        if (!isExternalStorageWritable()) {
            showToast("External storage is not available")
            return
        }

        activityScope.launch(Dispatchers.IO) {
            try {
                val fileName = "${sensorType}_Data_${System.currentTimeMillis()}.csv"
                val file = File(getExternalFilesDir(null), fileName)

                withContext(Dispatchers.IO) {
                    file.bufferedWriter().use { writer ->
                        try {
                            when (sensorType) {
                                "LUX" -> exportLuxData(writer)
                                "LIS2DH" -> exportLIS2DHData(writer)
                                "SOIL" -> exportSoilData(writer)
                                "WEATHER" -> exportWeatherData(writer)
                            }
                        } catch (e: Exception) {
                            throw e
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    showToast("Data exported successfully")
                    shareFile(file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error exporting data: ${e.message}")
                }
            }
        }
    }

    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }


    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share CSV File"))
    }

    private fun parseManufacturerData(data: ByteArray, deviceType: String?): String {
        return when (deviceType) {
            "SHT40" -> parseSHT40Data(data)
            "LIS2DH" -> parseLIS2DHData(data)
            "Soil Sensor" -> parseSoilSensorData(data)
            "Weather" -> parseWeatherData(data)
            "SPEED_DISTANCE" -> parseSDTData(data)
            "Metal Detector" -> parseMetalDetectorData(data)
            else -> "Unknown Device Type"
        }
    }

    private fun parseLIS2DHData(data: ByteArray): String {
        val builder = StringBuilder()
        try {
            if (data.size >= 7) {
                val deviceId = data[0].toUByte().toString()

                // Convert before-decimal to signed, keep after-decimal unsigned
                val xBeforeDecimal = data[1].toInt().toString()
                val xAfterDecimal = data[2].toUByte().toString()
                val yBeforeDecimal = data[3].toInt().toString()
                val yAfterDecimal = data[4].toUByte().toString()
                val zBeforeDecimal = data[5].toInt().toString()
                val zAfterDecimal = data[6].toUByte().toString()

                // Store reading at intervals
                addReading(
                    LIS2DHReading(
                        timestamp = System.currentTimeMillis(),
                        deviceId = deviceId,
                        x = if (xBeforeDecimal.startsWith("-")) "$xBeforeDecimal.$xAfterDecimal" else "$xBeforeDecimal.$xAfterDecimal",
                        y = if (yBeforeDecimal.startsWith("-")) "$yBeforeDecimal.$yAfterDecimal" else "$yBeforeDecimal.$yAfterDecimal",
                        z = if (zBeforeDecimal.startsWith("-")) "$zBeforeDecimal.$zAfterDecimal" else "$zBeforeDecimal.$zAfterDecimal"
                    )
                )

                builder.append("Device ID: $deviceId\n")
                builder.append("X: ${if (xBeforeDecimal.startsWith("-")) "$xBeforeDecimal.$xAfterDecimal" else "$xBeforeDecimal.$xAfterDecimal"}\n")
                builder.append("Y: ${if (yBeforeDecimal.startsWith("-")) "$yBeforeDecimal.$yAfterDecimal" else "$yBeforeDecimal.$yAfterDecimal"}\n")
                builder.append("Z: ${if (zBeforeDecimal.startsWith("-")) "$zBeforeDecimal.$zAfterDecimal" else "$zBeforeDecimal.$zAfterDecimal"}\n")
            }
        } catch (e: Exception) {
            builder.append("Error parsing data: ${e.message}\n")
        }
        return builder.toString()
    }


    private fun parseSDTData(data: ByteArray) : String {
        val builder = StringBuilder()


        // Print raw data in hexadecimal format
        builder.append("Raw BLE Data: ")
        builder.append(data.joinToString(" ") { byte -> String.format("%02X", byte) })
            .append("\n\n")

        return if (data.size >= 6) { // Update the minimum size as per your needs
            val deviceId = data[0].toUByte().toString()
            val speedBeforeDecimal = data[1].toUByte().toString()
            val speedAfterDecimal = data[2].toUByte().toString()
            val distanceBeforeDecimal = data[3].toUByte().toString()
            val distanceAfterDecimal = data[4].toUByte().toString()
            val speed = "$speedBeforeDecimal.$speedAfterDecimal"


            Handler(Looper.getMainLooper()).post {
                temperatureMeterView.visibility = View.VISIBLE
                temperatureMeterView.updateSensorTypeByDevice("SPEED_DISTANCE")
                temperatureMeterView.setTemperature(speed.toFloat())
            }



            builder.append("Device ID: $deviceId\n")
            builder.append("Speed: $speedBeforeDecimal.$speedAfterDecimal\n")
            builder.append("Distance: $distanceBeforeDecimal.$distanceAfterDecimal\n")
            builder.toString()
        } else {
            builder.append("Invalid Data: Data size (${data.size}) is too short.\n")
            builder.toString()
        }
    }

    private fun parseMetalDetectorData(data: ByteArray) : String {
        val builder = StringBuilder()

        // Print raw data in hexadecimal format
        builder.append("Raw BLE Data: ")
        builder.append(data.joinToString(" ") { byte -> String.format("%02X", byte) })
            .append("\n\n")

        return if (data.size >= 2) {
            val deviceId = data[0].toUByte().toString()
            val isMetalDetected = data[1].toInt() != 0 // Convert byte to boolean

            Handler(Looper.getMainLooper()).post {
                temperatureMeterView.visibility = View.VISIBLE
                // Add METAL_DETECTOR to your SensorType enum and configure it
                temperatureMeterView.updateSensorTypeByDevice("METAL_DETECTOR")

                // Set temperature to 50 if metal detected, 0 if not
                val temperature = if (isMetalDetected) 50f else 0f
                temperatureMeterView.setTemperature(temperature)
            }

            builder.append("Device ID: $deviceId\n")
            builder.append("Metal Detected: $isMetalDetected\n")
//            builder.append("Temperature Value: ${if (isMetalDetected) "50.0" else "0.0"}\n")
            builder.toString()
        } else {
            builder.append("Invalid Data: Data size (${data.size}) is too short.\n")
            builder.toString()
        }
    }


    private fun parseSoilSensorData(data: ByteArray): String {
        val builder = StringBuilder()
        try {
            if (data.size >= 11) {
                val deviceId = data[0].toUByte().toString()
                val nitrogen = data[1].toUByte().toString()
                val phosphorus = data[2].toUByte().toString()
                val potassium = data[3].toUByte().toString()
                val moisture = data[4].toUByte().toString()
                val temperatureBeforeDecimal = data[5].toUByte().toString()
                val temperatureAfterDecimal = data[6].toUByte().toString()
                val eCbeforeDecimal = data[7].toUByte().toString()
                val eCafterDecimal = data[8].toUByte().toString()
                val pHbeforeDecimal = data[9].toUByte().toString()
                val pHafterDecimal = data[10].toUByte().toString()

                // Store reading at intervals
                addReading(
                    SoilSensorReading(
                        timestamp = System.currentTimeMillis(),
                        deviceId = deviceId,
                        nitrogen = nitrogen,
                        phosphorus = phosphorus,
                        potassium = potassium,
                        moisture = moisture,
                        temperature = "$temperatureBeforeDecimal.$temperatureAfterDecimal",
                        electricalConductivity = "$eCbeforeDecimal.$eCafterDecimal",
                        pH = "$pHbeforeDecimal.$pHafterDecimal"
                    )
                )

                builder.append("Device ID: $deviceId\n")
                builder.append("Nitrogen: $nitrogen mg/kg\n")
                builder.append("Phosphorus: $phosphorus mg/kg\n")
                builder.append("Potassium: $potassium mg/kg\n")
                builder.append("Moisture: $moisture%\n")
                builder.append("Temperature: $temperatureBeforeDecimal.$temperatureAfterDecimal°C\n")
                builder.append("EC: $eCbeforeDecimal.$eCafterDecimal µS/cm\n")
                builder.append("pH: $pHbeforeDecimal.$pHafterDecimal\n")
            }
        } catch (e: Exception) {
            builder.append("Error parsing data: ${e.message}\n")
        }
        return builder.toString()
    }


    private fun parseWeatherData(data: ByteArray): String {
        if (data.size < 20) {
            return "Insufficient data. Expected at least 20 bytes but received ${data.size}."
        }

        val builder = StringBuilder()
        builder.append("Raw BLE Data: ")
        builder.append(data.joinToString(" ") { byte -> String.format("%02X", byte) })
            .append("\n\n")

        // Parse data
        val deviceId1 = data[0].toUByte().toString()
        val temperature1 = "${data[1].toUByte()}.${data[2].toUByte()}"
        val humidity1 = "${data[3].toUByte()}.${data[4].toUByte()}"
        val pressure1 = "${data[5].toUByte()}.${data[6].toUByte()}"
        val dewPointTemperature1 = "${data[7].toUByte()}.${data[8].toUByte()}"

        val deviceId2 = data[11].toUByte().toString()
        val temperature2 = "${data[12].toUByte()}.${data[13].toUByte()}"
        val humidity2 = "${data[14].toUByte()}.${data[15].toUByte()}"
        val pressure2 = "${data[16].toUByte()}.${data[17].toUByte()}"
        val dewPointTemperature2 = "${data[18].toUByte()}.${data[19].toUByte()}"

        // Store reading at intervals
        addReading(
            WeatherReading(
                timestamp = System.currentTimeMillis(),
                deviceId1 = deviceId1,
                temperature1 = temperature1,
                humidity1 = humidity1,
                pressure1 = pressure1,
                dewPointTemperature1 = dewPointTemperature1,
                deviceId2 = deviceId2,
                temperature2 = temperature2,
                humidity2 = humidity2,
                pressure2 = pressure2,
                dewPointTemperature2 = dewPointTemperature2
            )
        )

        builder.append("Parsed Weather Data:\n")
        builder.append("Device 1:\n")
        builder.append("  DeviceId: $deviceId1\n")
        builder.append("  Temperature: $temperature1°C\n")
        builder.append("  Humidity: $humidity1%\n")
        builder.append("  Pressure: $pressure1 hPa\n")
        builder.append("  Dew Point: $dewPointTemperature1°C\n\n")
        builder.append("Device 2:\n")
        builder.append("  DeviceId: $deviceId2\n")
        builder.append("  Temperature: $temperature2°C\n")
        builder.append("  Humidity: $humidity2%\n")
        builder.append("  Pressure: $pressure2 hPa\n")
        builder.append("  Dew Point: $dewPointTemperature2°C")

        return builder.toString()
    }


//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        try {
//            outState.apply {
//                putSerializable("lux_readings", ArrayList(luxReadings))
//                putSerializable("lis2dh_readings", ArrayList(lis2dhReadings))
//                putSerializable("soil_readings", ArrayList(soilReadings))
//                putSerializable("weather_readings", ArrayList(weatherReadings))
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error saving instance state", e)
//        }
//    }

    // Restore state with error handling
//    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
//        super.onRestoreInstanceState(savedInstanceState)
//        try {
//            (savedInstanceState.getSerializable("lux_readings") as? ArrayList<LuxSensorReading>)?.let {
//                luxReadings.addAll(it)
//            }
//            // ... similar for other sensor types ...
//        } catch (e: Exception) {
//            Log.e(TAG, "Error restoring instance state", e)
//        }
//    }

    private fun updateUIWithScanData(parsedData: String) {
        Handler(Looper.getMainLooper()).post { advertisingDataDetails.text = parsedData }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        try {
            // Clear all readings to prevent memory leaks
            luxReadings.clear()
            lis2dhReadings.clear()
            soilReadings.clear()
            weatherReadings.clear()
        } catch (_: Exception) {
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            val selectedDeviceName = intent.getStringExtra("DEVICE_NAME") ?: return
            val selectedDeviceAddress = intent.getStringExtra("DEVICE_ADDRESS") ?: return
            val deviceType = intent.getStringExtra("DEVICE_TYPE")
            startRealTimeScan(selectedDeviceAddress, selectedDeviceName, deviceType)
        } else {
            showToast("Permissions denied. Cannot scan for BLE devices.")
        }
    }
}