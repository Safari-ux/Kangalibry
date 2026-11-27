package com.example.turkids.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // 10.0.2.2 apunta al localhost del pc desde el emulador
    // en un celular real usar la ip del pc en la red local
    private const val BASE_URL = "http://10.0.2.2:8080/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
