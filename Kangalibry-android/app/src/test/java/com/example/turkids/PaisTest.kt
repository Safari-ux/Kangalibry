package com.example.turkids

import com.example.turkids.data.model.paisDesdeJson
import com.example.turkids.model.Pais
import com.example.turkids.model.banderaDesdeCodigo
import com.google.gson.JsonParser
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class PaisTest : StringSpec({

    val banderaChile = "\uD83C\uDDE8\uD83C\uDDF1"

    "la bandera se arma con el codigo ISO de dos letras" {
        banderaDesdeCodigo("CL") shouldBe banderaChile
        banderaDesdeCodigo("tr") shouldBe "\uD83C\uDDF9\uD83C\uDDF7"
    }

    "un codigo invalido no genera bandera" {
        banderaDesdeCodigo("").shouldBe("")
        banderaDesdeCodigo("CHL").shouldBe("")
        banderaDesdeCodigo("1A").shouldBe("")
    }

    "paisDesdeJson lee el nombre en espanol y la bandera de una lista" {
        val json = "[{\"name\":{\"common\":\"Chile\"},\"flag\":\"$banderaChile\"," +
            "\"translations\":{\"spa\":{\"common\":\"Chilito\"}}}]"

        paisDesdeJson(JsonParser.parseString(json)) shouldBe Pais(nombre = "Chilito", bandera = banderaChile)
    }

    "paisDesdeJson acepta un objeto suelto" {
        val json = "{\"name\":{\"common\":\"Chile\"},\"flag\":\"$banderaChile\"}"

        paisDesdeJson(JsonParser.parseString(json)) shouldBe Pais(nombre = "Chile", bandera = banderaChile)
    }

    "paisDesdeJson usa el nombre comun si no hay traduccion al espanol" {
        val json = "[{\"name\":{\"common\":\"Chile\"},\"translations\":{\"fra\":{\"common\":\"Chili\"}}}]"

        paisDesdeJson(JsonParser.parseString(json)) shouldBe Pais(nombre = "Chile", bandera = "")
    }

    "paisDesdeJson devuelve null si la respuesta no trae un pais" {
        paisDesdeJson(JsonParser.parseString("[]")).shouldBeNull()
        paisDesdeJson(JsonParser.parseString("{}")).shouldBeNull()
        paisDesdeJson(JsonParser.parseString("\"texto\"")).shouldBeNull()
    }
})
