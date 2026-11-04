import test from 'node:test';
import assert from 'node:assert/strict';
import { isCorrect, loadProgress, dayKey, words, stories } from '../src/game.js';
test('spelling accepts case and surrounding punctuation, rejects incorrect words', () => {
  assert.equal(isCorrect(' Garden! ', 'garden'), true);
  assert.equal(isCorrect('gardin', 'garden'), false);
  assert.equal(isCorrect('', 'garden'), false);
});
test('progress survives reload and resets only daily practice on a new day', () => {
  const saved = { stars: 12, completed: 4, attempts: 6, correct: 4, daily: 4, day: dayKey() };
  assert.deepEqual(loadProgress(JSON.stringify(saved)), saved);
  const tomorrow = loadProgress(JSON.stringify({ ...saved, day: '2000-01-01' }));
  assert.equal(tomorrow.daily, 0); assert.equal(tomorrow.stars, 12);
});
test('damaged storage recovers and invalid counters are ignored', () => {
  assert.equal(loadProgress('{').stars, 0);
  assert.equal(loadProgress('{"stars":-1,"attempts":"bad"}').attempts, 0);
  assert.equal(loadProgress('null').stars, 0);
});
test('each exercise has a valid answer and learning content', () => {
  assert.ok(words.length >= 8); assert.ok(stories.length >= 3);
  for (const item of words) { assert.match(item.word, /^[a-z]+$/); assert.ok(item.hint && item.sentence); }
  for (const story of stories) { assert.ok(story.choices[story.answer]); assert.ok(story.text.length > 100); }
});
