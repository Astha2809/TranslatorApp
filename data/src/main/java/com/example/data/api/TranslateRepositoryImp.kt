package com.example.data.api

import com.example.domain.data.TranslateRequest
import com.example.domain.data.TranslateRequestDomain
import com.example.domain.data.repository.TranslateRepository

import javax.inject.Inject

class TranslateRepositoryImp @Inject constructor(val geminiApiService: GeminiApiService) :
    TranslateRepository {
    override suspend fun translate(apikey : String,request: TranslateRequest): TranslateRequestDomain {

        return geminiApiService.generateContent(apikey,request)
    }

}

