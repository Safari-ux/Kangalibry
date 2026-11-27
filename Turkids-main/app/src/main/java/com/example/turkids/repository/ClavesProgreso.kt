package com.example.turkids.repository

import com.example.turkids.model.IdiomaPractica

// las claves llevan el idioma: estrellas y leccion en curso separadas por idioma
internal fun nombreClaveEstrellas(perfilId: String, idioma: IdiomaPractica, unidadId: String): String =
    "estrellas_${perfilId}_${idioma.name}_$unidadId"

internal fun prefijoEstrellas(perfilId: String): String = "estrellas_${perfilId}_"

internal fun prefijoLeccion(perfilId: String, idioma: IdiomaPractica): String =
    "leccion_${perfilId}_${idioma.name}_"

internal fun prefijoLecciones(perfilId: String): String = "leccion_${perfilId}_"
