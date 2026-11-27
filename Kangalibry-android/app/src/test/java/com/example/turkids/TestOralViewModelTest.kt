package com.example.turkids

import com.example.turkids.model.IdiomaPractica
import com.example.turkids.model.ResultadoOral
import com.example.turkids.viewmodel.TestOralViewModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class TestOralViewModelTest : StringSpec({

    "iniciar prepara cinco palabras para el idioma elegido" {
        val viewModel = TestOralViewModel()
        viewModel.iniciar(IdiomaPractica.TURCO, semilla = 1L)

        val estado = viewModel.estado.value
        estado.palabras shouldHaveSize 5
        estado.indiceActual shouldBe 0
        estado.aciertos shouldBe 0
        estado.finalizado shouldBe false
    }

    "practicando turco se muestra la palabra en turco y se espera la respuesta en espanol" {
        val viewModel = TestOralViewModel()
        viewModel.iniciar(IdiomaPractica.TURCO, semilla = 1L)

        val estado = viewModel.estado.value
        val palabra = estado.palabraActual.shouldNotBeNull()
        estado.textoMostrado shouldBe palabra.tr
        estado.respuestaEsperada shouldBe palabra.es
        estado.codigoIdiomaRespuesta shouldBe "es-ES"
    }

    "practicando espanol se muestra la palabra en espanol y se espera la respuesta en turco" {
        val viewModel = TestOralViewModel()
        viewModel.iniciar(IdiomaPractica.ESPANOL, semilla = 1L)

        val estado = viewModel.estado.value
        val palabra = estado.palabraActual.shouldNotBeNull()
        estado.textoMostrado shouldBe palabra.es
        estado.respuestaEsperada shouldBe palabra.tr
        estado.codigoIdiomaRespuesta shouldBe "tr-TR"
    }

    "una respuesta correcta suma un acierto" {
        val viewModel = TestOralViewModel()
        viewModel.iniciar(IdiomaPractica.TURCO, semilla = 1L)

        viewModel.recibirTexto(listOf(viewModel.estado.value.respuestaEsperada))

        viewModel.estado.value.resultado shouldBe ResultadoOral.CORRECTO
        viewModel.estado.value.aciertos shouldBe 1
    }

    "una respuesta incorrecta no suma y deja ver la correcta" {
        val viewModel = TestOralViewModel()
        viewModel.iniciar(IdiomaPractica.TURCO, semilla = 1L)

        viewModel.recibirTexto(listOf("zzzz"))

        viewModel.estado.value.resultado shouldBe ResultadoOral.INCORRECTO
        viewModel.estado.value.aciertos shouldBe 0
        viewModel.estado.value.respuestaEsperada.isNotEmpty() shouldBe true
    }

    "si no se escucha nada queda un mensaje y se puede volver a intentar" {
        val viewModel = TestOralViewModel()
        viewModel.iniciar(IdiomaPractica.TURCO, semilla = 1L)
        viewModel.escuchando()

        viewModel.recibirTexto(emptyList())

        val estado = viewModel.estado.value
        estado.resultado.shouldBeNull()
        estado.mensaje.shouldNotBeNull()
        estado.escuchando shouldBe false
    }

    "siguiente avanza solo despues de responder" {
        val viewModel = TestOralViewModel()
        viewModel.iniciar(IdiomaPractica.TURCO, semilla = 1L)

        viewModel.siguiente()
        viewModel.estado.value.indiceActual shouldBe 0

        viewModel.recibirTexto(listOf("zzzz"))
        viewModel.siguiente()
        viewModel.estado.value.indiceActual shouldBe 1
        viewModel.estado.value.resultado.shouldBeNull()
    }

    "despues de cinco respuestas el test termina con el puntaje" {
        val viewModel = TestOralViewModel()
        viewModel.iniciar(IdiomaPractica.TURCO, semilla = 1L)

        repeat(5) {
            viewModel.recibirTexto(listOf(viewModel.estado.value.respuestaEsperada))
            viewModel.siguiente()
        }

        viewModel.estado.value.finalizado shouldBe true
        viewModel.estado.value.aciertos shouldBe 5
    }

    "jugar de nuevo empieza otro test con el mismo idioma" {
        val viewModel = TestOralViewModel()
        viewModel.iniciar(IdiomaPractica.ESPANOL, semilla = 1L)
        repeat(5) {
            viewModel.recibirTexto(listOf("zzzz"))
            viewModel.siguiente()
        }

        viewModel.jugarDeNuevo(semilla = 2L)

        val estado = viewModel.estado.value
        estado.finalizado shouldBe false
        estado.aciertos shouldBe 0
        estado.idioma shouldBe IdiomaPractica.ESPANOL
        estado.palabras shouldHaveSize 5
    }
})
