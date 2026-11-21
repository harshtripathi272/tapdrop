import React from 'react';
import {C, KIND} from './timeline';
import {Icon, IconDisc} from './devices';
import {ease, pop, range} from './anim';

// Screens for the receiving phone, on a 360 x 760 canvas. They are generic stand-ins
// for what a phone shows after reading the watch, not copies of any real app.

const fill = {position: 'absolute', inset: 0} as const;
const SHEET = '#1C1D22';

const StatusBar: React.FC = () => (
  <div style={{position: 'absolute', top: 10, left: 22, right: 22, display: 'flex', justifyContent: 'space-between', fontSize: 13, fontWeight: 600, color: '#fff', opacity: 0.85}}>
    <span>10:09</span>
    <span>5G ▮▮▮</span>
  </div>
);

/** The phone before the tap: a lock screen. */
export const PhoneIdle: React.FC = () => (
  <div style={{...fill, background: '#0B0C10'}}>
    <StatusBar />
    <div style={{position: 'absolute', top: 150, left: 0, right: 0, textAlign: 'center', fontSize: 86, fontWeight: 600, letterSpacing: -2}}>10:09</div>
    <div style={{position: 'absolute', top: 256, left: 0, right: 0, textAlign: 'center', fontSize: 18, color: C.sub}}>Saturday, 21 November</div>
  </div>
);

/** A bottom sheet that springs up from frame 0 of `t`. */
const Sheet: React.FC<{t: number; height: number; children: React.ReactNode}> = ({t, height, children}) => {
  const p = pop(t, 0, 170, 18);
  return (
    <div
      style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, height, background: SHEET, borderRadius: '34px 34px 0 0',
        transform: `translateY(${(1 - p) * height}px)`, padding: '26px 24px', boxSizing: 'border-box',
        boxShadow: '0 -20px 60px rgba(0,0,0,0.5)',
      }}
    >
      <div style={{width: 44, height: 5, borderRadius: 3, background: '#4a4c54', margin: '0 auto 22px'}} />
      {children}
    </div>
  );
};

/** A button that fills and flips to a done state at `doneAt`. */
const Action: React.FC<{t: number; doneAt: number; color: string; label: string; done: string}> = ({t, doneAt, color, label, done}) => {
  const isDone = t >= doneAt;
  const press = isDone ? 1 - 0.06 * Math.sin(range(t, doneAt, doneAt + 8) * Math.PI) : 1;
  return (
    <div
      style={{
        height: 62, borderRadius: 20, background: isDone ? '#1f3d2c' : color, display: 'flex', alignItems: 'center',
        justifyContent: 'center', gap: 10, fontSize: 20, fontWeight: 700, color: isDone ? '#6fe0a0' : '#fff',
        transform: `scale(${press})`,
      }}
    >
      {isDone && <Icon glyph="check" size={24} color="#6fe0a0" />}
      {isDone ? done : label}
    </div>
  );
};

export const ContactResult: React.FC<{t: number}> = ({t}) => (
  <div style={{...fill, background: '#0e0f13'}}>
    <PhoneIdle />
    <div style={{...fill, background: `rgba(0,0,0,${range(t, 0, 8, 0, 0.55)})`}} />
    <Sheet t={t} height={470}>
      <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center'}}>
        <div
          style={{
            width: 104, height: 104, borderRadius: 52, background: KIND.contact.color, display: 'flex', alignItems: 'center',
            justifyContent: 'center', fontSize: 40, fontWeight: 700, transform: `scale(${pop(t, 4)})`,
          }}
        >
          AR
        </div>
        <div style={{marginTop: 18, fontSize: 30, fontWeight: 700, opacity: ease(t, 6, 14)}}>Asha Rao</div>
        <div style={{marginTop: 6, fontSize: 19, color: C.sub, opacity: ease(t, 8, 16)}}>+91 98765 43210</div>
        <div style={{marginTop: 2, fontSize: 17, color: C.dim, opacity: ease(t, 10, 18)}}>asha@example.com</div>
      </div>
      <div style={{marginTop: 30, opacity: ease(t, 10, 18)}}>
        <Action t={t} doneAt={28} color={KIND.contact.color} label="Save contact" done="Saved to contacts" />
      </div>
    </Sheet>
  </div>
);

