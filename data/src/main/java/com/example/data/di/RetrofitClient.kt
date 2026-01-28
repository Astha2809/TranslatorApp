package com.example.data.di

import com.example.data.api.GeminiApiService
import com.example.data.api.TranslateRepositoryImp
import com.example.domain.data.repository.TranslateRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

@Module
@InstallIn(SingletonComponent::class)
object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"


    @Provides
    fun moshi() : Moshi{
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    fun retrofit(moshi: Moshi) : Retrofit{
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    fun provideApiService() : GeminiApiService{
        val retrofit = retrofit(moshi())
        return retrofit.create(GeminiApiService::class.java)
    }

    @Provides
    fun provideTranslateRepositoryImp(repository : TranslateRepositoryImp) : TranslateRepository  {
        return repository
    }
}
