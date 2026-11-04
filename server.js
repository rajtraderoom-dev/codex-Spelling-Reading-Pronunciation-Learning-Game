import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { createSpeechService } from './speech.js';

const root = fileURLToPath(new URL('.', import.meta.url));
const types = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript', '.svg': 'image/svg+xml' };
const speech = createSpeechService({ apiKey: process.env.ELEVENLABS_API_KEY, voiceId: process.env.ELEVENLABS_VOICE_ID || undefined });
http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, 'http://localhost');
    const name = decodeURIComponent(url.pathname);
    if (!['GET', 'HEAD'].includes(req.method)) { res.writeHead(405, { Allow: 'GET, HEAD' }).end(); return; }
    if (name === '/api/speech-status') {
      res.writeHead(200, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
      res.end(JSON.stringify({ configured: speech.configured })); return;
    }
    if (name === '/api/speech') {
      if (req.method === 'HEAD') { res.writeHead(405).end(); return; }
      const origin = req.headers.origin;
      if (origin && new URL(origin).host !== req.headers.host) { res.writeHead(403).end(); return; }
      const result = await speech.audio(url.searchParams.get('text'));
      if (result.status !== 200) { res.writeHead(result.status, { 'Cache-Control': 'no-store' }).end('Audio unavailable'); return; }
      res.writeHead(200, { 'Content-Type': 'audio/mpeg', 'Cache-Control': 'private, max-age=86400', 'X-Content-Type-Options': 'nosniff' });
      res.end(result.audio); return;
    }
    const file = path.resolve(root, `.${name === '/' ? '/index.html' : name}`);
    if (!file.startsWith(root) || !['/index.html', '/src/app.js', '/src/game.js', '/src/style.css'].includes(name === '/' ? '/index.html' : name)) {
      res.writeHead(404).end('Not found'); return;
    }
    res.writeHead(200, { 'Content-Type': `${types[path.extname(file)]}; charset=utf-8`, 'X-Content-Type-Options': 'nosniff' });
    res.end(await readFile(file));
  } catch { res.writeHead(404).end('Not found'); }
}).listen(Number(process.env.PORT || 3000), '0.0.0.0', () => console.log('Word Garden listening on port ' + (process.env.PORT || 3000)));
