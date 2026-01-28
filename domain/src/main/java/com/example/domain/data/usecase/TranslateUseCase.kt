package com.example.domain.data.usecase

import com.example.domain.data.TranslateRequest
import com.example.domain.data.TranslateRequestDomain
import com.example.domain.data.TranslateResponseDomain
import com.example.domain.data.repository.TranslateRepository
import javax.inject.Inject

class TranslateUseCase @Inject constructor(
    private val repository: TranslateRepository
) {
    suspend operator fun invoke(apikey : String, request: TranslateRequest) : TranslateRequestDomain{
        return repository.translate(apikey, request)
    }
}