export const WifiResult: React.FC<{t: number}> = ({t}) => {
  const connected = t >= 30;
  return (
    <div style={{...fill, background: '#0e0f13'}}>
      <PhoneIdle />
      <div style={{...fill, background: `rgba(0,0,0,${range(t, 0, 8, 0, 0.55)})`}} />
      <Sheet t={t} height={420}>
        <div style={{display: 'flex', alignItems: 'center', gap: 18}}>
          <IconDisc glyph="wifi" color={KIND.wifi.color} size={72} />
          <div>
            <div style={{fontSize: 28, fontWeight: 700}}>Home 5G</div>
            <div style={{fontSize: 18, color: connected ? '#6fe0a0' : C.sub}}>{connected ? 'Connected' : 'Joining network…'}</div>
          </div>
        </div>
        <div style={{marginTop: 28, height: 8, borderRadius: 4, background: '#2c2e35', overflow: 'hidden'}}>
          <div style={{width: `${ease(t, 8, 30) * 100}%`, height: '100%', background: KIND.wifi.color}} />
        </div>
        <div style={{marginTop: 26, fontSize: 18, color: C.dim, lineHeight: '26px'}}>No password typed.<br />No one had to spell it out.</div>
        <div style={{marginTop: 26}}>
          <Action t={t} doneAt={30} color={KIND.wifi.color} label="Connecting" done="You're online" />
        </div>
      </Sheet>
    </div>
  );
};

export const PayResult: React.FC<{t: number}> = ({t}) => {
  const apps = ['Google Pay', 'PhonePe', 'Paytm'];
  return (
    <div style={{...fill, background: '#101114'}}>
      <StatusBar />
      <div style={{position: 'absolute', top: 84, left: 26, right: 26, opacity: ease(t, 0, 8)}}>
        <div style={{fontSize: 13, fontWeight: 700, letterSpacing: 2, color: KIND.pay.color}}>PAYMENT REQUEST</div>
        <div style={{marginTop: 10, fontSize: 82, fontWeight: 800, letterSpacing: -2, transform: `scale(${0.9 + 0.1 * pop(t, 2)})`, transformOrigin: 'left center'}}>₹250</div>
        <div style={{fontSize: 22, fontWeight: 600}}>to Asha Rao</div>
        <div style={{fontSize: 16, color: C.dim, marginTop: 2}}>asha@upi</div>
      </div>
      <Sheet t={t - 14} height={380}>
        <div style={{fontSize: 17, color: C.sub, marginBottom: 14}}>Pay with</div>
        {apps.map((app, i) => {
          const p = pop(t, 18 + i * 4);
          const chosen = i === 0 && t >= 38;
          return (
            <div
              key={app}
              style={{
                display: 'flex', alignItems: 'center', gap: 16, padding: '14px 16px', marginBottom: 10, borderRadius: 18,
                background: chosen ? '#3a2c12' : '#26272d', border: `2px solid ${chosen ? KIND.pay.color : 'transparent'}`,
                opacity: Math.min(1, p * 1.5), transform: `translateX(${(1 - p) * 40}px)`,
              }}
            >
              <div style={{width: 42, height: 42, borderRadius: 12, background: ['#4d7cfe', '#8e63d9', '#2e9aaf'][i], display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20, fontWeight: 800}}>
                {app[0]}
              </div>
              <div style={{fontSize: 21, fontWeight: 600}}>{app}</div>
            </div>
          );
        })}
      </Sheet>
    </div>
  );
};

export const MapResult: React.FC<{t: number}> = ({t}) => {
  const drop = pop(t, 8, 220, 12);
  return (
    <div style={{...fill, background: '#1a2230'}}>
      <svg width={360} height={760} style={fill}>
        <rect width={360} height={760} fill="#18202d" />
        <path d="M-20 520 C80 470 150 560 380 470 L380 760 L-20 760 Z" fill="#14303a" />
        <rect x={40} y={110} width={110} height={90} rx={10} fill="#212b3b" />
        <rect x={210} y={150} width={120} height={120} rx={10} fill="#212b3b" />
        <rect x={30} y={290} width={140} height={110} rx={10} fill="#1d3a2f" />
        {['M-10 250 L370 215', 'M180 -10 L190 770', 'M-10 430 L370 400', 'M60 -10 L20 770', 'M300 -10 L330 770'].map((d, i) => (
          <path key={i} d={d} stroke="#3a465c" strokeWidth={i < 3 ? 16 : 9} fill="none" />
        ))}
        <path d="M180 760 L186 330" stroke={KIND.location.color} strokeWidth={7} strokeDasharray="2 14" strokeLinecap="round" opacity={ease(t, 22, 34)} />
      </svg>
      <StatusBar />
      <div style={{position: 'absolute', left: 186, top: 330, width: 0, height: 0}}>
        <div style={{position: 'absolute', left: -60, top: -60, width: 120, height: 120, borderRadius: 60, background: KIND.location.color, opacity: 0.25 * (1 - ((t * 0.03) % 1)), transform: `scale(${0.4 + ((t * 0.03) % 1) * 1.2})`}} />
        <div style={{position: 'absolute', left: -38, top: -76 - (1 - drop) * 300, opacity: Math.min(1, drop * 2)}}>
          <svg width={76} height={92} viewBox="0 0 24 29">
            <path d="M12 28 C7 21 2 16 2 10.5 A10 10 0 0 1 22 10.5 C22 16 17 21 12 28 Z" fill={KIND.location.color} />
            <circle cx={12} cy={10.5} r={4} fill="#fff" />
          </svg>
        </div>
      </div>
      <Sheet t={t - 16} height={190}>
        <div style={{fontSize: 24, fontWeight: 700}}>Asha is here</div>
        <div style={{fontSize: 17, color: C.sub, marginTop: 4}}>4 min away · Open in Maps</div>
      </Sheet>
    </div>
  );
};

