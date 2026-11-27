package com.example.turkids

import com.example.turkids.data.model.Post
import com.example.turkids.repository.PostRepository
import com.example.turkids.viewmodel.PostViewModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModelTest : StringSpec({

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    "fetchPosts deja los posts en el estado cuando el repositorio responde" {
        runTest {
            val repositorio = mockk<PostRepository>()
            coEvery { repositorio.getPosts() } returns listOf(
                Post(userId = 1, id = 1, title = "Titulo 1", body = "Cuerpo 1"),
                Post(userId = 1, id = 2, title = "Titulo 2", body = "Cuerpo 2")
            )

            val viewModel = PostViewModel(repositorio)
            viewModel.fetchPosts()
            advanceUntilIdle()

            val estado = viewModel.estado.value
            estado.posts shouldHaveSize 2
            estado.posts.first().title shouldBe "Titulo 1"
            estado.cargando shouldBe false
            estado.error.shouldBeNull()
            coVerify(atLeast = 1) { repositorio.getPosts() }
        }
    }

    "si el repositorio falla el estado muestra un error y no hay posts" {
        runTest {
            val repositorio = mockk<PostRepository>()
            coEvery { repositorio.getPosts() } throws IOException("sin red")

            val viewModel = PostViewModel(repositorio)
            advanceUntilIdle()

            val estado = viewModel.estado.value
            estado.posts.shouldBeEmpty()
            estado.cargando shouldBe false
            estado.error.shouldNotBeNull()
        }
    }

    "fetchPosts permite reintentar despues de un error" {
        runTest {
            val repositorio = mockk<PostRepository>()
            coEvery { repositorio.getPosts() } throws IOException("sin red") andThen listOf(
                Post(userId = 1, id = 1, title = "Titulo 1", body = "Cuerpo 1")
            )

            val viewModel = PostViewModel(repositorio)
            viewModel.estado.value.error.shouldNotBeNull()

            viewModel.fetchPosts()
            advanceUntilIdle()

            viewModel.estado.value.posts shouldHaveSize 1
            viewModel.estado.value.error.shouldBeNull()
        }
    }

    val inicial = listOf(
        Post(userId = 1, id = 1, title = "Titulo 1", body = "Cuerpo 1"),
        Post(userId = 1, id = 2, title = "Titulo 2", body = "Cuerpo 2")
    )

    fun repositorioConLista(): PostRepository {
        val repositorio = mockk<PostRepository>()
        coEvery { repositorio.getPosts() } returns inicial
        return repositorio
    }

    "crearPost (POST) agrega el post al inicio y avisa que se creo" {
        runTest {
            val repositorio = repositorioConLista()
            coEvery { repositorio.createPost(any()) } returns Post(userId = 1, id = 101, title = "Nuevo", body = "Cuerpo")
            val viewModel = PostViewModel(repositorio)

            viewModel.crearPost("Nuevo", "Cuerpo")
            advanceUntilIdle()

            val estado = viewModel.estado.value
            estado.posts shouldHaveSize 3
            estado.posts.first().id shouldBe 101
            estado.posts.first().title shouldBe "Nuevo"
            estado.operacionEnCurso shouldBe false
            estado.mensaje shouldBe PostViewModel.MENSAJE_CREADO
            coVerify(exactly = 1) { repositorio.createPost(match { it.title == "Nuevo" && it.body == "Cuerpo" }) }
        }
    }

    "crear dos posts conserva los ids que asigna el servidor" {
        runTest {
            val repositorio = repositorioConLista()
            coEvery { repositorio.createPost(any()) } returnsMany listOf(
                Post(userId = 1, id = 5, title = "Nuevo", body = "Cuerpo"),
                Post(userId = 1, id = 6, title = "Nuevo", body = "Cuerpo")
            )
            val viewModel = PostViewModel(repositorio)

            viewModel.crearPost()
            viewModel.crearPost()
            advanceUntilIdle()

            viewModel.estado.value.posts.map { it.id } shouldBe listOf(6, 5, 1, 2)
        }
    }

    "si crearPost falla la lista no cambia y se informa el error" {
        runTest {
            val repositorio = repositorioConLista()
            coEvery { repositorio.createPost(any()) } throws IOException("sin red")
            val viewModel = PostViewModel(repositorio)

            viewModel.crearPost()
            advanceUntilIdle()

            val estado = viewModel.estado.value
            estado.posts shouldBe inicial
            estado.mensaje shouldBe PostViewModel.MENSAJE_SIN_CONEXION
            estado.error.shouldBeNull()
            estado.operacionEnCurso shouldBe false
        }
    }

    "actualizarPost (PUT) envia el post con el titulo editado y lo reemplaza en la lista" {
        runTest {
            val repositorio = repositorioConLista()
            coEvery { repositorio.updatePost(any(), any()) } answers { secondArg() }
            val viewModel = PostViewModel(repositorio)
            val editado = inicial.first().copy(
                title = "Titulo 1 editado",
                body = "Cuerpo 1 editado"
            )

            viewModel.actualizarPost(editado)
            advanceUntilIdle()

            val estado = viewModel.estado.value
            estado.posts shouldHaveSize 2
            estado.posts.first { it.id == 1 }.title shouldBe "Titulo 1 editado"
            estado.posts.first { it.id == 1 }.body shouldBe "Cuerpo 1 editado"
            estado.posts.first { it.id == 2 }.title shouldBe "Titulo 2"
            estado.mensaje shouldBe PostViewModel.MENSAJE_ACTUALIZADO
            coVerify(exactly = 1) {
                repositorio.updatePost(1, editado)
            }
        }
    }

    "eliminarPost (DELETE) quita el post de la lista" {
        runTest {
            val repositorio = repositorioConLista()
            coEvery { repositorio.deletePost(1) } just runs
            val viewModel = PostViewModel(repositorio)

            viewModel.eliminarPost(1)
            advanceUntilIdle()

            val estado = viewModel.estado.value
            estado.posts shouldHaveSize 1
            estado.posts.first().id shouldBe 2
            estado.mensaje shouldBe PostViewModel.MENSAJE_ELIMINADO
            coVerify(exactly = 1) { repositorio.deletePost(1) }
        }
    }

    "si eliminarPost falla el post sigue en la lista" {
        runTest {
            val repositorio = repositorioConLista()
            coEvery { repositorio.deletePost(any()) } throws IOException("sin red")
            val viewModel = PostViewModel(repositorio)

            viewModel.eliminarPost(1)
            advanceUntilIdle()

            viewModel.estado.value.posts shouldBe inicial
            viewModel.estado.value.mensaje shouldBe PostViewModel.MENSAJE_SIN_CONEXION
        }
    }

    "limpiarMensaje borra el aviso de la ultima operacion" {
        runTest {
            val repositorio = repositorioConLista()
            coEvery { repositorio.deletePost(any()) } just runs
            val viewModel = PostViewModel(repositorio)
            viewModel.eliminarPost(1)
            advanceUntilIdle()
            viewModel.estado.value.mensaje.shouldNotBeNull()

            viewModel.limpiarMensaje()

            viewModel.estado.value.mensaje.shouldBeNull()
        }
    }
})
