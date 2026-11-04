import test from 'node:test';
import assert from 'node:assert/strict';
import { createSpeechService } from '../speech.js';

test('unconfigured speech needs no credentials for the browser fallback', async () => {
  const speech = createSpeechService();
  assert.equal(speech.configured, false);
  assert.equal((await speech.audio('garden')).status, 503);
});
test('only lesson text reaches the provider; audio is cached across concurrent calls', async () => {
  let calls = 0;
  const service = createSpeechService({ apiKey: 'test-only', fetchAudio: async (url, options) => {
    calls++; assert.ok(url.startsWith('https://api.elevenlabs.io/'));
    assert.equal(options.headers['xi-api-key'], 'test-only');
    assert.equal(JSON.parse(options.body).text, 'garden');
    return new Response(new Uint8Array([1, 2, 3]));
  } });
  assert.equal((await service.audio('arbitrary paid content')).status, 400);
  const results = await Promise.all([service.audio('garden'), service.audio('garden')]);
  assert.equal(calls, 1); assert.equal(results[0].status, 200);
  assert.deepEqual(results[1].audio, Buffer.from([1, 2, 3]));
  await service.audio('garden'); assert.equal(calls, 1);
});
test('provider failures hide details and allow a later retry', async () => {
  let calls = 0;
  const service = createSpeechService({ apiKey: 'test-only', fetchAudio: async () => {
    calls++; if (calls === 1) return new Response('private provider error', { status: 401 });
    return new Response(new Uint8Array([1]));
  } });
  assert.deepEqual(await service.audio('garden'), { status: 502 });
  assert.equal((await service.audio('garden')).status, 200);
});
