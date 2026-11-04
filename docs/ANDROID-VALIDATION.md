# Android validation

## Build and data checks

- `./android/gradlew -p android --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`: passed using the checked-in wrapper, JDK 21, AGP 9.5.0 and SDK 37.2.
- Kotlin scoring tests: **9 passed, 0 failed, 0 skipped**. Covers edit-distance tolerance, short minimal-pair protection, recognizer homophones, missing/extra sentence-word alignment, empty speech, and spoken letter names.
- Android lint: **no issues found**.
- `python3 tools/check_android_content.py`: **5 passed**. Checks inventory bounds, unique words, syllable reconstruction, pattern references, safe starter words for sound-by-sound play, short sentence/echo structure, all 44 sound cues, audio files, and the source privacy boundary.
- All **36** bundled MP3 sound files decoded successfully with FFmpeg.
- Existing web game regression checks: **4 game tests + 3 speech-service tests passed**; JavaScript syntax checks passed. No live ElevenLabs request was required for this Android work.
- Official Gradle distribution SHA256 is pinned in the wrapper; generated wrapper JAR matched the official SHA256. SDK packages were installed with the official package manager. JDK and downloaded Gradle archives were checked against their published checksums.

## APK checks

- `apksigner verify`: passed (APK Signature Scheme v2, compatible with the minimum API 24).
- `aapt dump badging`: package `com.wordgarden.app`, version `1.0.0`, min SDK **24**, target SDK **37**.
- Packaged permissions: **RECORD_AUDIO only**. No Internet or external-storage permissions.
- Emoji2's downloadable-font initializer is removed from the merged manifest; the application explicitly initializes the bundled font.
- Install and launch on an Android 7/API 24 x86_64 emulator: passed.
- `releases/SHA256SUMS` identifies the exact copied APK; run `sha256sum -c releases/SHA256SUMS` from the repository root.

## Functional emulator checks

Using a disposable 480×800 Android 7 emulator:

- Guided listen/repeat/read/spell flow and spelling `cat` using letter tiles: passed.
- Parent arithmetic gate and quiet-mode preference: passed.
- Sound library, mouth cue and offline-sound playback control: passed.
- Microphone-unavailable message and continued access to tap practice on API 24: passed.
- Sentence building with word tiles in the selected pattern: passed.
- Safe starter-word selection under an advanced phonics pattern: passed.
- Chosen pattern and word position survived force-stop/relaunch: passed.
- Force-stop/relaunch preserved the numeric star count: passed (observed 8 before and 8 after relaunch using fresh accessibility dumps).

The initial UI run exposed feedback outside the viewport after scrolling; this was corrected by bringing feedback back into view.

## Not established by these checks

- Acoustic pronunciation quality, microphone accuracy, model availability, and partial-recognition behavior on a physical API 31+ phone. The emulator verifies the supported no-microphone path, not a real child's speech.
- Every drag gesture or every possible curriculum item through the UI. Curriculum integrity is checked across the entire bank; the UI checks exercise representative shared paths.
- Audible speaker quality: the headless emulator runs without an audio device. Audio assets were decoded and controls exercised; naturalness depends on the installed offline TTS voice.
- Play Store publication, production signing, fresh cloud-task restoration, or a published environment snapshot. The saved setup/start draft is separate from publication.

Exact implementation limits and setup steps are in `PROMPT-REVIEW.md` and `../android/README.md`.
