package com.example.turkids.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitPaises {

    private const val BASE_URL = "https://restcountries.com/"

    val api: PaisesApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PaisesApiService::class.java)
    }
}
