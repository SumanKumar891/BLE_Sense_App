package com.blesense.app

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiChatApi {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateChatResponse(
        @Body request: ChatRequest,
        @Query("key") apiKey: String
    ): ChatResponse
}

// Data classes for chat request and response (similar to translation)
data class ChatRequest(
    val contents: List<ChatContent>
)

data class ChatContent(
    val role: String,  // "user" or "model"
    val parts: List<ChatPart>
)

data class ChatPart(
    val text: String
)

data class ChatResponse(
    val candidates: List<ChatCandidate>
)

data class ChatCandidate(
    val content: ChatContent
)