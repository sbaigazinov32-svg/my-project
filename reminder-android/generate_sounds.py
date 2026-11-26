import wave, math, struct
from pathlib import Path
p=Path(__file__).parent/'app/src/main/res/raw'
p.mkdir(parents=True,exist_ok=True)
rate=22050
for stage,count in [(6,2),(2,3)]:
    samples=[]
    for i in range(int(rate*(count*0.55+0.1))):
        t=i/rate
        phase=t%0.55
        active=t<count*0.55 and phase<0.28
        envelope=max(0,min(1,phase/0.015,(0.28-phase)/0.015)) if active else 0
        samples.append(struct.pack('<h',int(11000*envelope*math.sin(2*math.pi*880*t))))
    with wave.open(str(p/f'signal{stage}.wav'),'wb') as w:
        w.setparams((1,2,rate,0,'NONE','not compressed'))
        w.writeframes(b''.join(samples))
