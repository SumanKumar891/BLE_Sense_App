package com.blesense.app

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException

interface GoogleTranslationApi {
    @GET("language/translate/v2")
    suspend fun translateText(
        @Query("q") text: String,
        @Query("target") targetLanguage: String,
        @Query("key") apiKey: String,
        @Query("source") sourceLanguage: String? = null
    ): GoogleTranslationResponse
}

data class GoogleTranslationResponse(
    val data: TranslationData
)

data class TranslationData(
    val translations: List<TranslatedText>
)

data class TranslatedText(
    val translatedText: String
)

interface TranslationService {
    suspend fun translateText(text: String, targetLanguage: String): String
}

object TranslationCache {
    private val cache = mutableMapOf<String, String>()
    fun get(key: String): String? = cache[key]
    fun put(key: String, value: String) {
        cache[key] = value
    }
}

class GoogleTranslationService(
    private val apiKey: String = BuildConfig.API_KEY
) : TranslationService {
    companion object {
        val translationRetrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl("https://translation.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        val geminiRetrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }

    private val translationApi = translationRetrofit.create(GoogleTranslationApi::class.java)

    override suspend fun translateText(
        text: String,
        targetLanguage: String
    ): String = translateBatch(listOf(text), targetLanguage).first()

    suspend fun translateBatch(
        texts: List<String>,
        targetLanguage: String
    ): List<String> {
        return try {
            val uncachedTexts = texts.filter { text ->
                TranslationCache.get("$text-$targetLanguage") == null
            }

            if (uncachedTexts.isEmpty()) {
                return texts.map { TranslationCache.get("$it-$targetLanguage")!! }
            }

            val response = translationApi.translateText(
                text = uncachedTexts.joinToString("\n"),
                targetLanguage = targetLanguage,
                apiKey = apiKey
            )

            val translatedList = response.data.translations.map { it.translatedText }
            uncachedTexts.forEachIndexed { index, original ->
                val translated = translatedList.getOrElse(index) { original }
                TranslationCache.put("$original-$targetLanguage", translated)
            }

            texts.map { TranslationCache.get("$it-$targetLanguage") ?: it }
        } catch (e: Exception) {
            when (e) {
                is retrofit2.HttpException -> {
                    val errorBody = e.response()?.errorBody()?.string() ?: "No error details"
                    println("Translation API Error ${e.code()}: $errorBody")
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
                println(it)
                e.printStackTrace()
            }
            texts
        }
    }
}