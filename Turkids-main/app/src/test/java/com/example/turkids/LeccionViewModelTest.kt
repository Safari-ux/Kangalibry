package com.example.turkids

import com.example.turkids.model.BancoDeContenido
import com.example.turkids.model.IdiomaPractica
import com.example.turkids.model.LeccionGuardada
import com.example.turkids.repository.ProgresoRepository
import com.example.turkids.viewmodel.LeccionViewModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class LeccionViewModelTest : StringSpec({

    val perfil = "perfil-prueba-1"
    val unidadSaludos = BancoDeContenido.unidades.first { it.id == "saludos" }

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    // Repositorio simulado: sin lección guardada, y las escrituras se aceptan sin hacer nada
    fun repositorioVacio(guardada: LeccionGuardada? = null): ProgresoRepository {
        val repositorio = mockk<ProgresoRepository>()
        coEvery { repositorio.leccionGuardada(any(), any()) } returns guardada
        coEvery { repositorio.guardarLeccion(any(), any()) } just runs
        coEvery { repositorio.borrarLeccion(any(), any()) } just runs
        return repositorio
    }

    fun LeccionViewModel.opcionIncorrecta(): String {
        val pregunta = estado.value.preguntaActual!!
        return pregunta.opciones.first { it != pregunta.respuestaCorrecta && it !in estado.value.descartadas }
    }

    fun LeccionViewModel.correcta(): String = estado.value.preguntaActual!!.respuestaCorrecta

    "acierto en el primer intento suma un acierto y deja continuar" {
        runTest {
            val viewModel = LeccionViewModel(repositorioVacio())
            viewModel.cargarUnidad("saludos", IdiomaPractica.TURCO, perfil)

            viewModel.seleccionarOpcion(viewModel.correcta())

            val estado = viewModel.estado.value
            estado.respondido shouldBe true
            estado.esCorrecta shouldBe true
            estado.aciertos shouldBe 1
            estado.intentosFallidos shouldBe 0
        }
    }

    "el primer error activa la segunda oportunidad sin cerrar la pregunta" {
        runTest {
            val viewModel = LeccionViewModel(repositorioVacio())
            viewModel.cargarUnidad("saludos", IdiomaPractica.TURCO, perfil)
            val incorrecta = viewModel.opcionIncorrecta()

            viewModel.seleccionarOpcion(incorrecta)

            val estado = viewModel.estado.value
            estado.respondido shouldBe false
            estado.intentosFallidos shouldBe 1
            estado.descartadas shouldContain incorrecta
            estado.aciertos shouldBe 0
        }
    }

    "acertar en la segunda oportunidad cuenta como acierto" {
        runTest {
            val viewModel = LeccionViewModel(repositorioVacio())
            viewModel.cargarUnidad("saludos", IdiomaPractica.TURCO, perfil)

            viewModel.seleccionarOpcion(viewModel.opcionIncorrecta())
            viewModel.seleccionarOpcion(viewModel.correcta())

            val estado = viewModel.estado.value
            estado.respondido shouldBe true
            estado.esCorrecta shouldBe true
            estado.aciertos shouldBe 1
        }
    }

    "una opcion descartada no gasta la segunda oportunidad" {
        runTest {
            val viewModel = LeccionViewModel(repositorioVacio())
            viewModel.cargarUnidad("saludos", IdiomaPractica.TURCO, perfil)
            val incorrecta = viewModel.opcionIncorrecta()

            viewModel.seleccionarOpcion(incorrecta)
            viewModel.seleccionarOpcion(incorrecta)

            val estado = viewModel.estado.value
            estado.respondido shouldBe false
            estado.intentosFallidos shouldBe 1
        }
    }

    "dos errores muestran la respuesta, no suman y el flujo sigue con Continuar" {
        runTest {
            val repositorio = repositorioVacio()
            val viewModel = LeccionViewModel(repositorio)
            viewModel.cargarUnidad("saludos", IdiomaPractica.TURCO, perfil)

            viewModel.seleccionarOpcion(viewModel.opcionIncorrecta())
            viewModel.seleccionarOpcion(viewModel.opcionIncorrecta())

            viewModel.estado.value.respondido shouldBe true
            viewModel.estado.value.esCorrecta shouldBe false
            viewModel.estado.value.aciertos shouldBe 0

            viewModel.siguientePregunta()

            val estado = viewModel.estado.value
            estado.indiceActual shouldBe 1
            estado.respondido shouldBe false
            estado.intentosFallidos shouldBe 0
            estado.descartadas shouldBe emptySet<String>()
            estado.aciertos shouldBe 0
        }
    }

    "al continuar se guarda el punto de retoma con los aciertos de ese momento" {
        runTest {
            val repositorio = repositorioVacio()
            val viewModel = LeccionViewModel(repositorio)
            viewModel.cargarUnidad("saludos", IdiomaPractica.TURCO, perfil)

            viewModel.seleccionarOpcion(viewModel.correcta())
            viewModel.siguientePregunta()

            coVerify(exactly = 1) {
                repositorio.guardarLeccion(
                    perfil,
                    match {
                        it.unidadId == "saludos" &&
                            it.idioma == IdiomaPractica.TURCO &&
                            it.indice == 1 &&
                            it.aciertos == 1
                    }
                )
            }
        }
    }

    "retoma una leccion guardada en la misma pregunta, con la misma semilla y los mismos aciertos" {
        runTest {
            val guardada = LeccionGuardada("saludos", IdiomaPractica.TURCO, semilla = 42L, indice = 2, aciertos = 1)
            val viewModel = LeccionViewModel(repositorioVacio(guardada))

            viewModel.cargarUnidad("saludos", IdiomaPractica.TURCO, perfil)

            val estado = viewModel.estado.value
            estado.indiceActual shouldBe 2
            estado.aciertos shouldBe 1
            estado.preguntas shouldBe BancoDeContenido.generarPreguntas(unidadSaludos, IdiomaPractica.TURCO, 42L)
        }
    }

    "una leccion guardada de otra unidad no se retoma" {
        runTest {
            val guardada = LeccionGuardada("animales", IdiomaPractica.TURCO, semilla = 7L, indice = 3, aciertos = 2)
            val viewModel = LeccionViewModel(repositorioVacio(guardada))

            viewModel.cargarUnidad("saludos", IdiomaPractica.TURCO, perfil)

            viewModel.estado.value.indiceActual shouldBe 0
            viewModel.estado.value.aciertos shouldBe 0
        }
    }

    "un indice guardado fuera de rango se acota a la ultima pregunta" {
        runTest {
            val guardada = LeccionGuardada("saludos", IdiomaPractica.TURCO, semilla = 1L, indice = 99, aciertos = 0)
            val viewModel = LeccionViewModel(repositorioVacio(guardada))

            viewModel.cargarUnidad("saludos", IdiomaPractica.TURCO, perfil)

            viewModel.estado.value.indiceActual shouldBe unidadSaludos.palabras.lastIndex
        }
    }

    "cargar de nuevo la misma unidad no vuelve a leer ni reinicia la leccion" {
        runTest {
            val repositorio = repositorioVacio()
            val viewModel = LeccionViewModel(repositorio)
            viewModel.cargarUnidad("saludos", IdiomaPractica.TURCO, perfil)
            viewModel.seleccionarOpcion(viewModel.correcta())

            viewModel.cargarUnidad("saludos", IdiomaPractica.TURCO, perfil)

            viewModel.estado.value.aciertos shouldBe 1
            coVerify(exactly = 1) { repositorio.leccionGuardada(any(), any()) }
        }
    }

    "una unidad inexistente no cambia el estado" {
        runTest {
            val repositorio = repositorioVacio()
            val viewModel = LeccionViewModel(repositorio)

            viewModel.cargarUnidad("no-existe", IdiomaPractica.TURCO, perfil)

            viewModel.estado.value.preguntas.size shouldBe 0
            coVerify(exactly = 0) { repositorio.leccionGuardada(any(), any()) }
        }
    }

    "antes de cargar la leccion responder o continuar no hace nada" {
        runTest {
            val repositorio = repositorioVacio()
            val viewModel = LeccionViewModel(repositorio)

            viewModel.seleccionarOpcion("Hola")
            viewModel.siguientePregunta()

            val estado = viewModel.estado.value
            estado.intentosFallidos shouldBe 0
            estado.descartadas shouldNotContain "Hola"
            estado.finalizado shouldBe false
            coVerify(exactly = 0) { repositorio.borrarLeccion(any(), any()) }
        }
    }

    "continuar sin haber respondido no salta la pregunta" {
        runTest {
            val viewModel = LeccionViewModel(repositorioVacio())
            viewModel.cargarUnidad("saludos", IdiomaPractica.TURCO, perfil)

            viewModel.siguientePregunta()

            viewModel.estado.value.indiceActual shouldBe 0
        }
    }

    "al terminar la leccion se borra el punto de retoma y se marca como finalizada" {
        runTest {
            val repositorio = repositorioVacio()
            val viewModel = LeccionViewModel(repositorio)
            viewModel.cargarUnidad("saludos", IdiomaPractica.TURCO, perfil)

            repeat(unidadSaludos.palabras.size) {
                viewModel.seleccionarOpcion(viewModel.correcta())
                viewModel.siguientePregunta()
            }

            val estado = viewModel.estado.value
            estado.finalizado shouldBe true
            estado.aciertos shouldBe unidadSaludos.palabras.size
            coVerify(exactly = 1) { repositorio.borrarLeccion(perfil, IdiomaPractica.TURCO) }
        }
    }

    "un doble toque en Continuar al final no finaliza dos veces" {
        runTest {
            val repositorio = repositorioVacio()
            val viewModel = LeccionViewModel(repositorio)
            viewModel.cargarUnidad("saludos", IdiomaPractica.TURCO, perfil)

            repeat(unidadSaludos.palabras.size - 1) {
                viewModel.seleccionarOpcion(viewModel.correcta())
                viewModel.siguientePregunta()
            }
            viewModel.seleccionarOpcion(viewModel.correcta())
            viewModel.siguientePregunta()
            viewModel.siguientePregunta()

            coVerify(exactly = 1) { repositorio.borrarLeccion(any(), any()) }
        }
    }
})
