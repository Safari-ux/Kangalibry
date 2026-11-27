package com.example.turkids.model

import java.text.Normalizer
import java.util.Locale

// los reconocedores de voz escriben los numeros con digitos
private val EQUIVALENCIAS_NUMEROS = mapOf(
    "1" to listOf("uno", "bir"),
    "2" to listOf("dos", "iki"),
    "3" to listOf("tres", "uc"),
    "4" to listOf("cuatro", "dort")
)

// normaliza: minusculas, sin tildes ni signos, para comparar lo hablado con lo esperado
fun normalizarRespuesta(texto: String): String =
    Normalizer.normalize(texto.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace('ı', 'i')
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

// correcta si algun candidato del reconocedor coincide ("el gato" vale para "gato")
fun coincideRespuesta(esperada: String, candidatos: List<String>): Boolean {
    val objetivo = normalizarRespuesta(esperada)
    if (objetivo.isEmpty()) return false
    return candidatos.map { normalizarRespuesta(it) }.any { candidato ->
        " $candidato ".contains(" $objetivo ") ||
            EQUIVALENCIAS_NUMEROS[candidato]?.contains(objetivo) == true
    }
}
