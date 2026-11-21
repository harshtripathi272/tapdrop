import React from 'react';
import {AbsoluteFill, useCurrentFrame} from 'remotion';
import {C, KIND, Kind, TAP} from './timeline';
import {IconDisc, Logo} from './devices';
import {HomeList, Row, ShareCard, SleepFace} from './watchScreens';
import {ContactResult, LinkResult, MapResult, PayResult, WifiResult, WriterScreen} from './phoneScreens';
import {Reveal} from './text';
import {TapScene} from './TapScene';
import {expo, expoIn, pop, range} from './anim';

// Everything on screen is demo data. Asha Rao does not exist.

const FAR = 2600;
const KINDS: Kind[] = ['contact', 'wifi', 'pay', 'location', 'link', 'learn'];

/** Scales a watch screen in when the watch switches to it. */
const SwitchIn: React.FC<{frame: number; children: React.ReactNode}> = ({frame, children}) => (
  <div style={{position: 'absolute', inset: 0, opacity: range(frame, 0, 4), transform: `scale(${0.86 + 0.14 * pop(frame, 0, 200, 18)})`}}>{children}</div>
);

// 0 s. The whole product in one tap, before anything is explained.
export const Intro: React.FC = () => (
  <TapScene
    kind="contact" label={{glyph: 'logo', name: 'TapDrop for Wear OS'}}
    headline="One tap." hint="Hold a phone to your watch."
    result="Shared." support="They have your contact."
    watch={(f) => <ShareCard kind="contact" title="Asha Rao" subtitle="+91 98765 43210" hint="Hold to a phone" frame={f} sentAt={TAP} />}
    phoneAfter={(t) => <ContactResult t={t} />}
  />
);

// 4 s. What the watch now is, then the six kinds counted in on the half-beat.
export const Claim: React.FC = () => {
  const frame = useCurrentFrame();
  const merge = expoIn(frame, 98, 110);
  const close = frame < 106 ? 0 : expoIn(frame, 106, 120, 60, FAR);
  return (
    <AbsoluteFill style={{background: KIND.contact.color}}>
      <AbsoluteFill style={{background: C.paper, clipPath: `circle(${expo(frame, 0, 18, 0, FAR)}px at 960px 540px)`}}>
        <div style={{position: 'absolute', left: 0, right: 0, top: 215, transform: `scale(${1 + 0.03 * range(frame, 0, 120)})`}}>
          <Reveal text={'Your watch is now\nan NFC tag.'} frame={frame} at={5} gap={4} size={150} align="center" />
          <div style={{marginTop: 34}}>
            <Reveal text="Hold any phone to it. No app on their side." frame={frame} at={30} size={44} weight={500} color={C.mute} align="center" />
          </div>
        </div>
        {KINDS.map((kind, i) => {
          const at = 60 + i * 7.5;
          const p = pop(frame, at, 240, 13);
          const home = 438 + i * 184;
          const x = home + (898 - home) * merge;
          return (
            <div key={kind} style={{position: 'absolute', left: x, top: 716, width: 124, opacity: frame >= at ? 1 : 0}}>
              <div style={{transform: `scale(${p})`}}>
                <IconDisc glyph={kind} color={KIND[kind].color} size={124} />
              </div>
              <div style={{marginTop: 18, textAlign: 'center', fontSize: 28, fontWeight: 600, letterSpacing: -0.3, color: C.ink, opacity: range(frame, at + 3, at + 8) * (1 - range(frame, 96, 102))}}>
                {KIND[kind].name}
              </div>
            </div>
          );
        })}
      </AbsoluteFill>
      {/* The six discs meet in the middle and the circle they make closes the scene to black. */}
      <AbsoluteFill style={{background: '#000', clipPath: `circle(${close}px at 960px 778px)`}} />
    </AbsoluteFill>
  );
};

