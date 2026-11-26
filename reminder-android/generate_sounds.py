import wave, math, struct
from pathlib import Path
p=Path(__file__).parent/'app/src/main/res/raw'
p.mkdir(parents=True,exist_ok=True)
for stage,duration in [(6,1.2),(2,3.0)]:
    with wave.open(str(p/f'signal{stage}.wav'),'wb') as w:
        w.setparams((1,2,22050,0,'NONE','not compressed'))
        w.writeframes(b''.join(struct.pack('<h',int(11000*math.sin(2*math.pi*880*t/22050)) if stage==2 or t/22050<0.3 or 0.6<t/22050<0.9 else 0) for t in range(int(22050*duration))))
