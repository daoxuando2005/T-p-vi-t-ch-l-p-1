package com.hocchu.mobile.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import java.util.Locale

class Speech(context: Context, private val onFailure: () -> Unit = {}) : TextToSpeech.OnInitListener {
    private val main = Handler(Looper.getMainLooper())
    private var closed = false
    private var ready = false
    private var pending: String? = null
    private val engine = TextToSpeech(context.applicationContext, this)
    override fun onInit(status: Int) {
        // Engine callbacks may arrive after disposal, or before construction returns.
        main.post { initialize(status) }
    }
    private fun initialize(status: Int) {
        if (closed) return
        if (status == TextToSpeech.SUCCESS) {
            val result = engine.setLanguage(Locale.forLanguageTag("vi-VN"))
            ready = result >= TextToSpeech.LANG_AVAILABLE
            engine.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {}
                @Deprecated("Required platform callback")
                override fun onError(utteranceId: String?) { reportFailure() }
                override fun onError(utteranceId: String?, errorCode: Int) { reportFailure() }
            })
            engine.setSpeechRate(.75f)
        }
        val queued = pending; pending = null
        if (queued != null) { if (ready) speak(queued) else reportFailure() }
    }
    private fun reportFailure() { main.post { if (!closed) onFailure() } }
    fun speak(text: String): Boolean {
        if (!ready) { pending = text; return false }
        if (closed) return false
        val accepted = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hocchu") == TextToSpeech.SUCCESS
        if (!accepted) reportFailure()
        return accepted
    }
    fun stop() { pending = null; engine.stop() }
    fun close() { closed = true; ready = false; stop(); engine.shutdown() }
}
