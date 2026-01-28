package com.example.domain.data

import com.squareup.moshi.Json

data class TranslateRequestDomain(
    val text : TranslateRequest
)

data class TranslateResponseDomain(
    val translatedText : String
)

data class Translation(
    val translatedText: String
)

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

data class Part(
    val text: String? = null,
    @Json(name = "inline_data") val inlineData: InlineData? = null
)

data class InlineData(
    @Json(name = "mime_type") val mimeType: String,
    val data: String // Base64-encoded image
)
