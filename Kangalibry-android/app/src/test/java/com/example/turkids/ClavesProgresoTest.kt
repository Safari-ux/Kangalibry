package com.example.turkids

import com.example.turkids.model.IdiomaPractica
import com.example.turkids.repository.nombreClaveEstrellas
import com.example.turkids.repository.prefijoEstrellas
import com.example.turkids.repository.prefijoLeccion
import com.example.turkids.repository.prefijoLecciones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClavesProgresoTest {

    private val perfil = "perfil-prueba-1"

    @Test
    fun laMismaUnidadTieneClavesDistintasPorIdioma() {
        val turco = nombreClaveEstrellas(perfil, IdiomaPractica.TURCO, "saludos")
        val espanol = nombreClaveEstrellas(perfil, IdiomaPractica.ESPANOL, "saludos")
        assertNotEquals(turco, espanol)
    }

    @Test
    fun mismoPerfilIdiomaYUnidadDanLaMismaClave() {
        assertEquals(
            nombreClaveEstrellas(perfil, IdiomaPractica.TURCO, "animales"),
            nombreClaveEstrellas(perfil, IdiomaPractica.TURCO, "animales")
        )
    }

    @Test
    fun perfilesDistintosNoComparten() {
        assertNotEquals(
            nombreClaveEstrellas("perfil-prueba-1", IdiomaPractica.TURCO, "saludos"),
            nombreClaveEstrellas("perfil-prueba-2", IdiomaPractica.TURCO, "saludos")
        )
    }

    @Test
    fun laClaveAntiguaSinIdiomaNoCoincideConNingunaClaveNueva() {
        val antigua = "estrellas_${perfil}_saludos"
        IdiomaPractica.entries.forEach { idioma ->
            assertNotEquals(antigua, nombreClaveEstrellas(perfil, idioma, "saludos"))
        }
    }

    @Test
    fun elReinicioDelPerfilAlcanzaLasEstrellasDeAmbosIdiomas() {
        IdiomaPractica.entries.forEach { idioma ->
            assertTrue(nombreClaveEstrellas(perfil, idioma, "numeros").startsWith(prefijoEstrellas(perfil)))
        }
    }

    @Test
    fun elPrefijoDeUnPerfilNoAlcanzaLasClavesDeOtro() {
        val otro = nombreClaveEstrellas("perfil-prueba-10", IdiomaPractica.TURCO, "saludos")
        assertFalse(otro.startsWith(prefijoEstrellas("perfil-prueba-1")))
    }

    @Test
    fun cadaIdiomaTieneSuPropiaLeccionEnCurso() {
        val turco = prefijoLeccion(perfil, IdiomaPractica.TURCO)
        val espanol = prefijoLeccion(perfil, IdiomaPractica.ESPANOL)
        assertNotEquals(turco, espanol)
        assertTrue(turco.startsWith(prefijoLecciones(perfil)))
        assertTrue(espanol.startsWith(prefijoLecciones(perfil)))
        assertFalse(turco.startsWith(espanol) || espanol.startsWith(turco))
    }
}
