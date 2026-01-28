package com.example.presentation.viewmodel

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PromptBuilder

import com.example.data.di.RetrofitClient
import com.example.domain.data.Content
import com.example.domain.data.InlineData
import com.example.domain.data.Part
import com.example.domain.data.TranslateRequest
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

            //    val response = geminiApiService.generateContent(BuildConfig.API_KEY, request)
               val response =  translateUseCase.invoke(BuildConfig.API_KEY, request)
                Log.d("Response from gemini",response.toString())
                response
//                if (response.isSuccessful) {
//                    val body = response.body()
//                    if (body != null && body.candidates.isNotEmpty()) {
//                        val firstCandidate = body.candidates.first()
//                        if (firstCandidate.content?.parts != null) {
//                            val description = firstCandidate.content!!.parts?.firstOrNull()?.text ?: ""
//                            _uiState.value = TranslatorUiState.Success(description, bitmap)
//                        } else {
//                            _uiState.value = TranslatorUiState.Error("Response blocked. Finish reason: ${firstCandidate.finishReason}")
//                        }
//                    } else {
//                        _uiState.value = TranslatorUiState.Error("Image description failed: No candidates received.")
//                    }
//                } else {
//                    val errorBodyString = response.errorBody()?.string()
//                    Log.e("TranslatorViewModel", "Image description failed with code ${response.code()}: $errorBodyString")
//
//                    val errorMessage = when (response.code()) {
//                        429 -> "You have exceeded your request limit. Please try again later."
//                        else -> "Image description failed (Error ${response.code()}). Please try again."
//                    }
//                    _uiState.value = TranslatorUiState.Error(errorMessage)
//                }
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

               // val response = geminiApiService.generateContent(BuildConfig.API_KEY, request)
                val response =  translateUseCase.invoke(apikey = BuildConfig.API_KEY,request = request)
                Log.d("Response", response.toString())
//                if (response.isSuccessful) {
//                    val body = response.body()
//                    if (body != null && body.candidates.isNotEmpty()) {
//                        val firstCandidate = body.candidates.first()
//                        if (firstCandidate.content?.parts != null) {
//                            val poem = firstCandidate.content!!.parts?.firstOrNull()?.text ?: ""
//                            _uiState.value = TranslatorUiState.Success(poem)
//                        } else {
//                            _uiState.value = TranslatorUiState.Error("Response blocked. Finish reason: ${firstCandidate.finishReason}")
//                        }
//                    } else {
//                        _uiState.value = TranslatorUiState.Error("Poem generation failed: No candidates received.")
//                    }
//                } else {
//                    val errorBodyString = response.errorBody()?.string()
//                    Log.e("TranslatorViewModel", "Poem generation failed with code ${response.code()}: $errorBodyString")
//
//                    val errorMessage = when (response.code()) {
//                        429 -> "You have exceeded your request limit. Please try again later."
//                        else -> "Poem generation failed (Error ${response.code()}). Please try again."
//                    }
//                    _uiState.value = TranslatorUiState.Error(errorMessage)
//                }
            } catch (e: Exception) {
                Log.e("TranslatorViewModel", "Poem generation failed", e)
                _uiState.value = TranslatorUiState.Error(e.localizedMessage ?: "An unknown error occurred")
            }
        }
    }

//    fun translate(text: String, language: String, generationConfig: GenerationConfig) {
//        if (BuildConfig.API_KEY.isBlank()) {
//            _uiState.value = TranslatorUiState.Error("API key not found. Please add it to your local.properties file.")
//            return
//        }
//
//        viewModelScope.launch {
//            _uiState.value = TranslatorUiState.Loading
//            try {
//                val prompt = PromptBuilder.buildTranslatePrompt(text, language)
//                val request = TranslateRequest(contents = listOf(Content(role = "user", parts = listOf(Part(text = prompt)))), generationConfig = generationConfig)
//
//           //     val response = geminiApiService.generateContent(BuildConfig.API_KEY, request)
//                val response =  translateUseCase.invoke(apikey = BuildConfig.API_KEY,request = request)
//
//                if (response.isSuccessful) {
//                    val body = response.body()
//                    if (body != null && body.candidates.isNotEmpty()) {
//                        val firstCandidate = body.candidates.first()
//                        if (firstCandidate.content?.parts != null) {
//                            val translatedText = firstCandidate.content!!.parts?.firstOrNull()?.text ?: ""
//                            _uiState.value = TranslatorUiState.Success(translatedText)
//                        } else {
//                            _uiState.value = TranslatorUiState.Error("Response blocked. Finish reason: ${firstCandidate.finishReason}")
//                        }
//                    } else {
//                        _uiState.value = TranslatorUiState.Error("Translation failed: No candidates received.")
//                    }
//                } else {
//                    val errorBodyString = response.errorBody()?.string()
//                    Log.e("TranslatorViewModel", "Translation failed with code ${response.code()}: $errorBodyString")
//
//                    val errorMessage = when (response.code()) {
//                        429 -> "You have exceeded your request limit. Please try again later."
//                        else -> "Translation failed (Error ${response.code()}). Please try again."
//                    }
//                    _uiState.value = TranslatorUiState.Error(errorMessage)
//                }
//            } catch (e: Exception) {
//                Log.e("TranslatorViewModel", "Translation failed", e)
//                _uiState.value = TranslatorUiState.Error(e.localizedMessage ?: "An unknown error occurred")
//            }
//        }
//    }
}
