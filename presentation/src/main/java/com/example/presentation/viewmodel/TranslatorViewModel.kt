package com.example.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PromptBuilder
import com.example.domain.data.Content
import com.example.domain.data.InlineData
import com.example.domain.data.Part
import com.example.domain.data.TranslateRequest
import com.example.domain.data.GenerationConfig
import com.example.domain.data.usecase.TranslateUseCase
import com.example.presentation.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

sealed interface TranslatorUiState {
    object Initial : TranslatorUiState
    object Loading : TranslatorUiState
    data class Success(val translatedText: String, val image: Bitmap? = null) : TranslatorUiState
    data class Error(val errorMessage: String) : TranslatorUiState
}

@HiltViewModel
class TranslatorViewModel @Inject constructor(private val translateUseCase: TranslateUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow<TranslatorUiState>(TranslatorUiState.Initial)
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()


    fun summarizePdf(context: Context, uri: Uri) {
        if (BuildConfig.API_KEY.isBlank()) {
            _uiState.value = TranslatorUiState.Error("API key not found. Please add it to your local.properties file.")
            return
        }

        viewModelScope.launch {
            _uiState.value = TranslatorUiState.Loading
            try {
                val pdfBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (pdfBytes != null) {
                    val base64Pdf = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
                    val prompt = PromptBuilder.buildSummarizePdfPrompt()
                    val inlineData = InlineData(mimeType = "application/pdf", data = base64Pdf)
                    val request = TranslateRequest(
                        contents = listOf(
                            Content(
                                role = "user",
                                parts = listOf(Part(text = prompt), Part(inlineData = inlineData))
                            )
                        )
                    )

                    val response = translateUseCase.invoke(BuildConfig.API_KEY, request)

                    val candidates = response.candidates
                    if (!candidates.isNullOrEmpty()) {
                        val firstCandidate = candidates[0]
                        val content = firstCandidate.content
                        val parts = content?.parts
                        if (!parts.isNullOrEmpty()) {
                            val description = parts[0].text ?: ""
                            _uiState.value = TranslatorUiState.Success(description)
                        } else {
                            val finishReason = firstCandidate.finishReason
                            if (finishReason != null) {
                                _uiState.value = TranslatorUiState.Error("Response blocked. Finish reason: $finishReason")
                            } else {
                                _uiState.value = TranslatorUiState.Error("Summarization failed: No content parts received.")
                            }
                        }
                    } else {
                        _uiState.value = TranslatorUiState.Error("Summarization failed: No candidates received.")
                    }
                } else {
                    _uiState.value = TranslatorUiState.Error("Failed to read PDF file.")
                }
            } catch (e: Exception) {
                Log.e("TranslatorViewModel", "Summarization failed", e)
                _uiState.value = TranslatorUiState.Error(e.localizedMessage ?: "An unknown error occurred")
            }
        }
    }

    fun describeImage(bitmap: Bitmap, style: String) {
        if (BuildConfig.API_KEY.isBlank()) {
            _uiState.value = TranslatorUiState.Error("API key not found. Please add it to your local.properties file.")
            return
        }

        viewModelScope.launch {
            _uiState.value = TranslatorUiState.Loading

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            try {
                val prompt = PromptBuilder.buildDescribeImagePrompt(style)
                val inlineData = InlineData(mimeType = "image/jpeg", data = base64Image)
                val request = TranslateRequest(
                    contents = listOf(
                        Content(
                            role = "user",
                            parts = listOf(Part(text = prompt), Part(inlineData = inlineData))
                        )
                    )
                )

                val response = translateUseCase.invoke(BuildConfig.API_KEY, request)

                val candidates = response.candidates
                if (!candidates.isNullOrEmpty()) {
                    val firstCandidate = candidates[0]
                    val content = firstCandidate.content
                    val parts = content?.parts
                    if (!parts.isNullOrEmpty()) {
                        val description = parts[0].text ?: ""
                        _uiState.value = TranslatorUiState.Success(description, bitmap)
                    } else {
                        val finishReason = firstCandidate.finishReason
                        if (finishReason != null) {
                            _uiState.value = TranslatorUiState.Error("Response blocked. Finish reason: $finishReason")
                        } else {
                            _uiState.value = TranslatorUiState.Error("Image description failed: No content parts received.")
                        }
                    }
                } else {
                    _uiState.value = TranslatorUiState.Error("Image description failed: No candidates received.")
                }
            } catch (e: Exception) {
                Log.e("TranslatorViewModel", "Image description failed", e)
                _uiState.value = TranslatorUiState.Error(e.localizedMessage ?: "An unknown error occurred")
            }
        }
    }

    fun generatePoem(topic: String) {
        if (BuildConfig.API_KEY.isBlank()) {
            _uiState.value = TranslatorUiState.Error("API key not found. Please add it to your local.properties file.")
            return
        }

        viewModelScope.launch {
            _uiState.value = TranslatorUiState.Loading
            try {
                val prompt = PromptBuilder.buildPoemPrompt(topic)
                val request = TranslateRequest(contents = listOf(Content(role = "user", parts = listOf(Part(text = prompt)))))

                val response = translateUseCase.invoke(apikey = BuildConfig.API_KEY, request = request)

                val candidates = response.candidates
                if (!candidates.isNullOrEmpty()) {
                    val firstCandidate = candidates[0]
                    val content = firstCandidate.content
                    val parts = content?.parts
                    if (!parts.isNullOrEmpty()) {
                        val poem = parts[0].text ?: ""
                        _uiState.value = TranslatorUiState.Success(poem)
                    } else {
                        val finishReason = firstCandidate.finishReason
                        if (finishReason != null) {
                            _uiState.value = TranslatorUiState.Error("Response blocked. Finish reason: $finishReason")
                        } else {
                            _uiState.value = TranslatorUiState.Error("Poem generation failed: No content parts received.")
                        }
                    }
                } else {
                    _uiState.value = TranslatorUiState.Error("Poem generation failed: No candidates received.")
                }
            } catch (e: Exception) {
                Log.e("TranslatorViewModel", "Poem generation failed", e)
                _uiState.value = TranslatorUiState.Error(e.localizedMessage ?: "An unknown error occurred")
            }
        }
    }

    fun translate(text: String, language: String, generationConfig: GenerationConfig) {
        if (BuildConfig.API_KEY.isBlank()) {
            _uiState.value = TranslatorUiState.Error("API key not found. Please add it to your local.properties file.")
            return
        }

        viewModelScope.launch {
            _uiState.value = TranslatorUiState.Loading
            try {
                val prompt = PromptBuilder.buildTranslatePrompt(text, language)
                val request = TranslateRequest(contents = listOf(Content(role = "user", parts = listOf(Part(text = prompt)))), generationConfig = generationConfig)

                val response = translateUseCase.invoke(apikey = BuildConfig.API_KEY, request = request)

                val candidates = response.candidates
                if (!candidates.isNullOrEmpty()) {
                    val firstCandidate = candidates[0]
                    val content = firstCandidate.content
                    val parts = content?.parts
                    if (!parts.isNullOrEmpty()) {
                        val translatedText = parts[0].text ?: ""
                        _uiState.value = TranslatorUiState.Success(translatedText)
                    } else {
                        val finishReason = firstCandidate.finishReason
                        if (finishReason != null) {
                            _uiState.value = TranslatorUiState.Error("Response blocked. Finish reason: $finishReason")
                        } else {
                            _uiState.value = TranslatorUiState.Error("Translation failed: No content parts received.")
                        }
                    }
                } else {
                    _uiState.value = TranslatorUiState.Error("Translation failed: No candidates received.")
                }
            } catch (e: Exception) {
                Log.e("TranslatorViewModel", "Translation failed", e)
                _uiState.value = TranslatorUiState.Error(e.localizedMessage ?: "An unknown error occurred")
            }
        }
    }
}
