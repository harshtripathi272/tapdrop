import {Easing, interpolate, spring} from 'remotion';
import {FPS} from './timeline';

const CLAMP = {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'} as const;

/** Linear map from a frame range to a value range, held at both ends. */
export const range = (frame: number, a: number, b: number, from = 0, to = 1) =>
  interpolate(frame, [a, b], [from, to], CLAMP);

/** The same, easing out: fast start, soft landing. */
export const ease = (frame: number, a: number, b: number, from = 0, to = 1) =>
  interpolate(frame, [a, b], [from, to], {...CLAMP, easing: Easing.out(Easing.cubic)});

/** A much sharper ease out. Most of the move happens in the first few frames. */
export const expo = (frame: number, a: number, b: number, from = 0, to = 1) =>
  interpolate(frame, [a, b], [from, to], {...CLAMP, easing: Easing.out(Easing.exp)});

/** The mirror image: barely moves, then rushes to the end. For exits and wipes into a cut. */
export const expoIn = (frame: number, a: number, b: number, from = 0, to = 1) =>
  interpolate(frame, [a, b], [from, to], {...CLAMP, easing: Easing.in(Easing.exp)});

/** 0 to 1 with a little overshoot, starting at `delay`. */
export const pop = (frame: number, delay = 0, stiffness = 190, damping = 15) =>
  spring({frame: frame - delay, fps: FPS, config: {stiffness, damping, mass: 0.7}});

/** A 0 to 1 to 0 bump centred on `at`, used for the hit of a tap. */
export const bump = (frame: number, at: number, attack = 3, release = 12) =>
  frame < at ? range(frame, at - attack, at, 0, 1) : range(frame, at, at + release, 1, 0);
