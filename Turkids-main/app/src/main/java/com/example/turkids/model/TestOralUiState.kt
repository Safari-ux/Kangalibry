package com.example.turkids.model

enum class ResultadoOral { CORRECTO, INCORRECTO }

data class TestOralUiState(
    val idioma: IdiomaPractica = IdiomaPractica.TURCO,
    val palabras: List<Palabra> = emptyList(),
    val indiceActual: Int = 0,
    val aciertos: Int = 0,
    val escuchando: Boolean = false,
    val resultado: ResultadoOral? = null,
    val escuchado: String? = null,
    val mensaje: String? = null,
    val finalizado: Boolean = false
) {
    private val preguntaEnTurco: Boolean get() = idioma == IdiomaPractica.TURCO

    val palabraActual: Palabra? get() = palabras.getOrNull(indiceActual)
    val total: Int get() = palabras.size

    val textoMostrado: String
        get() = palabraActual?.let { if (preguntaEnTurco) it.tr else it.es }.orEmpty()

    val respuestaEsperada: String
        get() = palabraActual?.let { if (preguntaEnTurco) it.es else it.tr }.orEmpty()

    val codigoIdiomaRespuesta: String get() = if (preguntaEnTurco) "es-ES" else "tr-TR"
    val nombreIdiomaRespuesta: String get() = if (preguntaEnTurco) "español" else "turco"
}
