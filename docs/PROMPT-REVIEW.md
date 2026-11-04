# Requested app versus the original game

The original delivery was a browser prototype with 8 spelling words, 3 reading stories, browser speech recognition, local browser counters, and optional cloud speech. It did not meet the Android, curriculum, offline/privacy, phonics, guided-teaching, game-variety, or parent-control requirements in the supplied prompt.

The native implementation is separate, in `android/`, so the working web game is preserved.

| Requirement | Native Android implementation |
| --- | --- |
| Installable Android app | Kotlin source and signed testing APK; Android 7+; target API 37, compile platform 37.2 |
| 400–600 words, 100–200 sentences | 508 words, 120 sentences, 12 groups, editable local JSON and SQLite content rows |
| All 44 English sounds | 44 entries, placement cues, drawn mouth diagrams, examples, 36 bundled sound recordings; eight glides modeled through an offline spoken example |
| Listen, repeat, read, spell | Guided word sequence before the chosen word game; speech attempts can be skipped |
| Normal/slow and syllable speech | Offline word and sentence models, slow audio, syllable chunks for longer words |
| Listening/reading games | Listen & Choose, starter-word Sound It Out, Missing Letter, tile spelling, custom typing keyboard, sentence building, Read Along |
| Speaking games | Say the Word, Echo Reading, Read the Sentence, Spell by Voice, minimal-pair Sound Drill |
| Gentle feedback/repetition | Model replay on an uncertain attempt, review of earlier words, no red failure marks, timers, losing, or lives |
| Recognition and fair matching | On-device recognizer on compatible Android 12+ devices; Levenshtein/phonetic transcript matching, letter names, sentence alignment, partial highlights where provided |
| Minimal pairs/coaching | v/w, p/b, t/d, s/sh, th/t, short/long vowels and f/v; repeated observed contrasts offer mouth-tip practice |
| No mic / denied / noise | Listen-and-tap alternatives and gentle retry; microphone is optional |
| Local-only privacy | No Internet permission, no recorded audio files, no analytics, private SQLite/preferences, backup/transfer excluded |
| Parent control | Arithmetic gate for settings, permission setup, voice settings and progress; confirmation for reset |
| Rewards/progress | Effort stars, earned stickers, daily five-step garden, breaks, word/sound/sentence practice counters and parent summary |
| Readable child UI | Rounded high-contrast theme, minimum 64dp buttons/tiles, icons plus spoken labels, bundled emoji font, quiet/mute, spaced font and mirrored keyboard option |
| Teacher-editable curriculum | JSON assets, generator source, curriculum validation, content-editing instructions |
| Repeatable development | Checksum-pinned Gradle wrapper, JVM/content tests, Android lint, optional emulator smoke script, GitHub Actions APK build |

## Exact limits

- The eight diphthongs use an offline spoken example and a mouth/glide cue; they do not have dedicated recorded isolated glide audio. Normal/slow playback is available. The other 36 sound entries have bundled recordings.
- Recognition returns text rather than acoustic phoneme measurements. Sound accuracy and reading fluency are practice proxies. The app cannot diagnose articulation or measure true phoneme quality from a transcript. This is explained in the parent summary.
- Android 7–11, and devices without an installed compatible on-device English recognizer, use listen-and-tap. No Vosk/Whisper model is bundled. This is deliberate: the app never switches to an online recognizer.
- Offline word/instruction speech depends on an installed English offline TTS voice. A parent may need to download that through Android settings once. The app itself cannot download voices and does not use ElevenLabs.
- The APK is debug-signed for direct testing/installation. Play Store publishing and production signing are not included.
- The generated curriculum uses simple repeated sentence frames and generic practice-word prompts. The JSON can be customized by a teacher; automatic speech matching is not a substitute for a reading specialist.

See `android/README.md` for build, install, voice setup, privacy, source tree, credits, and content editing instructions. Test evidence is recorded separately in `docs/ANDROID-VALIDATION.md`.
