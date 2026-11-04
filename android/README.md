# Word Garden for Android

A native Kotlin game for children aged 7–8, with offline lessons and an optional on-device microphone. The web game at the repository root is a separate, earlier application. Its ElevenLabs integration is **not used by this APK**.

## Install on a phone

Download `releases/WordGarden-debug.apk` from this repository onto an Android phone. Open the downloaded file and, if Android asks, allow that browser or file manager to install this app. This is a signed **debug/testing APK**, not a Play Store release. Android 7.0 (API 24) or newer is required. No account or API key is needed.

First, a grown-up should open **Grown-up corner**, solve the addition question, and check the offline English voice. If necessary, install English voice data through Android's text-to-speech settings, return to the app, and try a lesson in airplane mode. The app selects an installed English voice with no network requirement. Microphone setup is optional and parent-gated.

The APK contains **508 words, 120 sentences, 44 sound entries, 12 phonics patterns, 10 minimal-pair drills, 36 recorded sound demonstrations, and a bundled emoji font**. The eight gliding sounds use the offline voice's example word and mouth guidance rather than an isolated recording. Long vowel examples share the underlying recorded vowel. Voice quality depends on the device's installed offline speech engine.

## Build and validate

Use JDK 21, Android SDK platform **37.2**, and platform/build tools installed by Android Studio. Open this `android/` folder in Android Studio, or set `ANDROID_HOME` to your SDK directory (alternatively create ignored `local.properties` containing `sdk.dir=/absolute/sdk/path`). The app targets API 37, with minimum API 24. Gradle **9.8.2** is pinned by the wrapper with its official SHA256; Android Gradle Plugin **9.5.0** provides Kotlin support.

