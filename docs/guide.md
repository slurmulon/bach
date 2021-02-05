# Guide

The purpose of this guide is to help people learn `bach` well enough to put it to practical use.

It assumes the reader has zero experience with programming and it is primarily written for musicians, since `bach` is designed to be accessible to non-technical folks.

It also assumes that the reader has at least a basic understanding of music theory concepts such as tempo, meter, beats and measures.

If you are looking for a more technical and low-level resource on `bach`, head to the [Syntax](/syntax) page instead.

If you are interested in the rationale for `bach` and the problems it solves, first check out the [Background](/background) page.

## Intro

`bach` is a music notation that people and computers can easily read or write. It is entirely text-based and can be written in any text editor.

`bach` is different from other music notations because it is **semantic**, meaning it allows you to work with semantic music constructs such as chords and scales without needing to know the details.

Once the notation is compiled it can be used by apps to drive anything from real-time music players to sheet music generators.

## Basics

### Tracks

Tracks can represent a loop, a song, or really any sort of rhythmic timeline.

Tracks are the highest-level concept in `bach`, and so it's important to understand them on a macroscopic level before diving into the details.

We will begin by looking at a real-world example of a track. The following represents a chord progression for a soul song.


```bach
@Meter = 4|4
@Tempo = 44

:B = Chord('Bm')
:E = Chord('Em')
:F = Chord('F#m7')

!Play [
  4 -> {
    Scale('B minor')
    :B
  }
  2 -> :E
  2 -> :B
  2 -> :F
  2 -> :B
]
```

Putting special characters aside, we can easily see that several musical elements are represented here:

 - **Meter**: `4|4` or common time (default)
 - **Tempo**: `44` beats per minute
 - **Chord**: `Bm`, `Em` and `F#m7`
 - **Scale**: `B minor`
 - **Durations**: `4`, `2`

After this track is processed and loaded into an app, it will be interpreted like so:

1. The scale `B minor` and the chord `:B`, or `Bm`, will be played for `4` whole notes, then
1. The chord `:E`, or `Em`, will be played for `2` whole notes, then
1. The chord `:B`, or `Bm`, will be played for `2` whole notes, then
1. The chord `:F`, or `F#m7`, will be played for `2` whole notes, then
1. The chord `:B`, or `Bm`, will be played for `2` whole notes, then
1. Repeat as desired

Now that we have a basic understanding of what a track looks like and how it is interpreted, we can begin to explore the individual components that make up a track.

### Headers

Headers are simply meta-data, or data that describes other data.

Headers can only be defined at the top of the track, since they are contextual and used to describe or influence the track as a whole.

Headers are defined using the `@` symbol, followed by a unique name, an equal (`=`) sign, and then a value.

This example shows how we specify the meter and tempo of a track:

```bach
@Meter = 3|4
@Tempo = 110
```

`bach` allows you to define any headers you like, but `@Meter` and `@Tempo` are special and reserved since they influence how `bach` calculates beat durations.

#### Meter

The meter of a track defines its time signature, which specifies two important pieces of information: the number of beats in a measure and the beat unit.

```bach
@Meter = 5|8
```

The above meter tells `bach` that the track has 5 beats in a measure and that an 1/8th note is the beat unit, or the value of an individual beat.

When a meter is not specified in your track, `bach` assumes you are using common time (`4|4`).

As of now the meter can only be defined once and cannot change at any point in the track.

#### Tempo

The tempo of the track determines how many beats there are in a minute (otherwise known as "bpm").

When a tempo is not specified, `bach` defaults to 120 bpm.

Tempos can be expressed as either an integer (e.g. `120`) or a decimal (e.g. `110.5`).

As with meter, the tempo can only be defined once and cannot currently change at a later point.

### Elements

Elements are where

### Beats

### Collections

### Durations

> TODO: Mention how durations in a list determine determine not only the duration but act as a moving cursor throughout the list

> TODO: Provide an example using rests (and one without) to outline the above

The value of a duration can be an integer, a fraction, or a mathematical expression composed of either.

```
1    = Whole note (or one entire measure when `@Meter = 4|4`)
1/2  = Half note
1/4  = Quarter note
1/8  = Eighth note
1/16 = Sixteenth note

1/512 = Minimum duration
1024  = Maximum duration

2 + (1/2) = Two and a half whole notes
2 * (6/8) = Two and a half measures when `@Meter = 6|8`
```

