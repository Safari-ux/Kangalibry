package com.example.turkids.model

data class Pais(
    val nombre: String,
    val bandera: String
)

data class LugarDetectado(
    val codigoPais: String?,
    val nombrePais: String?,
    val coordenadas: String
)

// bandera en emoji desde el codigo iso de 2 letras; vacio si no es valido
fun banderaDesdeCodigo(codigo: String): String {
    val letras = codigo.trim().uppercase()
    if (letras.length != 2 || letras.any { it !in 'A'..'Z' }) return ""
    return letras.map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
}
