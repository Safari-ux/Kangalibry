package com.example.turkids.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turkids.model.BancoDeContenido
import com.example.turkids.model.IdiomaPractica
import com.example.turkids.model.ProgresoUiState
import com.example.turkids.model.calcularEstrellas
import com.example.turkids.repository.ProgresoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProgresoViewModel(private val repositorio: ProgresoRepository) : ViewModel() {

    private val idsUnidades = BancoDeContenido.unidades.map { it.id }

    // el estado sale de datastore con stateFlow: cada escritura se refleja sola
    // las estrellas dependen del perfil e idioma activos
    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<ProgresoUiState> = combine(
        repositorio.perfilActivo,
        repositorio.idiomaPractica
    ) { perfilId, idioma -> perfilId to idioma }
        .flatMapLatest { (perfilId, idioma) ->
            repositorio.estrellas(perfilId, idioma, idsUnidades).map { estrellas ->
                ProgresoUiState(
                    perfilActivoId = perfilId,
                    idioma = idioma,
                    estrellas = estrellas,
                    cargando = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProgresoUiState(cargando = true)
        )

    fun seleccionarPerfil(perfilId: String) {
        viewModelScope.launch { repositorio.guardarPerfilActivo(perfilId) }
    }

    // alTerminar se llama cuando el idioma ya quedo guardado
    fun seleccionarIdioma(idioma: IdiomaPractica, alTerminar: () -> Unit = {}) {
        viewModelScope.launch {
            repositorio.guardarIdiomaPractica(idioma)
            alTerminar()
        }
    }

    fun registrarResultado(unidadId: String, idioma: IdiomaPractica, aciertos: Int, total: Int) {
        val perfilId = estado.value.perfilActivoId
        viewModelScope.launch {
            repositorio.guardarEstrellas(perfilId, idioma, unidadId, calcularEstrellas(aciertos, total))
        }
    }

    fun reiniciar() {
        val perfilId = estado.value.perfilActivoId
        viewModelScope.launch { repositorio.reiniciar(perfilId) }
    }
}
