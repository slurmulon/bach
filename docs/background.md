# Background

If you have some experience in music production then you're aware that there are many ways to digitally notate and represent music.

You have guitar tabs, MIDI, sheet music generators, Digital Audio Workstations (DAWs) and much more.

But in the digital world of music the native language behind many tools is undoubtedly MIDI.

So if MIDI is such a dominant force then how is something like `bach` even necessary or relevant?

The next section answers that question as we discuss the problems and conditions that led to the creation of `bach`.

## Problems

It should be first made clear that the issues discussed here are not intended to cast MIDI or any other digital music standards in a negative light.

MIDI is ubiquitous and has stood the test of time for good reason. It allows musicians to digitally capture anything played on their MIDI instrument with a high degree of fidelity.

Because MIDI allows you to explicitly work with a note's pitch, notation, tempo and more, it becomes possible to control and modify notes individually or as a group easier than ever before.

This has been a life-changing technology for many musicians who can now digitally fine-tuning their notes instead of recording multiple takes or relying on indirect audio production techniques. The portable and shareable nature of MIDI further strengthens its dominant position among the masses.

But as with anything in life, MIDI is not perfect, and as music technology has evolved it has become obvious that there is potential for new technology to complement and fill the areas where MIDI and other music notations come short.

### Semantics

One limitation of MIDI is that it doesn't concern itself with high-level music constructs such as scales and chords. This is a great thing on a technical level since it keeps MIDI simple and focused, but on a practical level this can introduce limitations that inhibit productivity.

This means that, for most people, any time you need to write in a chord in your MIDI editor you have to determine which pitches/notes make up that chord and individually insert each of them to make up the entire chord.

Music theory (and music experience in general) makes it clear that identical chords and scales have identical notes, so there's ultimately no good reason to force musicians to re-determine these notes each time they want to write in a chord.

It's no different than forcing somebody to say "(2 x 8) - 6" instead of just saying "10".

In fact, pretty much all of the existing digital music notations suffer from this overlooked problem. 
Consider the following examples of the ABC and GUIDO notations (from Wikipedia):

**ABC**

```abc
<score lang="ABC">
X:1
T:The Legacy Jig
M:6/8
L:1/8
R:jig
K:G
GFG BAB | gfg gab | GFG BAB | d2A AFD |
GFG BAB | gfg gab | age edB |1 dBA AFD :|2 dBA ABd |:
efe edB | dBA ABd | efe edB | gdB ABd |
efe edB | d2d def | gfe edB |1 dBA ABd :|2 dBA AFD |]
</score>
```

**GUIDO**

```guido
[ \clef<"treble"> \key<"D"> \meter<"4/4">
 a1*1/2 b a/4. g/8 f#/4 g a/2 b a/4. g/8 f#/4 g
 a/2 a b c#2/4 d c#/2 b1 a/1 ]
```

With enough squinting of the eyes the information being represented in these notations becomes relatively clear.

But it's safe to say that neither of them can be considered easy to read or write for a non-technical person. They also don't support high-level musical constructs, instead working with individual notes like MIDI does.

MusicXML, another digital music notation, allows the problem of lacking musical constructs to be addressed. But its verbose nature means it also cannot be realistically written or understood by non-technical folks.

### Editors

Even if you have a great editor and workflow that avoids these limitations of MIDI, the problem is that the concept of, say, a chord, is still bound to your editor and your editor alone.

When you go into your editor and say "input a Cmin chord", at the end of the day it is only storing this information as MIDI, which is rightfully ignorant of chords, scales, etc.

If your editor is robust enough to auto-detect chords and scales when importing project changes, you're still dependent on the editor calculating this information instead of just reading it from **data**.

This is because musical semantics are naturally lost when your editor saves to MIDI, and therefore anyone who wants or needs the same working experience as you has to match your setup.

This might be perfectly fine for well-funded or small localized teams, but it doesn't work when you're trying to work in a productive manner with wider and less organized groups of people.

Given all of these considerations, it was decided to introduce `bach` into the world.

## Solutions

`bach` is a novel music notation because it addresses both of the major problems of alternative digital music notations.

 - Human-friendly and easy to read and write
 - Supports semantic music constructs such as chords and scales