export const LinkResult: React.FC<{t: number}> = ({t}) => (
  <div style={{...fill, background: '#0f1014'}}>
    <StatusBar />
    <div style={{position: 'absolute', top: 44, left: 16, right: 16, height: 46, borderRadius: 23, background: '#23252b', display: 'flex', alignItems: 'center', padding: '0 18px', fontSize: 17, color: C.sub}}>
      <span style={{color: '#6fe0a0', marginRight: 8}}>●</span>
      {'asha.design'.slice(0, Math.round(ease(t, 0, 12) * 11))}
    </div>
    <div style={{position: 'absolute', top: 110, left: 0, right: 0, opacity: ease(t, 10, 20), transform: `translateY(${(1 - ease(t, 10, 22)) * 40}px)`}}>
      <div style={{margin: '0 16px', height: 250, borderRadius: 26, background: KIND.link.color, padding: 26, boxSizing: 'border-box', display: 'flex', flexDirection: 'column', justifyContent: 'flex-end'}}>
        <div style={{fontSize: 38, fontWeight: 800, lineHeight: '42px'}}>Asha Rao</div>
        <div style={{fontSize: 19, opacity: 0.9, marginTop: 6}}>Product designer</div>
      </div>
      <div style={{display: 'flex', gap: 12, margin: '14px 16px 0'}}>
        {[0, 1].map((i) => (
          <div key={i} style={{flex: 1, height: 150, borderRadius: 22, background: '#23252b', opacity: ease(t, 18 + i * 4, 28 + i * 4)}} />
        ))}
      </div>
      <div style={{margin: '14px 16px 0', height: 16, width: 220, borderRadius: 8, background: '#2c2e35'}} />
      <div style={{margin: '10px 16px 0', height: 16, width: 150, borderRadius: 8, background: '#2c2e35'}} />
    </div>
  </div>
);

/** An NFC tag-writing app, for the Learn scene: a record ready to write, then written. */
export const WriterScreen: React.FC<{t: number; tapAt: number}> = ({t, tapAt}) => {
  const written = t >= tapAt;
  const records = [
    {glyph: 'link', color: KIND.link.color, title: 'URL', value: 'asha.design'},
    {glyph: 'contact', color: KIND.contact.color, title: 'Phone number', value: '+91 98765 43210'},
    {glyph: 'wifi', color: KIND.wifi.color, title: 'Wi-Fi network', value: 'Studio 5G'},
  ];
  return (
    <div style={{...fill, background: '#101114'}}>
      <StatusBar />
      <div style={{position: 'absolute', top: 58, left: 24, fontSize: 30, fontWeight: 800}}>Write tag</div>
      <div style={{position: 'absolute', top: 108, left: 24, fontSize: 16, color: C.dim}}>Any NFC tag-writing app</div>
      <div style={{position: 'absolute', top: 160, left: 16, right: 16}}>
        {records.map((r, i) => {
          const p = pop(t, 2 + i * 4);
          return (
            <div key={i} style={{display: 'flex', alignItems: 'center', gap: 14, padding: 16, marginBottom: 12, borderRadius: 20, background: '#22242a', opacity: Math.min(1, p * 1.5), transform: `translateY(${(1 - p) * 24}px)`}}>
              <IconDisc glyph={r.glyph} color={r.color} size={46} />
              <div>
                <div style={{fontSize: 19, fontWeight: 600}}>{r.title}</div>
                <div style={{fontSize: 15, color: C.sub}}>{r.value}</div>
              </div>
            </div>
          );
        })}
      </div>
      <div style={{position: 'absolute', left: 16, right: 16, bottom: 34}}>
        <Action t={t} doneAt={tapAt} color={KIND.learn.color} label="Write 3 records" done="Written to watch" />
      </div>
      {!written && (
        <div style={{position: 'absolute', left: 0, right: 0, bottom: 112, textAlign: 'center', fontSize: 16, color: C.sub, opacity: 0.5 + 0.5 * Math.sin(t / 4)}}>
          Hold near the watch
        </div>
      )}
    </div>
  );
};
