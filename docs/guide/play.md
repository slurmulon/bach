# Play

The final but arguably most important component of `bach` is `!Play`.
The reason it's so important is because it specifies the main entrypoint of a track.

In other words, `bach` looks for the `!Play` element first and foremost, then it processes everything referenced by `!Play` from there.

```bach
@Tempo = 82

!Play [
  8 -> {
    Scale('G mixolydian')
    Chord('G7')
  }
  4 -> Chord('C7')
  4 -> Chord('G7')
]
```

Only one `!Play` element is allowed and expected per track, and anything that isn't referenced by the element will be **ignored** during processing.