// 8 s. The drop: the name, on black.
export const Title: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill style={{background: '#000'}}>
      {[0, 6, 12].map((delay) => {
        const t = expo(frame, delay, delay + 40);
        const r = 110 + t * 1100;
        return frame < delay ? null : (
          <div
            key={delay}
            style={{
              position: 'absolute', left: 960 - r, top: 500 - r, width: r * 2, height: r * 2, borderRadius: '50%',
              border: `${Math.max(1, 6 - t * 5)}px solid ${C.accent}`, opacity: 1 - t, boxSizing: 'border-box',
            }}
          />
        );
      })}
      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', flexDirection: 'column', transform: `scale(${1 + 0.04 * range(frame, 0, 60)})`}}>
        <div style={{transform: `scale(${pop(frame, 0, 230, 11)})`}}>
          <Logo size={170} />
        </div>
        <div style={{marginTop: 30}}>
          <Reveal text="TapDrop" frame={frame} at={2} size={216} color="#fff" align="center" />
        </div>
        <div style={{marginTop: 18}}>
          <Reveal text="Tap your wrist. They get it." frame={frame} at={15} size={50} weight={500} color="#A1A1A6" align="center" />
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

// 10 s to 30 s. Five things people ask for, one tap each.

export const Contact: React.FC = () => (
  <TapScene
    kind="contact" prev="#000" index={1}
    headline={'At a\nmeetup.'} hint="Someone asks for your number."
    result={'Contact\nsaved.'} support="No app. No typing."
    watch={(f) => <ShareCard kind="contact" title="Asha Rao" subtitle="+91 98765 43210" hint="Hold to a phone" frame={f} sentAt={TAP} />}
    phoneAfter={(t) => <ContactResult t={t} />}
  />
);

export const Wifi: React.FC = () => (
  <TapScene
    kind="wifi" prev={KIND.contact.color} flip index={2}
    headline={'Friends\ncome over.'} hint="First question: what's the Wi-Fi?"
    result={"They're\nonline."} support="Nobody read out a password."
    watch={(f) => <ShareCard kind="wifi" title="Home 5G" subtitle="Wi-Fi network" hint="Hold to a phone" frame={f} sentAt={TAP} />}
    phoneAfter={(t) => <WifiResult t={t} />}
  />
);

export const Pay: React.FC = () => {
  const frame = useCurrentFrame();
  // Two clicks of the bezel, each on a beat, take the request from 150 to 250 rupees.
  const bezel = expo(frame, 15, 24) * 30 + expo(frame, 30, 39) * 30;
  const amount = frame < 18 ? 150 : frame < 33 ? 200 : 250;
  return (
    <TapScene
      kind="pay" prev={KIND.wifi.color} index={3} bezel={bezel}
      headline={'Splitting\nthe bill.'} hint="Turn the bezel to set the amount."
      result={'₹250\nrequested.'} support="They pick a UPI app and pay."
      watch={(f) => <ShareCard kind="pay" title={`₹${amount}`} subtitle="to Asha Rao" hint="Bezel sets the amount" frame={f} sentAt={TAP} />}
      phoneAfter={(t) => <PayResult t={t} />}
    />
  );
};

export const Location: React.FC = () => (
  <TapScene
    kind="location" prev={KIND.pay.color} flip index={4}
    headline={'Meet me\nhere.'} hint="Hand over the spot you are standing on."
    result={'Pin\ndropped.'} support="It opens in their maps app."
    watch={(f) => <ShareCard kind="location" title="My location" subtitle="12.9716, 77.5946" hint="Hold to a phone" frame={f} sentAt={TAP} />}
    phoneAfter={(t) => <MapResult t={t} />}
  />
);

export const Link: React.FC = () => (
  <TapScene
    kind="link" prev={KIND.location.color} index={5}
    headline={'Show\nyour work.'} hint="A portfolio, a menu, a sign-up form."
    result={'Link\nopened.'} support="Any link you like."
    watch={(f) => <ShareCard kind="link" title="Portfolio" subtitle="asha.design" hint="Hold to a phone" frame={f} sentAt={TAP} />}
    phoneAfter={(t) => <LinkResult t={t} />}
  />
);

const LEARNED: Row[] = [
  {kind: 'learn', title: 'asha.design', subtitle: 'Learned, 3 records'},
  {kind: 'contact', title: 'Asha Rao', subtitle: 'Contact'},
  {kind: 'wifi', title: 'Home 5G', subtitle: 'Wi-Fi'},
];

