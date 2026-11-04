import { words, stories, isCorrect, normalize, loadProgress, dayKey } from './game.js';

let saved; try { saved = localStorage.getItem('word-garden'); } catch {}
const progress = loadProgress(saved);
let mode = 'spelling', index = 0, storyIndex = 0, solved = false, sound = true, recognition;
const panel = document.querySelector('#practice');
const speechSupported = 'speechSynthesis' in window;
let preferredVoice = '';
try { preferredVoice = localStorage.getItem('word-garden-voice') || ''; } catch {}
let voices = [];
const voiceSettings = document.createElement('div');
voiceSettings.className = 'voice-settings';
voiceSettings.innerHTML = '<label for="voice-select">Reading voice</label><select id="voice-select" aria-label="Reading voice"><option value="">Best available English voice</option></select><button id="voice-preview" type="button">Try voice</button>';
document.querySelector('footer').before(voiceSettings);
const voiceSelect = voiceSettings.querySelector('select');
function voiceScore(voice) {
  return (/natural|neural|premium|enhanced/i.test(voice.name) ? 100 : 0)
    + (/Google|Microsoft.*Online/i.test(voice.name) ? 50 : 0)
    + (/Samantha|Daniel|Karen|Moira/i.test(voice.name) ? 20 : 0)
    + (/^en-US$/i.test(voice.lang) ? 5 : 0);
}
function refreshVoices() {
  voices = speechSynthesis.getVoices().filter(voice => /^en(?:-|_|$)/i.test(voice.lang)).sort((a, b) => voiceScore(b) - voiceScore(a));
  voiceSelect.replaceChildren(new Option('Best available English voice', ''));
  for (const voice of voices) voiceSelect.add(new Option(`${voice.name} (${voice.lang})`, voice.voiceURI));
  voiceSelect.value = voices.some(voice => voice.voiceURI === preferredVoice) ? preferredVoice : '';
}
if (speechSupported) { refreshVoices(); speechSynthesis.addEventListener('voiceschanged', refreshVoices); }
else voiceSettings.hidden = true;
voiceSelect.onchange = () => { preferredVoice = voiceSelect.value; try { localStorage.setItem('word-garden-voice', preferredVoice); } catch {} };
document.querySelector('#voice-preview').onclick = () => speak('Hello! Let’s discover something wonderful in our word garden.');
const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
function save() {
  if (progress.day !== dayKey()) { progress.day = dayKey(); progress.daily = 0; }
  try { localStorage.setItem('word-garden', JSON.stringify(progress)); } catch {}
  document.querySelector('#total-stars').textContent = progress.stars;
  document.querySelector('#daily-count').textContent = `${Math.min(progress.daily, 5)} / 5`;
  document.querySelector('#daily-progress').value = Math.min(progress.daily, 5);
  document.querySelector('#completed').textContent = progress.completed;
  document.querySelector('#accuracy').textContent = progress.attempts ? `${Math.round(progress.correct / progress.attempts * 100)}%` : '—';
}
function speak(text) {
  if (!speechSupported) { feedback('Spoken audio is unavailable in this browser. Use the clue or read the text below.', 'neutral'); return; }
  if (!sound) { feedback('Turn sound on using the button at the bottom to hear this word.', 'neutral'); return; }
  speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  const voice = voices.find(voice => voice.voiceURI === preferredVoice) || voices[0];
  if (voice) utterance.voice = voice;
  utterance.lang = voice?.lang || 'en-US';
  utterance.rate = 0.95;
  utterance.pitch = 1;
  utterance.onerror = event => { if (!['canceled', 'interrupted'].includes(event.error)) feedback('This voice could not play. Try another reading voice below.', 'neutral'); };
  speechSynthesis.speak(utterance);
}
function feedback(text, kind = '') { const el = document.querySelector('#feedback'); el.textContent = text; el.className = `feedback ${kind}`; }
function win() { if (solved) return; solved = true; progress.stars += 3; progress.completed++; progress.daily++; save(); document.querySelector('#next').hidden = false; }
function render() {
  solved = false;
  const item = words[index % words.length];
  const n = mode === 'reading' ? storyIndex + 1 : index + 1;
  panel.setAttribute('aria-labelledby', `tab-${mode}`);
  const top = `<div class="practice-top"><span class="pill">${mode === 'spelling' ? '✦ SPELLING PRACTICE' : mode === 'reading' ? '▤ STORY TIME' : '♫ SPEAKING PRACTICE'}</span><span class="round">${String(n).padStart(2, '0')} <span>/ ${mode === 'reading' ? stories.length : words.length}</span></span></div>`;
  const bottom = `<div id="feedback" class="feedback" role="status" aria-live="polite"></div><button id="next" class="primary next" hidden>${mode === 'reading' ? 'Next story' : 'Next word'} <span>→</span></button><div class="practice-bottom"><span>✧ A little progress is still progress.</span><span>+3 stars per discovery</span></div>`;
  if (mode === 'spelling') {
    panel.innerHTML = top + `<div class="exercise"><div class="word-art">${item.emoji}</div><h2>Listen closely. What’s the word?</h2><p>Give it a listen, then let your letters do the talking.</p><button class="listen" id="listen">◖)) <span>Listen to the word</span></button><p class="clue">${item.hint}</p><form id="spell-form"><label for="answer">YOUR BEST GUESS</label><div class="answer-row"><input id="answer" placeholder="Type the word here…" autocomplete="off" autocapitalize="off" spellcheck="false" required maxlength="40"><button class="primary" type="submit">Check word <span>↗</span></button></div></form><button class="hint-button" id="hint">Need a little hint?</button></div>` + bottom;
    document.querySelector('#listen').onclick = () => speak(item.word);
    document.querySelector('#hint').onclick = () => feedback(`Starts with “${item.word[0]}” and has ${item.word.length} letters.`, 'neutral');
    document.querySelector('#spell-form').onsubmit = event => {
      event.preventDefault(); if (solved) return;
      const value = document.querySelector('#answer').value;
      if (!normalize(value)) { feedback('Try typing some letters first.', 'neutral'); return; }
      progress.attempts++;
      if (isCorrect(value, item.word)) { progress.correct++; win(); feedback(`Beautiful work! “${item.word}” is right. +3 stars ✦`, 'success'); document.querySelector('#answer').disabled = true; }
      else { save(); feedback('Almost there! Listen again, or try a little hint.', 'retry'); }
    };
  } else if (mode === 'reading') {
    const story = stories[storyIndex];
    panel.innerHTML = top + `<div class="reading-exercise"><span class="story-label">A LITTLE STORY FOR YOU</span><h2>${story.title}</h2><p class="story">${story.text}</p><button class="listen" id="listen">◖)) Read along with me</button><h3>${story.question}</h3><div class="choices">${story.choices.map((choice, i) => `<button data-choice="${i}">${String.fromCharCode(65 + i)} <span>${choice}</span></button>`).join('')}</div></div>` + bottom;
    document.querySelector('#listen').onclick = () => speak(story.text);
    panel.querySelectorAll('[data-choice]').forEach(button => button.onclick = () => {
      if (solved) return;
      if (Number(button.dataset.choice) === story.answer) { button.classList.add('correct-choice'); win(); feedback('You found it! Wonderful reading. +3 stars ✦', 'success'); }
      else { feedback('Have another look at the story. You can try again!', 'retry'); button.disabled = true; }
    });
  } else {
    panel.innerHTML = top + `<div class="exercise"><div class="word-art">${item.emoji}</div><h2 class="speaking-word">${item.word}</h2><p>${item.sentence}</p><button class="listen" id="listen">◖)) Hear the pronunciation</button><p class="clue">Listen, take a breath, and say the word out loud.</p>${Recognition ? '<button class="primary" id="record">● Try with my microphone</button><p class="micro-note">Your browser may ask for microphone permission.<br>Speech recognition may use your browser’s online service.</p>' : '<p class="micro-note">Microphone recognition isn’t available in this browser.<br>You can still listen and practice out loud.</p>'}<button class="hint-button" id="self-practice">I practiced out loud ✓</button></div>` + bottom;
    document.querySelector('#listen').onclick = () => speak(item.word);
    document.querySelector('#self-practice').onclick = () => { win(); feedback('Practice makes progress! +3 stars for showing up. ✦', 'success'); };
    if (Recognition) document.querySelector('#record').onclick = () => {
      if (solved || recognition) return;
      if (speechSupported) speechSynthesis.cancel();
      const active = new Recognition(); recognition = active; active.lang = 'en-US'; active.interimResults = false;
      const button = document.querySelector('#record'); button.disabled = true; button.textContent = '● Listening…';
      active.onresult = event => { if (recognition !== active) return; const transcript = event.results[0][0].transcript; if (isCorrect(transcript, item.word)) { win(); feedback(`I heard “${transcript}”. Lovely work! +3 stars ✦`, 'success'); } else feedback(`I heard “${transcript}”. Listen once more and try again.`, 'retry'); };
      active.onerror = event => { if (recognition === active) feedback(event.error === 'not-allowed' ? 'Microphone access was declined. You can still practice out loud below.' : 'I couldn’t hear that. Try again or use the practice button below.', 'neutral'); };
      active.onend = () => { if (recognition === active) recognition = null; button.disabled = false; button.textContent = '● Try with my microphone'; };
      try { active.start(); } catch { recognition = null; button.disabled = false; button.textContent = '● Try with my microphone'; feedback('Microphone unavailable. Use the practice button below.', 'neutral'); }
    };
  }
  document.querySelector('#next').onclick = () => { if (mode === 'reading') storyIndex = (storyIndex + 1) % stories.length; else index = (index + 1) % words.length; render(); panel.querySelector('h2').setAttribute('tabindex', '-1'); panel.querySelector('h2').focus(); };
}
document.querySelectorAll('[data-mode]').forEach(tab => {
  tab.onclick = () => {
    if (recognition) { const active = recognition; recognition = null; active.abort(); }
    if (speechSupported) speechSynthesis.cancel();
    mode = tab.dataset.mode;
    document.querySelectorAll('[data-mode]').forEach(other => { const selected = other === tab; other.classList.toggle('active', selected); other.setAttribute('aria-selected', selected); other.tabIndex = selected ? 0 : -1; });
    render();
  };
  tab.onkeydown = event => { const tabs = [...document.querySelectorAll('[data-mode]')]; const i = tabs.indexOf(tab); let target;
    if (event.key === 'ArrowRight') target = tabs[(i + 1) % tabs.length];
    if (event.key === 'ArrowLeft') target = tabs[(i + tabs.length - 1) % tabs.length];
    if (event.key === 'Home') target = tabs[0]; if (event.key === 'End') target = tabs.at(-1);
    if (target) { event.preventDefault(); target.click(); target.focus(); }
  };
  tab.tabIndex = tab.dataset.mode === mode ? 0 : -1;
});
document.querySelector('#sound-toggle').onclick = event => { sound = !sound; event.currentTarget.textContent = sound ? '♫ Sound on' : '♫ Sound off'; event.currentTarget.setAttribute('aria-pressed', sound); if (!sound && speechSupported) speechSynthesis.cancel(); };
save(); render();
