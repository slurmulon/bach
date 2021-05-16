# Examples

All of the examples found here represent the loopable chord and scale progressions of backing/jam tracks.

You can use our open-source **[web editor](https://editor.codebach.tech)** to run any of the examples found here by simply copying and pasting the code into the editor and pressing play.

This collection will eventually be updated to include examples of full songs that include multiple parts.

If you have suggestions or want to contribute example tracks yourself, please head to the [Contribute](contribute) page.

## Basic

```bach
@meter = 4|4
@tempo = 128

:B = Chord('Bm')
:E = Chord('Em')
:F = Chord('F#m7')

play! [
  1 -> {
    Scale('B minor')
    :B
  }
  1/2 -> :E
  1/2 -> :B
  1/2 -> :F
  1/2 -> :B
]
```

```bach
@meter = 4|4
@tempo = 169

play! [
  7/8 -> {
    Scale('A aeolian')
    Chord('F')
  }
  1 -> Chord('G')
  2 + (1/8) -> Chord('Am')
]
```

```bach
@meter = 4|4
@tempo = 130

play! [
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

### Common meter

```
@tempo = 108

:A = [
  3/8 -> {
    Scale('E aeolian')
    Chord('Em9')
  }
  5/8 -> Chord('C')
  3/8 -> Chord('Em9')
  4/8 -> Chord('C')
  9/8 -> Chord('C/D')
]

:B = [
  3/8 -> Chord('B')
  5/8 -> Chord('Em9')
  1 -> Chord('Em9')
]

:C = [
  3/8 -> Chord('B')
  5/8 -> Chord('G')
  1 -> Chord('G')
]

play! [
  7 of :A
  2 of :B
  3 of [
    :C
    when !{ last? } do { :B }
  ]
  3/8 -> Chord('B')
  5/8 -> Chord('Em9')
  1 -> Chord('B7b13')
]
```

### Compound meters

```bach
@meter = 12|8
@tempo = 150

play! [
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

```
@meter = 6|8
@tempo = 166

:D = Chord('Dm')
:G = Chord('G7')
:C = Chord('Cmaj7')
:F = Chord('Fmaj7')
:B = Chord('Bm7b5')
:E = Chord('E7')
:A = Chord('A7')
:Am = Chord('Am')

:chorus = [
  bar -> {
    :D
    Scale('A aeolian')
  }
  bar -> :G
  bar -> :C
  bar -> :F
  bar -> :B
  bar -> :E
  bar -> :Am
]

play! [
  3 of [
    :chorus
    bar -> :A
  ]
  :chorus
  bar -> :E
  bar -> :A
  bar -> :D
  bar -> :F
  bar -> :E
  2 * bar -> :Am
]
```

### Mixed meters

```bach
@meter = 5|8
@tempo = 150

play! [
  3/8 -> {
    Scale('D dorian')
    Chord('Dm9')
  }
  2/8 -> Chord('Am9')
]
```

```bach
@meter = 3|4
@tempo = 132

play! [
  6/4 -> {
    Scale('C# phrygian')
    Chord('C#m')
  }
  6/4 -> Chord('Dmaj7')
]
```
