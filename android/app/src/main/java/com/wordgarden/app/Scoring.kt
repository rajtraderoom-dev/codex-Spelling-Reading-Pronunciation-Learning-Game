package com.wordgarden.app

/** Transcript matching is supportive feedback, not a clinical pronunciation score. */
object Scoring {
    // The recognizer may choose a different spelling for exactly the same
    // sound. Accept that in speech, while typed/tile spelling stays exact.
    private val homophones = listOf(
        setOf("see","sea"), setOf("bee","be"), setOf("tea","tee"),
        setOf("right","write"), setOf("night","knight"), setOf("sun","son"),
        setOf("one","won"), setOf("two","to","too"), setOf("four","for"),
        setOf("eight","ate"), setOf("no","know"), setOf("here","hear"),
        setOf("their","there"), setOf("blue","blew"), setOf("rain","reign","rein"),
        setOf("tail","tale"), setOf("mail","male"), setOf("sail","sale"),
        setOf("road","rode"), setOf("flower","flour"), setOf("week","weak"),
        setOf("wood","would"), setOf("new","knew"), setOf("wait","weight"),
        setOf("nose","knows"), setOf("hair","hare"), setOf("pair","pear")
    )
    fun normalize(text: String) = text.lowercase().replace(Regex("[^a-z ]"), "").trim().replace(Regex("\\s+"), " ")
    fun distance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val next = IntArray(b.length + 1); next[0] = i + 1
            for (j in b.indices) next[j + 1] = minOf(next[j] + 1, previous[j + 1] + 1, previous[j] + if (a[i] == b[j]) 0 else 1)
            previous = next
        }
        return previous.last()
    }
    // Collapse common alternate spellings, without collapsing teaching contrasts
    // such as v/w, p/b, ship/sheep, or t/d.
    fun phonetic(text: String) = normalize(text).replace("ph", "f").replace("ck", "k").replace("ee", "ee").replace("ea", "ee").replace(Regex("(.)\\1+"), "$1")
    fun matches(expected: String, heard: String): Boolean {
        val a = normalize(expected); val b = normalize(heard)
        if (a == b && a.isNotEmpty()) return true
        if (a.isEmpty() || b.isEmpty()) return false
        if (homophones.any { a in it && b in it }) return true
        if (a.length <= 4 || b.length <= 4) return false // protect minimal pairs
        return distance(a, b) <= (if (a.length >= 9) 2 else 1) || phonetic(a) == phonetic(b)
    }
    fun align(expected: String, heard: String): List<Boolean> {
        val target = normalize(expected).split(" ").filter { it.isNotEmpty() }
        val spoken = normalize(heard).split(" ").filter { it.isNotEmpty() }
        val dp = Array(target.size + 1) { IntArray(spoken.size + 1) }
        for (i in target.indices) dp[i + 1][0] = i + 1
        for (j in spoken.indices) dp[0][j + 1] = j + 1
        for (i in target.indices) for (j in spoken.indices) dp[i + 1][j + 1] = minOf(dp[i][j] + if (matches(target[i], spoken[j])) 0 else 1, dp[i][j + 1] + 1, dp[i + 1][j] + 1)
        val result = MutableList(target.size) { false }; var i = target.size; var j = spoken.size
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && dp[i - 1][j - 1] + (if (matches(target[i - 1], spoken[j - 1])) 0 else 1) == dp[i][j]) {
                result[i - 1] = matches(target[i - 1], spoken[j - 1]); i--; j--
            } else if (i > 0 && dp[i][j] == dp[i - 1][j] + 1) i-- else j--
        }
        return result
    }
    fun letters(text: String): String {
        val names = mapOf("ay" to "a", "bee" to "b", "be" to "b", "see" to "c", "sea" to "c", "dee" to "d", "ee" to "e", "eff" to "f", "gee" to "g", "aitch" to "h", "eye" to "i", "jay" to "j", "kay" to "k", "el" to "l", "ell" to "l", "em" to "m", "en" to "n", "oh" to "o", "pee" to "p", "queue" to "q", "cue" to "q", "are" to "r", "ar" to "r", "ess" to "s", "tee" to "t", "you" to "u", "vee" to "v", "ex" to "x", "why" to "y", "zed" to "z", "zee" to "z")
        return normalize(text).replace("double you", "w").replace("double u", "w").split(" ").mapNotNull { if (it.length == 1) it else names[it] }.joinToString("")
    }
}
