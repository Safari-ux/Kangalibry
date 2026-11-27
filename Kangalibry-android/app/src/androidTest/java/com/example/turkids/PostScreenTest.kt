package com.example.turkids

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.turkids.data.model.Post
import com.example.turkids.data.remote.ApiService
import com.example.turkids.repository.PostRepository
import com.example.turkids.ui.screens.PostScreen
import com.example.turkids.ui.theme.TurKidsTheme
import com.example.turkids.viewmodel.PostViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class PostScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class ApiFalsa(private val respuesta: () -> List<Post>) : ApiService {
        override suspend fun getPosts(): List<Post> = respuesta()
        override suspend fun createPost(post: Post): Post = post.copy(id = 101)
        override suspend fun updatePost(id: Int, post: Post): Post = post
        override suspend fun deletePost(id: Int) = Unit
    }

    private fun mostrarPantalla(api: ApiService) {
        val viewModel = PostViewModel(PostRepository(api))
        composeTestRule.setContent {
            TurKidsTheme {
                PostScreen(viewModel = viewModel)
            }
        }
    }

    private fun esperarTexto(texto: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun muestraLosPostsDeLaApiFalsa() {
        mostrarPantalla(
            ApiFalsa {
                listOf(
                    Post(userId = 1, id = 1, title = "Titulo falso 1", body = "Cuerpo falso 1"),
                    Post(userId = 1, id = 2, title = "Titulo falso 2", body = "Cuerpo falso 2")
                )
            }
        )

        esperarTexto("Titulo falso 1")

        composeTestRule.onNodeWithText("Publicaciones (API de prueba)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Titulo falso 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cuerpo falso 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Titulo falso 2").assertIsDisplayed()
    }

    @Test
    fun muestraElErrorYElBotonReintentarCuandoLaApiFalla() {
        mostrarPantalla(ApiFalsa { throw IOException("sin red") })

        esperarTexto("Reintentar")

        composeTestRule.onNodeWithText("No se pudo conectar. Revisa tu internet.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reintentar").assertIsDisplayed()
    }

    @Test
    fun alPulsarCrearPostSeAgregaALaLista() {
        mostrarPantalla(
            ApiFalsa { listOf(Post(userId = 1, id = 1, title = "Titulo falso 1", body = "Cuerpo falso 1")) }
        )
        esperarTexto("Titulo falso 1")

        composeTestRule.onNodeWithText("Crear post (POST)").performClick()

        esperarTexto("Publicación de prueba")
        composeTestRule.onNodeWithText("Publicación de prueba").assertIsDisplayed()
    }
}
