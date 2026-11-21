import React from 'react';
import {AbsoluteFill, Easing, interpolate, useCurrentFrame} from 'remotion';
import {C, KIND, Kind, TAP} from './timeline';
import {Phone, Ripple, Watch, phoneHeight} from './devices';
import {PhoneIdle} from './phoneScreens';
import {Chrome, Label, Reveal, Tick} from './text';
import {bump, expo, pop, range} from './anim';

const WATCH = 520;
const PHONE = 380;
const CY = 540;
const FAR = 2600; // a radius that covers the frame from any point on it
const CLAMP = {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'} as const;

type Copy = {
  /** Disc and name above the headline. Defaults to the item kind. */
  label?: {glyph: string; name: string};
  /** Big line before the tap: the situation. */
  headline: string;
  headlineGap?: number;
  /** Small line under it. */
  hint?: string;
  /** Big line after the tap: what just happened. */
  result: string;
  /** Small line under that. */
  support: string;
};

/**
 * The shot the video is built from. On a light stage, a line of type sits beside the
 * watch. A phone swings in and touches the watch at frame TAP, and the item's colour
 * floods the frame from the point of contact, replacing the line with the result.
 *
 * `prev` is the colour the last scene ended on. This scene opens by wiping the light
 * stage over it from the watch outwards, so every cut is a circle, like every tap.
 */
export const TapScene: React.FC<Copy & {
  kind: Kind; prev?: string; flip?: boolean; index?: number; bezel?: number; buzz?: boolean;
  watch: (frame: number) => React.ReactNode;
  phoneBefore?: React.ReactNode;
  phoneAfter: (sinceTap: number) => React.ReactNode;
}> = ({kind, prev, flip = false, index, bezel = 0, buzz = false, label, headline, headlineGap = 3, hint, result, support, watch, phoneBefore, phoneAfter}) => {
  const frame = useCurrentFrame();
  const dir = flip ? -1 : 1;
  const color = KIND[kind].color;
  const tag = label ?? {glyph: kind, name: KIND[kind].name};
  const watchX = flip ? 770 : 1150;
  const textLeft = flip ? 1130 : 120;
  const hit = bump(frame, TAP, 2, 14);

  // The phone: off screen, accelerating into the watch, a short hold, then out to rest.
  const touch = watchX + dir * (WATCH / 2 + PHONE / 2 - 84);
  const rest = watchX + dir * (WATCH / 2 + PHONE / 2 + 50);
  const away = watchX + dir * 1150;
  const landed = frame > TAP + 5;
  const phoneX = landed
    ? expo(frame, TAP + 5, TAP + 30, touch, rest)
    : interpolate(frame, [TAP - 22, TAP], [away, touch], {...CLAMP, easing: Easing.in(Easing.quad)});
  const phoneTilt = landed
    ? expo(frame, TAP + 5, TAP + 30, dir * -7, 0)
    : interpolate(frame, [TAP - 22, TAP], [dir * 20, dir * -7], {...CLAMP, easing: Easing.in(Easing.quad)});
  const phoneY = CY + range(frame, TAP - 22, TAP, 90, 0);
  const phoneScale = 1 - 0.03 * hit;

  const contactX = watchX + dir * (WATCH / 2 - 34);
  const shake = buzz && frame >= TAP ? Math.sin((frame - TAP) * 2.6) * 8 * range(frame, TAP, TAP + 18, 1, 0) : 0;
  const watchScale = (0.8 + 0.2 * pop(frame, 1, 150, 16)) * (1 + 0.04 * hit);
  // A slow push in for the whole shot, with a small punch on the tap.
  const camera = 1 + 0.03 * range(frame, 0, 120) + 0.018 * hit;

  const block = {position: 'absolute', left: textLeft, top: 318, width: 700} as const;

  return (
    <AbsoluteFill style={{background: prev ?? C.paper}}>
      <AbsoluteFill style={{transform: `scale(${camera})`}}>
        {/* The light stage, wiped in over the last scene's colour. */}
        <AbsoluteFill style={{background: C.paper, clipPath: prev ? `circle(${expo(frame, 0, 18, 0, FAR)}px at ${watchX}px ${CY}px)` : undefined}}>
          {index !== undefined && <Chrome index={index} />}
          <div style={block}>
            <Label glyph={tag.glyph} name={tag.name} color={tag.glyph === 'logo' ? C.ink : color} frame={frame} />
            <Reveal text={headline} frame={frame} at={5} gap={headlineGap} size={112} />
            {hint && (
              <div style={{marginTop: 30}}>
                <Reveal text={hint} frame={frame} at={16} size={38} weight={500} color={C.mute} lineHeight={1.3} />
              </div>
            )}
          </div>
        </AbsoluteFill>

        {/* The flood: the item's colour, growing from where the phone touched. */}
        <AbsoluteFill style={{background: color, clipPath: `circle(${frame < TAP ? 0 : expo(frame, TAP, TAP + 20, 0, FAR)}px at ${contactX}px ${CY}px)`}}>
          {index !== undefined && <Chrome index={index} onColor />}
          <div style={block}>
            <Label glyph={tag.glyph} name={tag.name} color={color} onColor frame={99} />
            <Reveal text={result} frame={frame} at={TAP + 3} size={112} color="#fff" />
            <div style={{display: 'flex', alignItems: 'center', gap: 16, marginTop: 30, opacity: frame >= TAP + 12 ? 1 : 0}}>
              <div style={{transform: `scale(${pop(frame, TAP + 12, 220, 13)})`}}>
                <Tick size={44} color={color} ground="#fff" />
              </div>
              <Reveal text={support} frame={frame} at={TAP + 13} size={38} weight={500} color="#fff" lineHeight={1.3} />
            </div>
          </div>
        </AbsoluteFill>

        {/* Rings leave the point of contact, behind the devices. */}
        <Ripple t={frame - TAP} x={contactX} y={CY} />

        <div style={{position: 'absolute', left: watchX - WATCH / 2 - dir * 18 * hit + shake, top: CY - WATCH / 2, transform: `scale(${watchScale})`}}>
          <Watch size={WATCH} bezel={bezel}>{watch(frame)}</Watch>
        </div>

        <div
          style={{
            position: 'absolute', left: phoneX - PHONE / 2, top: phoneY - phoneHeight(PHONE) / 2,
            transform: `rotate(${phoneTilt}deg) scale(${phoneScale})`,
          }}
        >
          <Phone width={PHONE}>{frame < TAP ? phoneBefore ?? <PhoneIdle /> : phoneAfter(frame - TAP)}</Phone>
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
