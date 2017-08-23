# warble

> :musical_score: Notation for musical loops and tracks with a focus on readability and productivity

---

[![Clojars Project](https://img.shields.io/clojars/v/warble.svg)](https://clojars.org/warble)

`warble` aims to establish a pragmatic and intuitive interface for representing musical loops and tracks. It assumes little about the interpreter and is founded upon traditional music theory concepts.

It is a very new project and is looking for contributors and ideas. If you would like to contribute, feel free to message me@madhax.io.

In the meantime please give the proposal a read and see if it piques your interest!

## Install

### Leinengen/Boot

`[warble "0.1.0-SNAPSHOT"]`

### Gradle

`compile "warble:warble:0.1.0-SNAPSHOT"`

### Maven

```
<dependency>
  <groupId>warble</groupId>
  <artifactId>warble</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Proposal

A more formal proposal will eventually be written, but for now this is the canonical source of documentation and ideas.

An [Extended Backus-Naur Form (EBNF)](https://en.wikipedia.org/wiki/Extended_Backus%E2%80%93Naur_Form) formatted definition of the grammar can be found in [grammar.bnf](https://github.com/slurmulon/warble/blob/master/grammar.bnf).

### Beats

`Loops` (or bars) are simply nestable collections of either `Chords`, `Scales`, `Notes`, Rests (`~`), or other loops (`[]` or `{}`). For the sake of brevity, these will be combinationally referred to as `Elements` in this proposal and in the source code.

The `Beat` at which any `Element` is played for (AKA its duration) is specified via the tuple-like `->` in a list (`[]`) or set (`{}`).

Beat tuples defined in lists will be played sequentially in the natural order and will not overlap.
Beat tuples defined in sets will be played in parallel and will overlap.

More formally:

```
[<duration> -> <set|list|element>]

N   = N measures or whole notes
1   = Whole note (one entire measure)
1/2 = Half note
1/4 = Quarter note
1/8 = Eight note
```

For instance, a loop playing a `Note(C2)` for an entire measure, starting at the first beat, would be specified like so:

```
1 -> Note('C2')
```

When a `Beat` identifier is not provided in an an assignment or list, it will be implied at run-time to be the index of each respective element as they are played, using the unit defined in the time signature (the default is common time, or `4/4`)

For instance:

```
[1/4 -> Note('C2'), 1/4 -> Note('F2')]
```

is the same as

```
[Note('C2'), Note('F2')]
```

All `Elements` must be instantiated in a `Beat` tuple (or implicitly marshalled into one), and the first parameter of every `Element` is a [`teoria`](https://github.com/saebekassebil/teoria)-supported string-like (surrounded with `'` or `"`) identifier such as `'C2'`, which is a second octave `C` note.

`Beats` may be written where the left hand side represents the duration of the beat and the right hand side of `+` represents the additional number of beats to play for:

```
1 + 1/2 -> Chord'(C2min6')
```

This is usefeul for specifying more complicated rhythms, like those seen in Jazz.
Multiple notes can be grouped together by hugging them in brackets `[ ]` and separating each element in the collection with either a `,` or whitespace:

```
:Mutliple = [
  1/2   -> Chord('D2min7')
  1+1/2 -> Chord('E2maj7')
  1+1/2 -> Chord('C2maj7')
]
```

As a convenience, elements may also be implicit, specified using either a backtick or a pound:

```
:Note  = `('C2')
:Chord = `('C2Maj7')
:Scale = `('C2 Minor')
```

or

```
:Note  = #('C2')
:Chord = #('C2Maj7')
:Scale = #('C2 Minor')
```

Determining the value of implicit elements is the responsibility of the warble interpreter.

---

`Elements` can also overlap the same `Beat` and will be played concurrently using sets (`{}`) (TODO: example)

### Loops


To assign a loop a unique name, prefix the name with the `:` operator:

```
:DasLoop = [1 -> Note('C2'), 1 -> Note('E2')]
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

Only the loops which are exported with the `!Play <identifier|element>` construct will end up being processed by the interpreter:

```
!Play :DasLoop
```

### Cadences

In music it's common to see cadence sections labeled as `A`, `B`, `C`, and so on. warble's syntax favors this nicely:

```
:A = Chord('F2maj')
:B = Chord('G2maj')
:C = Chord('C2maj')

:Song = [
  1 -> :A,
  1 -> :B,
  1 -> :C
  1 -> :A
]

!Play :Song
```

Destructured list assignments will soon be supported and will also favor cadences (**currently unsupported**):

```
[A, B, C, D] = [Chord('E7'), Chord('Emin7'), Chord('Cmaj7'), Chord('Dmaj7')]
```

### Color Labeling

Colors are useful for succesfully expressing a variety of data to the user at once:

```
:ABC = [
  1 -> {
    Scale('C2min',  #6CB359),
    Chord('D2min7', #AA5585)
  },
  1 -> Chord('G2Maj7', #D48B6A),
  2 -> Chord('C2Maj7', #FFDCAA)
]
```

Color values must be hexadecimal (no `red`, `blue`, etc. for now).

### Meta

Optional meta information about the track (aka "headers"), including the tempo and time signature, is specified with assignments at the top of the file and prefixed with the `@` operator:

```
@Title = 'My warble track'
@Time  = 4/4
@Tempo = 90
@Tags  = ['test', 'lullaby']

:ABC = [1/2 -> Chord('D2Mmin7'), 1/2 -> Chord('G2Min7'), 1 -> Chord('C2Maj7')]
```

### Play

Because Warble supports referencing with variables, it requires a mechanism for specifying which list of measures should be used for playing the track.

You can think of `Play` as your main method or default export.

```
:Song = [1 -> Chord('C2Maj7'), 1 -> Chord('A2Maj7')]

!Play :Song
```

Only one `!Play` definition is allowed per track file.

## Documentation

### Elements

 - `Note` = Single note in scientific notation
 - `Scale` = Scale in scientific notation
 - `Chord` = Chord in scientific notation
 - `~` = Rest
 - `#` = Implicit (the interpreter determines if it's scale, chord or note based on the notation itself)
 - `[]` = List (sequential / ordered)
 - `{}` = Set (parallel / unordered)

### Headers

 - `Instrument` (string, arbitrary)
 - `Title` (string, arbitrary)
 - `Tempo` (integer, beats per minute)
 - `Time` (ratio, time signature. ex: 4/4)
 - `Tags` (list or set of strings, arbitrary)

## Setup

`lein install`

## Testing

`lein test`

## Usage

```clojure
(ns my.namespace
  (:require [warble.lexer :as lexer]
            [warble.track :refer [compile-track]]))

; parses and compiles raw warble data into an interpretable hash-map
(compile-track (lexer/parse ":Foo = []"))
```

## Roadmap

 - [ ] Write technical specfiication
 - [ ] Destructured list assignments
 - [X] General work towards making the tracks iterable in a normalized fashion
 - [ ] Allow track linking with Hypermedia
 - [ ] Linkable sections with unique namespaces so that end users may bookmark and/or track progress
 - [ ] Hide Chord or Scale (so it's only functionally relevant and not highlighted to the user)
 - [ ] Note fitness / quality data (i.e. how well it fits a given scale or chord in the current context)
