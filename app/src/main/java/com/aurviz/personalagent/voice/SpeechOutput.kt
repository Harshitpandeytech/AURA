package com.aurviz.personalagent.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechOutput(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var textToSpeech: TextToSpeech? = TextToSpeech(appContext, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = textToSpeech?.setLanguage(Locale.getDefault()) != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    fun speak(text: String) {
        if (isReady && text.isNotBlank()) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        }
    }

    fun stop() {
        textToSpeech?.stop()
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isReady = false
    }

    private companion object {
        const val UTTERANCE_ID = "aura_response"
    }
}
