package com.blesense.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController

/**
 * MainScreen - The primary UI for scanning and displaying nearby BLE devices.
 * Displays a list of discovered Bluetooth Low Energy (BLE) devices with sensor data.
 *
 * @param navController Navigation controller to move between app screens
 * @param bluetoothViewModel ViewModel handling BLE scanning and device state
 */
@SuppressLint("MissingPermission")
@Composable
fun MainScreen(
    navController: NavHostController,
    bluetoothViewModel: BluetoothScanViewModel<Any?>
) {
    // Detect screen orientation (portrait/landscape)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // State to control visibility of robot control UI (not used currently)
    var showRobotControls by remember { mutableStateOf(false) }

    // Observe live list of discovered BLE devices
    val bluetoothDevices by bluetoothViewModel.devices.collectAsState()
    // Observe scanning status
    val isScanning by bluetoothViewModel.isScanning.collectAsState()

    // Get current Android context and activity
    val context = LocalContext.current
    val activity = context as ComponentActivity

    // Track whether required Bluetooth permissions are granted
    val isPermissionGranted = remember { mutableStateOf(checkBluetoothPermissions(context)) }

    // Dropdown menu state for sensor type selection
    var expanded by remember { mutableStateOf(false) }
    // Currently selected sensor filter (e.g., "SHT40", "Soil Sensor")
    var selectedSensor by remember { mutableStateOf("SHT40") }

    // Toggle to show all devices or just first 4
    var showAllDevices by remember { mutableStateOf(false) }

    // Observe dark mode state from ThemeManager
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()

    // Supported sensor types for filtering
    val sensorTypes = listOf(
        "SHT40", "LIS2DH", "Weather", "Lux Sensor",
        "Speed Distance", "Metal Detector", "Step Counter", "Soil Sensor",
        "Ammonia Sensor", "Optical Sensor", "DO Sensor", "DataLogger"
    )

    // Theme-based color palette
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF757575)
    val dividerColor = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFE0E0E0)

    // Get default Bluetooth adapter
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }

    // Check permissions on first composition
    LaunchedEffect(Unit) {
        isPermissionGranted.value = checkBluetoothPermissions(context)
    }

    // Handle runtime permission requests
    BluetoothPermissionHandler(
        onPermissionsGranted = { isPermissionGranted.value = true }
    )

    // BroadcastReceiver to monitor Bluetooth adapter state changes (ON/OFF)
    val bluetoothStateReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        // Restart scan when Bluetooth turns ON
                        if (state == BluetoothAdapter.STATE_ON && isPermissionGranted.value && !isScanning) {
                            bluetoothViewModel.startPeriodicScan(activity)
                        }
                    }
                }
            }
        }
    }

    // Register/unregister Bluetooth state receiver
    DisposableEffect(Unit) {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, filter)
        onDispose { context.unregisterReceiver(bluetoothStateReceiver) }
    }

    // Start scanning when permissions granted and Bluetooth is enabled
    DisposableEffect(isPermissionGranted.value, bluetoothAdapter?.isEnabled) {
        if (isPermissionGranted.value && bluetoothAdapter?.isEnabled == true && !isScanning) {
            bluetoothViewModel.startPeriodicScan(activity)
        }
        onDispose { bluetoothViewModel.stopScan() }
    }

    // Main Scaffold layout with TopAppBar
    Scaffold(
        backgroundColor = backgroundColor,
        topBar = {
            TopAppBar(
                backgroundColor = cardBackgroundColor,
                elevation = 8.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    // Back button
                    IconButton(
                        onClick = { navController.navigate("intermediate_screen") },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = textColor)
                    }

                    // App title
                    Text(
                        text = "BLE Sense",
                        fontFamily = helveticaFont,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        style = MaterialTheme.typography.h6,
                        textAlign = TextAlign.Center
                    )

                    // Theme toggle (Dark/Light mode)
                    IconButton(
                        onClick = { ThemeManager.toggleDarkMode(!isDarkMode) },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF5D4037)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Scrollable list of UI sections
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // === Nearby Devices Section ===
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = cardBackgroundColor
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            // Header: Title + Refresh + Sensor Menu
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nearby Devices (${bluetoothDevices.size})",
                                    style = MaterialTheme.typography.h6,
                                    color = textColor
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Manual refresh button
                                    IconButton(
                                        onClick = {
                                            if (isPermissionGranted.value && !isScanning) {
                                                if (bluetoothAdapter?.isEnabled == true) {
                                                    bluetoothViewModel.startPeriodicScan(activity)
                                                } else {
                                                    context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Refresh, "Refresh", tint = textColor)
                                    }

                                    // Sensor type dropdown
                                    Box {
                                        IconButton(onClick = { expanded = true }) {
                                            Icon(Icons.Default.MoreVert, "More Options", tint = textColor)
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.background(cardBackgroundColor)
                                        ) {
                                            sensorTypes.forEach { sensor ->
                                                DropdownMenuItem(
                                                    text = { Text(sensor, color = textColor) },
                                                    onClick = {
                                                        selectedSensor = sensor
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // === Permission & State Handling ===
                            when {
                                !isPermissionGranted.value -> {
                                    Text(
                                        "Bluetooth permissions required",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = textColor
                                    )
                                }
                                bluetoothAdapter?.isEnabled != true -> {
                                    Text(
                                        "Please enable Bluetooth",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = textColor
                                    )
                                }
                                bluetoothDevices.isEmpty() -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (isScanning) {
                                            CircularProgressIndicator(color = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF2196F3))
                                            Spacer(Modifier.height(8.dp))
                                            Text("Scanning for devices...", color = textColor)
                                        } else {
                                            Text("No devices found", color = textColor)
                                        }
                                    }
                                }
                                else -> {
                                    // Filter devices based on selected sensor type
                                    val filteredDevices = bluetoothDevices.filter { device ->
                                        when (selectedSensor) {
                                            "SHT40" -> device.name.contains("SHT", ignoreCase = true)
                                            "LIS2DH" -> device.name.contains("LIS2DH", ignoreCase = true) || device.name.contains("Activity", ignoreCase = true)
                                            "Soil Sensor" -> device.name.contains("Soil", ignoreCase = true)
                                            "Step Counter" -> device.name.contains("Step", ignoreCase = true)
                                            "DO Sensor" -> device.name.contains("DO", ignoreCase = true) || device.name.contains("Dissolved", ignoreCase = true)
                                            else -> true
                                        }
                                    }

                                    // Show limited or all devices
                                    val devicesToShow = if (showAllDevices) filteredDevices else filteredDevices.take(4)

                                    // Display each device
                                    devicesToShow.forEach { device ->
                                        BluetoothDeviceItem(
                                            device = device,
                                            navController = navController,
                                            selectedSensor = selectedSensor,
                                            isDarkMode = isDarkMode
                                        )
                                        Divider(color = dividerColor)
                                    }

                                    // Show More/Less toggle if more than 4 devices
                                    if (filteredDevices.size > 4) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable { showAllDevices = !showAllDevices }
                                                .background(
                                                    if (isDarkMode) Color(0xFF2A2A2A) else Color.LightGray,
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (showAllDevices) "Show Less" else "Show More",
                                                modifier = Modifier.padding(12.dp),
                                                textAlign = TextAlign.Center,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Optional: Add more sections (e.g., Game Devices) here later
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

/**
 * Checks required Bluetooth permissions based on Android version.
 *
 * @param context Application context
 * @return true if all required permissions are granted
 */
private fun checkBluetoothPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ requires SCAN and CONNECT permissions
        checkPermission(context, Manifest.permission.BLUETOOTH_SCAN) &&
                checkPermission(context, Manifest.permission.BLUETOOTH_CONNECT) &&
                checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        // Legacy permissions for older Android
        checkPermission(context, Manifest.permission.BLUETOOTH) &&
                checkPermission(context, Manifest.permission.BLUETOOTH_ADMIN) &&
                checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

/**
 * Helper to check a single permission.
 */
private fun checkPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Composable for displaying a single BLE device with name, address, RSSI, and sensor data.
 *
 * @param device The BLE device with metadata and sensor data
 * @param navController For navigation to detail screen
 * @param selectedSensor Current sensor filter
 * @param isDarkMode Current theme
 * @param showPreview Whether to show compact preview (not used here)
 */
@Composable
fun BluetoothDeviceItem(
    device: BluetoothScanViewModel.BluetoothDevice,
    navController: NavHostController,
    selectedSensor: String,
    isDarkMode: Boolean,
    showPreview: Boolean = false
) {
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF757575)
    val iconBackgroundColor = if (isDarkMode) Color(0xFF0D47A1) else Color(0xFFE3F2FD)
    val iconTintColor = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF2196F3)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                // Navigate to advertising screen with encoded device info
                navController.navigate(
                    "advertising/${device.name.replace("/", "-")}/${device.address}/${selectedSensor.ifEmpty { "SHT40" }}/${device.deviceId}"
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bluetooth icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(iconBackgroundColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.bluetooth),
                contentDescription = "Bluetooth Device",
                modifier = Modifier.size(24.dp),
                tint = iconTintColor
            )
        }

        Spacer(Modifier.width(12.dp))

        Column {
            // Device name
            Text(device.name, style = MaterialTheme.typography.subtitle1, color = textColor)
            // MAC Address
            Text("Address: ${device.address}", style = MaterialTheme.typography.caption, color = secondaryTextColor)
            // Signal strength
            Text("Signal Strength: ${device.rssi} dBm", style = MaterialTheme.typography.caption, color = secondaryTextColor)

            // Sensor data preview (if available)
            device.sensorData?.let { sensorData ->
                val dataText = when {
                    selectedSensor == "SHT40" && sensorData is BluetoothScanViewModel.SensorData.SHT40Data ->
                        "Temp: ${sensorData.temperature}°C, Humidity: ${sensorData.humidity}%"
                    selectedSensor == "LIS2DH" && sensorData is BluetoothScanViewModel.SensorData.LIS2DHData ->
                        "X: ${sensorData.x}, Y: ${sensorData.y}, Z: ${sensorData.z}"
                    selectedSensor == "Speed Distance" && sensorData is BluetoothScanViewModel.SensorData.SDTData ->
                        "Speed: ${sensorData.speed}m/s, Distance: ${sensorData.distance}m"
                    selectedSensor == "Ammonia Sensor" && sensorData is BluetoothScanViewModel.SensorData.AmmoniaSensorData ->
                        "Ammonia: ${sensorData.ammonia}"
                    selectedSensor == "Lux Sensor" && sensorData is BluetoothScanViewModel.SensorData.LuxSensorData ->
                        "Device ID: ${sensorData.deviceId}, Raw: ${sensorData.rawData}"
                    selectedSensor == "Soil Sensor" && sensorData is BluetoothScanViewModel.SensorData.SoilSensorData ->
                        "N: ${sensorData.nitrogen}, P: ${sensorData.phosphorus}, K: ${sensorData.potassium}\n" +
                                "Moisture: ${sensorData.moisture}%, Temp: ${sensorData.temperature}°C\n" +
                                "EC: ${sensorData.ec}, pH: ${sensorData.pH}"
                    selectedSensor == "DataLogger" && sensorData is BluetoothScanViewModel.SensorData.DataLoggerData ->
                        "Packet ID: ${sensorData.packetId}, Packets: ${sensorData.payloadPackets.size}"
                    else -> "No data for $selectedSensor"
                }

                Text(
                    text = dataText,
                    style = MaterialTheme.typography.caption,
                    color = if (isDarkMode) Color(0xFF64B5F6) else MaterialTheme.colors.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } ?: Text(
                "No sensor data available",
                style = MaterialTheme.typography.caption,
                color = if (isDarkMode) Color(0xFF64B5F6) else MaterialTheme.colors.primary
            )
        }
    }
}


/**
 * Preview for DataLogger raw packet info.
 */
@Composable
fun DataLoggerPreview(
    rawData: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .fillMaxWidth()
            .background(if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color.White, RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rawData,
            style = MaterialTheme.typography.caption,
            color = if (isSystemInDarkTheme()) Color(0xFF64B5F6) else Color(0xFF2196F3),
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

