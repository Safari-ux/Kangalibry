package com.example.turkids.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turkids.model.BancoDeContenido
import com.example.turkids.model.IdiomaPractica
import com.example.turkids.model.LeccionGuardada
import com.example.turkids.model.LeccionUiState
import com.example.turkids.repository.ProgresoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LeccionViewModel(private val repositorio: ProgresoRepository) : ViewModel() {

    private val _estado = MutableStateFlow(LeccionUiState())
    val estado: StateFlow<LeccionUiState> = _estado

    private var perfilId = ""
    private var unidadId = ""
    private var idioma = IdiomaPractica.TURCO
    private var semilla = 0L
    private var trabajoCarga: Job? = null
    private var finalizando = false

    // mientras carga de datastore no hay preguntas
    fun cargarUnidad(unidadId: String, idioma: IdiomaPractica, perfilId: String) {
        if (unidadId == this.unidadId && idioma == this.idioma && perfilId == this.perfilId) return
        val unidad = BancoDeContenido.unidades.firstOrNull { it.id == unidadId } ?: return

        this.unidadId = unidadId
        this.idioma = idioma
        this.perfilId = perfilId
        finalizando = false

        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            val guardada = repositorio.leccionGuardada(perfilId, idioma)
                ?.takeIf { it.unidadId == unidadId }

            semilla = guardada?.semilla ?: System.currentTimeMillis()

            val preguntas = BancoDeContenido.generarPreguntas(unidad, idioma, semilla)
            _estado.value = LeccionUiState(
                tituloUnidad = unidad.titulo,
                idioma = idioma,
                preguntas = preguntas,
                indiceActual = (guardada?.indice ?: 0).coerceAtMost(preguntas.lastIndex).coerceAtLeast(0),
                aciertos = guardada?.aciertos ?: 0
            )
        }
    }

    // el primer fallo da una segunda oportunidad, no castiga
    fun seleccionarOpcion(opcion: String) {
        val actual = _estado.value
        val pregunta = actual.preguntaActual ?: return
        if (actual.respondido || opcion in actual.descartadas) return

        val correcta = opcion == pregunta.respuestaCorrecta
        _estado.update {
            when {
                correcta -> it.copy(
                    opcionSeleccionada = opcion,
                    respondido = true,
                    esCorrecta = true,
                    aciertos = it.aciertos + 1
                )
                it.intentosFallidos == 0 -> it.copy(
                    descartadas = it.descartadas + opcion,
                    intentosFallidos = 1
                )
                else -> it.copy(
                    opcionSeleccionada = opcion,
                    respondido = true,
                    esCorrecta = false
                )
            }
        }
    }

    // el punto de retoma se guarda al avanzar, con los aciertos ya sumados
    fun siguientePregunta() {
        val actual = _estado.value
        // solo avanza si ya respondio: un doble toque no salta preguntas
        if (actual.preguntas.isEmpty() || !actual.respondido || actual.finalizado) return
        val siguienteIndice = actual.indiceActual + 1

        if (siguienteIndice >= actual.preguntas.size) {
            if (finalizando) return
            finalizando = true
            // se borra antes de marcar el final porque al terminar la pantalla navega
            viewModelScope.launch {
                repositorio.borrarLeccion(perfilId, idioma)
                _estado.update { it.copy(finalizado = true) }
            }
        } else {
            viewModelScope.launch {
                repositorio.guardarLeccion(
                    perfilId,
                    LeccionGuardada(unidadId, idioma, semilla, siguienteIndice, actual.aciertos)
                )
            }
            _estado.update {
                it.copy(
                    indiceActual = siguienteIndice,
                    opcionSeleccionada = null,
                    descartadas = emptySet(),
                    intentosFallidos = 0,
                    respondido = false,
                    esCorrecta = false
                )
            }
        }
    }
}