```sh
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Windows: use `gradlew.bat`. The output is `app/build/outputs/apk/debug/app-debug.apk`. Gradle downloads build dependencies on the first build. The installed game needs no Internet connection. For a store release, provide your own signing configuration through a separate publishing task; never commit private signing keys.

Curriculum checks, from the repository root:

```sh
python3 tools/check_android_content.py
```

Optional native smoke checks: boot a disposable emulator, install the debug APK, then run `ADB=/path/to/adb python3 tools/android_smoke.py`. This exercises the app and changes local emulator progress, so do not use a child's device. The script expects a 480×800 test screen and uses the current UI labels.

## Learning flow and games

Every word begins with **Listen → Repeat → Read → Spell/play**. The child hears the model before being asked to identify or spell it. Repeating aloud may be skipped; quiet mode allows visual and tap practice. Incorrect attempts receive a gentle model, not a penalty, and earlier uncertain words are revisited every third item. Normal, slow, and syllable audio controls are available for words. Sentences support normal speech and a slow word-by-word model.

- Guided Lesson, Listen & Choose, Sound It Out, Missing Letter, Spell It, Type It.
- Say the Word, Echo Reading, Read the Sentence, Spell by Voice, Sound Drill.
- Build the Sentence and Read Along with audio-following highlights.
- Sound Library with mouth diagrams, spoken placement tips, examples, recordings, and listen-and-tap practice.
- Word Families including -at, -op, -ight, and other shared endings.

Sound It Out uses regular three-letter starter words so a separate sound model for each letter is appropriate; complex spelling patterns use their whole-word/syllable models and sound-library cues instead.

Tap a letter/word tile, or long-press and drag it into the answer area. Type It uses a large on-screen alphabet keyboard with no autocorrect. Left-handed settings mirror the letter order. Tap the speaker-marked instruction or feedback text to hear it slowly. Parents can also enable slower speech everywhere. Lesson position and the chosen pattern persist across restarts. Menu buttons speak their labels before opening; long-pressing a normal button repeats its label. The reading option uses a system sans-serif font with extra letter and line spacing. Quiet mode suppresses speech; mute is available from Home.

Stars reward an attempt, not only a match, once per exercise screen. There are no timers, lives, losing states, ads, purchases, external child-facing links, or streak penalties. After five rewarded steps, the child can choose a break. Stickers and a five-step daily garden show effort.

## Microphone, privacy, and fallback

The manifest contains **no Internet permission**. Audio is never written to storage. Android 12/API 31 and newer are checked with `isOnDeviceRecognitionAvailable`; the app uses **only** `createOnDeviceSpeechRecognizer`, never the potentially online general recognizer. The system must already have a suitable offline English recognition engine/model. When the service/model is missing, the permission is denied, or the room is noisy, the app offers a friendly retry and listen-and-tap alternatives. Older Android versions always use the alternatives.

A parent must solve the arithmetic gate before the app requests microphone permission. Parents can also revoke permission in Android settings. No microphone permission is needed for the other games. Some device manufacturers do not provide on-device English recognition; the app does not download or bundle Vosk or Whisper as a substitute.

`Scoring.kt` combines normalized transcripts, Levenshtein matching, a small spelling-based phonetic layer with common recognizer homophones (such as see/sea), sequence alignment for missed sentence words, and letter-name parsing. Short-word contrasts are kept distinct so “vet/wet” and “ship/sheep” do not receive automatic matches. Partial transcripts highlight recognized sentence words when the device supplies them. Missed words are replayed; repeated known contrasts offer targeted practice.

**Limit:** transcript matching cannot measure tongue position or acoustic phoneme accuracy. Sound-practice scores are transcript/proxy measures, not clinical pronunciation assessments. Reading fluency is tracked as supported sentence practice, without words-per-minute testing. Confidence counts explicit self-reported tries. This distinction is also shown to parents.

SQLite stores anonymous per-item attempts, matches, heard flags, curriculum rows, daily stars, and practice counts. Settings stay in private local preferences. Cloud backup and device transfer are excluded. Reset is protected by the parent gate and a second confirmation. Nothing is sent to a server, and there are no analytics SDKs.

## Edit lesson content

The canonical editable files are in `app/src/main/assets/`:

- `words.json`: `id`, `word`, `pattern`, `level`, `picture`, `meaning`, `syllables`, `sentence`, `pictureChoice`, `soundOut`.
- `sentences.json`: `id`, `text` (3–7 words), `pattern`, `picture`, `meaning`, `phrases`.
- `phonemes.json`: all 44 entries with IPA, letter patterns, example, cue and mouth shape.
- `patterns.json` and `minimal_pairs.json`: groups and contrast drills.

Use unique stable IDs. Syllable chunks must join back to the spelling. `pictureChoice` should be true only if the emoji clearly represents the word. Family/category icons for abstract words are cues, not picture-answer choices. `soundOut` should be true only for regular three-letter words with one simple sound per letter. Rebuild after editing assets. Bundled JSON is also seeded into the private SQLite content table.

`tools/make_android_content.py` is the authoring source used to generate the JSON. Editing it and running it regenerates those five JSON files, overwriting manual JSON edits; choose either workflow deliberately. `audio_attribution.json` and the `phoneme_audio/` directory are independent of that generator.

The sound inventory follows a traditional broad British-English teaching model; accents differ. The curriculum is a starting teaching resource, with simple generated practice sentences and reusable word prompts. A teacher can replace example sentences, picture cues, or local pronunciations in the JSON. Do not treat transcript matches as an accent-correction score.

## Project tree

```text
android/
├── build.gradle.kts, settings.gradle.kts, gradle.properties
├── gradlew, gradlew.bat, gradle/wrapper/
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── assets/ (curriculum JSON, phoneme_audio/, attribution)
        │   ├── res/ (theme, icon, backup rules)
        │   └── java/com/wordgarden/app/
        │       ├── MainActivity.kt (screens and teaching flow)
        │       ├── Content.kt (asset models)
        │       ├── ProgressStore.kt (SQLite and local rewards)
        │       ├── OfflineVoice.kt (offline TTS and recorded sounds)
        │       ├── LocalRecognizer.kt (on-device speech only)
        │       ├── Scoring.kt (matching and sentence alignment)
        │       └── MouthView.kt (mouth-position cues)
        └── test/java/com/wordgarden/app/ScoringTest.kt
```

## Audio and font notices

Recorded phones are from the Wikipedia IPA charts, mirrored by `joshstephenson/PhoneticFlashCards`, with the /ʌ/ recording mirrored by `mcrombie/phoneme-chart`. See bundled `audio_attribution.json` for per-file source links. The recordings are attributed to the original Wikimedia contributors under Creative Commons Attribution-Share-Alike 3.0: https://creativecommons.org/licenses/by-sa/3.0/. MP3 files are redistributed unchanged except /ʌ/, converted from Ogg to MP3. These sound assets remain under that license. Credits are available in the parent corner.

AndroidX Emoji2 is Apache-2.0; its bundled Noto emoji font is distributed with the upstream font license. Dependencies and font are packaged by the official AndroidX library, with their license metadata retained in the APK.
