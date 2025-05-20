// Robotcontrol with horn and double joystick 20/05/25

@file:Suppress("DEPRECATION")
package com.example.ble_jetpackcompose

//noinspection UsingMaterialAndMaterial3Libraries
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.OutputStream
import java.util.UUID
import kotlin.random.Random
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.sqrt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.math.PI
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.toSize
import kotlin.math.abs

enum class ScanState {
    IDLE, SCANNING
}

// ================= BLUETOOTH SCANNING VIEW MODEL =================
class ClassicBluetoothViewModel : ViewModel() {
    // Using StateFlow for better state handling
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    internal val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private var receiverRegistered = false

    private val deviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        // Update devices list without duplicates
                        val currentDevices = _devices.value.toMutableList()
                        if (!currentDevices.contains(device)) {
                            currentDevices.add(device)
                            _devices.value = currentDevices
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _scanState.value = ScanState.IDLE
                }
            }
        }
    }

    @RequiresPermission(allOf = [
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    ])
    fun startScan(context: Context) {
        if (bluetoothAdapter == null) {
            _errorMessage.value = "Bluetooth not supported on this device"
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            _errorMessage.value = "Bluetooth is disabled"
            return
        }

        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) ==
                    PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermission) {
            _errorMessage.value = "Bluetooth permissions required"
            return
        }

        _scanState.value = ScanState.SCANNING
        _devices.value = emptyList()
        _errorMessage.value = null

        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            context.registerReceiver(deviceReceiver, filter)
            receiverRegistered = true
        }

        if (bluetoothAdapter!!.isDiscovering) {
            bluetoothAdapter!!.cancelDiscovery()
        }
        bluetoothAdapter!!.startDiscovery()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan(context: Context) {
        _scanState.value = ScanState.IDLE
        bluetoothAdapter?.cancelDiscovery()

        if (receiverRegistered) {
            try {
                context.unregisterReceiver(deviceReceiver)
                receiverRegistered = false
            } catch (e: Exception) {
                Log.e("ClassicBT", "Error unregistering receiver: ${e.message}")
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onCleared() {
        super.onCleared()
        // This is important to prevent memory leaks
        bluetoothAdapter?.cancelDiscovery()
    }
}

// ================= ROBOT CONTROL ACTIVITY =================
class RobotControlCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Lock to landscape before super.onCreate()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Make the activity fullscreen (hides notification bar)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    RobotControlScreen()
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Maintain landscape orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
}

// ================= ROBOT CONTROL VIEW MODEL =================
open class RobotControlViewModel : ViewModel() {
    private var outputStream: OutputStream? = null

    // Create connection state flow
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        initBluetooth()
    }

    private fun initBluetooth() {
        try {
            outputStream = BluetoothConnectionManager.bluetoothSocket?.outputStream
            _isConnected.value = isBluetoothConnected()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    open fun isBluetoothConnected(): Boolean {
        return BluetoothConnectionManager.isConnected()
    }

    open fun sendCommand(command: String) {
        try {
            Log.d("RobotCommand", "Sending command: $command")

            // Check if socket is still connected and re-initialize if needed
            if (!isBluetoothConnected()) {
                Log.e("RobotCommand", "Bluetooth not connected!")
                _isConnected.value = false
                return
            }

            // Get the current output stream or re-initialize
            if (outputStream == null) {
                outputStream = BluetoothConnectionManager.bluetoothSocket?.outputStream
                if (outputStream == null) {
                    Log.e("RobotCommand", "Failed to get output stream!")
                    return
                }
            }

            // Send the command
            outputStream?.write(command.toByteArray())
            outputStream?.flush()
            Log.d("RobotCommand", "Command sent successfully")
        } catch (e: Exception) {
            Log.e("RobotCommand", "Failed to send command", e)
            // Reset connection state on error
            _isConnected.value = false

            // Try to refresh the output stream
            try {
                outputStream = BluetoothConnectionManager.bluetoothSocket?.outputStream
            } catch (innerEx: Exception) {
                Log.e("RobotCommand", "Failed to refresh output stream", innerEx)
            }
        }
    }

    open fun handleSensorClick(sensorName: String, onDataReceived: (String, String) -> Unit) {
        if (isBluetoothConnected()) {
            when (sensorName) {
                "Temperature Sensor" -> {
                    val rawData = generateRandomRawData()
                    val temperature = rawData[0].toInt()
                    val humidity = rawData[1].toInt()
                    val rawDisplay = "Raw Data: ${rawData.contentToString()}"
                    val allData = "Temperature: $temperature°C\nHumidity: $humidity%"
                    onDataReceived(rawDisplay, allData)
                }
                else -> onDataReceived("Raw Data: N/A", "Default Data")
            }
        }
    }

    private fun generateRandomRawData(): ByteArray {
        val temperature = Random.nextInt(20, 40).toByte()
        val humidity = Random.nextInt(40, 80).toByte()
        return byteArrayOf(temperature, humidity)
    }
}

class FakeRobotControlViewModel : RobotControlViewModel() {
    override fun isBluetoothConnected(): Boolean = true
    override fun sendCommand(command: String) {}
    override fun handleSensorClick(sensorName: String, onDataReceived: (String, String) -> Unit) {
        onDataReceived("Raw Data: [20, 40]", "Temp: 20°C, Humidity: 40%")
    }
}

// ================= DEVICE SELECTION DIALOG =================
@Composable
fun DeviceSelectionDialog(
    devices: List<BluetoothDevice>,
    isScanning: Boolean,
    onDeviceSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.width(300.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select a Device", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (isScanning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scanning for devices...")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Device list or empty state
                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isScanning) "Searching..." else "No devices found",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    ) {
                        items(devices) { device ->
                            DeviceItem(
                                device = device,
                                onClick = { onDeviceSelected(device.address) }
                            )
                            Divider(color = Color.LightGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // This could be a refresh button if needed
                            onDismissRequest()
                        }
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(device: BluetoothDevice, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        val deviceName = remember(device) {
            try {
                device.name ?: "Unknown Device"
            } catch (e: SecurityException) {
                "Unknown Device"
            }
        }

        Text(
            text = deviceName,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = device.address,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

// ================= ROBOT CONTROL SCREEN =================
@Composable
fun RobotControlScreen(viewModel: RobotControlViewModel = viewModel()) {
    val context = LocalContext.current
    val bluetoothViewModel: ClassicBluetoothViewModel = viewModel()
    val configuration = LocalConfiguration.current

    // Track connection state
    var isConnected by remember { mutableStateOf(BluetoothConnectionManager.isConnected()) }

    if (configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
        LaunchedEffect(Unit) {
            // Optional: Show a message or handle rotation
        }
    }

    var selectedSensor by remember { mutableStateOf(SensorItem(0, "Select Sensor")) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogContent by remember { mutableStateOf("") }
    var showDeviceDialog by remember { mutableStateOf(false) }

    // State for scanning and devices
    val scanState by bluetoothViewModel.scanState.collectAsState()
    val devices by bluetoothViewModel.devices.collectAsState()
    val errorMessage by bluetoothViewModel.errorMessage.collectAsState()

    // Check connection status periodically
    LaunchedEffect(Unit) {
        while (true) {
            isConnected = BluetoothConnectionManager.isConnected()
            delay(1000) // Check every second
        }
    }

    // Permission handling
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            bluetoothViewModel.startScan(context)
            showDeviceDialog = true
        } else {
            Toast.makeText(context, "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    // Bluetooth enable launcher
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothEnabled = bluetoothAdapter?.isEnabled == true

        if (bluetoothEnabled) {
            // Check permissions before scanning
            val hasPermissions = bluetoothPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }

            if (hasPermissions) {
                bluetoothViewModel.startScan(context)
                showDeviceDialog = true
            } else {
                permissionsLauncher.launch(bluetoothPermissions)
            }
        } else {
            Toast.makeText(context, "Bluetooth must be enabled to scan for devices", Toast.LENGTH_SHORT).show()
        }
    }

    val sensorData = listOf(
        SensorItem(0, "Select Sensor"),
        SensorItem(R.drawable.ic_thermometer, "Temperature Sensor"),
        SensorItem(R.drawable.ic_accelerometer, "Accelerometer Sensor"),
        SensorItem(R.drawable.ic_pressure_sensor, "Pressure Sensor"),
        SensorItem(R.drawable.ic_turbo, "Turbo Sensor"),
        SensorItem(R.drawable.ic_motor, "Motor Sensor"),
        SensorItem(R.drawable.ic_switch, "Switch Sensor")
    )

    // 🖼️ Background Image
    val backgroundPainter = painterResource(id = R.drawable.racing_bg7)

    // Effect to clean up scanner when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            if (scanState == ScanState.SCANNING) {
                bluetoothViewModel.stopScan(context)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        Image(
            painter = backgroundPainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Connection Status Indicator
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(
                    if (isConnected) Color.Green.copy(alpha = 0.7f)
                    else Color.Red.copy(alpha = 0.7f),
                    CircleShape
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (isConnected) "Connected" else "Disconnected",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // 🔵 Bluetooth Button - Top Right
        BluetoothButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            onClick = {
                Log.d("BluetoothButton", "Clicked")

                // If already connected, disconnect first
                if (BluetoothConnectionManager.isConnected()) {
                    BluetoothConnectionManager.disconnect()
                    Toast.makeText(context, "Disconnected from device", Toast.LENGTH_SHORT).show()
                    return@BluetoothButton
                }

                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

                if (bluetoothAdapter == null) {
                    Log.e("BluetoothButton", "Bluetooth not supported")
                    Toast.makeText(context, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
                    return@BluetoothButton
                }

                // Check if Bluetooth is enabled
                if (!bluetoothAdapter.isEnabled) {
                    Log.d("BluetoothButton", "Bluetooth disabled, requesting enable...")
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    bluetoothEnableLauncher.launch(enableBtIntent)
                } else {
                    Log.d("BluetoothButton", "Bluetooth enabled, checking permissions...")
                    // Check permissions
                    val hasPermissions = bluetoothPermissions.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }

                    if (hasPermissions) {
                        Log.d("BluetoothButton", "Permissions granted, starting scan...")
                        bluetoothViewModel.startScan(context)
                        showDeviceDialog = true
                    } else {
                        Log.d("BluetoothButton", "Requesting permissions...")
                        permissionsLauncher.launch(bluetoothPermissions)
                    }
                }
            }
        )
        // 🎯 SensorSpinner - Center-Top
        SensorSpinner(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(top = 60.dp), // Moved down to accommodate connection status
            sensorData = sensorData,
            selectedSensor = selectedSensor,
            onSensorSelected = { sensor ->
                selectedSensor = sensor
                if (sensor.name != "Select Sensor") {
                    viewModel.handleSensorClick(sensor.name) { rawDisplay, allData ->
                        dialogContent = "$rawDisplay\n$allData"
                        showDialog = true
                    }
                }
            }
        )

        // 🔴 HornButton - Bottom Right
        HornButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            isBluetoothConnected = isConnected,
            onHornActive = { isActive ->
                if (isActive) {
                    viewModel.sendCommand("H") // Horn ON
                } else {
                    viewModel.sendCommand("C") // Horn OFF
                }
            }
        )

        // 🕹️ Left Joystick (Up/Down) - Center Left
        VerticalJoystick(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            onDirectionChange = { command ->
                // This will receive "U", "D", or "C" commands
                viewModel.sendCommand(command)
            }
        )


        // 🕹️ Right Joystick (Left/Right) - Center Right
        HorizontalJoystick(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            onDirectionChange = { direction ->
                when (direction) {
                    "L" -> viewModel.sendCommand("L") // Left
                    "R" -> viewModel.sendCommand("R") // Right
                    else -> viewModel.sendCommand("C") // Center/Stop
                }
            }
        )
    }
    // 💬 Dialogs
    if (showDialog) {
        SensorDataDialog(
            sensorName = selectedSensor.name,
            content = dialogContent,
            onDismiss = { showDialog = false }
        )
    }

    if (showDeviceDialog) {
        DeviceSelectionDialog(
            devices = devices,
            isScanning = scanState == ScanState.SCANNING,
            onDeviceSelected = { address ->
                connectToDevice(context, address)
                showDeviceDialog = false
            },
            onDismissRequest = {
                showDeviceDialog = false
                bluetoothViewModel.stopScan(context)
            }
        )
    }

    // Show error toast if there's an error
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            bluetoothViewModel.clearError()
        }
    }
}

@Composable
fun BluetoothButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(targetValue = 1f, animationSpec = tween(300), label = "")
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color.Gray.copy(alpha = 0.3f), CircleShape)
            .clickable { onClick() }
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_bluetooth),
            contentDescription = "Bluetooth Button",
            modifier = Modifier.size(50.dp)
        )
    }
}

