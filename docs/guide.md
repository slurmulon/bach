# Guide

The mission of this guide is to help people learn `bach` well enough to put it to practical use.

It assumes the reader has zero experience with programming and it is primarily written for musicians, since `bach` is designed to be accessible to non-technical folks.

If you are looking for a more technical and low-level resource on `bach`, head to the [Syntax](/syntax) page instead.

If you are interested in the rationale for `bach` and the problems it solves, first check out the [Background](/background) page.

## Intro

`bach` is a music notation that people and computers can easily read or write. It is entirely text-based and can be written in any text editor.

Once the notation is processed, it can be used in apps to drive anything from real-time music players to sheet music generators.

This processed notation is known as a "track". A track can represent a loop, a song, or any sort of rhythmic timeline.

Tracks are the highest-level concept in `bach`, and so it's important to understand them on a macroscopic level before diving into the details.

## Basics

### Tracks

To fully understand what a track is and how it can be used, the best place to start is to look at an example.

The following `bach` track represents a loopable chord progression for a soul song.

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
 - **Chord**: `Bm`, `F#m7`, etc.
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

#### Tempo

### Elements

### Beats

### Durations

### Variables

## Authoring

## Examples

## Limitations

 - No native semantics around instruments
 - No local repeater symbols, all tracks are currently considered loopable by default

## Help
