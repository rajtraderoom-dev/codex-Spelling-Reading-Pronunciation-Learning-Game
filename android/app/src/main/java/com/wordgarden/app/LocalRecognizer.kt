package com.wordgarden.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** Never instantiate the system's potentially online recognition service. */
class LocalRecognizer(private val activity: Activity) {
    private var recognizer: SpeechRecognizer? = null
    fun supported(): Boolean = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(activity)
    fun permitted() = activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED
    fun stop() { recognizer?.cancel(); recognizer?.destroy(); recognizer=null }
    fun listen(result: (List<String>)->Unit, message: (String)->Unit, finished: ()->Unit, partial: (String)->Unit = {}) {
        stop()
        if(Build.VERSION.SDK_INT < 31 || !supported() || !permitted()) { message("Let’s use listen and tap. A parent can set up the microphone."); finished(); return }
        try {
            val local = SpeechRecognizer.createOnDeviceSpeechRecognizer(activity); recognizer=local
            local.setRecognitionListener(object: RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { message("I’m listening. Take your time. 🌱") }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {} // deliberately discarded
                override fun onEndOfSpeech() { message("Thank you for trying. 🌷") }
                override fun onError(error: Int) { message(if(error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) "Let’s use listen and tap. Ask a parent about the microphone." else "I couldn’t hear you. Shall we try again?"); finished() }
                override fun onResults(results: Bundle?) { result(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()); finished() }
                override fun onPartialResults(partialResults: Bundle?) { partial(partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()) }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            local.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-IN")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
            })
        } catch (_: Exception) { message("Let’s use listen and tap today. You can still grow!"); finished() }
    }
}