@Composable
fun HornButton(
    modifier: Modifier = Modifier,
    isBluetoothConnected: Boolean,
    onHornActive: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isHornPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isHornPressed) 1.2f else 1f,
        animationSpec = tween(300),
        label = ""
    )

    // Background color that changes when pressed
    val backgroundColor by animateColorAsState(
        targetValue = if (isHornPressed) Color.Red.copy(alpha = 0.3f)
        else Color.Gray.copy(alpha = 0.3f),
        animationSpec = tween(300),
        label = "background color animation"
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(backgroundColor, CircleShape)
            .pointerInput(isBluetoothConnected) {
                if (!isBluetoothConnected) {
                    detectTapGestures {
                        Toast.makeText(context, "Connect Bluetooth first", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    detectTapGestures(
                        onPress = {
                            isHornPressed = true
                            onHornActive(true)
                            try {
                                awaitRelease()
                                isHornPressed = false
                                onHornActive(false)
                            } catch (e: Exception) {
                                isHornPressed = false
                                onHornActive(false)
                            }
                        },
                        onTap = {
                            // Not used in this case
                        }
                    )
                }
            }
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_horn),
            contentDescription = "Horn Button",
            modifier = Modifier.size(50.dp),
            colorFilter = if (isHornPressed) ColorFilter.tint(Color.Red) else null
        )
    }
}
// ================= VERTICAL JOYSTICK (FORWARD/BACKWARD) =================
@Composable
fun VerticalJoystick(
    modifier: Modifier = Modifier,
    onDirectionChange: (String) -> Unit
) {
    val density = LocalDensity.current
    var offset by remember { mutableStateOf(Offset.Zero) }
    val maxDistance = with(density) { 60.dp.toPx() }
    val deadZone = with(density) { 10.dp.toPx() }

    // Track the current command to avoid sending duplicate commands
    var currentCommand by remember { mutableStateOf("C") }

    Box(
        modifier = modifier
            .size(120.dp) // Make it square for perfect circle
            .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
            .clip(CircleShape) // Ensure content is clipped to circle
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Only allow vertical movement (lock X axis)
                        val newY = (offset.y + dragAmount.y).coerceIn(-maxDistance, maxDistance)
                        val newOffset = Offset(0f, newY)

                        // Update offset
                        offset = newOffset

                        // Determine direction based on position
                        val newCommand = when {
                            newY < -deadZone -> "U" // Up
                            newY > deadZone -> "D" // Down
                            else -> "C" // Center
                        }

                        // Only send command if it changed
                        if (newCommand != currentCommand) {
                            currentCommand = newCommand
                            onDirectionChange(newCommand)
                        }
                    },
                    onDragEnd = {
                        // Animate back to center
                        offset = Offset.Zero
                        if (currentCommand != "C") {
                            currentCommand = "C"
                            onDirectionChange("C")
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Visual center indicator (now horizontal line)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.5f))
        )

        // Joystick head with shadow
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                .size(70.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.LightGray.copy(alpha = 0.8f), CircleShape)
        )
    }
}
// ================= HORIZONTAL JOYSTICK (LEFT/RIGHT) =================
@Composable
fun HorizontalJoystick(
    modifier: Modifier = Modifier,
    onDirectionChange: (String) -> Unit
) {
    val density = LocalDensity.current
    var offset by remember { mutableStateOf(Offset.Zero) }
    val maxDistance = with(density) { 60.dp.toPx() }
    val deadZone = with(density) { 10.dp.toPx() }

    // Track the current command to avoid sending duplicate commands
    var currentCommand by remember { mutableStateOf("C") }

    Box(
        modifier = modifier
            .size(120.dp) // Square for perfect circle
            .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
            .clip(CircleShape) // Ensure content is clipped to circle
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Only allow horizontal movement (lock Y axis)
                        val newX = (offset.x + dragAmount.x).coerceIn(-maxDistance, maxDistance)
                        val newOffset = Offset(newX, 0f)

                        // Update offset
                        offset = newOffset

                        // Determine direction based on position
                        val newCommand = when {
                            newX < -deadZone -> "L" // Left
                            newX > deadZone -> "R" // Right
                            else -> "C" // Center
                        }

                        // Only send command if it changed
                        if (newCommand != currentCommand) {
                            currentCommand = newCommand
                            onDirectionChange(newCommand)
                        }
                    },
                    onDragEnd = {
                        // Animate back to center
                        offset = Offset.Zero
                        if (currentCommand != "C") {
                            currentCommand = "C"
                            onDirectionChange("C")
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Visual center indicator (vertical line)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(Color.White.copy(alpha = 0.5f))
        )

        // Joystick head with shadow
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                .size(70.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.LightGray.copy(alpha = 0.8f), CircleShape)
        )
    }
}
@SuppressLint("UseCompatLoadingForDrawables")
@Composable
fun SensorSpinner(
    modifier: Modifier = Modifier,
    sensorData: List<SensorItem>,
    selectedSensor: SensorItem,
    onSensorSelected: (SensorItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = modifier.width(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = selectedSensor.name,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier
                .background(Color.Gray.copy(alpha = 0.3f), CircleShape)
                .padding(12.dp)
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(180.dp)
        ) {
            sensorData.forEach { sensor ->
                DropdownMenuItem(onClick = {
                    onSensorSelected(sensor)
                    expanded = false
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (sensor.iconResId != 0) {
                            val drawable = context.resources.getDrawable(sensor.iconResId, null)
                            Image(
                                bitmap = drawable.toBitmap().asImageBitmap(),
                                contentDescription = sensor.name,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = sensor.name, color = Color.Black)
                    }
                }
            }
        }
    }
}


@SuppressLint("AutoboxingStateCreation")
@Composable
fun FloatingJoystickView(
    modifier: Modifier = Modifier,
    onDirectionChange: (String) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            }
            .zIndex(1f)
    )
