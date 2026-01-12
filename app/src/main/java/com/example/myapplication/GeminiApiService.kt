package com.example.myapplication

import com.squareup.moshi.Json
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApiService {
    @POST("v1beta/models/gemini-flash-latest:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: TranslateRequest
    ): Response<TranslateResponse>
}


data class TranslateRequest(
    val contents: List<Content>,
    @Json(name = "generation_config") val generationConfig: GenerationConfig? = null
)

data class GenerationConfig(
    val temperature: Float = 0.4f,
    @Json(name = "top_p") val topP: Float = 0.9f,
    @Json(name = "top_k") val topK: Int = 40,
    @Json(name = "max_output_tokens") val maxOutputTokens: Int = 100
)

data class Content(
    val role: String,
    val parts: List<Part>? = null // Parts can be null if the response is empty
)

// A part of a multimodal request
data class Part(
    val text: String? = null,
    @Json(name = "inline_data") val inlineData: InlineData? = null
)

// Represents the inline data (e.g., an image) for a multimodal request
data class InlineData(
    @Json(name = "mime_type") val mimeType: String,
    val data: String // Base64-encoded image
)

data class TranslateResponse(
    val candidates: List<Candidate>
)

data class Candidate(
    val content: Content? = null, // Content can be null if the response is blocked
    @Json(name = "finish_reason") val finishReason: String? = null
)

// Error parsing
data class GeminiErrorResponse(
    val error: GeminiError
)

data class GeminiError(
    val message: String
)
