import React from 'react';
import {loadFont} from '@remotion/google-fonts/Inter';
import {C} from './timeline';
import {expo} from './anim';

// latin-ext carries the rupee sign.
export const {fontFamily: FONT} = loadFont('normal', {
  weights: ['400', '500', '600', '700', '800'],
  subsets: ['latin', 'latin-ext'],
});

/** The app's line icons, the same shapes Icons.java draws on the watch. */
export const Icon: React.FC<{glyph: string; size: number; color?: string}> = ({glyph, size, color = '#fff'}) => {
  const stroke = {stroke: color, strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round', fill: 'none'} as const;
  const shapes: Record<string, React.ReactNode> = {
    contact: (
      <>
        <circle cx={12} cy={8.5} r={3.5} {...stroke} />
        <path d="M5 19.5 C5 15.5 8.5 14 12 14 C15.5 14 19 15.5 19 19.5" {...stroke} />
      </>
    ),
    wifi: (
      <>
        <circle cx={12} cy={18.5} r={1.3} fill={color} />
        <path d="M8.46 14.96 A5 5 0 0 1 15.54 14.96" {...stroke} />
        <path d="M5.46 11.96 A9.25 9.25 0 0 1 18.54 11.96" {...stroke} />
        <path d="M2.45 8.95 A13.5 13.5 0 0 1 21.55 8.95" {...stroke} />
      </>
    ),
    pay: (
      <text x={12} y={18.4} fontSize={18} fontWeight={700} textAnchor="middle" fill={color} fontFamily={FONT}>
        ₹
      </text>
    ),
    location: (
      <>
        <path d="M12 21.5 C8 17.5 5.5 14 5.5 10 A6.5 6.5 0 0 1 18.5 10 C18.5 14 16 17.5 12 21.5 Z" {...stroke} />
        <circle cx={12} cy={10} r={2.2} {...stroke} />
      </>
    ),
    link: (
      <g transform="rotate(-45 12 12)">
        <path d="M10 7.5 H8 A4.5 4.5 0 0 0 8 16.5 H10 M14 7.5 H16 A4.5 4.5 0 0 1 16 16.5 H14 M9 12 H15" {...stroke} />
      </g>
    ),
    learn: (
      <>
        <path d="M4 8 H11.5 L16 12 L11.5 16 H4 Z" {...stroke} />
        <circle cx={7.5} cy={12} r={1.1} fill={color} />
        <path d="M18.89 7.79 A4.5 5.5 0 0 1 18.89 16.21" {...stroke} />
      </>
    ),
    check: <path d="M5.5 12.5 L10 17 L18.5 7.5" {...stroke} />,
    plus: <path d="M12 6 V18 M6 12 H18" {...stroke} />,
  };
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" style={{display: 'block'}}>
      {shapes[glyph]}
    </svg>
  );
};

