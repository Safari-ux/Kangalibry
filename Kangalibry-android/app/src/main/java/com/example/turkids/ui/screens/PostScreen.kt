@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.turkids.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.turkids.data.model.Post
import com.example.turkids.viewmodel.PostViewModel

@Composable
fun PostScreen(
    viewModel: PostViewModel,
    onVolver: () -> Unit = {}
) {
    val estado by viewModel.estado.collectAsState()
    val mensajeError = estado.error
    val snackbarHostState = remember { SnackbarHostState() }

    var postAEditar by remember { mutableStateOf<Post?>(null) }
    var tituloEdit by remember { mutableStateOf("") }
    var cuerpoEdit by remember { mutableStateOf("") }

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!estado.operacionEnCurso) viewModel.crearPost() },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Crear post (POST)") }
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("Publicaciones (API de prueba)") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                estado.cargando -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                mensajeError != null -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = mensajeError, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.fetchPosts() }) {
                        Text("Reintentar")
                    }
                }

                estado.posts.isEmpty() -> Text(
                    text = "No hay publicaciones",
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(estado.posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            habilitado = !estado.operacionEnCurso,
                            onEditar = {
                                postAEditar = post
                                tituloEdit = post.title
                                cuerpoEdit = post.body
                            },
                            onEliminar = { viewModel.eliminarPost(post.id) }
                        )
                    }
                }
            }
        }
    }

    // dialogo de edicion: guardar solo se habilita con titulo y contenido no vacios y envia el post editado por put
    postAEditar?.let { post ->
        AlertDialog(
            onDismissRequest = { postAEditar = null },
            title = { Text("Editar publicación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tituloEdit,
                        onValueChange = { tituloEdit = it },
                        label = { Text("Título") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cuerpoEdit,
                        onValueChange = { cuerpoEdit = it },
                        label = { Text("Contenido") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = tituloEdit.isNotBlank() && cuerpoEdit.isNotBlank(),
                    onClick = {
                        viewModel.actualizarPost(
                            post.copy(
                                title = tituloEdit.trim(),
                                body = cuerpoEdit.trim()
                            )
                        )
                        postAEditar = null
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { postAEditar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun PostCard(
    post: Post,
    habilitado: Boolean,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = post.body,
                style = MaterialTheme.typography.bodyMedium
            )
            Row {
                TextButton(onClick = onEditar, enabled = habilitado) { Text("Editar (PUT)") }
                TextButton(onClick = onEliminar, enabled = habilitado) { Text("Eliminar (DELETE)") }
            }
        }
    }
}