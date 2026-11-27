package com.example.turkids.data.remote

import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path

interface PaisesApiService {
    @GET("v3.1/alpha/{codigo}?fields=name,flag,translations")
    suspend fun getPais(@Path("codigo") codigo: String): JsonElement
}
