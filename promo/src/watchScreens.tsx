import React from 'react';
import {C, KIND, Kind} from './timeline';
import {Icon, IconDisc} from './devices';
import {pop, range} from './anim';

// Screens for the watch, laid out on its real 438 x 438 canvas and copied from the app.

const fill = {position: 'absolute', inset: 0} as const;
const centred = {position: 'absolute', left: 0, width: 438, textAlign: 'center'} as const;

/**
 * The share screen. Rings travel to the edge while the watch is ready. From `sentAt`
 * on, one bright ring bursts out, the icon becomes a tick and the hint reads "Sent".
 */
export const ShareCard: React.FC<{
  kind: Kind; title: string; subtitle: string; hint: string; frame: number; sentAt?: number; sentHint?: string;
}> = ({kind, title, subtitle, hint, frame, sentAt = 99999, sentHint = 'Sent'}) => {
  const color = KIND[kind].color;
  const sent = frame >= sentAt;
  const since = frame - sentAt;
  const size = title.length <= 7 ? 58 : title.length <= 10 ? 50 : title.length <= 13 ? 42 : 34;
  const iconScale = sent ? 1 + 0.25 * Math.sin(range(since, 0, 12) * Math.PI) : 1;

  return (
    <div style={{...fill, background: '#000'}}>
      <svg width={438} height={438} style={fill}>
        <circle cx={219} cy={219} r={210} fill="none" stroke={color} strokeWidth={sent ? 8 : 4} opacity={sent ? 1 : 0.42} />
        {!sent &&
          [0, 36].map((offset) => {
            const t = ((frame + offset) % 72) / 72;
            const eased = 1 - (1 - t) * (1 - t);
            return (
              <circle
                key={offset} cx={219} cy={219} r={210 * (0.86 + 0.14 * eased)} fill="none" stroke={color}
                strokeWidth={5 - 3 * t} opacity={0.6 * (1 - t) * (1 - t)}
              />
            );
          })}
        {sent && since < 16 && (
          <circle
            cx={219} cy={107 + range(since, 0, 16) * 112} r={30 + range(since, 0, 16) * 180} fill="none" stroke={color}
            strokeWidth={16 - range(since, 0, 16) * 12} opacity={1 - range(since, 0, 16)}
          />
        )}
      </svg>
      <div style={{position: 'absolute', left: 189, top: 77, transform: `scale(${iconScale})`}}>
        <IconDisc glyph={sent ? 'check' : kind} color={color} size={60} />
      </div>
      <div style={{...centred, top: 158, fontSize: size, lineHeight: '64px', fontWeight: 700, color: C.text, letterSpacing: -0.5}}>
        {title}
      </div>
      <div style={{...centred, top: 228, fontSize: 23, color: C.sub}}>{subtitle}</div>
      <div style={{...centred, top: 276, fontSize: 20, fontWeight: sent ? 700 : 400, color: sent ? C.text : C.dim}}>
        {sent ? sentHint : hint}
      </div>
      <div
        style={{
          position: 'absolute', left: 149, top: 330, width: 140, height: 70, borderRadius: 35, background: C.container,
          display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 26, fontWeight: 500, color: C.text,
        }}
      >
        Edit
      </div>
    </div>
  );
};

export type Row = {kind: Kind; title: string; subtitle: string};

/** The home list: rows in one rounded container. `appear` staggers them in from that frame. */
export const HomeList: React.FC<{rows: Row[]; frame: number; appear?: number; scroll?: number; highlight?: number}> = ({
  rows, frame, appear = 0, scroll = 0, highlight = -1,
}) => (
  <div style={{...fill, background: '#000'}}>
    <div style={{position: 'absolute', left: 0, top: -scroll, width: 438}}>
      <div style={{textAlign: 'center', fontSize: 31, fontWeight: 700, color: C.text, paddingTop: 56, paddingBottom: 26}}>TapDrop</div>
      <div style={{margin: '0 13px', borderRadius: 50, background: C.container, overflow: 'hidden'}}>
        {rows.map((row, i) => {
          const p = pop(frame, appear + i * 3);
          return (
            <div key={i}>
              {i > 0 && <div style={{height: 2, margin: '0 34px', background: C.divider}} />}
              <div
                style={{
                  display: 'flex', alignItems: 'center', padding: '23px 28px 23px 23px', opacity: Math.min(1, p * 1.4),
                  transform: `translateY(${(1 - p) * 26}px)`, background: i === highlight ? 'rgba(255,255,255,0.1)' : 'transparent',
                }}
              >
                <IconDisc glyph={row.kind} color={KIND[row.kind].color} size={64} />
                <div style={{marginLeft: 21}}>
                  <div style={{fontSize: 32, color: C.text, lineHeight: '38px'}}>{row.title}</div>
                  <div style={{fontSize: 25, color: C.sub, lineHeight: '31px'}}>{row.subtitle}</div>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  </div>
);

/** The watch asleep: a dim clock on black. `wake` 0 to 1 brightens it. */
export const SleepFace: React.FC<{wake?: number}> = ({wake = 0}) => (
  <div style={{...fill, background: '#000'}}>
    <div style={{...centred, top: 140, fontSize: 118, fontWeight: 600, letterSpacing: -3, color: '#fff', opacity: 0.16 + wake * 0.2}}>10:09</div>
    <div style={{...centred, top: 268, fontSize: 26, color: '#fff', opacity: 0.12 + wake * 0.14}}>Sat 21 Nov</div>
  </div>
);

/** A small tick badge used where the watch confirms something off the share screen. */
export const Confirm: React.FC<{frame: number; at: number; color: string; label: string}> = ({frame, at, color, label}) => {
  const p = pop(frame, at);
  if (frame < at) return null;
  return (
    <div style={{...fill, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.82)'}}>
      <div style={{width: 120, height: 120, borderRadius: 60, background: color, display: 'flex', alignItems: 'center', justifyContent: 'center', transform: `scale(${p})`}}>
        <Icon glyph="check" size={76} />
      </div>
      <div style={{marginTop: 22, fontSize: 34, fontWeight: 700, color: C.text, opacity: p}}>{label}</div>
    </div>
  );
};
