package com.blesense.app

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(navController: NavHostController) {
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF2F2F7)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray
    val bubbleColorUser = if (isDarkMode) Color(0xFF1E88E5) else Color(0xFF2196F3)
    val bubbleColorBot = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val iconTint = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF007AFF)
    val appBarGradientStart = if (isDarkMode) Color(0xFF1E88E5) else Color(0xFF2196F3)
    val appBarGradientEnd = if (isDarkMode) Color(0xFF1565C0) else Color(0xFF1976D2)
    val appBarTextColor = Color.White
    val statusIndicatorColor = Color(0xFF4CAF50)

    var userInput by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<Pair<String, String>>() }
    val coroutineScope = rememberCoroutineScope()
    var showWelcomeMessage by remember { mutableStateOf(true) }
    var welcomeVisible by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Animation states
    val welcomeAlpha by animateFloatAsState(
        targetValue = if (welcomeVisible && showWelcomeMessage) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = EaseInOut)
    )
    val welcomeOffset by animateFloatAsState(
        targetValue = if (welcomeVisible && showWelcomeMessage) 0f else 50f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutBack)
    )
    val statusPulse by rememberInfiniteTransition().animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Auto-scroll to bottom when new message is added
    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    // Trigger welcome animation
    LaunchedEffect(Unit) {
        if (history.isEmpty()) {
            delay(300)
            welcomeVisible = true
            delay(3000)
            showWelcomeMessage = false
        }
    }

    MaterialTheme {
        Scaffold(
            backgroundColor = backgroundColor,
            topBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp),
                    elevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(appBarGradientStart, appBarGradientEnd)
                                )
                            )
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { navController.navigateUp() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = appBarTextColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SmartToy,
                                    contentDescription = "Bot Avatar",
                                    tint = appBarTextColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "BLE Sense Chatbot",
                                    fontFamily = helveticaFont,
                                    style = MaterialTheme.typography.h6.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = appBarTextColor
                                    )
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = statusIndicatorColor.copy(alpha = statusPulse),
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isOnline) "Online" else "Offline",
                                        fontSize = 12.sp,
                                        color = appBarTextColor.copy(alpha = 0.8f),
                                        fontFamily = helveticaFont
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        history.clear()
                                        showWelcomeMessage = true
                                        welcomeVisible = true
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Clear Chat",
                                        tint = appBarTextColor.copy(alpha = 0.9f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { isOnline = !isOnline },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                                        contentDescription = "Connection Status",
                                        tint = appBarTextColor.copy(alpha = 0.9f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(backgroundColor)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    state = listState
                ) {
                    if (history.isEmpty() && showWelcomeMessage) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                                    .alpha(welcomeAlpha)
                                    .offset(y = welcomeOffset.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Welcome to",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Light,
                                        color = secondaryTextColor,
                                        fontFamily = helveticaFont,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "BLE Sense Chatbot",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iconTint,
                                        fontFamily = helveticaFont,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Text(
                                        text = "Ask me anything about BLE Sense!",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = secondaryTextColor,
                                        fontFamily = helveticaFont,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    items(history) { (role, msg) ->
                        val isUser = role == "user"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isUser) bubbleColorUser else bubbleColorBot,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFE0E0E0),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg,
                                    color = if (isUser) Color.White else textColor,
                                    fontSize = 16.sp,
                                    fontFamily = helveticaFont
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                color = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = textColor,
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            placeholderColor = secondaryTextColor
                        ),
                        placeholder = { Text("Ask about BLE Sense or say 'Go to game'") },
                        enabled = !isSending
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (userInput.isNotBlank() && !isSending) {
                                isSending = true
                                showWelcomeMessage = false
                                val message = userInput
                                history.add("user" to message)
                                coroutineScope.launch {
                                    delay(300) // Debounce
                                    val reply = GeminiChatService.getChatResponse(history, message)
                                    if (reply.startsWith("Error:")) {
                                        history.add("bot" to "Sorry, I couldn't connect to the server. Please try again.")
                                    } else if (reply.startsWith("navigate:")) {
                                        val route = reply.removePrefix("navigate:").trim()
                                        navController.navigate(route)
                                    } else {
                                        history.add("bot" to reply)
                                    }
                                    isSending = false
                                }
                                userInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = iconTint,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSending
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}