package com.example.turkids.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turkids.data.model.Post
import com.example.turkids.model.PostUiState
import com.example.turkids.repository.PostRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class PostViewModel(private val repositorio: PostRepository) : ViewModel() {

    // stateFlow privado mutable: la pantalla solo observa estado y las corrutinas lo actualizan con update
    private val _estado = MutableStateFlow(PostUiState())
    val estado: StateFlow<PostUiState> = _estado

    init {
        fetchPosts()
    }

    // get /posts: carga la lista con estados de carga, exito y error (reintentar vuelve a llamar)
    fun fetchPosts() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            try {
                val posts = repositorio.getPosts()
                _estado.update { it.copy(cargando = false, posts = posts) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _estado.update { it.copy(cargando = false, error = MENSAJE_SIN_CONEXION) }
            } catch (e: HttpException) {
                _estado.update { it.copy(cargando = false, error = "El servidor respondió con un error (${e.code()}).") }
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = "No se pudieron cargar las publicaciones.") }
            }
        }
    }

    // post /posts: el servidor asigna el id y devuelve el post creado
    fun crearPost(titulo: String = TITULO_PRUEBA, cuerpo: String = CUERPO_PRUEBA) {
        ejecutarOperacion(MENSAJE_CREADO) {
            val creado = repositorio.createPost(Post(userId = USUARIO_PRUEBA, id = 0, title = titulo, body = cuerpo))
            _estado.update { actual -> actual.copy(posts = listOf(creado) + actual.posts) }
        }
    }

    // put /posts/{id}: envia el post editado tal cual y lo reemplaza en la lista
    fun actualizarPost(post: Post) {
        ejecutarOperacion(MENSAJE_ACTUALIZADO) {
            val actualizado = repositorio.updatePost(post.id, post)
            _estado.update { actual ->
                actual.copy(posts = actual.posts.map { if (it.id == post.id) actualizado else it })
            }
        }
    }

    // delete /posts/{id}: quita el post de la lista
    fun eliminarPost(id: Int) {
        ejecutarOperacion(MENSAJE_ELIMINADO) {
            repositorio.deletePost(id)
            _estado.update { actual -> actual.copy(posts = actual.posts.filterNot { it.id == id }) }
        }
    }

    fun limpiarMensaje() {
        _estado.update { it.copy(mensaje = null) }
    }

    // una escritura a la vez; si falla la lista no cambia y se avisa en mensaje
    private fun ejecutarOperacion(mensajeExito: String, accion: suspend () -> Unit) {
        if (_estado.value.operacionEnCurso) return
        viewModelScope.launch {
            _estado.update { it.copy(operacionEnCurso = true, mensaje = null) }
            val mensaje = try {
                accion()
                mensajeExito
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                MENSAJE_SIN_CONEXION
            } catch (e: HttpException) {
                "El servidor respondió con un error (${e.code()})."
            } catch (e: Exception) {
                "No se pudo completar la operación."
            }
            _estado.update { it.copy(operacionEnCurso = false, mensaje = mensaje) }
        }
    }

    companion object {
        const val MENSAJE_SIN_CONEXION = "No se pudo conectar. Revisa tu internet."
        const val MENSAJE_CREADO = "Publicación creada (POST)"
        const val MENSAJE_ACTUALIZADO = "Publicación actualizada (PUT)"
        const val MENSAJE_ELIMINADO = "Publicación eliminada (DELETE)"
        const val TITULO_PRUEBA = "Publicación de prueba"
        const val CUERPO_PRUEBA = "Creada desde Kangalibry con Retrofit"
        const val USUARIO_PRUEBA = 1
    }
}