// 30 s. Learn mode: any tag-writing app can write to the watch, and the watch keeps it.
export const Learn: React.FC = () => {
  const frame = useCurrentFrame();
  const listAt = TAP + 24;
  return (
    <TapScene
      kind="learn" prev={KIND.link.color} flip index={6}
      headline={'Teach it\nanything.'} hint="Any NFC tag app can write to it."
      result={'New item\nlearned.'} support="The watch keeps it for next time."
      watch={(f) =>
        f < listAt ? (
          <ShareCard kind="learn" title="Learn" subtitle="Ready for a tag" hint="Write from any tag app" frame={f} sentAt={TAP} sentHint="Saved" />
        ) : (
          <SwitchIn frame={f - listAt}>
            <HomeList rows={LEARNED} frame={f - listAt} appear={2} highlight={0} />
          </SwitchIn>
        )
      }
      phoneBefore={<WriterScreen t={frame} tapAt={TAP} />}
      phoneAfter={(t) => <WriterScreen t={t + TAP} tapAt={TAP} />}
    />
  );
};

// 34 s. Quick tap. The music drops out with the screen, and comes back on the tap.
export const QuickTap: React.FC = () => (
  <TapScene
    kind="contact" prev={KIND.learn.color} index={7} buzz
    label={{glyph: 'check', name: 'Quick tap'}}
    headline={'App closed.\nScreen off.'} headlineGap={15}
    result={'Still\nworks.'} support="Pick one item for quick tap."
    watch={(f) => <SleepFace wake={range(f, TAP, TAP + 4) * range(f, TAP + 4, TAP + 40, 1, 0.25)} />}
    phoneAfter={(t) => <ContactResult t={t} />}
  />
);

const CARDS = [
  {ground: '#000', ink: '#fff', text: 'No app\non their phone.', size: 170},
  {ground: C.paper, ink: C.ink, text: 'No internet\npermission.', size: 170},
  {ground: C.accent, ink: '#fff', text: '50 KB.', size: 330},
  {ground: '#000', ink: '#fff', text: 'Open source.', size: 200},
];

// 38 s. Four reasons to trust it, one a second, cut hard on the beat.
export const Facts: React.FC = () => {
  const frame = useCurrentFrame();
  const i = Math.min(3, Math.floor(frame / 30));
  const local = frame - i * 30;
  const card = CARDS[i];
  return (
    <AbsoluteFill style={{background: card.ground, alignItems: 'center', justifyContent: 'center'}}>
      <div style={{transform: `scale(${1 + 0.05 * range(local, 0, 30)})`}}>
        <Reveal text={card.text} frame={local} at={0} gap={2} size={card.size} color={card.ink} align="center" lineHeight={1.02} />
      </div>
    </AbsoluteFill>
  );
};

// 42 s. Where to get it.
export const Outro: React.FC = () => {
  const frame = useCurrentFrame();
  const pill = pop(frame, 30, 200, 15);
  return (
    <AbsoluteFill style={{background: '#000'}}>
      <AbsoluteFill style={{background: C.paper, clipPath: `circle(${expo(frame, 0, 20, 0, FAR)}px at 960px 540px)`, alignItems: 'center', justifyContent: 'center', flexDirection: 'column'}}>
        <div style={{transform: `scale(${pop(frame, 2, 210, 12)})`}}>
          <Logo size={128} />
        </div>
        <div style={{marginTop: 24}}>
          <Reveal text="TapDrop" frame={frame} at={5} size={168} align="center" />
        </div>
        <div style={{marginTop: 20}}>
          <Reveal text="Your watch is the tag." frame={frame} at={15} size={46} weight={500} color={C.mute} align="center" />
        </div>
        <div
          style={{
            marginTop: 50, padding: '26px 56px', borderRadius: 60, fontSize: 42, fontWeight: 600, letterSpacing: -0.6,
            background: C.ink, color: '#fff', opacity: frame >= 30 ? 1 : 0, transform: `scale(${0.7 + 0.3 * pill})`,
          }}
        >
          github.com/harshtripathi272/tapdrop
        </div>
        <div style={{marginTop: 30}}>
          <Reveal text="Free. Give the repo to your AI coding agent and it sets up your watch." frame={frame} at={45} size={32} weight={500} color={C.mute} align="center" />
        </div>
        <div style={{display: 'flex', gap: 20, marginTop: 44}}>
          {KINDS.map((kind, i) => (
            <div key={kind} style={{transform: `scale(${pop(frame, 60 + i * 3.75, 240, 12)})`}}>
              <IconDisc glyph={kind} color={KIND[kind].color} size={58} />
            </div>
          ))}
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
