# Word Garden

A responsive spelling, reading, and pronunciation game. Earn stars, explore short stories, and build a daily practice habit. Progress stays in this browser using local storage.

## Run

Requires Node.js 24.5 or newer. No packages, database, or build step are needed. An optional ElevenLabs API key enables natural AI speech.

```sh
npm start
```

The server listens on port 3000; override with `PORT=3001 npm start`.

## Check

```sh
npm test
npm run check
```

Speech synthesis depends on installed browser voices. Microphone practice uses the browser's SpeechRecognition API, which is not available everywhere and may use a browser-managed online service. Microphone access requires a secure context (HTTPS or localhost). A manual practice option is always available; speech recognition checks the recognized word, not pronunciation quality. The app does not record or store microphone audio.

All game content is in `src/game.js`, interaction code in `src/app.js`, and styling in `src/style.css`. Font styling falls back to local fonts if the optional Google Fonts request is unavailable.

## Natural AI voice (ElevenLabs)

1. Create an ElevenLabs API key with text-to-speech permission and available credits.
2. In GitHub Codespaces, add a Codespaces Secret named `ELEVENLABS_API_KEY`, grant this repository access, and restart the Codespace so the secret is injected. Never put the key in source code or browser code.
3. Run `git pull origin main`, then `node --version`. If needed, use `nvm install 24` and `nvm use 24`.
4. Stop the old server with Ctrl+C and run `npm start` again. Refresh the game and choose **Natural AI voice · ElevenLabs** under Reading voice.

The default voice is Sarah. Set optional server variable `ELEVENLABS_VOICE_ID` to another voice ID from your ElevenLabs account. The app generates audio only for its fixed lesson words, stories, and preview phrase. Results are cached in server memory until restart, avoiding repeat charges during that session. Keep the Codespaces forwarded port private: anyone with access to the running game can request lesson audio and use the account's speech credits. Set a spending limit in your provider account.

With no key, or on an API error, the game uses browser speech. A configured key is not proof of account access or available credits. If natural speech fails, check the key's permissions, credit balance, and selected voice in ElevenLabs. No audio request sends microphone recordings; only the built-in lesson text goes to ElevenLabs.
