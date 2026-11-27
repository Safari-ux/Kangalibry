package com.example.turkids

import com.example.turkids.data.remote.PaisesApiService
import com.example.turkids.model.Pais
import com.example.turkids.repository.PaisRepository
import com.google.gson.JsonParser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.IOException

class PaisRepositoryTest : StringSpec({

    val banderaChile = "\uD83C\uDDE8\uD83C\uDDF1"

    "getPais devuelve el nombre y la bandera que entrega la API" {
        runTest {
            val api = mockk<PaisesApiService>()
            val json = "[{\"name\":{\"common\":\"Chile\"},\"flag\":\"$banderaChile\"}]"
            coEvery { api.getPais("CL") } returns JsonParser.parseString(json)

            val pais = PaisRepository(api).getPais("CL")

            pais shouldBe Pais(nombre = "Chile", bandera = banderaChile)
            coVerify(exactly = 1) { api.getPais("CL") }
        }
    }

    "si la API no trae la bandera se arma con el codigo del pais" {
        runTest {
            val api = mockk<PaisesApiService>()
            coEvery { api.getPais("CL") } returns JsonParser.parseString("[{\"name\":{\"common\":\"Chile\"}}]")

            PaisRepository(api).getPais("CL") shouldBe Pais(nombre = "Chile", bandera = banderaChile)
        }
    }

    "getPais devuelve null si la respuesta no tiene un pais" {
        runTest {
            val api = mockk<PaisesApiService>()
            coEvery { api.getPais("XX") } returns JsonParser.parseString("[]")

            PaisRepository(api).getPais("XX").shouldBeNull()
        }
    }

    "getPais propaga el error cuando falla la red" {
        runTest {
            val api = mockk<PaisesApiService>()
            coEvery { api.getPais("CL") } throws IOException("sin red")

            shouldThrow<IOException> { PaisRepository(api).getPais("CL") }
        }
    }
})
