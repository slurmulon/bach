# Guide

The mission of this guide is to help people learn `bach` well enough to put it to practical use.

It assumes the reader has zero experience with programming and is primarily written for musicians, since `bach` is designed to be accessible to non-technical folks.

If you are looking for a more technical and low-level resource on `bach`, head to the [Syntax](/syntax) page instead.

## Background

Anybody with experience in music production is aware that there are many ways to notate music.

You have sheet music, guitar tabs, Digital Audio Workstations (DAWs), MIDI and much more.

But in the digital world of music, the native language behind it all is undoubtedly MIDI.

So if MIDI is king then how is `bach` even necessary or relevant? The next section answers that question as we discuss the problems and conditions that led to the creation of `bach`.

### Problem

MIDI is ubiquitous and has stood the test of time for good reason. It allows musicians to digitally capture anything played on their MIDI instrument with a high degree of fidelity.

Because MIDI allows you to explicitly work with pitches, bends, durations and more, it becomes possible to control and modify notes individually or as a group.

This is a superior experience for many musicians who prefer digitally fine-tuning their notes instead of recording multiple takes or relying on indirect audio production techniques. The portable and shareable nature of MIDI further strengthens its dominant position among the masses.

One limitation of MIDI is that it doesn't concern itself with high-level music constructs such as scales and chords. This is a great thing on a technical level since it keeps MIDI simple and focused, but on a practical level this can introduce limitations that severely inhibit productivity.

This means that any time you need to write in a chord in your MIDI editor you have to determine which pitches/notes make up that chord and individually insert each of them to make up the entire chord.

Music theory (and music experience in general) makes it clear that identical chords and scales have identical notes, so there's ultimately no good reason to force musicians to re-determine these notes each time they want to write in a chord. It's no different than forcing somebody to say "(2 x 8) - 6" instead of just saying "10".

In fact, pretty much all of the existing digital music notations suffer from this overlooked problem. ABC and GUIDO are certainly easier to read and write than MIDI, but they still fail to address essential high-level music constructs such as scales and chords.

Consider the following examples from Wikipedia:

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

But it's safe to say that neither of them can be considered easy to read or write for a non-technical person. They also don't support high-level musical constructs, instead working with individual notes like MIDI.

MusicXML allows the problem of lacking musical constructs to be addressed, but its verbose nature means it cannot be realistically written or understood by non-technical folks.

Given all of these considerations, it was decided to bring `bach` into the world.

`bach` is a novel music notation because it addresses both of the major problems of alternative digital music notations.

 - Human-friendly and easy to read and write
 - Supports semantic music constructs such as chords and scales
