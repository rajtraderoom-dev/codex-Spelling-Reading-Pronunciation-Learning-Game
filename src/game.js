export const words = [
  { word: 'garden', hint: 'A place where flowers and vegetables grow.', emoji: '🌷', sentence: 'We planted tiny seeds in the garden.' },
  { word: 'rabbit', hint: 'A furry animal with long ears that loves to hop.', emoji: '🐇', sentence: 'The rabbit hopped across the grass.' },
  { word: 'sunshine', hint: 'The warm, bright light that comes from the sun.', emoji: '☀️', sentence: 'The sunshine warmed my face.' },
  { word: 'butterfly', hint: 'An insect with beautiful wings that visits flowers.', emoji: '🦋', sentence: 'A butterfly landed on the pink flower.' },
  { word: 'apple', hint: 'A crunchy fruit that can be red or green.', emoji: '🍎', sentence: 'I packed an apple for my lunch.' },
  { word: 'friend', hint: 'Someone you like to play and spend time with.', emoji: '🤝', sentence: 'My friend helped me water the plants.' },
  { word: 'rainbow', hint: 'A colorful arc in the sky after rain.', emoji: '🌈', sentence: 'We saw a rainbow after the storm.' },
  { word: 'little', hint: 'Another word for small.', emoji: '🌱', sentence: 'A little seed can become a tall tree.' }
];
export const stories = [
  { title: 'A tiny seed, a big adventure', text: 'Mia found a tiny seed in her pocket. She planted it in a sunny spot and watered it every day. One morning, a little green leaf peeked out of the soil. Mia smiled. Her garden was beginning to grow!', question: 'What helped Mia’s seed grow?', choices: ['Sunshine and water', 'A dark cupboard', 'A new pocket'], answer: 0 },
  { title: 'An unexpected friend', text: 'Leo sat under the apple tree with his book. A small rabbit hopped over and settled beside him. Leo read his story out loud. The rabbit twitched its ears. From that day on, Leo always had a reading buddy.', question: 'Who became Leo’s reading buddy?', choices: ['A butterfly', 'A rabbit', 'His teacher'], answer: 1 },
  { title: 'After the rain', text: 'The rain stopped, and Ava put on her boots. Outside, the leaves sparkled with drops of water. She looked up and spotted a rainbow. She counted seven colors and called her brother to come and see.', question: 'How many colors did Ava count?', choices: ['Five', 'Three', 'Seven'], answer: 2 }
];
export function normalize(value) { return value.toLowerCase().replace(/[^a-z]/g, ''); }
export function isCorrect(input, target) { return normalize(input) === normalize(target); }
export function dayKey() { return new Date().toLocaleDateString('en-CA'); }
export function freshProgress() { return { stars: 0, completed: 0, attempts: 0, correct: 0, day: dayKey(), daily: 0 }; }
export function loadProgress(raw) {
  try { const data = JSON.parse(raw); const base = freshProgress(); for (const key of ['stars', 'completed', 'attempts', 'correct', 'daily']) { if (Number.isFinite(data?.[key]) && data[key] >= 0) base[key] = data[key]; } if (data?.day !== base.day) base.daily = 0; return base; } catch { return freshProgress(); }
}
