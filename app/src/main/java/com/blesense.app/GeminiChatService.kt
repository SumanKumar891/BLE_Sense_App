package com.blesense.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

object GeminiChatService {
    private val apiKey: String = BuildConfig.API_KEY
    private val chatApi: GeminiChatApi by lazy {
        GoogleTranslationService.geminiRetrofit.create(GeminiChatApi::class.java)
    }

    // Cache for frequent queries
    private val responseCache = ConcurrentHashMap<String, String>()

    // Limit history to last 3 messages to reduce payload
    private const val MAX_HISTORY_SIZE = 3
    private const val MAX_RETRIES = 3
    private const val INITIAL_RETRY_DELAY_MS = 1000L

    // Simplified system prompt referencing local lookup
    private val systemPrompt = """
        You are an assistant for the BLE Sense app. Respond to:
        - Basic greetings (e.g., "Hi", "Hello") with friendly replies.
        - Questions about the BLE Sense app (e.g., features, usage) based on its documentation.
        - Navigation requests in the format: navigate:<route>, where <route> is one of: settings_screen, home_screen, profile_screen, game_loading, robot_screen, optical_sensor_screen, water_quality_screen, game_screen, chart_screen, chart_screen_2 (lowercase, no extra text). E.g., "Go to settings" → "navigate:settings_screen".
        For unrelated topics, reply: "Sorry, I can only answer questions about the BLE Sense app or respond to basic greetings." Keep responses concise.
    """.trimIndent()

    // Local lookup for common app-related queries
    private val localResponses = mapOf(
        "what is ble sense" to "BLE Sense is an Android app for Bluetooth Low Energy (BLE) communication, enabling device scanning, secure connections, data exchange, and visualization for IoT applications like health, smart home, and agriculture.",
        "what are the app features" to "BLE Sense offers BLE device scanning, secure connections, data visualization (charts/3D), sensors (temperature, humidity, accelerometer, soil, light), games (Hunt the Heroes, robot control), dark mode, language selection, and Firebase authentication.",
        "how to use ble sense" to "Install BLE Sense, grant Bluetooth/location permissions, use bottom navigation (Games, Bluetooth, Settings), scan/connect sensors in Bluetooth section, view data in charts, and adjust preferences in Settings.",
        "go to settings" to "navigate:settings_screen",
        "go to game" to "navigate:game_screen",
        "go to robot control" to "navigate:robot_screen",
        "hi" to "Hello! How can I help with BLE Sense?",
        "hello" to "Hi there! Ask about BLE Sense or navigate to a screen."
    )

    suspend fun getChatResponse(history: List<Pair<String, String>>, userMessage: String): String {
        // Check cache
        val cacheKey = "$userMessage-${history.takeLast(MAX_HISTORY_SIZE).joinToString("|")}"
        responseCache[cacheKey]?.let { return it }

        // Check local responses for common queries
        val normalizedInput = userMessage.trim().lowercase()
        localResponses[normalizedInput]?.let { response ->
            responseCache[cacheKey] = response
            return response
        }

        // Fallback to API with retry logic
        return withContext(Dispatchers.IO) {
            var attempt = 0
            while (attempt < MAX_RETRIES) {
                try {
                    val contents = mutableListOf<ChatContent>()
                    contents.add(
                        ChatContent(
                            role = "user",
                            parts = listOf(ChatPart(systemPrompt))
                        )
                    )
                    history.takeLast(MAX_HISTORY_SIZE).forEach { (role, text) ->
                        contents.add(
                            ChatContent(
                                role = if (role == "user") "user" else "model",
                                parts = listOf(ChatPart(text))
                            )
                        )
                    }
                    contents.add(
                        ChatContent(
                            role = "user",
                            parts = listOf(ChatPart(userMessage))
                        )
                    )

                    val request = ChatRequest(contents)
                    val response = chatApi.generateChatResponse(request, apiKey)
                    val reply = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "Sorry, no response from the AI. Please try again."

                    // Cache non-navigation responses
                    if (!reply.startsWith("navigate:") && normalizedInput !in localResponses) {
                        responseCache[cacheKey] = reply
                    }

                    return@withContext reply
                } catch (e: HttpException) {
                    val errorBody = e.response()?.errorBody()?.string() ?: "No error details"
                    println("Gemini API Error ${e.code()}: $errorBody")
                    when (e.code()) {
                        429 -> {
                            if (attempt < MAX_RETRIES - 1) {
                                delay(INITIAL_RETRY_DELAY_MS * (1 shl attempt))
                                attempt++
                                continue
                            }
                            return@withContext "Error: Too Many Requests (quota exceeded). Please try again later. [$errorBody]"
                        }
                        404 -> return@withContext "Error: Resource not found (check model: gemini-1.5-flash or endpoint) [$errorBody]"
                        403 -> return@withContext "Error: Forbidden (check API key or enable Generative Language API) [$errorBody]"
                        400 -> return@withContext "Error: Invalid Request (check parameters) [$errorBody]"
                        else -> return@withContext "Error: HTTP ${e.code()} - ${e.message()} [$errorBody]"
                    }
                } catch (e: IOException) {
                    return@withContext "Error: Network issue - ${e.message}"
                } catch (e: Exception) {
                    return@withContext "Error: ${e.message}"
                }
            }
            "Error: Max retries reached. Please try again later."
        }
    }
}