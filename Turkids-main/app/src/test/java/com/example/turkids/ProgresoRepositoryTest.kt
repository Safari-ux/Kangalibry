package com.example.turkids

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.example.turkids.model.IdiomaPractica
import com.example.turkids.model.LeccionGuardada
import com.example.turkids.repository.ProgresoRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// DataStore<Preferences> en memoria: evita el bug de File.renameTo() de DataStore en JVM sobre Windows.
// Respeta el contrato (lecturas por Flow y escrituras atómicas), por eso ProgresoRepository se prueba igual.
private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val mutex = Mutex()
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        mutex.withLock {
            val updated = transform(state.value)
            state.value = updated
            updated
        }
}

class ProgresoRepositoryTest : StringSpec({

    val perfil = "perfil-prueba-1"
    val unidades = listOf("saludos", "animales", "numeros")

    class Entorno {
        val dataStore: DataStore<Preferences> = InMemoryPreferencesDataStore()
        val repositorio = ProgresoRepository(dataStore)

        suspend fun estrellas(perfilId: String, idioma: IdiomaPractica): Map<String, Int> =
            repositorio.estrellas(perfilId, idioma, unidades).first()
    }

    // Cada prueba crea su propio DataStore en memoria: no hay archivos, locks ni limpieza
    suspend fun conEntorno(prueba: suspend Entorno.() -> Unit) {
        Entorno().prueba()
    }

    "sin datos guardados todas las unidades tienen 0 estrellas" {
        conEntorno {
            estrellas(perfil, IdiomaPractica.TURCO) shouldBe mapOf("saludos" to 0, "animales" to 0, "numeros" to 0)
        }
    }

    "las estrellas guardadas se leen de vuelta" {
        conEntorno {
            repositorio.guardarEstrellas(perfil, IdiomaPractica.TURCO, "saludos", 3)
            repositorio.guardarEstrellas(perfil, IdiomaPractica.TURCO, "animales", 2)

            estrellas(perfil, IdiomaPractica.TURCO) shouldBe mapOf("saludos" to 3, "animales" to 2, "numeros" to 0)
        }
    }

    "las estrellas de cada idioma son independientes" {
        conEntorno {
            repositorio.guardarEstrellas(perfil, IdiomaPractica.TURCO, "saludos", 3)
            repositorio.guardarEstrellas(perfil, IdiomaPractica.ESPANOL, "saludos", 1)

            estrellas(perfil, IdiomaPractica.TURCO)["saludos"] shouldBe 3
            estrellas(perfil, IdiomaPractica.ESPANOL)["saludos"] shouldBe 1
            estrellas(perfil, IdiomaPractica.ESPANOL)["animales"] shouldBe 0
        }
    }

    "las estrellas de cada perfil son independientes" {
        conEntorno {
            repositorio.guardarEstrellas("perfil-prueba-1", IdiomaPractica.TURCO, "saludos", 3)

            estrellas("perfil-prueba-2", IdiomaPractica.TURCO)["saludos"] shouldBe 0
        }
    }

    "solo se conserva el mejor resultado de cada unidad" {
        conEntorno {
            repositorio.guardarEstrellas(perfil, IdiomaPractica.TURCO, "saludos", 3)
            repositorio.guardarEstrellas(perfil, IdiomaPractica.TURCO, "saludos", 1)
            estrellas(perfil, IdiomaPractica.TURCO)["saludos"] shouldBe 3

            repositorio.guardarEstrellas(perfil, IdiomaPractica.TURCO, "animales", 1)
            repositorio.guardarEstrellas(perfil, IdiomaPractica.TURCO, "animales", 2)
            estrellas(perfil, IdiomaPractica.TURCO)["animales"] shouldBe 2
        }
    }

    "otro repositorio sobre el mismo DataStore lee lo que se guardo" {
        conEntorno {
            repositorio.guardarEstrellas(perfil, IdiomaPractica.TURCO, "numeros", 2)

            val otroRepositorio = ProgresoRepository(dataStore)

            otroRepositorio.estrellas(perfil, IdiomaPractica.TURCO, unidades).first()["numeros"] shouldBe 2
        }
    }

    "el perfil y el idioma activos se guardan y se leen" {
        conEntorno {
            repositorio.guardarPerfilActivo("perfil-prueba-2")
            repositorio.guardarIdiomaPractica(IdiomaPractica.ESPANOL)

            repositorio.perfilActivo.first() shouldBe "perfil-prueba-2"
            repositorio.idiomaPractica.first() shouldBe IdiomaPractica.ESPANOL
        }
    }

    "reiniciar borra estrellas y leccion en curso de ambos idiomas" {
        conEntorno {
            repositorio.guardarEstrellas(perfil, IdiomaPractica.TURCO, "saludos", 3)
            repositorio.guardarEstrellas(perfil, IdiomaPractica.ESPANOL, "saludos", 2)
            repositorio.guardarLeccion(perfil, LeccionGuardada("animales", IdiomaPractica.TURCO, 5L, 2, 1))

            repositorio.reiniciar(perfil)

            estrellas(perfil, IdiomaPractica.TURCO)["saludos"] shouldBe 0
            estrellas(perfil, IdiomaPractica.ESPANOL)["saludos"] shouldBe 0
            repositorio.leccionGuardada(perfil, IdiomaPractica.TURCO).shouldBeNull()
        }
    }

    "la leccion en curso se guarda por idioma y se borra al terminar" {
        conEntorno {
            val leccion = LeccionGuardada("animales", IdiomaPractica.TURCO, semilla = 99L, indice = 2, aciertos = 1)
            repositorio.guardarLeccion(perfil, leccion)

            repositorio.leccionGuardada(perfil, IdiomaPractica.TURCO) shouldBe leccion
            repositorio.leccionGuardada(perfil, IdiomaPractica.ESPANOL).shouldBeNull()

            repositorio.borrarLeccion(perfil, IdiomaPractica.TURCO)

            repositorio.leccionGuardada(perfil, IdiomaPractica.TURCO).shouldBeNull()
        }
    }
})
