package com.example.turkids.ui.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Locale

// envuelve speechRecognizer: la pantalla solo pide escuchar y recibe los textos
class ReconocedorDeVoz(private val contexto: Context) {

    private var reconocedor: SpeechRecognizer? = null

    fun disponible(): Boolean = SpeechRecognizer.isRecognitionAvailable(contexto)

    fun escuchar(idioma: Locale, alResultado: (List<String>) -> Unit, alError: (Int) -> Unit) {
        detener()
        val nuevo = SpeechRecognizer.createSpeechRecognizer(contexto)
        nuevo.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                alResultado(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty())
            }

            override fun onError(error: Int) {
                alError(error)
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        reconocedor = nuevo

        val intento = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, idioma.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        nuevo.startListening(intento)
    }

    fun detener() {
        reconocedor?.destroy()
        reconocedor = null
    }

    fun liberar() {
        detener()
    }
}

fun mensajeDeErrorDeVoz(codigo: Int): String = when (codigo) {
    SpeechRecognizer.ERROR_NO_MATCH,
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No te escuché bien. Inténtalo de nuevo."

    SpeechRecognizer.ERROR_NETWORK,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
    SpeechRecognizer.ERROR_SERVER,
    SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "No hay conexión con el reconocedor de voz. Revisa tu internet."

    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta el permiso del micrófono."
    SpeechRecognizer.ERROR_AUDIO -> "Hay un problema con el micrófono."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El reconocedor está ocupado. Espera un momento."

    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Falta el idioma en el reconocedor de voz del teléfono."

    else -> "No se pudo escuchar. Inténtalo de nuevo."
}

// se crea al entrar a la pantalla, se libera al salir y se detiene si la app deja de verse
@Composable
fun rememberReconocedorDeVoz(): ReconocedorDeVoz {
    val contexto = LocalContext.current
    val reconocedor = remember { ReconocedorDeVoz(contexto) }

    DisposableEffect(reconocedor) {
        onDispose { reconocedor.liberar() }
    }

    val ciclo = (LocalActivity.current as ComponentActivity).lifecycle
    DisposableEffect(ciclo, reconocedor) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_STOP) reconocedor.detener()
        }
        ciclo.addObserver(observador)
        onDispose { ciclo.removeObserver(observador) }
    }

    return reconocedor
}
