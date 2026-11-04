package com.wordgarden.app

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Select only installed, non-network voices. Never fall back to cloud TTS. */
class OfflineVoice(private val context: Context, private val status: (String) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var ready = false
    var muted = false
    var quiet = false
    var slowMode = false
    private var token = 0
    private var onWord: ((Int) -> Unit)? = null
    private var complete: (() -> Unit)? = null
    private var text = ""
    private var player: android.media.MediaPlayer? = null
    init {
        engine = TextToSpeech(context) { code ->
            val tts = engine
            if(code == TextToSpeech.SUCCESS && tts != null) {
                val voice = tts.voices?.filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }?.sortedByDescending { it.quality }?.firstOrNull()
                if(voice != null) { tts.voice = voice; ready = true; status("Offline voice is ready.") }
                else status("Parent setup: install an English offline voice in Android speech settings.")
            } else status("Parent setup: an offline English speech engine is needed.")
        }
    }
    fun available() = ready
    fun stop() { token++; engine?.stop(); player?.release(); player=null; onWord=null; complete=null }
    fun refresh() {
        val tts=engine ?: return
        val voice=tts.voices?.filter { it.locale.language=="en" && !it.isNetworkConnectionRequired }?.maxByOrNull {it.quality}
        ready=voice!=null
        if(voice!=null) tts.voice=voice
    }
    fun sound(id: String, example: String, slow: Boolean=false) {
        stop(); if(muted || quiet)return
        try {
            val file=context.assets.openFd("phoneme_audio/$id.mp3")
            val media=android.media.MediaPlayer(); player=media
            media.setDataSource(file.fileDescriptor,file.startOffset,file.length); file.close()
            media.prepare(); if(slow || slowMode) media.playbackParams=media.playbackParams.setSpeed(0.75f)
            media.setOnCompletionListener { if(player===it) {it.release();player=null} }
            media.start()
        } catch (_:Exception) { player?.release();player=null;say(example,slow) }
    }
    fun say(value: String, slow: Boolean = false, highlight: ((Int) -> Unit)? = null, done: (() -> Unit)? = null) {
        stop(); if(!ready || muted || quiet) { done?.invoke(); return }
        text=value; onWord=highlight; complete=done
        val myToken=token
        engine?.setSpeechRate(if(slow || slowMode) 0.65f else 0.9f)
        engine?.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
            override fun onStart(id: String?) { handler.post { if(token==myToken) onWord?.invoke(0) } }
            override fun onDone(id: String?) { handler.post { if(token==myToken) { onWord?.invoke(-1); val callback=complete; complete=null; callback?.invoke() } } }
            @Deprecated("Platform callback") override fun onError(id: String?) { handler.post { if(token==myToken) { status("Let’s try the listen button again."); val callback=complete; complete=null; callback?.invoke() } } }
            override fun onRangeStart(id: String?, start: Int, end: Int, frame: Int) { handler.post { if(token==myToken) onWord?.invoke(text.take(start).count { it==' ' }) } }
        })
        engine?.speak(value,TextToSpeech.QUEUE_FLUSH,Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,if(quiet) 0f else 1f) },"garden-$token")
    }
    /** Word sequencing is deterministic even for engines without range callbacks. */
    fun sequence(parts: List<String>, slow: Boolean = true, highlight: (Int)->Unit = {}, done: ()->Unit = {}) {
        if(parts.isEmpty()) { done(); return }
        fun step(index: Int) {
            if(index>=parts.size) { highlight(-1); done(); return }
            highlight(index)
            say(parts[index],slow,done={ step(index+1) })
        }
        step(0)
    }
    fun close() { stop(); engine?.shutdown() }
}
