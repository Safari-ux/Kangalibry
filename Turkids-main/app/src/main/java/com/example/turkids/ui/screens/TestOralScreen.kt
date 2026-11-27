@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.turkids.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.turkids.model.ResultadoOral
import com.example.turkids.model.TestOralUiState
import com.example.turkids.ui.utils.abrirAjustesDeLaApp
import com.example.turkids.ui.utils.mensajeDeErrorDeVoz
import com.example.turkids.ui.utils.rememberReconocedorDeVoz
import com.example.turkids.viewmodel.ProgresoViewModel
import com.example.turkids.viewmodel.TestOralViewModel
import java.util.Locale

@Composable
fun TestOralScreen(
    viewModel: TestOralViewModel,
    progresoViewModel: ProgresoViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    val progreso by progresoViewModel.estado.collectAsState()
    val contexto = LocalContext.current
    val reconocedor = rememberReconocedorDeVoz()
    var sinPermiso by remember { mutableStateOf(false) }

    LaunchedEffect(progreso.idioma) { viewModel.iniciar(progreso.idioma) }

    fun empezarAEscuchar() {
        if (!reconocedor.disponible()) {
            viewModel.errorDeEscucha("Este teléfono no tiene reconocimiento de voz.")
            return
        }
        viewModel.escuchando()
        reconocedor.escuchar(
            idioma = Locale.forLanguageTag(estado.codigoIdiomaRespuesta),
            alResultado = { viewModel.recibirTexto(it) },
            alError = { viewModel.errorDeEscucha(mensajeDeErrorDeVoz(it)) }
        )
    }

    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        sinPermiso = !concedido
        if (concedido) empezarAEscuchar()
    }

    fun alPulsarMicrofono() {
        if (estado.escuchando) {
            reconocedor.detener()
            viewModel.errorDeEscucha("Escucha cancelada.")
            return
        }
        val concedido = ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (concedido) {
            sinPermiso = false
            empezarAEscuchar()
        } else {
            pedirPermiso.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test oral") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                estado.finalizado -> ResultadoFinal(
                    aciertos = estado.aciertos,
                    total = estado.total,
                    onJugarDeNuevo = { viewModel.jugarDeNuevo() },
                    onVolver = onVolver
                )

                estado.palabraActual == null -> CircularProgressIndicator()

                else -> PreguntaOral(
                    estado = estado,
                    sinPermiso = sinPermiso,
                    onMicrofono = { alPulsarMicrofono() },
                    onSiguiente = { viewModel.siguiente() },
                    onAbrirAjustes = { contexto.abrirAjustesDeLaApp() }
                )
            }
        }
    }
}

@Composable
private fun PreguntaOral(
    estado: TestOralUiState,
    sinPermiso: Boolean,
    onMicrofono: () -> Unit,
    onSiguiente: () -> Unit,
    onAbrirAjustes: () -> Unit
) {
    val palabra = estado.palabraActual ?: return

    LinearProgressIndicator(
        progress = { estado.indiceActual.toFloat() / estado.total },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = "Pregunta ${estado.indiceActual + 1} de ${estado.total}", fontSize = 14.sp)
    Spacer(modifier = Modifier.height(16.dp))

    Text(text = palabra.emoji, fontSize = 72.sp)
    Text(
        text = estado.textoMostrado,
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Di en voz alta cómo se dice en ${estado.nombreIdiomaRespuesta}",
        fontSize = 18.sp,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))

    val resultado = estado.resultado
    if (resultado == null) {
        Button(onClick = onMicrofono) {
            Text(if (estado.escuchando) "⏹  Detener" else "🎤  Hablar", fontSize = 20.sp)
        }
        if (estado.escuchando) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Escuchando...", fontSize = 16.sp)
        }
    } else {
        val correcto = resultado == ResultadoOral.CORRECTO
        Text(
            text = if (correcto) "¡Muy bien!" else "Casi. Se dice: ${estado.respuestaEsperada}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (correcto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        estado.escuchado?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Escuché: $it", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSiguiente) { Text("Continuar") }
    }

    estado.mensaje?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    }
    if (sinPermiso) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Para jugar necesito usar el micrófono.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onAbrirAjustes) { Text("Abrir ajustes") }
    }
}

@Composable
private fun ResultadoFinal(
    aciertos: Int,
    total: Int,
    onJugarDeNuevo: () -> Unit,
    onVolver: () -> Unit
) {
    Text(text = "🎉", fontSize = 72.sp)
    Text(
        text = "¡Test terminado!",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = "Dijiste bien $aciertos de $total palabras", fontSize = 18.sp)
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onJugarDeNuevo) { Text("Jugar de nuevo") }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = onVolver) { Text("Volver al inicio") }
}
