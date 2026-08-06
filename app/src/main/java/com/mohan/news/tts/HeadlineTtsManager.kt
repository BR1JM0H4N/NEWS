package com.mohan.news.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import java.util.UUID

enum class TtsState { IDLE, SPEAKING, PAUSED }

/**
 * Wraps Android's system TextToSpeech engine to read a queue of headlines aloud,
 * with adjustable speed, pitch, and voice.
 */
class HeadlineTtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingSpeed = 1.0f
    private var pendingPitch = 1.0f
    private var pendingVoiceName: String? = null

    var onStateChanged: ((TtsState) -> Unit)? = null
    var onHeadlineIndexChanged: ((Int) -> Unit)? = null

    var state: TtsState = TtsState.IDLE
        private set(value) {
            field = value
            onStateChanged?.invoke(value)
        }

    private var queue: List<String> = emptyList()
    private var currentIndex = -1

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) {
                tts?.language = Locale.getDefault()
                applyRate(pendingSpeed)
                applyPitch(pendingPitch)
                pendingVoiceName?.let { applyVoiceByName(it) }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        currentIndex++
                        if (currentIndex < queue.size) {
                            onHeadlineIndexChanged?.invoke(currentIndex)
                            speakInternal(queue[currentIndex])
                        } else {
                            state = TtsState.IDLE
                            currentIndex = -1
                        }
                    }

                    @Deprecated("Deprecated in API")
                    override fun onError(utteranceId: String?) {
                        state = TtsState.IDLE
                    }
                })
            }
        }
    }

    fun availableVoices(): List<Voice> {
        return tts?.voices?.filter { it.locale?.language == Locale.getDefault().language }?.toList()
            ?: emptyList()
    }

    fun setSpeed(speed: Float) {
        pendingSpeed = speed
        applyRate(speed)
    }

    fun setPitch(pitch: Float) {
        pendingPitch = pitch
        applyPitch(pitch)
    }

    fun setVoiceByName(name: String?) {
        pendingVoiceName = name
        if (name != null) applyVoiceByName(name)
    }

    private fun applyRate(speed: Float) {
        tts?.setSpeechRate(speed)
    }

    private fun applyPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }

    private fun applyVoiceByName(name: String) {
        tts?.voices?.firstOrNull { it.name == name }?.let { tts?.voice = it }
    }

    /** Begin reading a list of headlines in order. */
    fun readHeadlines(headlines: List<String>) {
        if (!isReady || headlines.isEmpty()) return
        stop()
        queue = headlines
        currentIndex = 0
        onHeadlineIndexChanged?.invoke(currentIndex)
        state = TtsState.SPEAKING
        speakInternal(queue[currentIndex])
    }

    private fun speakInternal(text: String) {
        val id = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun pause() {
        if (state == TtsState.SPEAKING) {
            tts?.stop()
            state = TtsState.PAUSED
        }
    }

    fun resume() {
        if (state == TtsState.PAUSED && currentIndex in queue.indices) {
            state = TtsState.SPEAKING
            speakInternal(queue[currentIndex])
        }
    }

    fun stop() {
        tts?.stop()
        state = TtsState.IDLE
        currentIndex = -1
        queue = emptyList()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
