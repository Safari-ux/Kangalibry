package com.example.turkids

import com.example.turkids.data.model.Post
import com.example.turkids.data.remote.ApiService
import com.example.turkids.repository.PostRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import java.io.IOException

class PostRepositoryTest : StringSpec({

    "getPosts devuelve la lista que entrega la API" {
        runTest {
            val api = mockk<ApiService>()
            val esperados = listOf(
                Post(userId = 1, id = 1, title = "Titulo 1", body = "Cuerpo 1"),
                Post(userId = 1, id = 2, title = "Titulo 2", body = "Cuerpo 2")
            )
            coEvery { api.getPosts() } returns esperados

            val repositorio = PostRepository(api)
            val resultado = repositorio.getPosts()

            resultado shouldHaveSize 2
            resultado shouldBe esperados
            coVerify(exactly = 1) { api.getPosts() }
        }
    }

    "getPosts devuelve una lista vacia si la API no tiene publicaciones" {
        runTest {
            val api = mockk<ApiService>()
            coEvery { api.getPosts() } returns emptyList()

            PostRepository(api).getPosts() shouldBe emptyList<Post>()
        }
    }

    "getPosts propaga el error cuando la API falla" {
        runTest {
            val api = mockk<ApiService>()
            coEvery { api.getPosts() } throws IOException("sin red")

            shouldThrow<IOException> { PostRepository(api).getPosts() }
        }
    }

    "createPost envia el post a la API y devuelve el que ella responde" {
        runTest {
            val api = mockk<ApiService>()
            val enviado = Post(userId = 1, id = 0, title = "Nuevo", body = "Cuerpo nuevo")
            val respuesta = enviado.copy(id = 101)
            coEvery { api.createPost(enviado) } returns respuesta

            PostRepository(api).createPost(enviado) shouldBe respuesta

            coVerify(exactly = 1) { api.createPost(enviado) }
        }
    }

    "updatePost usa el id indicado y devuelve el post actualizado" {
        runTest {
            val api = mockk<ApiService>()
            val enviado = Post(userId = 1, id = 5, title = "Editado", body = "Cuerpo")
            coEvery { api.updatePost(5, enviado) } returns enviado

            PostRepository(api).updatePost(5, enviado) shouldBe enviado

            coVerify(exactly = 1) { api.updatePost(5, enviado) }
        }
    }

    "deletePost llama a la API con el id" {
        runTest {
            val api = mockk<ApiService>()
            coEvery { api.deletePost(7) } just runs

            PostRepository(api).deletePost(7)

            coVerify(exactly = 1) { api.deletePost(7) }
        }
    }

    "las operaciones de escritura propagan el error de la API" {
        runTest {
            val api = mockk<ApiService>()
            val post = Post(userId = 1, id = 1, title = "T", body = "B")
            coEvery { api.createPost(any()) } throws IOException("sin red")
            coEvery { api.updatePost(any(), any()) } throws IOException("sin red")
            coEvery { api.deletePost(any()) } throws IOException("sin red")
            val repositorio = PostRepository(api)

            shouldThrow<IOException> { repositorio.createPost(post) }
            shouldThrow<IOException> { repositorio.updatePost(1, post) }
            shouldThrow<IOException> { repositorio.deletePost(1) }
        }
    }
})
