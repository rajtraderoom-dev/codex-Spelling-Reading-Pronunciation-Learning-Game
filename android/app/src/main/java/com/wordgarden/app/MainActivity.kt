package com.wordgarden.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlin.random.Random
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.widget.EmojiButton
import androidx.emoji2.widget.EmojiTextView

/** Native, single-activity presentation. Content, scoring, speech and persistence
 * live in separate modules so lessons can be edited without changing the UI. */
class MainActivity : Activity() {
    private lateinit var content: Content
    private lateinit var store: ProgressStore
    private lateinit var voice: OfflineVoice
    private lateinit var mic: LocalRecognizer
    private lateinit var body: LinearLayout
    private lateinit var scroller: ScrollView
    private lateinit var mascot: TextView
    private lateinit var note: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val preferences by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private val green = Color.rgb(40,93,73)
    private val cream = Color.rgb(247,247,239)
    private val ink = Color.rgb(37,62,49)
    private var route: ()->Unit = { home() }
    private var rewardGiven = false
    private var busy = false
    private var selectedPattern: String? = null
        set(value) { field=value; preferences.edit().putString("pattern",value).apply() }
    private var cursor = 0
        set(value) { field=value; preferences.edit().putInt("cursor",value).apply() }
    private var parentUntil = 0L
    private var sessionCount = 0
    private var screenId = 0
    private var voiceStatus = "Checking the installed offline English voice."
    private val modes = listOf("🌱 Guided lesson", "👂 Listen & Choose", "🔤 Sound It Out", "🧩 Missing Letter", "🧺 Spell It", "⌨ Type It", "🗣 Say the Word", "💬 Echo Reading", "📖 Read the Sentence", "🎤 Spell by Voice", "🧱 Build the Sentence", "📚 Read Along", "👄 Sound Drill")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EmojiCompat.init(BundledEmojiCompatConfig(this).setReplaceAll(true))
        content = Content(this); store = ProgressStore(this); store.seed(this)
        cursor=preferences.getInt("cursor",0)
        selectedPattern=preferences.getString("pattern",null)?.takeIf { id -> content.patterns.any {it.id==id} }
        mic = LocalRecognizer(this)
        voice = OfflineVoice(this) { message -> voiceStatus=message }
        applySettings(); home()
        if(android.os.Build.VERSION.SDK_INT>=33) onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT) { home() }
    }
    private fun dp(value: Int) = (value*resources.displayMetrics.density).toInt()
    private fun applySettings() { voice.muted=preferences.getBoolean("mute",false); voice.quiet=preferences.getBoolean("quiet",false); voice.slowMode=preferences.getBoolean("slow",false) }
    private fun text(value: String, size: Float = 21f, bold: Boolean = false): TextView = EmojiTextView(this).apply {
        text=value; textSize=size; setTextColor(ink); setPadding(dp(8),dp(10),dp(8),dp(10)); gravity=Gravity.CENTER
        typeface=Typeface.create(if(preferences.getBoolean("readable",false)) "sans-serif" else "sans-serif-rounded",if(bold) Typeface.BOLD else Typeface.NORMAL)
        if(preferences.getBoolean("readable",false)) { letterSpacing=0.08f; setLineSpacing(dp(6).toFloat(),1.25f) }
        importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    private fun rounded(color: Int, radius: Float=24f) = GradientDrawable().apply { setColor(color); cornerRadius=dp(radius.toInt()).toFloat() }
    private fun button(label: String, parent: LinearLayout = body, action: ()->Unit): Button = EmojiButton(this).apply {
        text=label; textSize=19f; isAllCaps=false; minHeight=dp(68); minimumHeight=dp(68); setTextColor(green)
        background=rounded(Color.rgb(231,238,219)); setPadding(dp(12),dp(12),dp(12),dp(12))
        layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(dp(3),dp(5),dp(3),dp(5)) }
        contentDescription=label
        setOnClickListener { voice.say(label,done=action) }
        setOnLongClickListener { voice.say(label); true }
        onFocusChangeListener=View.OnFocusChangeListener { _, focused -> if(focused) voice.say(label) }
        parent.addView(this)
    }
    private fun sayButton(label: String, value: String, slow: Boolean=false, parent: LinearLayout=body, after: ()->Unit = {}) = button(label,parent) { voice.say(value,slow,done=after) }
    private fun page(title: String, instruction: String, back: Boolean=true) {
        screenId++; rewardGiven=false; busy=false; if(::voice.isInitialized) voice.stop(); if(::mic.isInitialized) mic.stop()
        window.decorView.setBackgroundColor(cream)
        val scroll=ScrollView(this); scroll.isFillViewport=true; scroller=scroll
        body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(16),dp(18),dp(28)) }
        scroll.addView(body); setContentView(scroll)
        // Apply safe drawing insets for edge-to-edge Android 15+ targets.
        scroll.setOnApplyWindowInsetsListener { view,insets -> view.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom); insets }
        scroll.requestApplyInsets()
        if(back) button("🏠 Home") { home() }
        mascot=text("🌱",42f).also { body.addView(it) }
        body.addView(text(title,29f,true))
        note=text(getString(R.string.spoken_prompt,instruction),20f).also { view ->
            view.minHeight=dp(64);view.contentDescription="Hear this slowly"
            view.setOnClickListener { voice.say(note.text.toString(),true) }
            body.addView(view)
        }
        val id=screenId
        handler.postDelayed({ if(screenId==id) voice.say(instruction) },350)
    }
    private fun message(value: String, speak: Boolean=true) {
        note.text=getString(R.string.spoken_prompt,value)
        // Feedback stays visible even after a child has scrolled to the tiles.
        scroller.post { scroller.smoothScrollTo(0,(note.top-dp(12)).coerceAtLeast(0)) }
        if(speak) voice.say(value,true)
    }
    private fun effort(kind: String, item: String, success: Boolean) {
        store.record(kind,item,success)
        if(!rewardGiven) { rewardGiven=true; store.reward(); sessionCount++ }
        mascot.text=if(success) "🌻 ✨" else "🌱 💛"
    }
    private fun endControls(retry: ()->Unit, next: ()->Unit) {
        button("🔁 Practice again") { retry() }
        button(if(sessionCount>=5) "🌼 Next, or take a little break" else "➡ Next little step") { if(sessionCount>=5) breakTime(next) else next() }
        button("⏭ Skip for now") { next() }
    }
    private fun breakTime(next: ()->Unit) {
        page("Look how you’ve grown!", "You have tried five little steps. Would you like a rest?")
        body.addView(text("⭐ ${store.stars()} stars   🌷 🌼 🌸",30f))
        button("🌙 Take a break") { sessionCount=0; home() }
        button("🌱 Keep exploring") { sessionCount=0; next() }
    }
    private fun home() {
        route={home()}; page("Word Garden", "Hello, little explorer! Tap a picture. Let’s grow together.",false)
        body.addView(text("⭐ ${store.stars()}    🌷 Today: ${store.today()} little tries",20f,true))
        body.addView(text((0 until 5).joinToString("  ") { if(it<store.today()) "🌻" else "🌱" },27f))
        button("🌱 My little lesson") { begin("Guided lesson") }
        button("👄 Sound library") { soundLibrary() }
        button("🎲 Play a little game") { gameMenu() }
        button("🌈 Pick a phonics path") { patterns() }
        button("🏡 Word families") { families() }
        button("⭐ My garden") { childProgress() }
        button(if(voice.muted) "🔇 Sound is off" else "🔊 Sound is on") { preferences.edit().putBoolean("mute",!voice.muted).apply(); applySettings(); home() }
        button("👪 Grown-up corner") { parentGate { parentHome() } }
        if(!preferences.getBoolean("setup",false)) {
            body.addView(text("A grown-up can set up an offline voice and optional microphone in the grown-up corner.",16f))
        }
    }
    private fun patterns() {
        route={patterns()}; page("Pick a path", "Tap a picture to choose your sound family.")
        button("🌈 All sounds together") { selectedPattern=null; gameMenu() }
        for(pattern in content.patterns) button("${pattern.picture} ${pattern.title}") { selectedPattern=pattern.id; cursor=0; gameMenu() }
    }
    private fun gameMenu() {
        route={gameMenu()}; page("Little games", "Pick one little game. We will listen together first.")
        selectedPattern?.let { id -> body.addView(text(content.patterns.first { it.id==id }.title,18f)) }
        for(mode in modes) button(mode) {
            when {
                mode.contains("Sound Drill") -> pairDrill()
                mode.contains("Sentence") || mode.contains("Echo") || mode.contains("Read Along") -> sentenceGame(mode)
                else -> begin(mode.substringAfter(' '))
            }
        }
    }
    private fun nextWord(picturesOnly: Boolean=false, starterSounds: Boolean=false): Word {
        val eligible=content.words.filter { (!picturesOnly || it.pictureChoice) && (!starterSounds || it.soundOut) }
        val pool=eligible.filter { selectedPattern==null || it.pattern==selectedPattern }.ifEmpty {eligible}
        // Every third item revisits a word with an earlier uncertain attempt.
        val review=store.rows().firstOrNull { it.kind=="word" && it.attempts>it.successes && pool.any { word -> word.id==it.item } }
        return if(cursor%3==2 && review!=null) pool.first { it.id==review.item } else pool[cursor%pool.size]
    }
    private fun begin(mode: String, word: Word=nextWord(mode.contains("Choose"),mode.contains("Sound It Out")), stage: Int=0) {
        route={begin(mode,word,stage)}
        if(stage>=3) { wordGame(mode,word); return }
        val title=listOf("1 · Listen", "2 · Repeat", "3 · Read")[stage]
        val instructions=listOf("Let’s hear one new word. Tap the speaker.", "Your turn! Say it with me. You can always skip.", "Listen, then tap the word we practiced.")
        page(title,instructions[stage]); body.addView(text(word.picture,64f))
        if(stage>0) body.addView(text(word.word,38f,true))
        val next=button("➡ I’m ready") { begin(mode,word,stage+1) }; next.isEnabled=stage!=0
        fun listened() { store.heard("word",word.id); next.isEnabled=true }
        sayButton("🔊 Listen",word.word,after={listened()})
        sayButton("🐢 Listen slowly",word.word,true,after={listened()})
        button("🧩 Little sound chunks") { voice.sequence(word.syllables,true,done={listened()}) }
        if(!voice.available() || voice.muted || voice.quiet) {
            body.addView(text(word.word,36f,true)); next.isEnabled=true
            body.addView(text("Quiet practice: look, point, and try with a grown-up.",17f))
        }
        if(stage==1) {
            speakAttempt(word.word,"word",word.id, false) { begin(mode,word,2) }
            button("🤫 I said it quietly") { effort("confidence",word.id,true); begin(mode,word,2) }
        }
        if(stage==2) {
            next.visibility=View.GONE
            val choices=(listOf(word)+content.words.filter { it.word!=word.word }.shuffled().take(2)).shuffled()
            for(choice in choices) button("📖 ${choice.word}") {
                if(choice.word==word.word) { effort("reading",word.id,true); begin(mode,word,3) }
                else { effort("reading",word.id,false); message("Let’s listen again together."); voice.say(word.word,true) }
            }
        }
        button("⏭ Skip for now") { cursor++; begin(mode) }
    }
    private fun audioWord(word: Word) {
        body.addView(text(word.picture,56f)); body.addView(text(word.word,36f,true))
        sayButton("🔊 Hear the word",word.word)
        sayButton("🐢 Hear it slowly",word.word,true)
        button("🧩 Say the little chunks") { voice.sequence(word.syllables) }
        sayButton("💡 Hear the meaning",word.meaning,true)
        sayButton("💬 Hear it in a sentence",word.sentence,true)
    }
    private fun wordGame(mode: String, word: Word) {
        route={wordGame(mode,word)}; page(mode, when {
            mode.contains("Choose") -> "Listen. Tap the picture for our word."
            mode.contains("Voice") -> "Say the letters, one at a time. You can also tap them."
            mode.contains("Word") -> "Listen first. Now say the word with me."
            else -> "Let’s build the word we practiced. You can do it."
        })
        audioWord(word)
        fun next() { cursor++; begin(mode) }
        fun check(value: String) {
            val ok=Scoring.normalize(value).replace(" ","")==word.word
            effort("word",word.id,ok)
            if(ok) { message("You built it! Your practice is helping you grow."); button("🌷 Keep growing") { next() } }
            else { message("Let’s listen again together."); voice.sequence(word.syllables) }
        }
        when {
            mode.contains("Choose") -> {
                val options=(listOf(word)+content.words.filter { it.pictureChoice && it.picture!=word.picture && it.word!=word.word }.distinctBy { it.picture }.shuffled().take(2)).shuffled()
                for(option in options) button(option.picture) { if(option.word==word.word) { effort("word",word.id,true); message("You listened carefully! Lovely trying."); button("➡ Next") { next() } } else { effort("word",word.id,false); message("Let’s listen again together."); voice.say(word.word,true) } }
            }
            mode.contains("Say the Word") -> {
                speakAttempt(word.word,"word",word.id,false) { next() }
                button("🤫 I practiced out loud") { effort("confidence",word.id,true); message("Thank you for trying. Your voice matters!") }
                button("👆 Listen and tap instead") { wordGame("Listen & Choose",word) }
            }
            mode.contains("Missing") -> {
                val missing=word.word.length/2
                body.addView(text(word.word.mapIndexed { i,c -> if(i==missing) "＿" else c.toString() }.joinToString(" "),32f,true))
                val letters=(listOf(word.word[missing])+('a'..'z').filter { it!=word.word[missing] }.shuffled().take(2)).shuffled()
                for(letter in letters) button(letter.toString()) { voice.say(letter.toString()); check(if(letter==word.word[missing]) word.word else "") }
            }
            else -> {
                val built=text("",32f,true); body.addView(built)
                var answer=""
                fun update(value: String) {
                    answer=value.take(word.word.length+2); built.text=answer.ifEmpty { "＿" }
                    val letter=value.lastOrNull()?.toString().orEmpty()
                    val sound=content.sounds.firstOrNull { it.letters.split(", ").contains(letter) }
                    if(mode.contains("Sound It Out") && sound!=null) voice.sound(sound.id,sound.example) else voice.say(letter)
                }
                if(mode.contains("Voice")) {
                    val microphone=button("🎤 Say the letters") {}
                    microphone.setOnClickListener {
                        if(busy) return@setOnClickListener
                        if(!mic.permitted() || !mic.supported()) { message("You can tap every letter below. A grown-up can set up the microphone."); return@setOnClickListener }
                        voice.stop(); busy=true; microphone.isEnabled=false
                        val id=screenId
                        mic.listen({ results -> if(screenId==id) { update(Scoring.letters(results.firstOrNull().orEmpty())); message("Here are the letters I heard. You can tap to change them.",false) } },{ if(screenId==id) message(it,false) },{ if(screenId==id) { busy=false; microphone.isEnabled=true } })
                    }
                }
                val keys=if(mode.contains("Type") || mode.contains("Voice")) "abcdefghijklmnopqrstuvwxyz".toList() else word.word.toList().shuffled()
                val grid=GridLayout(this).apply { columnCount=if(resources.configuration.screenWidthDp<380) 4 else 5; alignmentMode=GridLayout.ALIGN_BOUNDS }; body.addView(grid)
                val tileButtons=mutableListOf<Button>()
                val ordered=if(preferences.getBoolean("left",false)) keys.reversed() else keys
                for(letter in ordered) {
                    val tile=Button(this).apply { text=letter.toString(); textSize=25f; isAllCaps=false; setTextColor(green); background=rounded(Color.rgb(237,230,207),15f); minimumWidth=dp(64); minWidth=dp(64); minimumHeight=dp(64); minHeight=dp(64)
                        layoutParams=GridLayout.LayoutParams().apply { width=dp(64); height=dp(68); setMargins(dp(1),dp(3),dp(1),dp(3)) }
                    }
                    grid.addView(tile); tileButtons.add(tile)
                    fun add() { update(answer+letter); if(!mode.contains("Type") && !mode.contains("Voice")) tile.isEnabled=false }
                    tile.setOnClickListener { add() }
                    tile.setOnLongClickListener { tile.startDragAndDrop(ClipData.newPlainText("letter",letter.toString()),View.DragShadowBuilder(tile),tile,0); true }
                }
                built.minHeight=dp(72); built.background=rounded(Color.rgb(240,242,225))
                built.setOnDragListener { _,event -> when(event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> event.clipDescription?.label=="letter"
                    DragEvent.ACTION_DROP -> { val tile=event.localState as? Button; if(tile?.isEnabled==true) { tile.performClick(); true } else false }
                    else -> true
                } }
                button("⌫ Try those letters again") { answer=""; built.text="＿"; tileButtons.forEach { it.isEnabled=true } }
                button("🌱 Check my word") { check(answer) }
            }
        }
        endControls({wordGame(mode,word)},{next()})
    }
    private fun speakAttempt(expected: String, kind: String, item: String, sentence: Boolean, next: ()->Unit) {
        val heard=text("",22f); body.addView(heard)
        val button=button("🎤 My turn to speak") {}
        button.setOnClickListener {
            if(busy) return@setOnClickListener
            if(!mic.permitted() || !mic.supported()) { message("Listen and tap works too. A grown-up can set up your microphone."); return@setOnClickListener }
            voice.stop(); busy=true; button.isEnabled=false; val id=screenId
            mic.listen({ candidates ->
                if(screenId!=id) return@listen
                val transcript=candidates.maxByOrNull { if(sentence) Scoring.align(expected,it).count { value->value } else if(Scoring.matches(expected,it)) 1 else 0 }.orEmpty()
                heard.text=getString(R.string.heard_transcript,transcript)
                val alignment=Scoring.align(expected,transcript)
                val ok=if(sentence) alignment.all { it } else Scoring.matches(expected,transcript)
                effort(kind,item,ok)
                if(sentence) {
                    val tokens=expected.split(" ")
                    heard.text=tokens.mapIndexed { i,word -> if(alignment.getOrElse(i){false}) "🌿 $word" else "🌱 $word" }.joinToString("  ")
                }
                if(ok) { message("I heard your lovely effort. Let’s keep growing!"); button("🌷 Next little step") { next() } }
                else {
                    message("Lovely trying. Let’s listen again together.",false)
                    val missing=if(sentence) expected.split(" ").filterIndexed { index,_->!alignment.getOrElse(index){false} } else content.words.find { it.word==expected }?.syllables ?: listOf(expected)
                    voice.sequence(missing,true)
                    val confusion=content.pairs.firstOrNull { pair -> (expected==pair.a && candidates.any {Scoring.normalize(it)==pair.b}) || (expected==pair.b && candidates.any {Scoring.normalize(it)==pair.a}) }
                    if(confusion!=null) {
                        store.record("contrast",confusion.sound,false)
                        val tries=store.rows().first {it.kind=="contrast" && it.item==confusion.sound}
                        if(tries.attempts-tries.successes>=2) button("👄 Try our two little sounds") {pairDrill(confusion)}
                    }
                    if(!sentence) {
                        val record=store.rows().find { it.kind==kind && it.item==item }
                        if(record!=null && record.attempts-record.successes>=2) button("👄 Help my mouth learn") { val sound=content.sounds.firstOrNull { expected.startsWith(it.letters.substringBefore(',')) }; if(sound!=null) soundDetail(sound) else pairDrill() }
                    }
                }
            },{if(screenId==id) message(it,false)},{if(screenId==id) {busy=false; button.isEnabled=true}}, partial={ transcript ->
                if(screenId==id && sentence) {
                    val aligned=Scoring.align(expected,transcript)
                    val display=android.text.SpannableString(expected)
                    var offset=0
                    expected.split(" ").forEachIndexed { i,word ->
                        if(aligned.getOrElse(i){false}) display.setSpan(android.text.style.BackgroundColorSpan(Color.rgb(212,235,184)),offset,offset+word.length,0)
                        offset+=word.length+1
                    }
                    heard.text=display
                }
            })
        }
        if(!mic.supported()) body.addView(text("On this device, we use listen and tap. No microphone is needed.",16f))
    }
    private fun sentenceGame(mode: String, sentence: Sentence?=null, modeled: Boolean=false) {
        val pool=content.sentences.filter { selectedPattern==null || it.pattern==selectedPattern }.ifEmpty { content.sentences }
        val item=sentence ?: pool[cursor%pool.size]
        route={sentenceGame(mode,item,modeled)}
        page(mode.substringAfter(' '),"First listen. Then we will try one little phrase.")
        body.addView(text(item.picture,52f))
        val sentenceWords=item.text.split(" ")
        val wordViews=mutableListOf<TextView>()
        val line=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; body.addView(line)
        // Individual 64dp word buttons form a readable one-handed vertical list.
        sentenceWords.forEach { word ->
            val view=text(word,28f,true).apply { minHeight=dp(64); background=rounded(Color.rgb(239,242,226)); setOnClickListener { voice.say(word,true) }; contentDescription="Listen to $word" }
            line.addView(view,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(4) }); wordViews.add(view)
        }
        fun highlight(index: Int) { wordViews.forEachIndexed { i,view -> view.setBackground(rounded(if(i==index) Color.rgb(241,213,139) else Color.rgb(239,242,226))) } }
        fun next() { cursor++; sentenceGame(mode) }
        sayButton("🔊 Listen normally",item.text)
        button("🐢 Listen and follow") { voice.sequence(sentenceWords,true,{highlight(it)},{store.heard("sentence",item.id); if(!modeled) sentenceGame(mode,item,true)}) }
        sayButton("💡 Hear the meaning",item.meaning,true)
        if(!modeled && voice.available() && !voice.muted && !voice.quiet) {
            body.addView(text("Tap the turtle to hear every word first.",20f)); button("⏭ Skip for now") { next() }; return
        }
        when {
            mode.contains("Build") -> {
                val built=text("Tap or drag the word tiles here.",25f); built.minHeight=dp(72); body.addView(built)
                val selected=mutableListOf<String>(); val tiles=mutableListOf<Button>()
                for(word in sentenceWords.shuffled()) {
                    val tile=button(word) { }
                    tiles.add(tile)
                    tile.setOnClickListener { selected.add(word); built.text=selected.joinToString(" "); tile.isEnabled=false; voice.say(word) }
                    tile.setOnLongClickListener { tile.startDragAndDrop(ClipData.newPlainText("word",word),View.DragShadowBuilder(tile),tile,0); true }
                }
                built.setOnDragListener { _,event -> when(event.action) { DragEvent.ACTION_DRAG_STARTED -> event.clipDescription?.label=="word"; DragEvent.ACTION_DROP -> { val tile=event.localState as? Button; if(tile?.isEnabled==true) tile.performClick(); true };else -> true } }
                button("⌫ Start my sentence again") { selected.clear(); built.text=""; tiles.forEach { it.isEnabled=true } }
                button("🌱 Check my sentence") { val ok=selected==sentenceWords; effort("sentence",item.id,ok); if(ok) message("Your sentence is blooming!") else { message("Let’s listen again together.",false); voice.sequence(sentenceWords,true,{highlight(it)}) } }
            }
            mode.contains("Echo") -> {
                var phraseIndex=0
                val phrase=text(item.phrases[0],28f,true); body.addView(phrase)
                button("🔊 Hear my little phrase") { voice.say(item.phrases[phraseIndex],true) }
                button("🎤 Echo my phrase") {
                    if(!mic.supported() || !mic.permitted()) { message("Say it quietly, then tap the leaf. Your trying counts!"); return@button }
                    if(busy) return@button
                    voice.stop(); busy=true; val id=screenId
                    mic.listen({results -> if(screenId==id) { val ok=results.any { Scoring.align(item.phrases[phraseIndex],it).all { match->match } }; effort("echo",item.id,ok); if(ok) message("Lovely echo! Tap the leaf for the next phrase.") else { message("Let’s listen again together.",false); voice.say(item.phrases[phraseIndex],true) } } },{if(screenId==id)message(it,false)},{if(screenId==id)busy=false})
                }
                button("🌿 I tried. Next little phrase") { effort("confidence",item.id,true); if(phraseIndex<item.phrases.lastIndex) { phraseIndex++; phrase.text=item.phrases[phraseIndex]; voice.say(item.phrases[phraseIndex],true) } else { voice.say(item.text,true); message("You practiced every part. Now you know the whole sentence!",false) } }
            }
            mode.contains("Read Along") -> {
                button("📚 Read with my voice") { voice.sequence(sentenceWords,false,{highlight(it)},{effort("fluency",item.id,true); message("We read it together. Beautiful effort!")}) }
                button("🤫 I followed quietly") { effort("confidence",item.id,true); message("Every little try helps you grow.") }
            }
            else -> {
                speakAttempt(item.text,"sentence",item.id,true) { next() }
                button("👆 Listen and tap instead") { sentenceGame("🧱 Build the Sentence",item,true) }
                button("🤫 I practiced quietly") { effort("confidence",item.id,true); message("Thank you for trying. You are growing!") }
            }
        }
        endControls({sentenceGame(mode,item,true)},{next()})
    }
    private fun soundLibrary() {
        route={soundLibrary()}; page("Sound garden", "Choose one little sound. We will listen and try together.")
        content.sounds.forEach { sound -> button("${sound.picture} ${sound.letters}  /${sound.ipa}/") { soundDetail(sound) } }
    }
    private fun soundDetail(sound: Sound) {
        route={soundDetail(sound)}; page("${sound.picture}  /${sound.ipa}/", "Watch the mouth. Listen to our sound in a word.")
        body.addView(MouthView(this,sound.mouth),LinearLayout.LayoutParams(-1,dp(180)))
        body.addView(text(sound.cue,23f)); body.addView(text("${sound.letters}  ·  ${sound.example}",27f,true))
        button("👄 Hear the sound") { voice.sound(sound.id,sound.example) }
        button("🐢 Hear the sound slowly") { voice.sound(sound.id,sound.example,true) }
        sayButton("🔊 Hear the example",sound.example)
        sayButton("🐢 Hear it slowly",sound.example,true)
        sayButton("👄 Help my mouth",sound.cue,true)
        speakAttempt(sound.example,"sound",sound.id,false) { soundLibrary() }
        button("🌱 I tried the sound") { effort("sound-practice",sound.id,true); message("That was a brave little try!") }
        button("👂 Listen and find it") {
            page("Find our sound", "Listen. Tap the picture we practiced.")
            sayButton("🔊 Listen",sound.example,true)
            val options=(listOf(sound)+content.sounds.filter { it.example!=sound.example && it.picture!=sound.picture }.shuffled().take(2)).shuffled()
            options.forEach { option -> button(option.picture) { val ok=option.id==sound.id; effort("sound",sound.id,ok); if(ok) message("You listened so carefully!") else {message("Let’s listen again together.",false);voice.say(sound.example,true)} } }
            endControls({soundDetail(sound)},{soundLibrary()})
        }
        endControls({soundDetail(sound)},{soundLibrary()})
    }
    private fun pairDrill(pair: PairDrill=content.pairs[cursor%content.pairs.size]) {
        route={pairDrill(pair)}; page("Two little sounds", "Let’s listen to two words. Notice how your mouth moves.")
        body.addView(text(pair.sound,30f,true)); body.addView(MouthView(this,if(pair.sound.startsWith("v")||pair.sound.startsWith("f")) "teeth" else "open"),LinearLayout.LayoutParams(-1,dp(170)))
        body.addView(text(pair.cue,22f)); sayButton("👄 Hear the mouth tip",pair.cue,true)
        sayButton("🔊 ${pair.a}",pair.a); sayButton("🐢 ${pair.a} slowly",pair.a,true)
        sayButton("🔊 ${pair.b}",pair.b); sayButton("🐢 ${pair.b} slowly",pair.b,true)
        var target=if(Random.nextBoolean()) pair.a else pair.b
        button("👂 Listen. Which word?") { voice.say(target,true) }
        for(value in listOf(pair.a,pair.b)) button("👆 $value") { val ok=value==target; effort("contrast",pair.sound,ok); if(ok) {message("You noticed the little difference!");target=if(Random.nextBoolean())pair.a else pair.b} else {message("Let’s listen again together.",false); voice.say(target,true)} }
        speakAttempt(pair.a,"contrast",pair.sound,false) { cursor++; pairDrill() }
        endControls({pairDrill(pair)},{cursor++;pairDrill()})
    }
    private fun families() {
        route={families()}; page("Word families", "Words can share a little ending. Pick a house.")
        for(ending in listOf("at","an","ap","ag","en","et","ig","in","ip","it","og","op","ot","ug","un","ut","ake","ame","ine","ight","ay","ee","ow")) {
            val words=content.words.filter { it.word.endsWith(ending) }
            if(words.size>1) button("🏡  -$ending") { family(ending,words) }
        }
    }
    private fun family(ending: String, words: List<Word>) {
        route={family(ending,words)}; page("The -$ending family", "These words share an ending. Tap to hear each friend.")
        for(word in words) button("${word.picture} ${word.word}") { voice.say(word.word,true); store.heard("word",word.id) }
        button("🌱 Practice one friend") { begin("Guided lesson",words.random()) }
        endControls({family(ending,words)},{families()})
    }
    private fun childProgress() {
        route={childProgress()}; page("Your growing garden", "Every little try grows your garden.")
        body.addView(text("⭐ ${store.stars()} stars",36f,true))
        val stickers=listOf("🌱","🌷","🦋","🌈","🦁","🚀","🌻","🐝")
        body.addView(text(stickers.take((store.stars()/5+1).coerceAtMost(stickers.size)).joinToString("  "),40f))
        body.addView(text("${store.today()} little tries today. No rush. No losing.",23f))
        button("🌱 Grow another word") { begin("Guided lesson") }
        button("👪 See learning details") { parentGate { progressSummary() } }
        button("🔁 Practice again") { begin("Guided lesson") }
    }
    private fun parentGate(action: ()->Unit) {
        // A fresh challenge is required each time settings are entered.
        val a=Random.nextInt(11,20); val b=Random.nextInt(4,10)
        page("Grown-ups only", "Please ask a grown-up to help with this little puzzle.")
        body.addView(text("$a + $b = ?",32f,true))
        val answer=EditText(this).apply { inputType=InputType.TYPE_CLASS_NUMBER; textSize=28f; minHeight=dp(64); hint="Answer"; contentDescription="Parent puzzle answer" }; body.addView(answer)
        button("🔑 Open grown-up corner") { if(answer.text.toString().toIntOrNull()==a+b) { parentUntil=System.currentTimeMillis()+5*60_000; action() } else message("Please ask your grown-up to try the puzzle.") }
        button("🌱 Back to my garden") { home() }
    }
    private fun parentAllowed(action: ()->Unit): Boolean {
        if(System.currentTimeMillis()>parentUntil) { parentGate(action);return false }; return true
    }
    private fun parentHome() {
        if(!parentAllowed {parentHome()})return
        route={parentHome()};page("Grown-up corner","A calm place to set up learning. No accounts, ads, purchases, or audio uploads.")
        body.addView(text("Offline voice: ${if(voice.available()) "ready" else "not installed"}\nOn-device recognition: ${if(mic.supported()) "available" else "unavailable; tap games remain available"}\nMicrophone permission: ${if(mic.permitted()) "allowed" else "not granted"}",18f))
        body.addView(text(voiceStatus,17f))
        button("⚙ Learning settings") { settings() }
        button("📊 Learning summary") { progressSummary() }
        button("🎤 Set up microphone") {
            if(!parentAllowed {parentHome()})return@button
            AlertDialog.Builder(this).setTitle("Optional on-device microphone").setMessage("Your child can play without a microphone. When enabled, speech is processed only by Android’s on-device recognizer. This app has no Internet permission and never saves audio. If an offline engine is unavailable, listen-and-tap is used. Android 12 or later is required for on-device recognition.")
                .setPositiveButton("Allow on-device mic") { _,_-> if(mic.supported()) requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),10) else message("This device has no on-device recognizer. All listen-and-tap activities are available.") }.setNegativeButton("Use listen and tap",null).show()
        }
        button("🔊 Android offline voice settings") {
            if(!parentAllowed {parentHome()})return@button
            try { startActivity(Intent("com.android.settings.TTS_SETTINGS")) } catch (_:Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        }
        body.addView(text("Choose an installed English voice that works in airplane mode. Voice data may need a one-time download in Android settings. The app never selects a network voice. Sound examples reflect a broad British-English phoneme inventory; accents vary. Transcript feedback is not a clinical assessment of pronunciation.",17f))
        button("✅ Setup is done") { preferences.edit().putBoolean("setup",true).apply(); parentUntil=0; home() }
        button("📜 Audio credits") {
            if(!parentAllowed {parentHome()})return@button
            page("Audio credits","Recorded sounds come from the Wikipedia IPA charts, mirrored by Josh Stephenson’s PhoneticFlashCards. Licensed CC BY-SA 3.0. Original file names and source references are in the bundled audio_attribution.json. Some sounds use your installed offline English voice.")
            body.addView(text("Sources: Wikipedia IPA vowel and consonant charts; github.com/joshstephenson/PhoneticFlashCards. License: creativecommons.org/licenses/by-sa/3.0. Original source links and any format-conversion notes are included in audio_attribution.json with this app’s source code.",17f))
            button("👪 Grown-up corner") {parentHome()}
        }
    }
    private fun settings() {
        if(!parentAllowed {settings()})return
        route={settings()};page("Learning settings","Choose what feels comfortable for your child.")
        for((key,label) in listOf("mute" to "🔇 Mute speech", "quiet" to "🤫 Quiet mode: visual and tap practice", "slow" to "🐢 Slower speech everywhere", "readable" to "📖 Spaced, readable font", "left" to "🤚 Left-handed keyboard")) {
            val enabled=preferences.getBoolean(key,false)
            button("${if(enabled) "☑" else "☐"} $label") { if(parentAllowed {settings()}) {preferences.edit().putBoolean(key,!enabled).apply();applySettings();settings()} }
        }
        button("🧹 Reset learning progress") { if(parentAllowed {settings()}) AlertDialog.Builder(this).setTitle("Reset local progress?").setMessage("This removes stars and learning counters from this device.").setPositiveButton("Reset") {_,_->store.clear();cursor=0;settings()}.setNegativeButton("Keep progress",null).show() }
        button("👪 Back to grown-up corner") { parentHome() }
    }
    private fun progressSummary() {
        if(!parentAllowed {progressSummary()})return
        route={progressSummary()};page("Learning summary","Effort comes first. These are practice signals, not school grades.")
        val rows=store.rows();val speaking=rows.filter { it.kind=="confidence" || it.kind=="echo" || it.kind=="sentence" }
        val mastered=rows.count { it.kind=="word" && it.successes>=3 }
        body.addView(text("${content.words.size} words · ${content.sentences.size} sentences · 44 sounds\n${store.stars()} effort stars · $mastered words practiced successfully 3+ times\n${speaking.sumOf {it.attempts}} speaking/reading practice attempts",19f))
        body.addView(text("🌱 Sounds to revisit",24f,true))
        val soundRows=rows.filter { it.kind=="sound" || it.kind=="contrast" }
        if(soundRows.isEmpty()) body.addView(text("No sound checks yet. Start with one friendly sound.",17f))
        for(row in soundRows) {
            val label=content.sounds.find { it.id==row.item }?.let { "/${it.ipa}/ · ${it.example}" } ?: row.item
            button("$label: ${row.successes}/${row.attempts} matched") { val sound=content.sounds.find { it.id==row.item };if(sound!=null)soundDetail(sound)else pairDrill() }
        }
        body.addView(text("🌷 Word practice",24f,true))
        for(row in rows.filter { it.kind=="word" }.take(60)) button("${row.item}: ${row.successes}/${row.attempts} matched${if(row.successes>=3) " · growing strong" else " · keep exploring"}") { content.words.find { it.id==row.item }?.let {begin("Guided lesson",it)} }
        body.addView(text("📚 Sentence reading",24f,true))
        for(row in rows.filter {it.kind=="sentence" || it.kind=="fluency" || it.kind=="echo"}.take(30)) body.addView(text("${content.sentences.find {it.id==row.item}?.text ?: row.item}: ${row.successes}/${row.attempts} supported tries",17f))
        body.addView(text("Speaking confidence: ${rows.filter {it.kind=="confidence"}.sumOf {it.attempts}} self-reported tries.\nReading fluency uses supported sentence completion; no speed pressure or diagnostic words-per-minute score. Empty areas mean not yet practiced.",17f))
        button("👪 Back to grown-up corner") {parentHome()}
    }
    // API 33+ uses the native predictive-back callback registered in onCreate.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Deprecated("Legacy API 24–32 back callback") override fun onBackPressed() { home() }
    override fun onPause() { super.onPause();voice.stop();mic.stop();busy=false;parentUntil=0 }
    override fun onResume() { super.onResume(); if(::voice.isInitialized)voice.refresh() }
    override fun onDestroy() { voice.close();mic.stop();store.close();handler.removeCallbacksAndMessages(null);super.onDestroy() }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) { super.onRequestPermissionsResult(requestCode,permissions,grantResults); if(requestCode==10)parentGate {parentHome()} }
}
