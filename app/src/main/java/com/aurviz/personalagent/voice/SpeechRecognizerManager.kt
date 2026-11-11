package com.aurviz.personalagent.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechRecognizerManager(
    context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onPartialResult(text: String)
        fun onResult(text: String)
        fun onError(message: String)
    }

    private val appContext = context.applicationContext
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
        setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onPartialResults(results: Bundle?) {
                listener.onPartialResult(results.texts().firstOrNull().orEmpty())
            }
            override fun onResults(results: Bundle?) {
                listener.onResult(results.texts().firstOrNull().orEmpty())
            }
            override fun onError(error: Int) {
                listener.onError("Speech recognition failed (code $error)")
            }
        })
    }

    fun start() {
        check(SpeechRecognizer.isRecognitionAvailable(appContext)) {
            "Speech recognition is not available on this device"
        }
        recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        })
    }

    fun stop() = recognizer.stopListening()
    fun destroy() = recognizer.destroy()
}

private fun Bundle?.texts(): List<String> =
    this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
