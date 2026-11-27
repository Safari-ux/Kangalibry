package com.example.turkids.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val NOMBRE_ARCHIVO = "progreso_turkids"

// una sola instancia por proceso; migra una vez los datos del sharedPreferences antiguo
val Context.progresoDataStore: DataStore<Preferences> by preferencesDataStore(
    name = NOMBRE_ARCHIVO,
    produceMigrations = { contexto ->
        listOf(SharedPreferencesMigration(contexto, NOMBRE_ARCHIVO))
    }
)
