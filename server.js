import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = fileURLToPath(new URL('.', import.meta.url));
const types = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript', '.svg': 'image/svg+xml' };
http.createServer(async (req, res) => {
  try {
    const name = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
    const file = path.resolve(root, `.${name === '/' ? '/index.html' : name}`);
    if (!file.startsWith(root) || !['/index.html', '/src/app.js', '/src/game.js', '/src/style.css'].includes(name === '/' ? '/index.html' : name)) {
      res.writeHead(404).end('Not found'); return;
    }
    res.writeHead(200, { 'Content-Type': `${types[path.extname(file)]}; charset=utf-8`, 'X-Content-Type-Options': 'nosniff' });
    res.end(await readFile(file));
  } catch { res.writeHead(404).end('Not found'); }
}).listen(Number(process.env.PORT || 3000), '0.0.0.0', () => console.log('Word Garden listening on port ' + (process.env.PORT || 3000)));
