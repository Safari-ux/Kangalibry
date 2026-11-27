package com.example.turkids.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.turkids.model.ValidadorUsuario
import com.example.turkids.repository.PaisRepository
import com.example.turkids.repository.PostRepository
import com.example.turkids.repository.ProgresoRepository
import com.example.turkids.repository.UbicacionRepository

// fabrica simple: recibe las dependencias desde mainActivity
class TurKidsViewModelFactory(
    private val progresoRepository: ProgresoRepository,
    private val ubicacionRepository: UbicacionRepository,
    private val validadorUsuario: ValidadorUsuario,
    private val postRepository: PostRepository,
    private val paisRepository: PaisRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel()
        modelClass.isAssignableFrom(ProgresoViewModel::class.java) ->
            ProgresoViewModel(progresoRepository)
        modelClass.isAssignableFrom(LeccionViewModel::class.java) ->
            LeccionViewModel(progresoRepository)
        modelClass.isAssignableFrom(UbicacionViewModel::class.java) ->
            UbicacionViewModel(ubicacionRepository, paisRepository)
        modelClass.isAssignableFrom(UsuarioViewModel::class.java) ->
            UsuarioViewModel(validadorUsuario)
        modelClass.isAssignableFrom(PostViewModel::class.java) ->
            PostViewModel(postRepository)
        modelClass.isAssignableFrom(TestOralViewModel::class.java) ->
            TestOralViewModel()
        else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    } as T
}
