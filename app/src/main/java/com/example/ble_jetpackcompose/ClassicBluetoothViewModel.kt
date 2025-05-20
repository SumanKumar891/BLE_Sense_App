//package com.example.ble_jetpackcompose
//
//import android.Manifest
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.BluetoothDevice
//import android.bluetooth.BluetoothManager
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.content.pm.PackageManager
//import android.os.Build
//import android.util.Log
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.annotation.RequiresPermission
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.Bluetooth
//import androidx.compose.material.icons.filled.Refresh
//import androidx.compose.material.icons.filled.Close
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewmodel.compose.viewModel
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//
//enum class ScanState {
//    IDLE, SCANNING
//}
//
//class ClassicBluetoothViewModel : ViewModel() {
//    // Using StateFlow for better state handling
//    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
//    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()
//
//    private val _scanState = MutableStateFlow(ScanState.IDLE)
//    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
//
//    internal val _errorMessage = MutableStateFlow<String?>(null)
//    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
//
//    private val bluetoothAdapter: BluetoothAdapter? by lazy {
//        BluetoothAdapter.getDefaultAdapter()
//    }
//
//    private var receiverRegistered = false
//
//    private val deviceReceiver = object : BroadcastReceiver() {
//        override fun onReceive(ctx: Context, intent: Intent) {
//            when (intent.action) {
//                BluetoothDevice.ACTION_FOUND -> {
//                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
//                    } else {
//                        @Suppress("DEPRECATION")
//                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
//                    }
//
//                    device?.let {
//                        // Update devices list without duplicates
//                        val currentDevices = _devices.value.toMutableList()
//                        if (!currentDevices.contains(device)) {
//                            currentDevices.add(device)
//                            _devices.value = currentDevices
//                        }
//                    }
//                }
//                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
//                    _scanState.value = ScanState.IDLE
//                }
//            }
//        }
//    }
//
//    @RequiresPermission(allOf = [
//        Manifest.permission.BLUETOOTH_SCAN,
//        Manifest.permission.BLUETOOTH_CONNECT
//    ])
//    fun startScan(context: Context) {
//        if (bluetoothAdapter == null) {
//            _errorMessage.value = "Bluetooth not supported on this device"
//            return
//        }
//
//        if (!bluetoothAdapter!!.isEnabled) {
//            _errorMessage.value = "Bluetooth is disabled"
//            return
//        }
//
//        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
//                    PackageManager.PERMISSION_GRANTED &&
//                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
//                    PackageManager.PERMISSION_GRANTED
//        } else {
//            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
//                    PackageManager.PERMISSION_GRANTED &&
//                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) ==
//                    PackageManager.PERMISSION_GRANTED
//        }
//
//        if (!hasPermission) {
//            _errorMessage.value = "Bluetooth permissions required"
//            return
//        }
//
//        _scanState.value = ScanState.SCANNING
//        _devices.value = emptyList()
//        _errorMessage.value = null
//
//        if (!receiverRegistered) {
//            val filter = IntentFilter().apply {
//                addAction(BluetoothDevice.ACTION_FOUND)
//                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
//            }
//            context.registerReceiver(deviceReceiver, filter)
//            receiverRegistered = true
//        }
//
//        if (bluetoothAdapter!!.isDiscovering) {
//            bluetoothAdapter!!.cancelDiscovery()
//        }
//        bluetoothAdapter!!.startDiscovery()
//    }
//
//    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
//    fun stopScan(context: Context) {
//        _scanState.value = ScanState.IDLE
//        bluetoothAdapter?.cancelDiscovery()
//
//        if (receiverRegistered) {
//            try {
//                context.unregisterReceiver(deviceReceiver)
//                receiverRegistered = false
//            } catch (e: Exception) {
//                Log.e("ClassicBT", "Error unregistering receiver: ${e.message}")
//            }
//        }
//    }
//
//    fun clearError() {
//        _errorMessage.value = null
//    }
//
//    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
//    override fun onCleared() {
//        super.onCleared()
//        // This is important to prevent memory leaks
//        bluetoothAdapter?.cancelDiscovery()
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ClassicBluetoothDevicePickerScreen(
//    viewModel: ClassicBluetoothViewModel = viewModel(),
//    onDeviceSelected: (String) -> Unit,
//    onBackPressed: () -> Unit,
//    scanDuration: Long = 10000 // Default 10 seconds scan
//) {
//    val context = LocalContext.current
//    val discoveredDevices by viewModel.devices.collectAsState()
//    val scanState by viewModel.scanState.collectAsState()
//    val errorMessage by viewModel.errorMessage.collectAsState()
//
//    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//        arrayOf(
//            Manifest.permission.BLUETOOTH_SCAN,
//            Manifest.permission.BLUETOOTH_CONNECT
//        )
//    } else {
//        arrayOf(
//            Manifest.permission.BLUETOOTH,
//            Manifest.permission.BLUETOOTH_ADMIN,
//            Manifest.permission.ACCESS_FINE_LOCATION
//        )
//    }
//
//    val permissionsLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        val allGranted = permissions.all { it.value }
//        if (allGranted) {
//            viewModel.startScan(context)
//        } else {
//            viewModel.clearError()
//            viewModel._errorMessage.value = "Bluetooth permissions required to scan for devices"
//        }
//    }
//
//    // Check permissions and start scan when screen loads
//    LaunchedEffect(Unit) {
//        val hasPermissions = bluetoothPermissions.all {
//            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
//        }
//
//        if (hasPermissions) {
//            viewModel.startScan(context)
//            delay(scanDuration)
//            if (scanState == ScanState.SCANNING) {
//                viewModel.stopScan(context)
//            }
//        } else {
//            permissionsLauncher.launch(bluetoothPermissions)
//        }
//    }
//
//    // Cleanup when leaving the screen
//    DisposableEffect(Unit) {
//        onDispose {
//            if (scanState == ScanState.SCANNING) {
//                viewModel.stopScan(context)
//            }
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Nearby Devices") },
//                navigationIcon = {
//                    IconButton(onClick = onBackPressed) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                },
//                actions = {
//                    IconButton(
//                        onClick = {
//                            if (scanState == ScanState.SCANNING) {
//                                viewModel.stopScan(context)
//                            } else {
//                                viewModel.startScan(context)
//                            }
//                        }
//                    ) {
//                        Icon(
//                            if (scanState == ScanState.SCANNING) Icons.Default.Close else Icons.Default.Refresh,
//                            contentDescription = if (scanState == ScanState.SCANNING) "Stop" else "Refresh"
//                        )
//                    }
//                }
//            )
//        }
//    ) { paddingValues ->
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .background(MaterialTheme.colorScheme.background)
//        ) {
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(16.dp)
//            ) {
//                // Error message display
//                errorMessage?.let { message ->
//                    AlertMessage(
//                        message = message,
//                        onDismiss = { viewModel.clearError() }
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                }
//
//                // Scanning indicator
//                if (scanState == ScanState.SCANNING) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.Center
//                    ) {
//                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("Scanning for devices...", fontSize = 14.sp)
//                    }
//                    Spacer(modifier = Modifier.height(8.dp))
//                }
//
//                // Device list or empty state
//                if (discoveredDevices.isEmpty()) {
//                    EmptyState(
//                        isScanning = scanState == ScanState.SCANNING,
//                        onRetry = { viewModel.startScan(context) }
//                    )
//                } else {
//                    DeviceList(
//                        devices = discoveredDevices,
//                        onDeviceSelected = { device ->
//                            viewModel.stopScan(context)
//                            onDeviceSelected(device.address)
//                        }
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun AlertMessage(message: String, onDismiss: () -> Unit) {
//    Card(
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.errorContainer
//        ),
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(12.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Text(
//                text = message,
//                color = MaterialTheme.colorScheme.onErrorContainer,
//                modifier = Modifier.weight(1f))
//            IconButton(
//                onClick = onDismiss,
//                modifier = Modifier.size(24.dp)
//            ) {
//                Icon(
//                    Icons.Default.Close,
//                    contentDescription = "Dismiss",
//                    tint = MaterialTheme.colorScheme.onErrorContainer
//                )
//            }
//        }
//    }
//}
//
//@Composable
//private fun EmptyState(isScanning: Boolean, onRetry: () -> Unit) {
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(32.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Icon(
//            Icons.Default.Bluetooth,
//            contentDescription = "Bluetooth",
//            modifier = Modifier.size(64.dp),
//            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//        Text(
//            text = if (isScanning) "Searching for devices..." else "No devices found",
//            fontSize = 18.sp,
//            textAlign = TextAlign.Center
//        )
//        if (!isScanning) {
//            Spacer(modifier = Modifier.height(16.dp))
//            Button(onClick = onRetry) {
//                Text("Try Again")
//            }
//        }
//    }
//}
//
//@Composable
//private fun DeviceList(
//    devices: List<BluetoothDevice>,
//    onDeviceSelected: (BluetoothDevice) -> Unit
//) {
//    LazyColumn(modifier = Modifier.fillMaxSize()) {
//        items(devices) { device ->
//            DeviceItem(
//                device = device,
//                onClick = { onDeviceSelected(device) }
//            )
//            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
//        }
//    }
//}
//
//@Composable
//private fun DeviceItem(device: BluetoothDevice, onClick: () -> Unit) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(onClick = onClick)
//            .padding(16.dp)
//    ) {
//        val deviceName = remember(device) {
//            try {
//                device.name ?: "Unknown Device"
//            } catch (e: SecurityException) {
//                "Unknown Device"
//            }
//        }
//
//        Text(
//            text = deviceName,
//            fontWeight = FontWeight.Bold,
//            fontSize = 16.sp
//        )
//        Text(
//            text = device.address,
//            fontSize = 14.sp,
//            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
//        )
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ClassicBluetoothDevicePicker(
//    onDeviceSelected: (String) -> Unit,
//    onBackPressed: () -> Unit = {}
//) {
//    val context = LocalContext.current
//    val viewModel: ClassicBluetoothViewModel = viewModel()
//
//    var bluetoothEnabled by remember { mutableStateOf(false) }
//    var permissionsGranted by remember { mutableStateOf(false) }
//
//    val bluetoothAdapter: BluetoothAdapter? = remember {
//        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
//        manager?.adapter
//    }
//
//    // Permission checker
//    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//        arrayOf(
//            Manifest.permission.BLUETOOTH_SCAN,
//            Manifest.permission.BLUETOOTH_CONNECT
//        )
//    } else {
//        arrayOf(
//            Manifest.permission.BLUETOOTH,
//            Manifest.permission.BLUETOOTH_ADMIN,
//            Manifest.permission.ACCESS_FINE_LOCATION
//        )
//    }
//
//    // Bluetooth enable launcher
//    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        bluetoothEnabled = bluetoothAdapter?.isEnabled == true
//    }
//
//    // Permission launcher
//    val permissionsLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        permissionsGranted = permissions.all { it.value }
//    }
//
//    // Check permissions and Bluetooth state when composable is launched
//    LaunchedEffect(Unit) {
//        // Check Bluetooth adapter
//        if (bluetoothAdapter == null) {
//            viewModel._errorMessage.value = "Bluetooth not supported on this device"
//            return@LaunchedEffect
//        }
//
//        // Check if Bluetooth is enabled
//        bluetoothEnabled = bluetoothAdapter.isEnabled
//        if (!bluetoothEnabled) {
//            // Try to enable Bluetooth
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            bluetoothEnableLauncher.launch(enableBtIntent)
//        }
//
//        // Check permissions
//        permissionsGranted = requiredPermissions.all {
//            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
//        }
//
//        if (!permissionsGranted) {
//            permissionsLauncher.launch(requiredPermissions)
//        }
//    }
//
//    // Determine what to show
//    val readyToScan = bluetoothAdapter != null && bluetoothEnabled && permissionsGranted
//
//    if (readyToScan) {
//        ClassicBluetoothDevicePickerScreen(
//            viewModel = viewModel,
//            onDeviceSelected = onDeviceSelected,
//            onBackPressed = onBackPressed
//        )
//    } else {
//        Box(
//            modifier = Modifier.fillMaxSize(),
//            contentAlignment = Alignment.Center
//        ) {
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center,
//                modifier = Modifier.padding(16.dp)
//            ) {
//                when {
//                    bluetoothAdapter == null -> {
//                        Text("Bluetooth is not supported on this device", textAlign = TextAlign.Center)
//                    }
//                    !bluetoothEnabled -> {
//                        Text("Bluetooth is disabled", textAlign = TextAlign.Center)
//                        Spacer(modifier = Modifier.height(16.dp))
//                        Button(
//                            onClick = {
//                                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                                bluetoothEnableLauncher.launch(enableBtIntent)
//                            }
//                        ) {
//                            Text("Enable Bluetooth")
//                        }
//                    }
//                    !permissionsGranted -> {
//                        Text("Bluetooth permissions are required", textAlign = TextAlign.Center)
//                        Spacer(modifier = Modifier.height(16.dp))
//                        Button(
//                            onClick = {
//                                permissionsLauncher.launch(requiredPermissions)
//                            }
//                        ) {
//                            Text("Grant Permissions")
//                        }
//                    }
//                    else -> {
//                        CircularProgressIndicator()
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//                TextButton(onClick = onBackPressed) {
//                    Text("Go Back")
//                }
//            }
//        }
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun PreviewClassicBluetoothPicker() {
//    MaterialTheme {
//        ClassicBluetoothDevicePicker(
//            onDeviceSelected = {},
//            onBackPressed = {}
//        )
//    }
//}