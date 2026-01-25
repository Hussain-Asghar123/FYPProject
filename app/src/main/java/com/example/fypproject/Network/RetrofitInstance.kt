package com.example.fypproject.Network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.fypproject.BuildConfig
import com.example.fypproject.Network.ApiService


object RetrofitInstance {

    private val BASE_URL = BuildConfig.BASE_URL

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}