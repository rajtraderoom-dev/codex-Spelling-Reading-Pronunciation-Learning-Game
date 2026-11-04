package com.wordgarden.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Only anonymous local learning counters are retained; no microphone audio. */
class ProgressStore(context: Context) : SQLiteOpenHelper(context, "garden.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE progress (kind TEXT, item TEXT, attempts INTEGER NOT NULL DEFAULT 0, successes INTEGER NOT NULL DEFAULT 0, heard INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(kind,item))")
        db.execSQL("CREATE TABLE rewards (day TEXT PRIMARY KEY, stars INTEGER NOT NULL DEFAULT 0, sessions INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE content (kind TEXT, item TEXT, json TEXT, PRIMARY KEY(kind,item))")
    }
    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {}
    fun seed(context: Context) {
        writableDatabase.beginTransaction()
        try {
            for (kind in listOf("words", "sentences", "phonemes", "patterns", "minimal_pairs")) {
                val raw = context.assets.open("$kind.json").bufferedReader().use { it.readText() }
                val rows = org.json.JSONArray(raw)
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    writableDatabase.insertWithOnConflict("content", null, ContentValues().apply { put("kind",kind); put("item",row.optString("id",i.toString())); put("json",row.toString()) }, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }
    fun heard(kind: String, item: String) {
        writableDatabase.execSQL("INSERT OR IGNORE INTO progress(kind,item) VALUES (?,?)", arrayOf(kind,item))
        writableDatabase.execSQL("UPDATE progress SET heard=1 WHERE kind=? AND item=?", arrayOf(kind,item))
    }
    fun record(kind: String, item: String, success: Boolean) {
        heard(kind,item)
        writableDatabase.execSQL("UPDATE progress SET attempts=attempts+1, successes=successes+? WHERE kind=? AND item=?", arrayOf<Any>(if(success) 1 else 0, kind, item))
    }
    fun reward() {
        writableDatabase.execSQL("INSERT OR IGNORE INTO rewards(day) VALUES (date('now','localtime'))")
        writableDatabase.execSQL("UPDATE rewards SET stars=stars+1,sessions=sessions+1 WHERE day=date('now','localtime')")
    }
    fun stars(): Int = readableDatabase.rawQuery("SELECT COALESCE(SUM(stars),0) FROM rewards",null).use { it.moveToFirst(); it.getInt(0) }
    fun today(): Int = readableDatabase.rawQuery("SELECT COALESCE(SUM(sessions),0) FROM rewards WHERE day=date('now','localtime')",null).use { it.moveToFirst(); it.getInt(0) }
    data class Row(val kind: String, val item: String, val attempts: Int, val successes: Int, val heard: Boolean)
    fun rows(): List<Row> = readableDatabase.rawQuery("SELECT kind,item,attempts,successes,heard FROM progress ORDER BY (attempts-successes) DESC,item",null).use { cursor ->
        buildList { while(cursor.moveToNext()) add(Row(cursor.getString(0),cursor.getString(1),cursor.getInt(2),cursor.getInt(3),cursor.getInt(4)==1)) }
    }
    fun clear() { writableDatabase.delete("progress",null,null); writableDatabase.delete("rewards",null,null) }
}
