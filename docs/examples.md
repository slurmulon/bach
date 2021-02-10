# Examples

All of the examples found here represent the loopable chord and scale progressions of backing/jam tracks.

This collection will eventually be updated to include examples of full songs that include multiple parts.

If you have suggestions or want to contribute example tracks yourself, please head to the [Contribute](contribute) page.

> :construction:
>
> Tooling support is currently limited and none of these example tracks have runnable audio (yet, but will soon).
>
> We are working on an open-source `bach` editor that will allow you to play all of these examples with their associated audio, generated via the Web Audio API.
>
> Until then, you will have to use your imagination, or use something like `tone.js` (until we write an official `bach-tone` library, that is).

## Basic

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

## Advanced

### Compound meters

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

### Mixed meters

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