package com.example.domain.data.repository

import com.example.domain.data.TranslateRequest
import com.example.domain.data.TranslateRequestDomain
import com.example.domain.data.TranslateResponseDomain

interface TranslateRepository {
    suspend fun translate(apikey : String,request : TranslateRequest) : TranslateRequestDomain
}