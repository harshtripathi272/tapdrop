// The whole video on one page. 30 fps at 120 BPM, so a beat is 15 frames and a bar
// is 60. Every scene starts on a bar line, and every tap lands on the downbeat of
// the scene's second bar. scripts/make-audio.mjs follows the same times.
export const FPS = 30;
export const BEAT = 15;
export const WIDTH = 1920;
export const HEIGHT = 1080;

/** Frame within a tap scene at which the phone touches the watch: 2.0 s in. */
export const TAP = 60;

export const SCENES = {
  intro: {from: 0, length: 120},       //  0 s  one tap, before anything is explained
  claim: {from: 120, length: 120},     //  4 s  what the watch now is, and the six kinds
  title: {from: 240, length: 60},      //  8 s  the drop: the name
  contact: {from: 300, length: 120},   // 10 s
  wifi: {from: 420, length: 120},      // 14 s
  pay: {from: 540, length: 120},       // 18 s
  location: {from: 660, length: 120},  // 22 s
  link: {from: 780, length: 120},      // 26 s
  learn: {from: 900, length: 120},     // 30 s
  quickTap: {from: 1020, length: 120}, // 34 s  the music drops out with the screen
  facts: {from: 1140, length: 120},    // 38 s  four cards, one a second
  outro: {from: 1260, length: 120},    // 42 s
} as const;

export const TOTAL = 1380; // 46 s

export const C = {
  // The stage: one light ground, one ink, one accent. Nothing else.
  paper: '#F5F5F7',
  ink: '#1D1D1F',
  mute: '#6E6E73',
  accent: '#4D7CFE',
  // The watch and phone screens, in the app's own dark colours.
  bg: '#07080B',
  container: '#252527',
  divider: '#47474B',
  text: '#FFFFFF',
  sub: '#B4B4B8',
  dim: '#8A8A8E',
};

export type Kind = 'contact' | 'wifi' | 'pay' | 'location' | 'link' | 'learn';

/** The app's own colours. A tap floods the stage with the colour of what was shared. */
export const KIND: Record<Kind, {color: string; name: string}> = {
  contact: {color: '#5B74E8', name: 'Contact'},
  wifi: {color: '#2FA968', name: 'Wi-Fi'},
  pay: {color: '#E8922E', name: 'Pay me'},
  location: {color: '#E5566A', name: 'Location'},
  link: {color: '#8E63D9', name: 'Link'},
  learn: {color: '#2E9AAF', name: 'Learn'},
};
