package com.wordgarden.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Word(val id: String, val word: String, val pattern: String, val picture: String, val meaning: String, val syllables: List<String>, val sentence: String, val pictureChoice: Boolean, val soundOut: Boolean)
data class Sentence(val id: String, val text: String, val pattern: String, val picture: String, val meaning: String, val phrases: List<String>)
data class Sound(val id: String, val ipa: String, val letters: String, val example: String, val picture: String, val cue: String, val mouth: String)
data class Pattern(val id: String, val title: String, val picture: String, val level: Int)
data class PairDrill(val a: String, val b: String, val sound: String, val cue: String)
class Content(private val context: Context) {
    private fun rows(name: String): List<JSONObject> {
        val array = JSONArray(context.assets.open("$name.json").bufferedReader().use { it.readText() })
        return (0 until array.length()).map { array.getJSONObject(it) }
    }
    private fun strings(row: JSONObject, key: String): List<String> = row.getJSONArray(key).let { a -> (0 until a.length()).map { a.getString(it) } }
    val words = rows("words").map { Word(it.getString("id"), it.getString("word"), it.getString("pattern"), it.getString("picture"), it.getString("meaning"), strings(it,"syllables"), it.getString("sentence"), it.optBoolean("pictureChoice",false), it.optBoolean("soundOut",false)) }
    val sentences = rows("sentences").map { Sentence(it.getString("id"), it.getString("text"), it.getString("pattern"), it.getString("picture"), it.getString("meaning"), strings(it,"phrases")) }
    val sounds = rows("phonemes").map { Sound(it.getString("id"), it.getString("ipa"), it.getString("letters"), it.getString("example"), it.getString("picture"), it.getString("cue"), it.getString("mouth")) }
    val patterns = rows("patterns").map { Pattern(it.getString("id"), it.getString("title"), it.getString("picture"), it.getInt("level")) }
    val pairs = rows("minimal_pairs").map { PairDrill(it.getString("a"), it.getString("b"), it.getString("sound"), it.getString("cue")) }
}
