package com.example.turkids.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.turkids.model.IdiomaPractica
import com.example.turkids.model.LeccionGuardada
import com.example.turkids.model.PerfilesPrueba
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

// avance local con datastore: lecturas con flow, escrituras suspend
class ProgresoRepository(private val dataStore: DataStore<Preferences>) {

    private val datos: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val perfilActivo: Flow<String> = datos
        .map { it[CLAVE_PERFIL] ?: PerfilesPrueba.lista.first().id }
        .distinctUntilChanged()

    val idiomaPractica: Flow<IdiomaPractica> = datos
        .map { idiomaDesde(it[CLAVE_IDIOMA]) ?: IdiomaPractica.TURCO }
        .distinctUntilChanged()

    fun estrellas(
        perfilId: String,
        idioma: IdiomaPractica,
        unidadIds: List<String>
    ): Flow<Map<String, Int>> =
        datos
            .map { prefs -> unidadIds.associateWith { prefs[claveEstrellas(perfilId, idioma, it)] ?: 0 } }
            .distinctUntilChanged()

    suspend fun guardarPerfilActivo(perfilId: String) {
        escribir { it[CLAVE_PERFIL] = perfilId }
    }

    suspend fun guardarIdiomaPractica(idioma: IdiomaPractica) {
        escribir { it[CLAVE_IDIOMA] = idioma.name }
    }

    // se conserva el mejor resultado por unidad e idioma
    suspend fun guardarEstrellas(
        perfilId: String,
        idioma: IdiomaPractica,
        unidadId: String,
        estrellas: Int
    ) {
        val clave = claveEstrellas(perfilId, idioma, unidadId)
        escribir { prefs -> prefs[clave] = maxOf(prefs[clave] ?: 0, estrellas) }
    }

    suspend fun leccionGuardada(perfilId: String, idioma: IdiomaPractica): LeccionGuardada? {
        val prefs = datos.first()
        val prefijo = prefijoLeccion(perfilId, idioma)
        val unidadId = prefs[stringPreferencesKey("${prefijo}unidad")] ?: return null
        return LeccionGuardada(
            unidadId = unidadId,
            idioma = idioma,
            semilla = prefs[longPreferencesKey("${prefijo}semilla")] ?: 0L,
            indice = prefs[intPreferencesKey("${prefijo}indice")] ?: 0,
            aciertos = prefs[intPreferencesKey("${prefijo}aciertos")] ?: 0
        )
    }

    suspend fun guardarLeccion(perfilId: String, leccion: LeccionGuardada) {
        val prefijo = prefijoLeccion(perfilId, leccion.idioma)
        escribir { prefs ->
            prefs[stringPreferencesKey("${prefijo}unidad")] = leccion.unidadId
            prefs[longPreferencesKey("${prefijo}semilla")] = leccion.semilla
            prefs[intPreferencesKey("${prefijo}indice")] = leccion.indice
            prefs[intPreferencesKey("${prefijo}aciertos")] = leccion.aciertos
        }
    }

    suspend fun borrarLeccion(perfilId: String, idioma: IdiomaPractica) {
        val prefijo = prefijoLeccion(perfilId, idioma)
        escribir { prefs -> quitarConPrefijos(prefs, listOf(prefijo)) }
    }

    suspend fun reiniciar(perfilId: String) {
        val prefijos = listOf(prefijoEstrellas(perfilId), prefijoLecciones(perfilId))
        escribir { prefs -> quitarConPrefijos(prefs, prefijos) }
    }

    // la escritura termina aunque se cierre la pantalla que la pidio
    private suspend fun escribir(cambio: (MutablePreferences) -> Unit) {
        withContext(NonCancellable) {
            dataStore.edit { prefs -> cambio(prefs) }
        }
    }

    private fun quitarConPrefijos(prefs: MutablePreferences, prefijos: List<String>) {
        prefs.asMap().keys
            .filter { clave -> prefijos.any { clave.name.startsWith(it) } }
            .forEach { clave ->
                @Suppress("UNCHECKED_CAST")
                prefs.remove(clave as Preferences.Key<Any>)
            }
    }

    private fun idiomaDesde(nombre: String?): IdiomaPractica? =
        IdiomaPractica.entries.firstOrNull { it.name == nombre }

    private fun claveEstrellas(perfilId: String, idioma: IdiomaPractica, unidadId: String) =
        intPreferencesKey(nombreClaveEstrellas(perfilId, idioma, unidadId))

    private companion object {
        val CLAVE_PERFIL = stringPreferencesKey("perfil_activo")
        val CLAVE_IDIOMA = stringPreferencesKey("idioma_practica")
    }
}
