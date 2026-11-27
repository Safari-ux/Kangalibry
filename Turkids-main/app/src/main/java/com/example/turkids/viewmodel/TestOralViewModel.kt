package com.example.turkids.viewmodel

import androidx.lifecycle.ViewModel
import com.example.turkids.model.BancoDeContenido
import com.example.turkids.model.IdiomaPractica
import com.example.turkids.model.ResultadoOral
import com.example.turkids.model.TestOralUiState
import com.example.turkids.model.coincideRespuesta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class TestOralViewModel : ViewModel() {

    private val _estado = MutableStateFlow(TestOralUiState())
    val estado: StateFlow<TestOralUiState> = _estado

    fun iniciar(idioma: IdiomaPractica, semilla: Long = System.currentTimeMillis()) {
        val actual = _estado.value
        if (actual.idioma == idioma && actual.palabras.isNotEmpty()) return
        empezar(idioma, semilla)
    }

    fun jugarDeNuevo(semilla: Long = System.currentTimeMillis()) {
        empezar(_estado.value.idioma, semilla)
    }

    fun escuchando() {
        _estado.update { it.copy(escuchando = true, mensaje = null) }
    }

    fun recibirTexto(candidatos: List<String>) {
        if (candidatos.isEmpty()) {
            errorDeEscucha(MENSAJE_SIN_VOZ)
            return
        }
        _estado.update { actual ->
            if (actual.palabraActual == null || actual.resultado != null) return@update actual
            val correcto = coincideRespuesta(actual.respuestaEsperada, candidatos)
            actual.copy(
                escuchando = false,
                escuchado = candidatos.first(),
                mensaje = null,
                resultado = if (correcto) ResultadoOral.CORRECTO else ResultadoOral.INCORRECTO,
                aciertos = if (correcto) actual.aciertos + 1 else actual.aciertos
            )
        }
    }

    fun errorDeEscucha(mensaje: String) {
        _estado.update { it.copy(escuchando = false, mensaje = mensaje) }
    }

    fun siguiente() {
        _estado.update { actual ->
            if (actual.resultado == null) return@update actual
            val siguienteIndice = actual.indiceActual + 1
            if (siguienteIndice >= actual.palabras.size) {
                actual.copy(finalizado = true, resultado = null, escuchado = null)
            } else {
                actual.copy(indiceActual = siguienteIndice, resultado = null, escuchado = null, mensaje = null)
            }
        }
    }

    private fun empezar(idioma: IdiomaPractica, semilla: Long) {
        val palabras = BancoDeContenido.unidades
            .flatMap { it.palabras }
            .shuffled(Random(semilla))
            .take(RONDAS)
        _estado.value = TestOralUiState(idioma = idioma, palabras = palabras)
    }

    private companion object {
        const val RONDAS = 5
        const val MENSAJE_SIN_VOZ = "No te escuché bien. Inténtalo de nuevo."
    }
}