/** A solid disc in the item's colour with a white icon, as in the app's lists. */
export const IconDisc: React.FC<{glyph: string; color: string; size: number; iconColor?: string}> = ({glyph, color, size, iconColor = '#fff'}) => (
  <div
    style={{
      width: size, height: size, borderRadius: size, background: color, flexShrink: 0,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}
  >
    <Icon glyph={glyph} size={size * 0.6} color={iconColor} />
  </div>
);

/** The TapDrop mark: a drop over two ripples. `invert` is for coloured grounds: white tile, blue drop. */
export const Logo: React.FC<{size: number; invert?: boolean}> = ({size, invert = false}) => {
  const tile = invert ? '#fff' : C.accent;
  const mark = invert ? C.accent : '#fff';
  return (
    <div style={{width: size, height: size, borderRadius: size * 0.26, background: tile, flexShrink: 0}}>
      <svg width={size} height={size} viewBox="0 0 108 108" style={{display: 'block'}}>
        <path d="M54 26 C54 26 41 42 41 52 A13 13 0 0 0 67 52 C67 42 54 26 54 26 Z" fill={mark} />
        <path d="M38 72 Q54 80 70 72" fill="none" stroke={mark} strokeWidth={4.5} strokeLinecap="round" />
        <path d="M31 79 Q54 91 77 79" fill="none" stroke={mark} strokeOpacity={0.7} strokeWidth={4.5} strokeLinecap="round" />
      </svg>
    </div>
  );
};

/**
 * The watch, drawn flat: a strap that runs off the top and bottom of the frame, a
 * rounded-square case, and a round bezel that turns. Children are laid out on a
 * 438 x 438 canvas, the real screen's size, so app screens use the app's own numbers.
 */
export const Watch: React.FC<{size: number; bezel?: number; children: React.ReactNode}> = ({size, bezel = 0, children}) => {
  const screen = size * 0.76;
  const ticks = Array.from({length: 60}, (_, i) => i);
  return (
    <div style={{position: 'relative', width: size, height: size}}>
      <div style={{position: 'absolute', left: size * 0.19, width: size * 0.62, top: -size * 2.5, height: size * 6, background: '#2B2B2F'}}>
        <div style={{position: 'absolute', left: '7%', top: 0, bottom: 0, width: 2, background: 'rgba(0,0,0,0.28)'}} />
        <div style={{position: 'absolute', right: '7%', top: 0, bottom: 0, width: 2, background: 'rgba(0,0,0,0.28)'}} />
      </div>
      {[0.3, 0.57].map((top) => (
        <div
          key={top}
          style={{position: 'absolute', left: size * 0.975, top: size * top, width: size * 0.05, height: size * 0.13, borderRadius: size * 0.018, background: '#19191B'}}
        />
      ))}
      <div
        style={{
          position: 'absolute', inset: 0, borderRadius: size * 0.31, background: '#19191B',
          boxShadow: `0 ${size * 0.05}px ${size * 0.13}px rgba(0,0,0,0.3)`,
        }}
      />
      <svg width={size} height={size} viewBox="0 0 100 100" style={{position: 'absolute', inset: 0, transform: `rotate(${bezel}deg)`}}>
        <circle cx={50} cy={50} r={47.6} fill="#0A0A0B" />
        <circle cx={50} cy={50} r={47.6} fill="none" stroke="#3A3A3F" strokeWidth={0.35} />
        {ticks.map((i) => {
          const major = i % 5 === 0;
          return (
            <line
              key={i} x1={50} y1={major ? 4.4 : 5.4} x2={50} y2={major ? 9.6 : 8.2}
              stroke={major ? '#E4E4E8' : '#6A6A70'} strokeWidth={major ? 0.75 : 0.4} strokeLinecap="round"
              transform={`rotate(${i * 6} 50 50)`}
            />
          );
        })}
      </svg>
      <div
        style={{
          position: 'absolute', left: (size - screen) / 2, top: (size - screen) / 2, width: screen, height: screen,
          borderRadius: '50%', overflow: 'hidden', background: '#000',
        }}
      >
        <div style={{width: 438, height: 438, transform: `scale(${screen / 438})`, transformOrigin: '0 0', position: 'relative', fontFamily: FONT, color: '#fff'}}>
          {children}
        </div>
      </div>
    </div>
  );
};

/** Height of a phone of the given width, so callers can centre it. */
export const phoneHeight = (width: number) => 760 * ((width - width * 0.06) / 360) + width * 0.06;

/** A phone, drawn flat. Children are laid out on a 360 x 760 canvas and scaled to fit. */
export const Phone: React.FC<{width: number; children: React.ReactNode}> = ({width, children}) => {
  const frame = width * 0.03;
  const scale = (width - frame * 2) / 360;
  return (
    <div
      style={{
        width, height: phoneHeight(width), borderRadius: width * 0.15, padding: frame, boxSizing: 'border-box',
        background: '#19191B', boxShadow: `0 ${width * 0.08}px ${width * 0.2}px rgba(0,0,0,0.28)`,
      }}
    >
      <div style={{width: '100%', height: '100%', borderRadius: width * 0.122, overflow: 'hidden', background: '#000', position: 'relative'}}>
        <div style={{width: 360, height: 760, transform: `scale(${scale})`, transformOrigin: '0 0', position: 'relative', fontFamily: FONT, color: '#fff'}}>
          {children}
          <div style={{position: 'absolute', top: 12, left: 164, width: 32, height: 12, borderRadius: 8, background: '#000'}} />
        </div>
      </div>
    </div>
  );
};

/** Flat rings leaving the point where watch and phone meet. `t` is frames since the tap. */
export const Ripple: React.FC<{t: number; x: number; y: number; color?: string; reach?: number}> = ({t, x, y, color = '#fff', reach = 620}) => {
  if (t < 0 || t > 40) return null;
  return (
    <div style={{position: 'absolute', left: x, top: y, width: 0, height: 0}}>
      {[0, 7].map((delay) => {
        const p = expo(t, delay, delay + 30);
        if (t < delay) return null;
        const r = 120 + p * reach;
        return (
          <div
            key={delay}
            style={{
              position: 'absolute', left: -r, top: -r, width: r * 2, height: r * 2, borderRadius: '50%',
              border: `${Math.max(2, 16 - p * 14)}px solid ${color}`, opacity: (1 - p) * 0.9, boxSizing: 'border-box',
            }}
          />
        );
      })}
    </div>
  );
};
