# Word Garden

A responsive spelling, reading, and pronunciation game. Earn stars, explore short stories, and build a daily practice habit. Progress stays in this browser using local storage.

## Run

Requires Node.js 22 or newer. No packages, API keys, database, or build step are needed.

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
