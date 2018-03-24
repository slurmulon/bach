# warble

> :musical_score: Notation for musical loops and tracks with a focus on readability and productivity

---

[![Clojars Project](https://img.shields.io/clojars/v/warble.svg)](https://clojars.org/warble)

`warble` aims to establish a pragmatic, intuitive and highly readable syntax for representing musical loops and tracks.

## Sections

- [Goals](https://github.com/slurmulon/warble#goals)
- [Design](https://github.com/slurmulon/warble#design)
- [Install](https://github.com/slurmulon/warble#install)
- [Setup](https://github.com/slurmulon/warble#setup)
- [Testing](https://github.com/slurmulon/warble#testing)
- [Usage](https://github.com/slurmulon/warble#usage)
  * [CLI](https://github.com/slurmulon/warble#cli)
  * [Library](https://github.com/slurmulon/warble#library)
- [Documentation](https://github.com/slurmulon/warble#documentation)
  * [Beats](https://github.com/slurmulon/warble#beats)
  * [Variables](https://github.com/slurmulon/warble#variables)
  * [Cadences](https://github.com/slurmulon/warble#cadences)
  * [Attributes](https://github.com/slurmulon/warble#attributes)
  * [Headers](https://github.com/slurmulon/warble#headers)
  * [Play](https://github.com/slurmulon/warble#play)
- [Glossary](https://github.com/slurmulon/warble#glossary)
  * [Elements](https://github.com/slurmulon/warble#elements)
  * [Headers](https://github.com/slurmulon/warble#headers-1)
  * [Operators](https://github.com/slurmulon/warble#operators)
  * [Primitives](https://github.com/slurmulon/warble#primitives)
- [Related](https://github.com/slurmulon/warble#related)
- [Roadmap](https://github.com/slurmulon/warble#roadmap)

## Goals

- Allow for alternative real-time representations of music (e.g. visual instead of just audio)
- Seamless synchronization with associated audio tracks by minimizing the complexities around timing
- Adhere to traditional Western music theory concepts
- Easy to translate from sheet music
- Small learning curve
- Highly productive
- Simple, composable and scalable constructs
- Trivial to interpret compiled output. Writing `warble` engines should be easy!
- Keep your definitions DRY

## Design

`warble` is a notation for writing tracks that are ultimately interpreted by a `warble` engine.

This module, by itself, can only parse and compile plaintext `warble` data into [`warble.json`](https://github.com/slurmulon/warble-json-schema).

`warble.json` makes it trivial, especially for JavaScript engines, to sequentially process a `warble` music track and synchronize it in real-time with audio.

In general `warble` allows people to create modules and/or applications that need to synchronize data with music in real-time.

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


## Documentation

An [Extended Backus-Naur Form (EBNF)](https://en.wikipedia.org/wiki/Extended_Backus%E2%80%93Naur_Form) formatted definition of the grammar can be found in [grammar.bnf](https://github.com/slurmulon/warble/blob/master/resources/grammar.bnf).

### Beats

`Loops` are simply nestable collections of `Chords`, `Scales`, `Notes`, `Rests` (`~`), or other `Loops`.

For the sake of brevity, these entities will be combinationally referred to as `Elements` in this proposal and potentially in the source code.

The `Beat` at which any `Element` is played for (interpreted as its duration) is specified via the tuple-like `->` in a `ListLoop` (`[]`) or `SetLoop` (`{}`).

```
<duration> -> <element>
```

`Beat` tuples defined in `ListLoops`, or `Lists`, will be played sequentially in the natural order and will not overlap.

```
[<duration> -> <element>]
```

`Beat` tuples defined in `SetLoops`, or `Sets`, will be played in parallel and will overlap.

```
{<duration -> <element>}
```

---

The value of a `Beat`'s `<duration>` can be:

```
1    = Whole note (one entire measure)
1/2  = Half note
1/4  = Quarter note
1/8  = Eighth note
1/16 = Sixteenth note
...
1/512 = Minimum duration
```

A `Loop` playing a `Note('C2')` for an entire measure, starting at the first `Beat`, would be specified like so:

```
[1 -> Note('C2')]
```

When a `Beat` identifier is not provided in an an assignment or list it will be implied at run-time to be the index of each respective element as they are played, using the unit defined in the time signature (the default is common time, or `4|4`)

For instance:

```
[1/4 -> Note('C2'), 1/4 -> Note('F2')]
```

is the same as:

```
[Note('C2'), Note('F2')]
```

---

All `Elements` must be instantiated in a `Beat` tuple (or implicitly converted into one), and the first parameter of every `Element` is a string formatted in [`scientific pitch notation (SPN)`](https://en.wikipedia.org/wiki/Scientific_pitch_notation) (surrounded with `'` or `"`) such as `'C2'`, which is a second octave `C` note.

`Beat` durations can also use basic mathematical operators. This makes the translation between sheet music and `warble` an easy task.

```
1 + 1/2 -> Chord'(C2min6')
```

This is usefeul for specifying more complicated rhythms, like those seen in jazz.

```
:Mutliple = [
  1/2   -> Chord('D2min7')
  1+1/2 -> Chord('E2maj7')
  1+1/2 -> Chord('C2maj7')
]
```

You may also use the `-`, `*` and `/` operators.

---

As a convenience, `Elements` may also be implicit, specified using `#`:

```
:Note  = #('C2')
:Chord = #('C2Maj7')
:Scale = #('C2 Minor')
```

Determining the semantic value of implicit `Elements` (i.e. whether it's a `Note`, `Chord`, etc.) is the responsibility of the `warble` interpreter.

### Variables

To assign a variable, prefix a unique name with the `:` operator and provide a value (`<element|list|set>`):

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
  1 -> Chord('G2maj7', color: #D48B6A, voicing: 2)
  2 -> Chord('C2maj7', color: #FFDCAA, voicing: 2)
]
```

### Headers

Optional header information, including the **tempo** and **time signature**, is specified with assignments at the top of the file and prefixed with the `@` operator:

Headers outside of those defined in the documentation are allowed and can be interpreted freely by the end user, just like `X-` headers in HTTP. The value of custom headers can be of any primitive type.

```
@Title  = 'My warble track'
@Time   = 4|4
@Tempo  = 90
@Tags   = ['test', 'lullaby']
@Custom = 'so special'

:ABC = [
  1/2 -> Chord('D2min7')
  1/2 -> Chord('G2min7')
  1 -> Chord('C2maj7')
]
```

### Play

Because `warble` supports references, it requires a mechanism for specifying which data should be used for playing the track. You can think of `Play` as your main method or default export.

In other words, you need to tell it which values should ultimately be made available to the `warble` interpreter.

Any `Elements` that aren't being referenced or used by the value exported with `!Play` will be **ignored** during compilation.

```
:Ignored  = [1 -> Chord('D2min6'), 1 -> Chord('A2min9')]
:Utilized = [1 -> Chord('C2Maj7'), 1 -> Chord('A2Maj7')]

!Play :Utilized
```

Only one `!Play` definition is allowed per track file.

## Glossary

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
 - `Time` (meter, time signature. ex: `6|8`, `4|4`)
 - `Tags` (list or set of strings, arbitrary)
 - `Link` (string, url)

### Operators

 - `+` = Add
 - `-` = Subtract
 - `/` = Divide
 - `*` = Multiply
 - `|` = Meter (primarily for time signatures)

### Primitives

 - `'foo'` or `"bar"` = string
 - `123` = number
 - `#000000` = color

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
 - [x] Arbitrary classification of notes (i.e. `Note('C2', class: "blue")`)
 - [x] Chord voicings/inversions (i.e. `Chord('C2maj7', inversion: 1)`)
 - [x] Traids (root, 1st, 2nd)
