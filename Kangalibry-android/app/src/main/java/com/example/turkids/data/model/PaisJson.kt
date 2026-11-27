package com.example.turkids.data.model

import com.example.turkids.model.Pais
import com.google.gson.JsonElement
import com.google.gson.JsonObject

// restcountries responde una lista con un pais; tambien se acepta un objeto suelto
fun paisDesdeJson(elemento: JsonElement): Pais? {
    val objeto = when {
        elemento.isJsonArray -> elemento.asJsonArray.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
        elemento.isJsonObject -> elemento.asJsonObject
        else -> null
    } ?: return null

    val nombre = objeto.objetoHijo("translations").objetoHijo("spa").texto("common")
        ?: objeto.objetoHijo("name").texto("common")
        ?: return null

    return Pais(nombre = nombre, bandera = objeto.texto("flag").orEmpty())
}

private fun JsonObject?.objetoHijo(clave: String): JsonObject? {
    val hijo = this?.get(clave)
    return if (hijo != null && hijo.isJsonObject) hijo.asJsonObject else null
}

private fun JsonObject?.texto(clave: String): String? {
    val valor = this?.get(clave)
    return if (valor != null && valor.isJsonPrimitive) valor.asString else null
}
