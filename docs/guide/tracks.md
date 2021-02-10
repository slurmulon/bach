# Tracks

Tracks can represent a loop, a song, or really any sort of rhythmic timeline.

Tracks are the highest-level concept in `bach`, so it's important to understand them on a macroscopic level before diving into other areas.

We will begin by looking at a real-world example of a track, and this track will be referenced throughout this guide.

## Example

The following track represents the chord progression of a soul song.

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

Putting special characters aside, we can see that several musical elements are clearly represented:

 - **Meter**: `4|4` or common time (default)
 - **Tempo**: `44` beats per minute
 - **Chord**: `Bm`, `Em` and `F#m7`
 - **Scale**: `B minor`
 - **Durations**: `4`, `2`

Once this track is processed and loaded into an app, it will be interpreted like so:

1. The scale `B minor` and the chord `:B`, or `Bm`, will be played for `4` whole notes, then
1. The chord `:E`, or `Em`, will be played for `2` whole notes, then
1. The chord `:B`, or `Bm`, will be played for `2` whole notes, then
1. The chord `:F`, or `F#m7`, will be played for `2` whole notes, then
1. The chord `:B`, or `Bm`, will be played for `2` whole notes, then
1. Repeat as desired

> We will go into what "processed" means in the [Authoring](#authoring) section.

Now that you have a basic understanding of what a track is and how it's interpreted, we can begin to explore the individual components that make up a track.
