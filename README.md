# warble

> :musical_score: Notation for musical loops and tracks with a focus on readability and productivity

---

[![Clojars Project](https://img.shields.io/clojars/v/warble.svg)](https://clojars.org/warble)

`warble` aims to establish a pragmatic, intuitive and highly readable syntax for representing musical loops and tracks.

## Goals

- Adhere to traditional Western music theory concepts
- Small learning curve
- Highly productive
- Simple, composable and scalable constructs
- Trivial to interpret compiled output. Writing `warble` engines should be easy!
- Allow for alternative representations of music (i.e. visual instead of just audio)
- Seamless synchronization with associated audio tracks by minimizing the complexities around timing
- Keep your definitions DRY

## Design

`warble` is simply a notation for writing tracks that are ultimately interpreted by a `warble` engine.

This module, by itself, can only parse and compile plaintext `warble` data into [`warble.json`](https://github.com/slurmulon/warble-json-schema).

`warble.json` makes it trivial, especially for JavaScript engines, to sequentially process a `warble` music track and synchronize it in real-time with audio.

In general `warble` allows people to create modules and/or applications that need to associate arbitrary data with music in real-time.

## Install

### Leinengen/Boot

`[warble "0.2.0-SNAPSHOT"]`

### Gradle

`compile "warble:warble:0.2.0-SNAPSHOT"`

### Maven

```
<dependency>
  <groupId>warble</groupId>
  <artifactId>warble</artifactId>
  <version>0.2.0-SNAPSHOT</version>
</dependency>
```

## Notation

A more formal description of `warble`'s notation will eventually be written, but for now this is the canonical source of documentation.

An [Extended Backus-Naur Form (EBNF)](https://en.wikipedia.org/wiki/Extended_Backus%E2%80%93Naur_Form) formatted definition of the grammar can be found in [grammar.bnf](https://github.com/slurmulon/warble/blob/master/resources/grammar.bnf).

### Beats

`Loops` are simply nestable collections of either `Chords`, `Scales`, `Notes`, `Rests` (`~`), or other `Loops`.

For the sake of brevity, these will be combinationally referred to as `Elements` in this proposal and potentially in the source code.

The `Beat` at which any `Element` is played for (interpreted as its duration) is specified via the tuple-like `->` in a list (`[]`) or set (`{}`).

`Beat` tuples defined in lists will be played sequentially in the natural order and will not overlap.
`Beat` tuples defined in sets will be played in parallel and will overlap.

More formally:

```
[<duration> -> <list|set|element>]
```

Where `<duration>` can be:

```
N   = N measures or whole notes
1   = Whole note (one entire measure)
1/2 = Half note
1/4 = Quarter note
1/8 = Eighth note
...
1/512 = Minimum duration
```

For instance, a `Loop` playing a `Note('C2')` for an entire measure, starting at the first beat, would be specified like so:

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

All `Elements` must be instantiated in a `Beat` tuple (or implicitly converted into one), and the first parameter of every `Element` is a [`teoria`](https://github.com/saebekassebil/teoria)-supported string-like (surrounded with `'` or `"`) identifier such as `'C2'`, which is a second octave `C` note.

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

You may also use `-`, `*` and `/`, although these operators, especially multiply and divide, are experimental and require more testing.

---

As a convenience, `Elements` may also be implicit, specified using `#`:

```
:Note  = #('C2')
:Chord = #('C2Maj7')
:Scale = #('C2 Minor')
```

Determining the value of implicit `Elements` is the responsibility of the `warble` interpreter.

---

`Elements` can also overlap the same `Beat` and will be played concurrently using sets (`{}`) (TODO: example)

### Variables

To assign an variable, prefix a unique name with the `:` operator and provide a value (`<element|list|set>`):

```
:DasLoop = [1 -> Note('C2'), 1 -> Note('E2')]
```

Once assigned a name, variables may be dynamically referenced anywhere else in the track:

```
:CoolLooop = :DasLoop
```

### Cadences

In music it's common to see cadence sections labeled as `A`, `B`, `C`, and so on. `warble`'s syntax favors this nicely:

```
:A = Chord('F2maj')
:B = Chord('G2maj')
:C = Chord('C2maj')

:Song = [
  1 -> :A
  1 -> :B
  1 -> :C
  1 -> :A
]

!Play :Song
```

Destructured list assignments will soon be supported and will also favor cadences (**currently unsupported**):

```
:[A, B, C, D] = [Chord('E7'), Chord('Emin7'), Chord('Cmaj7'), Chord('Dmaj7')]
```

### Attributes

Arbitrary attributes may be associated with `Elements` using the `<key>: <value>` syntax. These attributes allow you to cusotmize the representations and interpretations of your `Elements`.

For instance, colors are useful for succesfully expressing a variety of data to the user at once. You might also want to specify the specific voicing of a chord.

```
:ABC = [
  1 -> {
    Scale('C2min',  color: #6CB359)
    Chord('D2min7', color: #AA5585, voicing: 1)
  },
  1 -> Chord('G2Maj7', color: #D48B6A, voicing: 2)
  2 -> Chord('C2Maj7', color: #FFDCAA, voicing: 2)
]
```

### Meta

Optional meta information about the track (aka "headers"), including the tempo and time signature, is specified with assignments at the top of the file and prefixed with the `@` operator:

```
@Title = 'My warble track'
@Time  = 4/4
@Tempo = 90
@Tags  = ['test', 'lullaby']

:ABC = [
  1/2 -> Chord('D2Mmin7')
  1/2 -> Chord('G2Min7')
  1 -> Chord('C2Maj7')
]
```

### Play

Because `warble` supports references, it requires a mechanism for specifying which data should be used for playing the track. You can think of `Play` as your main method or default export.

In other words, you need to tell it which values should be made available to the `warble` interpreter.

Any `Elements` that aren't being referenced or used by the value exported with `!Play` will be **ignored** during compilation.

```
:Ignored  = [1 -> Chord('D2min6'), 1 -> Chord('A2min9')]
:Utilized = [1 -> Chord('C2Maj7'), 1 -> Chord('A2Maj7')]

!Play :Utilized
```

Only one `!Play` definition is allowed per track file.

## Documentation

### Elements

 - `Note` = Single note in scientific notation
 - `Scale` = Scale in scientific notation
 - `Chord` = Chord in scientific notation
 - `Mode` = Mode in scientific notation
 - `Triad` = Triad of notes in scientific notation
 - `~` = Rest
 - `#` = Implicit (the interpreter determines if it's scale, chord or note based on the notation itself)
 - `[]` = List (sequential / ordered)
 - `{}` = Set (parallel / unordered)

### Headers

 - `Audio` (url)
 - `Instrument` (string, arbitrary)
 - `Title` (string, arbitrary)
 - `Desc` (string, arbitrary)
 - `Tempo` (integer, beats per minute)
 - `Time` (ratio, time signature. ex: 4/4)
 - `Tags` (list or set of strings, arbitrary)
 - `Link` (url)

## Setup

`lein install`

## Testing

`lein test`

## Usage

### CLI

First be sure that you have a binary executable (requires `lein` to be installed) available on your `PATH`:

```sh
$ cd warble
$ lein bin
```

Then you can just execute the resulting binary like so:

```sh
$ target/warble -i /path/to/track.warb compile
```

Currently supports the following actions:

- `parse`: creates an Abstract Syntax Tree (AST) from vanilla `warble` data
- `compile`: parses and compiles vanilla `warble` data into `warble.json`, an intermediary JSON micro-format that allows for simple interpretation of tracks
- `help`

### Library

```clojure
(ns my.namespace
  (:require [warble.ast :as ast]
            [warble.track :refer [compile-track]]))

; parses and compiles raw warble data into an interpretable hash-map
(compile-track (ast/parse ":Foo = []"))
```

## Related

- [`warble-json-schema`](https://github.com/slurmulon/warble-json-schema) contains the official JSON Schema definition for the `warble.json` format
- [`warble-rest-api`](https://github.com/slurmulon/warble-rest-api) is a RESTful HTTP service that allows compilation of `warble` tracks into `warble.json`
- [`juke`](https://github.com/slurmulon/juke) is the official NodeJS `warble` interpreter library

## Roadmap

 - [ ] Write technical specfiication
 - [ ] Semantic `ref` values for any `Element`
 - [X] General work towards making the tracks iterable in a normalized fashion
 - [ ] Destructured list assignments
 - [ ] Application of collection variables (i.e. dereference and flatten value into surrounding list)
 - [ ] Allow user to define sections of a track that should loop forever (`!Loop`)
 - [ ] Allow track linking with Hypermedia
 - [ ] Linkable sections with unique namespaces so that end users may bookmark and/or track progress, or specify areas to loop
 - [ ] Hide Chord or Scale (so it's only functionally relevant and not highlighted to the user)
 - [ ] Note fitness / quality data (i.e. how well it fits a given scale or chord in the current context)
 - [x] Support arbitrary classification of notes (i.e. `Note('C2', class: "blue")`)
 - [x] Support chord voicings/inversions (i.e. `Chord('C2maj7', inversion: 1)`)
 - [x] Support traids (root, 1st, 2nd)
