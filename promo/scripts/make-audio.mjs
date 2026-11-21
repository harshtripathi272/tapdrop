// Composes the promo's soundtrack and writes it to public/track.wav. Every sound is
// synthesised here, so there is no music licence to worry about.
//
// 120 BPM in A minor, 46 seconds. A bar is 2 s and a chord lasts one bar. The times
// mirror src/timeline.ts: change one and you must change the other.
//
//    0 s  intro: the hook on a mallet, finger snaps, a bell on the first tap at 2 s
//    4 s  build: bass and claps come in, six rising notes count in the six item kinds
//    8 s  the drop, after an eighth of a beat of silence
//   10 s  full groove. A bell and a low boom land on every tap: 12, 16, 20 ... 32 s
//   34 s  breakdown: the drums drop out while the watch's screen is off
//   36 s  everything returns on the quick-tap
//   38 s  a stab on each of the four fact cards
//   42 s  last chord, ringing out
import {writeFileSync, mkdirSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';

const SR = 44100;
const DURATION = 46;
const N = SR * DURATION;
const TAU = 2 * Math.PI;

// Separate buses, so the bass and chords can duck under the kick and each group can be
// brought to a known level at the end.
const bus = () => [new Float32Array(N), new Float32Array(N)];
const DRUMS = bus();
const BASS = bus();
const MUSIC = bus();
const FX = bus();
const SEND = bus(); // what goes to the reverb

const at = (t) => Math.round(t * SR);
const put = (b, i, l, r) => {
  if (i >= 0 && i < N) {
    b[0][i] += l;
    b[1][i] += r === undefined ? l : r;
  }
};

// A tiny deterministic noise source, so every render of the track is identical.
let seed = 22222;
const noise = () => {
  seed = (seed * 1664525 + 1013904223) >>> 0;
  return seed / 2147483648 - 1;
};

// ---- instruments -----------------------------------------------------------------

const KICKS = [];
function kick(t, g = 1) {
  KICKS.push(t);
  const s = at(t);
  let phase = 0;
  for (let i = 0; i < SR * 0.4; i++) {
    const x = i / SR;
    phase += (TAU * (46 + 140 * Math.exp(-x * 40))) / SR;
    const click = i < SR * 0.004 ? noise() * 0.25 * (1 - i / (SR * 0.004)) : 0;
    put(DRUMS, s + i, (Math.sin(phase) * Math.min(1, x * 800) * Math.exp(-x * 7) + click) * g);
  }
}

// Three quick bursts and a tail are what make noise read as a clap.
function clap(t, g = 1) {
  const s = at(t);
  let low = 0;
  let band = 0;
  for (let i = 0; i < SR * 0.28; i++) {
    const x = i / SR;
    let env = x >= 0.03 ? Math.exp(-(x - 0.03) * 20) : 0;
    for (const d of [0, 0.011, 0.021]) if (x >= d && x < d + 0.01) env = Math.max(env, Math.exp(-(x - d) * 320));
    const n = noise();
    low += 0.45 * (n - low);
    band += 0.6 * (n - low - band);
    const v = band * env * 0.5 * g;
    put(DRUMS, s + i, v, v * 0.92);
    put(SEND, s + i, v * 0.2);
  }
}

function hat(t, g = 1, pan = 0, open = false) {
  const s = at(t);
  const length = open ? 0.3 : 0.06;
  const decay = open ? 13 : 85;
  let a = 0;
  let b = 0;
  for (let i = 0; i < SR * length; i++) {
    const x = i / SR;
    const n = noise();
    a += 0.82 * (n - a);
    const high = n - a;
    b += 0.75 * (high - b);
    const v = (high - b) * Math.exp(-x * decay) * 0.3 * g;
    put(DRUMS, s + i, v * (1 - pan), v * (1 + pan));
  }
}

// A sine for weight, with a filtered saw an octave up so it is still heard on a phone speaker.
function bass(t, freq, length, g = 1) {
  const s = at(t);
  let phase = 0;
  let low = 0;
  for (let i = 0; i < SR * length; i++) {
    const x = i / SR;
    phase += freq / SR;
    const saw = 2 * ((phase * 2) % 1) - 1;
    low += (0.04 + 0.25 * Math.exp(-x * 16)) * (saw - low);
    const env = Math.min(1, x * 300) * Math.min(1, (length - x) * 50) * (0.75 + 0.25 * Math.exp(-x * 5));
    put(BASS, s + i, (Math.sin(TAU * phase) * 0.62 + low * 0.3 + Math.sin(TAU * phase * 4) * 0.1) * env * g);
  }
}

// A chord of detuned saws through a filter that snaps shut: the bright, short stab.
const DETUNE = [-0.14, -0.07, 0, 0.07, 0.14];
function stab(t, notes, length, g = 1, bright = 1) {
  const s = at(t);
  const voices = [];
  for (const note of notes) {
    DETUNE.forEach((d, k) => voices.push({f: note * 2 ** (d / 12), p: (k * 0.37 + note * 0.013) % 1, pan: (k - 2) * 0.35}));
  }
  let l1 = 0, l2 = 0, r1 = 0, r2 = 0;
  const total = length + 0.25;
  for (let i = 0; i < SR * total; i++) {
    const x = i / SR;
    let l = 0;
    let r = 0;
    for (const v of voices) {
      v.p += v.f / SR;
      if (v.p >= 1) v.p -= 1;
      const saw = 2 * v.p - 1;
      l += saw * (1 - v.pan * 0.5);
      r += saw * (1 + v.pan * 0.5);
    }
    const cutoff = Math.min(0.9, (0.05 + 0.45 * Math.exp(-x * 8)) * bright);
    l1 += cutoff * (l - l1);
    l2 += cutoff * (l1 - l2);
    r1 += cutoff * (r - r1);
    r2 += cutoff * (r1 - r2);
    const env = Math.min(1, x * 250) * Math.exp(-x * 3.5) * (x < length ? 1 : Math.max(0, 1 - (x - length) / 0.25));
    put(MUSIC, s + i, l2 * env * 0.034 * g, r2 * env * 0.034 * g);
    put(SEND, s + i, (l2 + r2) * env * 0.004 * g);
  }
}

// The lead: a two-operator FM mallet, bright at the strike and round after it.
function mallet(t, freq, g = 1, pan = 0, length = 0.7) {
  const s = at(t);
  for (let i = 0; i < SR * length; i++) {
    const x = i / SR;
    const mod = Math.sin(TAU * freq * 3 * x) * (2.4 * Math.exp(-x * 20) + 0.25);
    const tone = Math.sin(TAU * freq * x + mod) + 0.35 * Math.sin(TAU * freq * 2 * x) * Math.exp(-x * 9);
    const v = tone * Math.min(1, x * 600) * Math.exp(-x * 6) * 0.2 * g;
    put(MUSIC, s + i, v * (1 - pan), v * (1 + pan));
    put(SEND, s + i, v * 0.35);
  }
}

// A slow, dark chord under everything.
function pad(t, notes, length, g = 1) {
  const s = at(t);
  const phases = notes.flatMap(() => [0, 0]);
  let lowL = 0;
  let lowR = 0;
  for (let i = 0; i < SR * length; i++) {
    const x = i / SR;
    let l = 0;
    let r = 0;
    notes.forEach((f, k) => {
      phases[k * 2] += (f * 1.004) / SR;
      phases[k * 2 + 1] += (f * 0.996) / SR;
      l += 2 * (phases[k * 2] % 1) - 1;
      r += 2 * (phases[k * 2 + 1] % 1) - 1;
    });
    lowL += 0.04 * (l - lowL);
    lowR += 0.04 * (r - lowR);
    const env = Math.min(1, x / 0.3) * Math.min(1, (length - x) / 0.4);
    put(MUSIC, s + i, lowL * env * 0.05 * g, lowR * env * 0.05 * g);
  }
}

// The sound of something landing on a phone: FM bells in the chord of the moment.
function bell(t, freqs, g = 1) {
  const s = at(t);
  for (let i = 0; i < SR * 2; i++) {
    const x = i / SR;
    let v = 0;
    freqs.forEach((f, k) => {
      const d = k * 0.06;
      if (x >= d) v += Math.sin(TAU * f * (x - d) + Math.sin(TAU * f * 3.5 * (x - d)) * 2.2 * Math.exp(-(x - d) * 5)) * Math.exp(-(x - d) * 2.8);
    });
    v *= Math.min(1, x * 900) * 0.34 * g;
    put(FX, s + i, v);
    put(SEND, s + i, v * 0.5);
  }
}

// The weight under a tap: a sine that falls away, and a puff of dull noise.
function boom(t, g = 1) {
  const s = at(t);
  let phase = 0;
  let low = 0;
  for (let i = 0; i < SR * 1.2; i++) {
    const x = i / SR;
    phase += (TAU * (38 + 70 * Math.exp(-x * 9))) / SR;
    low += 0.08 * (noise() - low);
    put(FX, s + i, (Math.sin(phase) * Math.exp(-x * 3.2) + low * Math.exp(-x * 12) * 1.5) * Math.min(1, x * 400) * 0.9 * g);
  }
}

function thud(t, g = 1) {
  const s = at(t);
  let phase = 0;
  for (let i = 0; i < SR * 0.35; i++) {
    const x = i / SR;
    phase += (TAU * (44 + 30 * Math.exp(-x * 30))) / SR;
    put(FX, s + i, Math.sin(phase) * Math.min(1, x * 500) * Math.exp(-x * 10) * 0.7 * g);
  }
}

function crash(t, g = 1) {
  const s = at(t);
  let low = 0;
  for (let i = 0; i < SR * 2.5; i++) {
    const x = i / SR;
    const n = noise();
    low += 0.6 * (n - low);
    const v = (n - low) * Math.exp(-x * 2.2) * 0.3 * g;
    put(FX, s + i, v, v * 0.9);
    put(SEND, s + i, v * 0.2);
  }
}

// Noise that swells into a moment and stops dead on it.
function swell(end, length, g = 1) {
  const s = at(end - length);
  let low = 0;
  for (let i = 0; i < SR * length; i++) {
    const x = i / (SR * length);
    const n = noise();
    low += (0.03 + 0.6 * x * x) * (n - low);
    const pan = x * 2 - 1;
    const v = low * x * x * x * 0.35 * g;
    put(FX, s + i, v * (1 - pan * 0.4), v * (1 + pan * 0.4));
  }
}

// The long rise into the drop: opening noise over a climbing tone.
function riser(from, to, g = 1) {
  const s = at(from);
  const length = to - from;
  let low = 0;
  let phase = 0;
  for (let i = 0; i < SR * length; i++) {
    const x = i / (SR * length);
    const n = noise();
    low += (0.02 + 0.5 * x * x) * (n - low);
    phase += (TAU * (220 * 2 ** (x * 2.5))) / SR;
    put(FX, s + i, (low * 0.4 + Math.sin(phase) * 0.12) * x * x * g);
  }
}

// One click of the bezel.
function tick(t, g = 1) {
  const s = at(t);
  for (let i = 0; i < SR * 0.03; i++) {
    const x = i / SR;
    const v = (Math.sin(TAU * 2600 * x) + noise() * 0.6) * Math.exp(-x * 260) * 0.5 * g;
    put(FX, s + i, v);
    put(SEND, s + i, v * 0.25);
  }
}

// ---- the tune ---------------------------------------------------------------------

// `lead` is the hook: eight eighth-notes to the bar, 0 for a rest.
const AM = {sub: 55.0, notes: [220.0, 261.63, 329.63, 440.0], lead: [659.25, 0, 659.25, 587.33, 0, 523.25, 0, 440.0]};
const F = {sub: 43.65, notes: [174.61, 220.0, 261.63, 349.23], lead: [523.25, 0, 523.25, 587.33, 0, 659.25, 783.99, 0]};
const CM = {sub: 65.41, notes: [196.0, 261.63, 329.63, 392.0], lead: [659.25, 0, 659.25, 587.33, 0, 523.25, 0, 392.0]};
const G = {sub: 49.0, notes: [196.0, 246.94, 293.66, 392.0], lead: [587.33, 0, 587.33, 659.25, 0, 587.33, 493.88, 0]};
const LOOP = [AM, F, CM, G];
const chordAt = (t) => (t < 8 ? LOOP[Math.floor(t / 2)] : t < 10 || t >= 42 ? CM : LOOP[Math.floor((t - 10) / 2) % 4]);

const DROP = 8;
const END = 42;
const TAPS = [2, 12, 16, 20, 24, 28, 32, 36];
const CUTS = [10, 14, 18, 22, 26, 30, 34, 38, 39, 40, 41];

for (let t = 0; t < END; t += 2) {
  const chord = chordAt(t);
  const slot = (k) => t + k * 0.25;
  const full = (t >= DROP && t < 34) || t >= 36;
  const breakdown = t >= 34 && t < 36;

  pad(t, chord.notes.slice(0, 3), 2.05, breakdown ? 1.4 : full ? 0.8 : 1);

  if (breakdown) {
    // "App closed." and "Screen off." land on these two thuds. Then a clock, and nothing else.
    thud(t, 1);
    thud(t + 0.5, 1);
    for (let b = 0; b < 4; b++) hat(t + b * 0.5 + 0.25, 0.3);
    continue;
  }

  // The hook. Bar 6 leaves room for the six rising notes instead.
  if (t !== 6) chord.lead.forEach((f, k) => f && mallet(slot(k), f, t < 4 ? 0.7 : t < DROP ? 0.85 : 1, k % 2 ? 0.3 : -0.3));
  // From the third feature on, a quiet sixteenth-note sparkle on top.
  if (full && t >= 18) {
    for (let k = 0; k < 16; k++) mallet(t + k * 0.125, chord.notes[[0, 1, 2, 3, 2, 1][k % 6]] * 4, 0.26, k % 2 ? -0.6 : 0.6, 0.25);
  }

  if (t >= 2) {
    bass(slot(0), chord.sub, 0.34);
    bass(slot(3), chord.sub, 0.34);
    bass(slot(6), chord.sub, 0.24);
    if (full) bass(slot(5), chord.sub * 2, 0.1, 0.6);
  }

  if (full) {
    for (const k of [0, 3, 6]) stab(slot(k), chord.notes, 0.3);
    for (const k of [0, 2, 4, 6]) kick(slot(k));
    for (const k of [2, 6]) clap(slot(k));
    for (let k = 0; k < 8; k++) {
      if (k % 2) hat(slot(k), 0.8, 0.2);
      hat(slot(k) + 0.125, 0.22, k % 2 ? -0.3 : 0.3);
    }
    if ((t / 2) % 2 === 1) hat(slot(7), 0.6, 0, true);
  } else if (t >= 4) {
    // The build: the stabs open up bar by bar, the kick is only on one and three.
    for (const k of [0, 3, 6]) stab(slot(k), chord.notes, 0.3, 0.6, 0.4 + (t - 4) * 0.12);
    kick(slot(0), 0.8);
    if (t < 6) kick(slot(4), 0.8);
    clap(slot(2), 0.7);
    if (t < 6) clap(slot(6), 0.7);
    for (const k of [1, 3, 5, 7]) if (slot(k) < 7) hat(slot(k), 0.5, 0.2);
  } else {
    clap(slot(2), 0.3);
    clap(slot(6), 0.3);
    if (t === 0) thud(0, 0.9);
  }
}

// Six rising notes, one for each item kind as its disc appears.
[440.0, 523.25, 587.33, 659.25, 783.99, 880.0].forEach((f, k) => mallet(6 + k * 0.25, f, 1.15, 0, 0.9));

// The roll and the rise into the drop.
riser(6, 7.875, 1);
for (let k = 0; k < 4; k++) clap(7 + k * 0.125, 0.3 + k * 0.06);
for (let k = 0; k < 6; k++) clap(7.5 + k * 0.0625, 0.55 + k * 0.07);

crash(DROP, 1);
boom(DROP, 1);

for (const t of TAPS) {
  const chord = chordAt(t);
  bell(t, [chord.notes[2] * 2, chord.notes[0] * 4, chord.notes[1] * 4], 1);
  boom(t, 1);
  swell(t, 0.5, 0.9);
}
crash(36, 0.8);
for (const t of CUTS) swell(t, 0.3, 0.6);

// The two clicks of the bezel in the pay scene.
tick(18.5);
tick(19.0);

// One hit for each fact card.
for (const t of [38, 39, 40, 41]) {
  stab(t, [...chordAt(t).notes, chordAt(t).notes[0] * 2], 0.5, 1.3, 1.3);
  boom(t, 0.5);
}

// The last chord.
crash(END, 0.9);
boom(END, 1);
stab(END, [130.81, 196.0, 261.63, 329.63, 392.0, 523.25], 3.0, 1.4, 1.3);
pad(END, [261.63, 329.63, 392.0], 3.9, 1.6);
bass(END, 65.41, 2.5, 1);
bell(END, [523.25, 783.99, 1046.5], 0.9);
[523.25, 659.25, 783.99, 1046.5, 1318.5].forEach((f, k) => mallet(END + 0.25 + k * 0.125, f, 0.8, k % 2 ? 0.4 : -0.4, 1.2));

// ---- mix ---------------------------------------------------------------------------

// The eighth of a beat of silence before the drop. The reverb is left to ring through it.
const gapFrom = at(DROP - 0.125);
const gapTo = at(DROP);
const ramp = Math.round(SR * 0.004);
for (const b of [DRUMS, BASS, MUSIC, FX, SEND]) {
  for (let i = gapFrom - ramp; i < gapTo; i++) {
    const g = i < gapFrom ? (gapFrom - i) / ramp : 0;
    b[0][i] *= g;
    b[1][i] *= g;
  }
}

// Sidechain: the bass and the chords duck out of the way of every kick, which is
// what gives the groove its pump.
const duck = new Float32Array(N).fill(1);
for (const t of KICKS) {
  const s = at(t);
  for (let i = 0; i < SR * 0.4 && s + i < N; i++) duck[s + i] = Math.min(duck[s + i], 1 - Math.exp(-(i / SR) * 11));
}
for (let i = 0; i < N; i++) {
  const bassGain = 0.2 + 0.8 * duck[i];
  const musicGain = 0.45 + 0.55 * duck[i];
  BASS[0][i] *= bassGain;
  BASS[1][i] *= bassGain;
  MUSIC[0][i] *= musicGain;
  MUSIC[1][i] *= musicGain;
}

// A small room: eight comb filters into four all-pass filters on each side.
function reverb(input, offset) {
  const sum = new Float32Array(N);
  for (const base of [1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617]) {
    const d = base + offset;
    const buf = new Float32Array(d);
    let idx = 0;
    let damp = 0;
    for (let i = 0; i < N; i++) {
      const y = buf[idx];
      damp = y * 0.7 + damp * 0.3;
      buf[idx] = input[i] + damp * 0.84;
      if (++idx === d) idx = 0;
      sum[i] += y;
    }
  }
  let signal = sum;
  for (const base of [556, 441, 341, 225]) {
    const d = base + offset;
    const buf = new Float32Array(d);
    const out = new Float32Array(N);
    let idx = 0;
    for (let i = 0; i < N; i++) {
      const delayed = buf[idx];
      out[i] = delayed - signal[i];
      buf[idx] = signal[i] + delayed * 0.5;
      if (++idx === d) idx = 0;
    }
    signal = out;
  }
  return signal;
}
const WET = [reverb(SEND[0], 0), reverb(SEND[1], 23)];

// Bring each group to a set peak, so the balance does not depend on guessed gains.
const peakOf = (b) => {
  let p = 0;
  for (let i = 0; i < N; i++) p = Math.max(p, Math.abs(b[0][i]), Math.abs(b[1][i]));
  return p;
};
const LEVELS = [
  ['drums', DRUMS, 0.9],
  ['bass', BASS, 0.5],
  ['music', MUSIC, 0.5],
  ['fx', FX, 0.62],
  ['reverb', WET, 0.2],
];
const L = new Float32Array(N);
const R = new Float32Array(N);
for (const [name, b, target] of LEVELS) {
  const peak = peakOf(b);
  const g = peak > 0 ? target / peak : 0;
  console.log(`${name.padEnd(7)} peak ${peak.toFixed(3)} -> ${target}`);
  for (let i = 0; i < N; i++) {
    L[i] += b[0][i] * g;
    R[i] += b[1][i] * g;
  }
}

// Master: take out rumble below hearing, round off the peaks, normalise, fade the end.
let hpL = 0;
let hpR = 0;
let peak = 0;
for (let i = 0; i < N; i++) {
  hpL += 0.004 * (L[i] - hpL);
  hpR += 0.004 * (R[i] - hpR);
  L[i] = Math.tanh((L[i] - hpL) * 1.25);
  R[i] = Math.tanh((R[i] - hpR) * 1.25);
  peak = Math.max(peak, Math.abs(L[i]), Math.abs(R[i]));
}
const scale = 0.92 / peak;
const fade = SR * 1.0;

const data = Buffer.alloc(44 + N * 4);
data.write('RIFF', 0);
data.writeUInt32LE(36 + N * 4, 4);
data.write('WAVEfmt ', 8);
data.writeUInt32LE(16, 16);
data.writeUInt16LE(1, 20);
data.writeUInt16LE(2, 22);
data.writeUInt32LE(SR, 24);
data.writeUInt32LE(SR * 4, 28);
data.writeUInt16LE(4, 32);
data.writeUInt16LE(16, 34);
data.write('data', 36);
data.writeUInt32LE(N * 4, 40);
for (let i = 0; i < N; i++) {
  const out = i > N - fade ? (N - i) / fade : 1;
  data.writeInt16LE(Math.round(L[i] * scale * out * 32767), 44 + i * 4);
  data.writeInt16LE(Math.round(R[i] * scale * out * 32767), 46 + i * 4);
}

const target = join(dirname(fileURLToPath(import.meta.url)), '..', 'public', 'track.wav');
mkdirSync(dirname(target), {recursive: true});
writeFileSync(target, data);
console.log(`wrote ${target} (${DURATION}s, ${(data.length / 1048576).toFixed(1)} MB)`);
