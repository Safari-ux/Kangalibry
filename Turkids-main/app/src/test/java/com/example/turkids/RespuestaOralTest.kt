package com.example.turkids

import com.example.turkids.model.coincideRespuesta
import com.example.turkids.model.normalizarRespuesta
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RespuestaOralTest : StringSpec({

    "normalizar quita mayusculas, tildes y signos" {
        normalizarRespuesta("  ¡Sí! ") shouldBe "si"
        normalizarRespuesta("Teşekkürler") shouldBe "tesekkurler"
        normalizarRespuesta("Hayır") shouldBe "hayir"
        normalizarRespuesta("İki") shouldBe "iki"
    }

    "una respuesta correcta coincide aunque cambien mayusculas y tildes" {
        coincideRespuesta("Sí", listOf("sí")) shouldBe true
        coincideRespuesta("Gracias", listOf("GRACIAS.")) shouldBe true
    }

    "la palabra turca se acepta sin diacriticos" {
        coincideRespuesta("Teşekkürler", listOf("tesekkurler")) shouldBe true
        coincideRespuesta("Hayır", listOf("hayir")) shouldBe true
        coincideRespuesta("Üç", listOf("uc")) shouldBe true
    }

    "los numeros dichos como digitos tambien cuentan" {
        coincideRespuesta("Uno", listOf("1")) shouldBe true
        coincideRespuesta("Cuatro", listOf("4")) shouldBe true
        coincideRespuesta("Bir", listOf("1")) shouldBe true
        coincideRespuesta("Dört", listOf("4")) shouldBe true
    }

    "se acepta una frase que contiene la palabra esperada" {
        coincideRespuesta("Gato", listOf("el gato")) shouldBe true
    }

    "se acepta si cualquiera de las alternativas del reconocedor coincide" {
        coincideRespuesta("Perro", listOf("pero", "perro", "pelo")) shouldBe true
    }

    "una palabra distinta no coincide" {
        coincideRespuesta("Gato", listOf("perro")) shouldBe false
        coincideRespuesta("Gato", listOf("gatos")) shouldBe false
    }

    "sin candidatos no coincide" {
        coincideRespuesta("Gato", emptyList()) shouldBe false
    }
})
