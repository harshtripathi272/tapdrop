import React from 'react';
import {AbsoluteFill, Audio, Sequence, staticFile} from 'remotion';
import {C, SCENES} from './timeline';
import {FONT} from './devices';
import {Claim, Contact, Facts, Intro, Learn, Link, Location, Outro, Pay, QuickTap, Title, Wifi} from './scenes';

const ORDER: [keyof typeof SCENES, React.FC][] = [
  ['intro', Intro], ['claim', Claim], ['title', Title], ['contact', Contact], ['wifi', Wifi], ['pay', Pay],
  ['location', Location], ['link', Link], ['learn', Learn], ['quickTap', QuickTap], ['facts', Facts], ['outro', Outro],
];

export const Video: React.FC = () => (
  <AbsoluteFill style={{background: C.paper, color: C.ink, fontFamily: FONT}}>
    {ORDER.map(([name, Scene]) => (
      <Sequence key={name} name={name} from={SCENES[name].from} durationInFrames={SCENES[name].length}>
        <Scene />
      </Sequence>
    ))}
    <Audio src={staticFile('track.wav')} />
  </AbsoluteFill>
);