//    {
//        JoystickViewComposable(
//            modifier = modifier.offset { IntOffset(offsetX.toInt(), offsetY.toInt()) },
//            onDirectionChange = onDirectionChange
//        )
//    }
}

@Composable
fun SensorDataDialog(sensorName: String, content: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(300.dp)
                .padding(16.dp),
            backgroundColor = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = sensorName, fontSize = 20.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = content, fontSize = 16.sp, color = Color.Black)
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun connectToDevice(context: Context, address: String) {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    val device = bluetoothAdapter.getRemoteDevice(address)
    val uuid = device.uuids?.firstOrNull()?.uuid ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // First disconnect any existing connection
    BluetoothConnectionManager.disconnect()

    Thread {
        try {
            // Create a new socket
            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)

            // Cancel discovery to prevent connection slowdowns
            bluetoothAdapter.cancelDiscovery()

            // Try to connect with timeout handling
            try {
                // Connect to the device with a timeout
                socket.connect()
            } catch (connectException: Exception) {
                // Close the socket
                try {
                    socket.close()
                } catch (closeException: Exception) { }

                // Try the fallback method for certain devices
                try {
                    Log.d("BluetoothConnect", "Trying fallback connection...")
                    val fallbackSocket = createFallbackSocket(device)
                    fallbackSocket?.connect()
                    if (fallbackSocket?.isConnected == true) {
                        BluetoothConnectionManager.bluetoothSocket = fallbackSocket
                        (context as? Activity)?.runOnUiThread {
                            Toast.makeText(context, "Connected to ${device.name} (fallback)", Toast.LENGTH_SHORT).show()
                        }
                        return@Thread
                    }
                } catch (fallbackException: Exception) {
                    Log.e("BluetoothConnect", "Fallback connection failed", fallbackException)
                    (context as? Activity)?.runOnUiThread {
                        Toast.makeText(context, "Connection failed: ${fallbackException.message}", Toast.LENGTH_LONG).show()
                    }
                    return@Thread
                }

                throw connectException
            }

            // Store the connected socket
            BluetoothConnectionManager.bluetoothSocket = socket

            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }.start()
}


data class SensorItem(val iconResId: Int, val name: String)


// Fallback method using reflection for problematic Android devices
@SuppressLint("MissingPermission", "DiscouragedPrivateApi")
private fun createFallbackSocket(device: BluetoothDevice): BluetoothSocket? {
    try {
        val m = device.javaClass.getMethod(
            "createRfcommSocket",
            *arrayOf<Class<*>>(Int::class.javaPrimitiveType as Class<*>)
        )
        return m.invoke(device, 1) as BluetoothSocket
    } catch (e: Exception) {
        Log.e("BluetoothConnect", "Fallback socket creation failed", e)
    }
    return null
}

// Add this to the object definition
object BluetoothConnectionManager {
    var bluetoothSocket: BluetoothSocket? = null

    fun disconnect() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Error closing socket: ${e.message}")
        }
    }

    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }
}
// === PREVIEWS ===

@Preview(showBackground = true)
@Composable
fun PreviewRobotControlScreen() {
    RobotControlScreen(viewModel = FakeRobotControlViewModel())
}