To adhere with music theory, durations are strictly based on **common time** (`@Meter = 4|4`).

This means that, in your definitions, `1` always means 4 quarter notes, and only equates with a full measure when the number of beats in a measure is 4 (as in `4|4`, `3|4`, `5|4`, etc.).

The examples in the remainder of this section assume common time, since this is the default when a `@Meter` header is not provided.

#### Examples

A list playing a `Note('C2')` for an entire measure, starting at the first beat in the measure, would be specified like so:

```
[1 -> Note('C2')]
```

If you wanted to start playing the note on the second beat of the measure, then simply rest (`~`) on the first beat:

```
[1/4 -> ~, 1 -> Note('C2')]
```

When a beat is defined without a duration (in other words, just an element), the duration is implied to be one beat of the meter.

For instance, when the meter is common time:

```
[1/4 -> Note('C2'), 1/4 -> Note('F2')]
```

is the same as:

```
[Note('C2'), Note('F2')]
```

`Beat` durations can also use basic mathematical operators. This makes the translation between sheet music and `bach` an easy task.

```
1 + 1/2 -> Chord'(C2min6')
```

This is usefeul for specifying more complicated rhythms, like those seen in jazz.

```
:PartA = [
  1/2   -> Chord('D2min7')
  1+1/2 -> Chord('E2maj7')
  1+1/2 -> Chord('C2maj7')
]
```



### Variables

## Authoring

Now that we are familiar with the fundamentals, we can begin putting `bach` to practical use by authoring some tracks.

Regardless of your level of familiarity or expertise, the ideal way to write `bach` tracks is to always start off with a similar example.

It's much better to start off with a block of marble and carve out a sculpture than to slowly build up a sculpture piece by piece.

That is why we have provided a robust collection of [examples tracks](#examples) for you to copy and modify to your liking.

But before we can even begin to make use of these examples (let alone change or build upon them), we must become familiar with the tools available to us.

### Tooling

### Audio

You have a `bach` track written, so how do we associate and synchronize it with audio?

The first thing to note is that, in order to keep `bach` simple and focused, `bach` doesn't explicitly concern itself with audio data.

By taking this approach, it allows `bach` to rhythmically align with music produced by a human or to generate music on a computer.

On a practical level, this means it's up to your editor or application to associate audio data with your `bach` tracks.

## Examples

### Basic

```bach
@Meter = 4|4
@Tempo = 44

:B = Chord('Bm')
:E = Chord('Em')
:F = Chord('F#m7')

!Play [
  4 -> {
    Scale('B minor')
    :B
  }
  2 -> :E
  2 -> :B
  2 -> :F
  2 -> :B
]
```

```bach
@Meter = 4|4
@Tempo = 169

!Play [
  7/8 -> {
    Scale('A aeolian')
    Chord('F')
  }
  1 -> Chord('G')
  2 + (1/8) -> Chord('Am')
]
```

```bach
@Meter = 4|4
@Tempo = 130

!Play [
  3/8 -> {
    Scale('G aeolian')
    Chord('Gmin')
  }
  5/8 -> Chord('Eb')
  3/8 -> Chord('Cmin7')
  5/8 -> Chord('Bb')
]
```

### Advanced

#### Compound meters

```bach
@Meter = 12|8
@Tempo = 150

!Play [
  12/8 -> {
    Scale('A minor')
    Chord('A')
  }
  12/8 -> Chord('A7')
  12/8 -> Chord('D7')
  12/8 -> Chord('D#7')

  6/8 -> Chord('A')
  6/8 -> Chord('F#m7')

  6/8 -> Chord('Bm7')
  6/8 -> Chord('E7')

  6/8 -> Chord('A7')
  6/8 -> Chord('D7')
]
```

#### Mixed meters

```bach
@Meter = 5|8
@Tempo = 150

!Play [
  3/8 -> {
    Scale('D dorian')
    Chord('Dm9')
  }
  2/8 -> Chord('Am9')
]
```

```bach
@Meter = 3|4
@Tempo = 100

!Play [
  6/4 -> {
    Scale('C# phrygian')
    Chord('C#m')
  }
  6/4 -> Chord('Dmaj7')
]
```


## Limitations

 - No native semantics around instruments
 - No local repeater symbols, all tracks are currently considered loopable by default

## Help
