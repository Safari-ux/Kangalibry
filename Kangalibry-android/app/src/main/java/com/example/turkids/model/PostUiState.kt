package com.example.turkids.model

import com.example.turkids.data.model.Post

data class PostUiState(
    val cargando: Boolean = false,
    val posts: List<Post> = emptyList(),
    val error: String? = null,
    // true mientras se ejecuta un post, put o delete
    val operacionEnCurso: Boolean = false,
    // resultado de la ultima escritura; la lista se conserva aunque falle
    val mensaje: String? = null
)
