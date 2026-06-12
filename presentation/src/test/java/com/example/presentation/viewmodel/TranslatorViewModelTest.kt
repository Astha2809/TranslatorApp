package com.example.presentation.viewmodel

import app.cash.turbine.test
import com.example.domain.data.Candidate
import com.example.domain.data.Content
import com.example.domain.data.GenerationConfig
import com.example.domain.data.Part
import com.example.domain.data.TranslateRequestDomain
import com.example.domain.data.usecase.TranslateUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranslatorViewModelTest {

    private val translateUseCase = mockk<TranslateUseCase>()
    private lateinit var viewModel: TranslatorViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TranslatorViewModel(translateUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `translate updates uiState to Success when use case returns data`() = runTest {
        // Given
        val text = "Hello"
        val language = "Spanish"
        val config = GenerationConfig()
        val expectedTranslation = "Hola"

        val response = TranslateRequestDomain(
            candidates = listOf(
                Candidate(
                    content = Content(
                        parts = listOf(Part(text = expectedTranslation))
                    )
                )
            )
        )

        coEvery { translateUseCase.invoke(any(), any()) } returns response

        // When & Then
        viewModel.uiState.test {
            assertEquals(TranslatorUiState.Initial, awaitItem())

            viewModel.translate(text, language, config)

            assertEquals(TranslatorUiState.Loading, awaitItem())
            val result = awaitItem()
            assert(result is TranslatorUiState.Success)
            assertEquals(expectedTranslation, (result as TranslatorUiState.Success).translatedText)
        }
    }

    @Test
    fun `translate updates uiState to Error when use case throws exception`() = runTest {
        // Given
        val text = "Hello"
        val language = "Spanish"
        val config = GenerationConfig()
        val errorMessage = "Network Error"

        coEvery { translateUseCase.invoke(any(), any()) } throws Exception(errorMessage)

        // When & Then
        viewModel.uiState.test {
            assertEquals(TranslatorUiState.Initial, awaitItem())

            viewModel.translate(text, language, config)

            assertEquals(TranslatorUiState.Loading, awaitItem())
            val result = awaitItem()
            assert(result is TranslatorUiState.Error)
            assertEquals(errorMessage, (result as TranslatorUiState.Error).errorMessage)
        }
    }

    @Test
    fun `generatePoem updates uiState to Success when use case returns data`() = runTest {
        // Given
        val topic = "Nature"
        val expectedPoem = "The trees are green..."

        val response = TranslateRequestDomain(
            candidates = listOf(
                Candidate(
                    content = Content(
                        parts = listOf(Part(text = expectedPoem))
                    )
                )
            )
        )

        coEvery { translateUseCase.invoke(any(), any()) } returns response

        // When & Then
        viewModel.uiState.test {
            assertEquals(TranslatorUiState.Initial, awaitItem())

            viewModel.generatePoem(topic)

            assertEquals(TranslatorUiState.Loading, awaitItem())
            val result = awaitItem()
            assert(result is TranslatorUiState.Success)
            assertEquals(expectedPoem, (result as TranslatorUiState.Success).translatedText)
        }
    }
}
