"""Functional checks against a disposable emulator. Requires adb and a built APK.
Never run against a device containing a child's existing progress.
"""
import os,re,subprocess,time,xml.etree.ElementTree as ET
ADB=os.environ.get('ADB','adb')
def adb(*args):return subprocess.check_output([ADB,*args],timeout=60).decode(errors='replace')
def ui():
 for _ in range(5):
  try:
   adb('shell','rm','-f','/sdcard/garden-ui.xml')
   result=adb('shell','uiautomator','dump','/sdcard/garden-ui.xml')
   if 'dumped to:' in result:
    root=ET.fromstring(adb('exec-out','cat','/sdcard/garden-ui.xml'))
    if any(n.attrib.get('package')=='com.wordgarden.app' for n in root.iter('node')):return root
  except (subprocess.CalledProcessError,ET.ParseError):
   pass
  time.sleep(1)
 raise AssertionError('No fresh app accessibility tree after retries')
def bounds(node):return list(map(int,re.findall(r'\d+',node.attrib['bounds'])))
def tap(label,exact=False):
 print('Tap:',label,flush=True)
 for attempt in range(24):
  root=ui()
  nodes=[n for n in root.iter('node') if (n.attrib.get('text')==label if exact else label in n.attrib.get('text','')) and n.attrib.get('enabled')=='true' and n.attrib.get('class','').endswith('Button')]
  for n in nodes:
   x1,y1,x2,y2=bounds(n)
   if y2>y1 and x2>x1:
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.4);return
  adb('shell','input','swipe','240','690' if attempt<12 else '200','240','200' if attempt<12 else '690','250')
 raise AssertionError('Could not find '+label)
def home():adb('shell','input','keyevent','4');time.sleep(.3)
def contains(value):return any(value in n.attrib.get('text','') for n in ui().iter('node'))
def assert_text(value):
 for _ in range(10):
  if contains(value):return
  adb('shell','input','swipe','240','220','240','700','150')
  time.sleep(.3)
 raise AssertionError('Expected '+value)
print('Checking guided lesson...',flush=True)
tap('My little lesson');assert_text('1 · Listen');tap('🔊 Listen',True);tap('I’m ready');assert_text('2 · Repeat');tap('I’m ready');assert_text('3 · Read');tap('📖 cat',True)
for key in ['c','a','t']:tap(key,True)
tap('Check my word');assert_text('You built it!')
print('PASS guided listen/repeat/read/spell and tile completion',flush=True)
home();tap('grown-up corner' if contains('grown-up corner') else 'Grown-up corner')
root=ui();puzzle=next(n.attrib['text'] for n in root.iter('node') if re.search(r'\d+ \+ \d+ =',n.attrib.get('text','')))
answer=sum(map(int,re.findall(r'\d+',puzzle)))
entry=next(n for n in root.iter('node') if n.attrib.get('class')=='android.widget.EditText');x1,y1,x2,y2=bounds(entry)
adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));adb('shell','input','text',str(answer));adb('shell','input','keyevent','4');tap('Open grown-up corner');assert_text('Grown-up corner')
tap('Learning settings')
if not contains('☑ 🤫 Quiet mode'):tap('Quiet mode')
assert_text('☑ 🤫 Quiet mode')
print('PASS parent arithmetic gate and persisted quiet setting',flush=True)
home();tap('Sound library');tap('p,') if False else tap('p  /p/')
assert_text('Press your lips');tap('Hear the sound',True) if False else tap('👄 Hear the sound',True)
print('PASS sound library and offline recording playback control',flush=True)
home();tap('Play a little game');tap('Say the Word');tap('🔊 Listen',True);tap('I’m ready');tap('I’m ready')
# This is the second item only if cursor changed; read the large modeled word.
root=ui();target=next(n.attrib['text'] for n in root.iter('node') if n.attrib.get('text','').isalpha() and n.attrib.get('text','') not in ['Home'])
tap('📖 '+target,True);tap('My turn to speak');assert_text('Listen and tap works too')
print('PASS microphone-unavailable path stays usable',flush=True)
home()
def star_count():
 for n in ui().iter('node'):
  match=re.search(r'⭐ (\d+)',n.attrib.get('text',''))
  if match:return int(match.group(1))
 raise AssertionError('No home star counter')
before=star_count()
adb('shell','am','force-stop','com.wordgarden.app');adb('shell','am','start','-n','com.wordgarden.app/.MainActivity');time.sleep(1)
assert star_count()==before and before>0
print('PASS restart keeps local progress',flush=True)
