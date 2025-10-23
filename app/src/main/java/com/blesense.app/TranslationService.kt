package com.blesense.app

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException

// Interface defining the Google Cloud Translation API endpoint
interface GoogleTranslationApi {
    // GET request to translate text using the Google Translation API
    @GET("language/translate/v2")
    suspend fun translateText(
        @Query("q") text: String, // Text to translate
        @Query("target") targetLanguage: String, // Target language code (e.g., "es" for Spanish)
        @Query("key") apiKey: String, // API key for authentication
        @Query("source") sourceLanguage: String? = null // Optional source language code
    ): GoogleTranslationResponse // Response containing translated text
}

// Data class to represent the API response structure
data class GoogleTranslationResponse(
    val data: TranslationData // Nested data containing translations
)

// Data class to hold the list of translations
data class TranslationData(
    val translations: List<TranslatedText> // List of translated text objects
)

// Data class for individual translated text
data class TranslatedText(
    val translatedText: String // The translated text
)

// Interface for translation service abstraction
interface TranslationService {
    // Translates a single text to the target language
    suspend fun translateText(text: String, targetLanguage: String): String
}

// Singleton object to cache translations for performance optimization
object TranslationCache {
    private val cache = mutableMapOf<String, String>() // In-memory cache for translations
    // Retrieve cached translation for a given key
    fun get(key: String): String? = cache[key]
    // Store a translation in the cache
    fun put(key: String, value: String) {
        cache[key] = value
    }
}

// Implementation of TranslationService using Google Cloud Translation API
class GoogleTranslationService(
    private val apiKey: String = BuildConfig.API_KEY // API key from build configuration
) : TranslationService {
    // Companion object for lazy-initialized Retrofit instances
    companion object {
        // Retrofit instance for Google Translation API
        val translationRetrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl("https://translation.googleapis.com/") // Base URL for translation API
                .addConverterFactory(GsonConverterFactory.create()) // JSON converter for responses
                .build()
        }
        // Retrofit instance for Gemini API (not used in this class but included for potential future use)
        val geminiRetrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/") // Base URL for Gemini API
                .addConverterFactory(GsonConverterFactory.create()) // JSON converter for responses
                .build()
        }
    }

    // Create API service instance from Retrofit
    private val translationApi = translationRetrofit.create(GoogleTranslationApi::class.java)

    // Translates a single text by delegating to batch translation
    override suspend fun translateText(
        text: String,
        targetLanguage: String
    ): String = translateBatch(listOf(text), targetLanguage).first()

    // Translates multiple texts in a batch, utilizing caching for efficiency
    suspend fun translateBatch(
        texts: List<String>, // List of texts to translate
        targetLanguage: String // Target language code
    ): List<String> {
        return try {
            // Filter out texts that are already cached
            val uncachedTexts = texts.filter { text ->
                TranslationCache.get("$text-$targetLanguage") == null
            }

            // If all texts are cached, return cached translations
            if (uncachedTexts.isEmpty()) {
                return texts.map { TranslationCache.get("$it-$targetLanguage")!! }
            }

            // Call the API to translate uncached texts
            val response = translationApi.translateText(
                text = uncachedTexts.joinToString("\n"), // Combine texts with newline separator
                targetLanguage = targetLanguage,
                apiKey = apiKey
            )

            // Extract translated texts from response
            val translatedList = response.data.translations.map { it.translatedText }
            // Cache the translations
            uncachedTexts.forEachIndexed { index, original ->
                val translated = translatedList.getOrElse(index) { original } // Fallback to original if translation fails
                TranslationCache.put("$original-$targetLanguage", translated)
            }

            // Return translations for all texts (cached or newly fetched)
            texts.map { TranslationCache.get("$it-$targetLanguage") ?: it }
        } catch (e: Exception) {
            // Handle various types of exceptions
            when (e) {
                is retrofit2.HttpException -> {
                    // Extract error details from HTTP response
                    val errorBody = e.response()?.errorBody()?.string() ?: "No error details"
                    println("Translation API Error ${e.code()}: $errorBody")
                    // Map HTTP status codes to specific error messages
                    when (e.code()) {
                        404 -> "Translation API Error: Resource not found (check endpoint) [$errorBody]"
                        403 -> "Translation API Error: Forbidden (check API key or enable API) [$errorBody]"
                        429 -> "Translation API Error: Too Many Requests (quota exceeded) [$errorBody]"
                        400 -> "Translation API Error: Invalid Request (check parameters) [$errorBody]"
                        else -> "Translation API Error: HTTP ${e.code()} - ${e.message()} [$errorBody]"
                    }
                }
                is IOException -> "Translation API Error: Network error - ${e.message}"
                else -> "Translation API Error: ${e.message}"
            }.also {
                println(it) // Log the error
                e.printStackTrace() // Print stack trace for debugging
            }
            texts // Return original texts on error
        }
    }
}