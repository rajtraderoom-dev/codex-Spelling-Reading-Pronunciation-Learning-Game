import { words, stories } from './src/game.js';

export const previewText = 'Hello! Let’s discover something wonderful in our word garden.';
const allowedText = new Set([...words.map(item => item.word), ...stories.map(item => item.text), previewText]);

// Only the fixed lesson content can spend speech credits. Successful audio is
// reused for the lifetime of this server, including simultaneous requests.
export function createSpeechService({ apiKey, voiceId = 'EXAVITQu4vr4xnSDxMaL', fetchAudio = fetch } = {}) {
  const cache = new Map();
  return {
    configured: Boolean(apiKey),
    async audio(text) {
      if (!allowedText.has(text)) return { status: 400 };
      if (!apiKey) return { status: 503 };
      if (!cache.has(text)) {
        const pending = (async () => {
          const response = await fetchAudio(`https://api.elevenlabs.io/v1/text-to-speech/${encodeURIComponent(voiceId)}?output_format=mp3_44100_128`, {
            method: 'POST',
            headers: { 'xi-api-key': apiKey, 'Content-Type': 'application/json', Accept: 'audio/mpeg' },
            body: JSON.stringify({ text, model_id: 'eleven_flash_v2_5', voice_settings: { stability: 0.5, similarity_boost: 0.75 } }),
            signal: AbortSignal.timeout(20000)
          });
          if (!response.ok) throw new Error('Speech provider unavailable');
          const audio = Buffer.from(await response.arrayBuffer());
          if (!audio.length) throw new Error('Empty audio');
          return audio;
        })();
        cache.set(text, pending);
      }
      try { return { status: 200, audio: await cache.get(text) }; }
      catch { cache.delete(text); return { status: 502 }; }
    }
  };
}
