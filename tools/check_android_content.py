"""Validate the shipped curriculum and privacy boundary without Android tooling."""
import json, re, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'android/app/src/main/assets'
load=lambda name: json.loads((ASSETS/f'{name}.json').read_text())
class CurriculumTest(unittest.TestCase):
 def test_required_inventory(self):
  self.assertTrue(400<=len(load('words'))<=600)
  self.assertTrue(100<=len(load('sentences'))<=200)
  self.assertEqual(44,len(load('phonemes')))
  self.assertGreaterEqual(len(load('patterns')),6)
 def test_complete_unique_words_and_syllables(self):
  words=load('words'); self.assertEqual(len(words),len({w['id'] for w in words}))
  patterns={p['id'] for p in load('patterns')}
  starters=[w for w in words if w['soundOut']]
  self.assertGreaterEqual(len(starters),60)
  for w in starters:
   self.assertEqual(3,len(w['word']))
   self.assertNotIn(w['word'],{'has','his','gem'})
   self.assertNotIn('x',w['word'])
  for w in words:
   self.assertIn(w['pattern'],patterns)
   self.assertEqual(w['word'],''.join(w['syllables']))
   for field in ['picture','meaning','sentence']:self.assertTrue(w[field],w['word'])
 def test_short_sentences_and_echo_phrases(self):
  for s in load('sentences'):
   self.assertTrue(3<=len(s['text'].split())<=7,s['text'])
   self.assertEqual(s['text'],' '.join(s['phrases']))
 def test_sound_cues_and_recordings(self):
  sounds=load('phonemes');self.assertEqual(44,len({s['ipa'] for s in sounds}))
  for sound in sounds:
   self.assertIn(sound['mouth'],['open','closed','smile','round','teeth','tongue'])
   self.assertTrue(sound['cue'] and sound['example'] and sound['letters'])
  for attribution in load('audio_attribution'):
   self.assertGreater((ASSETS/'phoneme_audio'/f"{attribution['id']}.mp3").stat().st_size,1000)
 def test_no_internet_or_external_storage_permissions(self):
  manifest=(ROOT/'android/app/src/main/AndroidManifest.xml').read_text()
  self.assertNotIn('android.permission.INTERNET',manifest)
  self.assertNotIn('READ_EXTERNAL_STORAGE',manifest)
  recognition=(ROOT/'android/app/src/main/java/com/wordgarden/app/LocalRecognizer.kt').read_text()
  self.assertIn('createOnDeviceSpeechRecognizer',recognition)
  self.assertNotIn('SpeechRecognizer.createSpeechRecognizer(',recognition)
if __name__=='__main__':unittest.main(verbosity=2)
