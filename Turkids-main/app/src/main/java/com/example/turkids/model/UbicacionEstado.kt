package com.example.turkids.model

sealed interface UbicacionEstado {
    data object Verificando : UbicacionEstado
    data object NoEnUso : UbicacionEstado
    data class EnUso(val lugar: String) : UbicacionEstado
}

fun UbicacionEstado.texto(): String = when (this) {
    is UbicacionEstado.EnUso -> "Ubicación actual: $lugar"
    UbicacionEstado.Verificando -> "Obteniendo ubicación..."
    UbicacionEstado.NoEnUso -> "Tu ubicación no está en uso"
}

enum class ProblemaUbicacion {
    NINGUNO,
    // permiso rechazado pero se puede volver a pedir
    PERMISO_DENEGADO,
    // permiso bloqueado: solo se cambia en ajustes
    PERMISO_BLOQUEADO,
    UBICACION_DESACTIVADA
}
