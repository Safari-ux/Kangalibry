package com.example.turkids.repository

import com.example.turkids.data.model.paisDesdeJson
import com.example.turkids.data.remote.PaisesApiService
import com.example.turkids.data.remote.RetrofitPaises
import com.example.turkids.model.Pais
import com.example.turkids.model.banderaDesdeCodigo

class PaisRepository(private val api: PaisesApiService = RetrofitPaises.api) {

    suspend fun getPais(codigo: String): Pais? {
        val pais = paisDesdeJson(api.getPais(codigo)) ?: return null
        return if (pais.bandera.isEmpty()) pais.copy(bandera = banderaDesdeCodigo(codigo)) else pais
    }
}
