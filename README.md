# warble

> :musical_score: ABC-inspired notation for representing musical tracks and loops with a focus on readability and productivity

---

`warble` aims to establish a pragmatic and intuitive interface for representing musical loops and tracks. It assumes little about the interpreter and is founded upon traditional music theory concepts.

It is a very new project and is looking for contributors and ideas. If you would like to contribute, feel free to message me@madhax.io.

In the meantime please give the proposal a read and see if it piques your interest!

## Proposal

A more formal proposal will eventually be written, but for now this is the canonical source of documentation and ideas.

An [Extended Backus-Naur Form (EBNF)](https://en.wikipedia.org/wiki/Extended_Backus%E2%80%93Naur_Form) formatted definition of the grammar can be found in [grammar.bnf](https://github.com/slurmulon/warble/blob/master/grammar.bnf).

### Beats

`Loops` (or tracks) are simply nested collections of either `Chords`, `Scales`, `Notes`, `Rests`, or other `Loops`. For the sake of brevity, these will be combinationally referred to as `Elements` in this proposal and potential in the source code.

The `Beat` at which any `Element` may be played at is specified via a tuple-like `->`. For instance, a loop playing a `Note(C2)` on the first beat of the measure would be written as such:

```
1 -> Note('C2')
```

All `Elements` must be instantiated in a `Beat` tuple, and the first parameter of every `Element` is a [`teoria`](https://github.com/saebekassebil/teoria)-supported string-like (surrounded with `'` or `"`) identifier such as `'C2'`, which is a second octave `C` note.

`Beats` may be written where the left hand side represents the starting beat and the right hand side of `+` represents the additional number of beats to delay playing:

```
1 + 1/2 -> Chord'(C2min6')
```

This is usefeul for specifying more complicated rhythms, like those seen in Jazz.
Multiple notes can be grouped together by hugging them in brackets `[ ]` and separating each element in the collection with a `,`:

```
:Mutliple = [
  1/2   -> Chord('D2min7'),
  2+1/2 -> Chord('E2maj7'),
  3+1/2 -> Chord('C2maj7')
]
```

---

`Elements` can also overlap the same `Beat` and will be played concurrently (TODO: example)

### Loops


To assign a loop a unique name, prefix the name with the `:` operator:

```
:DasLoop = [1 -> Note('C2')]
```

Once assigned a name, any `Element` may be dynamically referenced in other loops:

```
:CoolLooop = [:DasLoop]
```

`Elements` in collections will be played in sequential order:

```
:Ordered = [:First, :Second, :Third]
```

Multiple `Elements` may be played on a single beat:

```
:DasLoop = [
  1 -> [
    Chord('D2min'),
    Note('C2')
  ]
]
```

### Sequences and Transitions

`Elements` will continue to play until another `Element` is encountered. Multple chords cannot be played at once, but a `Scale`, `Note`, and `Chord` can be played simultaneously:

```
:ABC = [
  1 -> [
    [
      1 -> Scale('C2 Minor'),
      1 -> Note('E2'),
      1+1/2 -> Note('B2')
    ],
    Chord('D2min7'),
  ],
  2 -> Chord('G2Maj7'),
  3 -> Chord('C2Maj7')
]

:DEF = [
  1 -> [
    Scale('D2 Major'),
    Chord('E2maj7')
  ],
  2+1/2 -> Chord('A2min7'),
  3+1/2 -> Chord('D2maj7')
]

Play [:ABC, :DEF] Forever
```

There will be support down the road for named sequences so that individual instruments / layers of the track can be more easily worked with.

### Color Labeling

Colors are useful for succesfully expressing a variety of data to the user at once:

```
@ABC = [
  1 -> [
    Scale('C2min',  #6CB359),
    Chord('D2min7', #AA5585)
  ],
  2 -> Chord('G2Maj7', #D48B6A),
  3 -> Chord('C2Maj7', #FFDCAA)
]
```

Color values must be hexadecimal (no `red`, `blue`, etc. for now).

## Documentation

### Elements

 - `Note`
 - `Scale`
 - `Chord`
 - `Rest`
 - `Tempo`
 - `TimeSig`
 - `Times`
 - `Play`
 - `Stop`
 - `Forever`

## Setup

`lein install`

## Testing

`lein test`

## Usage

```clojure
(ns my.namespace
  (:require [warble.lexer :as lexer]))

(lexer/tokenize ":Foo = []")

```

## Roadmap

 - [ ] Write technical specfiication
 - [ ] Create bi-directional [Overtone](https://github.com/overtone/overtone/) interface and parser
 - [ ] Create [SuperCollider](http://supercollider.github.io/) interface and parser
 - [ ] Hide Chord or Scale (so it's only functionally relevant and not highlighted to the user)
