package com.example.turkids.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turkids.model.Pais
import com.example.turkids.model.ProblemaUbicacion
import com.example.turkids.model.UbicacionEstado
import com.example.turkids.model.banderaDesdeCodigo
import com.example.turkids.repository.PaisRepository
import com.example.turkids.repository.UbicacionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UbicacionViewModel(
    private val repositorio: UbicacionRepository,
    private val paises: PaisRepository
) : ViewModel() {

    private val _estado = MutableStateFlow<UbicacionEstado>(UbicacionEstado.Verificando)
    val estado: StateFlow<UbicacionEstado> = _estado

    private val _problema = MutableStateFlow(ProblemaUbicacion.NINGUNO)
    val problema: StateFlow<ProblemaUbicacion> = _problema

    var solicitudIniciada = false
        private set

    private var permisoRespondido = false
    private var puedeVolverAPedir = false
    private var trabajo: Job? = null

    fun iniciarSolicitud() {
        solicitudIniciada = true
    }

    // puedeVolverAPedir: el sistema aun permite mostrar el dialogo de permiso
    fun registrarResultado(puedeVolverAPedir: Boolean = false) {
        solicitudIniciada = true
        permisoRespondido = true
        this.puedeVolverAPedir = puedeVolverAPedir
        actualizar()
    }

    fun permisoConcedido(): Boolean = repositorio.permisoConcedido()

    fun actualizar() {
        trabajo?.cancel()
        trabajo = viewModelScope.launch { _estado.value = consultarEstado() }
    }

    private suspend fun consultarEstado(): UbicacionEstado {
        if (!repositorio.permisoConcedido()) {
            _problema.value = when {
                !permisoRespondido -> ProblemaUbicacion.NINGUNO
                puedeVolverAPedir -> ProblemaUbicacion.PERMISO_DENEGADO
                else -> ProblemaUbicacion.PERMISO_BLOQUEADO
            }
            return UbicacionEstado.NoEnUso
        }
        if (!repositorio.ubicacionActivada()) {
            _problema.value = ProblemaUbicacion.UBICACION_DESACTIVADA
            return UbicacionEstado.NoEnUso
        }

        _problema.value = ProblemaUbicacion.NINGUNO
        if (_estado.value == UbicacionEstado.NoEnUso) _estado.value = UbicacionEstado.Verificando

        val lugar = repositorio.obtenerLugarActual() ?: return UbicacionEstado.NoEnUso
        val pais = consultarPais(lugar.codigoPais)

        val texto = when {
            pais != null -> "${pais.bandera} ${pais.nombre}".trim()
            !lugar.nombrePais.isNullOrBlank() ->
                "${banderaDesdeCodigo(lugar.codigoPais.orEmpty())} ${lugar.nombrePais}".trim()
            else -> lugar.coordenadas
        }
        return UbicacionEstado.EnUso(texto)
    }

    // si falla la api publica se usa el pais del geocodificador
    private suspend fun consultarPais(codigo: String?): Pais? {
        if (codigo.isNullOrBlank()) return null
        return try {
            paises.getPais(codigo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}
