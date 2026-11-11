package com.hocchu.mobile.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class Speech(context: Context) : TextToSpeech.OnInitListener {
    private var ready = false
    private var pending: String? = null
    private val engine = TextToSpeech(context.applicationContext, this)
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = engine.setLanguage(Locale.forLanguageTag("vi-VN"))
            ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            engine.setSpeechRate(.75f)
            pending?.let { speak(it) }
            pending = null
        }
    }
    fun speak(text: String): Boolean {
        if (!ready) { pending = text; return false }
        return engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hocchu") == TextToSpeech.SUCCESS
    }
    fun stop() { pending = null; engine.stop() }
    fun close() { stop(); engine.shutdown() }
}
