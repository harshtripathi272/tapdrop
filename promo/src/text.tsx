import React from 'react';
import {C} from './timeline';
import {Icon, IconDisc, Logo} from './devices';
import {expo, range} from './anim';

/**
 * Lines of type that rise into place from behind an invisible edge, one after the
 * other. No fades. `\n` starts a new line and `gap` is the delay between lines.
 */
export const Reveal: React.FC<{
  text: string; frame: number; at?: number; gap?: number; size: number; weight?: number; color?: string;
  align?: 'left' | 'center'; lineHeight?: number; tracking?: number;
}> = ({text, frame, at = 0, gap = 3, size, weight = 700, color = C.ink, align = 'left', lineHeight = 1.06, tracking}) => (
  <div style={{fontSize: size, fontWeight: weight, color, lineHeight, textAlign: align, letterSpacing: tracking ?? (size >= 60 ? -size * 0.045 : -size * 0.015)}}>
    {text.split('\n').map((line, i) => {
      const start = at + i * gap;
      return (
        // The padding keeps descenders inside the mask; the negative margin gives the space back.
        <div key={i} style={{overflow: 'hidden', padding: `0 ${size * 0.06}px ${size * 0.16}px`, margin: `0 ${-size * 0.06}px ${-size * 0.16}px`}}>
          <div style={{transform: `translateY(${(1 - expo(frame, start, start + 16)) * 118}%)`}}>{line}</div>
        </div>
      );
    })}
  </div>
);

/** The small line above a headline: the item's disc and its name. `onColor` is the version for a flooded stage. */
export const Label: React.FC<{glyph: string; name: string; color: string; onColor?: boolean; frame: number}> = ({glyph, name, color, onColor = false, frame}) => (
  <div style={{display: 'flex', alignItems: 'center', gap: 16, marginBottom: 28, opacity: range(frame, 2, 7), transform: `translateY(${(1 - expo(frame, 2, 16)) * 24}px)`}}>
    {glyph === 'logo' ? (
      <Logo size={54} invert={onColor} />
    ) : (
      <IconDisc glyph={glyph} color={onColor ? '#fff' : color} iconColor={onColor ? color : '#fff'} size={54} />
    )}
    <span style={{fontSize: 30, fontWeight: 600, letterSpacing: -0.4, color: onColor ? '#fff' : color}}>{name}</span>
  </div>
);

/** The corners every feature scene shares: the mark top left, the count top right. */
export const Chrome: React.FC<{onColor?: boolean; index: number; total?: number}> = ({onColor = false, index, total = 7}) => (
  <>
    <div style={{position: 'absolute', left: 64, top: 50, display: 'flex', alignItems: 'center', gap: 12, fontSize: 26, fontWeight: 600, letterSpacing: -0.4, color: onColor ? '#fff' : C.ink}}>
      <Logo size={36} invert={onColor} />
      TapDrop
    </div>
    <div style={{position: 'absolute', right: 64, top: 54, fontSize: 24, fontWeight: 500, fontVariantNumeric: 'tabular-nums', color: onColor ? 'rgba(255,255,255,0.85)' : C.mute}}>
      {index} / {total}
    </div>
  </>
);

/** A tick in a small disc, for the line under a result. */
export const Tick: React.FC<{size: number; color: string; ground: string}> = ({size, color, ground}) => (
  <div style={{width: size, height: size, borderRadius: size, background: ground, flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
    <Icon glyph="check" size={size * 0.64} color={color} />
  </div>
);
