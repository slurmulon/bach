# Syntax

## Design

`bach`'s syntax is designed to make defining rhythmic data and timelines easy. It's primary use case is music, but its generic design enables rhythmic synchronization with nearly anything.

The second design goal of the syntax is to maximize human readability and flexibility. This is primarily achived by allowing semantic musical constructs to be defined.

Semantic musical constructs, such as scales and chords, always contain the same notes, and therfore it makes no sense to force the user to determine them every time, let alone write them as code. Pre-existing music notations such as ABC and GUIDO suffer from this problem.

Therefore it's important to be able to refer to these constructs in a semantic way, hoisting interpretation of the semantics to a high-level bach interpreter (such as [`bach-js`](https://github.com/slurmulon/bach-js)).

In other words, although `bach` is a "semantic" music notation, it actually doesn't concern itself with the details of the semantics, only establishing their presence. This maximizes flexibility and portability, while simultaneously keeping complexity low for the user.

Lastly, `bach` is a static notation. Although it supports basic variables and mathemetical expressions, it does not support conditionals, functions, classes or any other dynamic run-time features.

This is completely by design. Instead of re-inventing the wheel and ending up with an unnecessarily complicated syntax, it was decided to hoist all of this behavior to high-level intepreters. This allows users to dynamically generate or manipulate `bach` or `bach.json` data in their language of choice without sacrificing functionality.

## Grammar

An [Extended Backus-Naur Form (EBNF)](https://en.wikipedia.org/wiki/Extended_Backus%E2%80%93Naur_Form) formatted definition of the grammar can be found in [bach.ast](https://github.com/slurmulon/bach/blob/master/src/bach/ast.cljc).

## Documentation

This section describes the syntactic constructs of `bach` and how they relate to each other, primarily using focused and minimal examples.

For a practical and detailed guide on using `bach` refer to the [Guide](/guide) page.

For a summarized list of all supported constructs jump to the [Constructs](#constructs) section.

## Elements

An `Element` is the atomic construct of `bach` and is used for establishing data semantics.

`Elements` are much like classes in other languages, since `Elements` of the same kind are assumed to have the same meaning and similar behavior.

Individual `Elements` sharing the same kind can have their behavior and meaning customized by providing them with [attributes](#attributes).

`bach` defines and reserves several musical `Elements` to ensure consistent interpretation in the domain of music.

Regardless, users are not only allowed but _encouraged_ to define their own `Element` semantics, tailoring and adapting `bach` to alternative domains.

To find a list of every reserved construct supported by `bach` (such as `Note`, `Chord`, `Scale`, etc.), refer to the ["Constructs -> Elements"](#elements-1) section.

### Syntax

```
<kind>(<arg>, ...)
```

### Examples

```
Scale('D')

Chord('A', shape: 'G')

Foo('bar')
```

### Semantics

`Element` semantics are established by simply giving the `Element` a consistent human-friendly name, or `kind`.

`Elements` with equal `kind` values are considered semantically identical.

The naming convention for `Elements` is to capitalize `kind`, but lower-case is also acceptable.

`kinds` may only contain alpha-numeric characters.

`kinds` are considered case-insensitive by both the core `bach` library and `bach.json` interpreters when comparing `kind` equality.

It is the responsibility of the high-level `bach` interpreter to establish and enforce the meaning of an `Element`'s semantic `kind` (for example, which notes are in a Cmin chord).

If you're interested in seeing how this all comes together, [`bach-js`](https://github.com/slurmulon/bach-js) is the official `bach` interpreter library for `nodejs` and its code is the defacto reference.

### Values

An `Element` can be defined with an arbitrary number of arguments.

The first argument provided to an `Element` determines its semantic value and can be used to compare equality.

For [reserved musical elements](#elements-1) this value must be a case-insensitive UTF-8 string formatted in [`scientific pitch notation (SPN)`](https://en.wikipedia.org/wiki/Scientific_pitch_notation) (surrounded with `'` or `"`) such as `'C2'`, which is a second octave `C` note.

#### Examples

```
Note('C2')

Chord('Cmin7')

Scale('E mixolydian')
```

### Attributes

Attributes can be defined as any argument provided to an `Element` beyond the first (i.e. its value).

Arbitrary attributes may be associated with `Elements` using the `<key>: <value>` syntax, where `<key>` is a `string` and `<value>` can be any [primitive](#primitives).

These attributes allow you to cusotmize the representations and interpretations of your `Elements`.

For instance, you might want to assign a unique id or specify the voicing of an individual chord.

#### Example

```
Chord('D2min7', id: 2, voicing: 1)
```

## Beats

`Beats` represent an `Element` that will be played for a specific [duration](#durations).

The duration that a `Beat` is played for, in whole notes, is specified using the tuple symbol `->`.

The duration is defined on the left-hand side while the associated `Element` is defined on the right-hand side.

### Syntax

```
<duration> -> <element>
```

### Examples

```
1 -> Chord('F#')

1/2 -> Scale('A lydian')
```

## Collections

A `Collection` refers to a `List` or a `Set` of data, or some [combination of both](#nesting).

### Lists

A `List` is an ordered `Collection` of `Beats` or `Elements` and is fundamental in defining rhythms.

`Beats` defined in `Lists` will be played sequentially in the natural order (left to right, or top to bottom) and will **not** overlap.

`Lists` may contain any number of `Beats` or `Elements`.

#### Syntax

```
[<duration> -> <element>, <duration> -> <element>, ...]

[<beat>, <beat>, ...]
```

#### Example

```
[1 -> Chord('A'), 1 -> Chord('E')]
```

### Sets

`Elements` defined in `Sets` will be played in parallel.

`Sets` are primarily useful for grouping multiple `Elements` together at a certain point in time.

`Sets` may contain any number of `Elements`.

`Sets` may **not** contain `Beats` since the duration value is irrelevant.

#### Syntax

```
{ <element>, <element>, ... }
```

#### Example

```
{ Scale('E lydian'), Chord('E') }
```

### Nesting

Nesting functionality is limited by design, as it helps keep high-level interpretation as linear and simple as possible.

The rules are as follows:

 - `Lists` may contain `Sets`
 - `Lists` may **not** contain other `Lists`
 - `Sets` may **not** contain `Sets` or `Lists`

As a result, `Lists` **cannot** be nested in another `Collection` at _any_ level.

## Variables

Variables allow you to capture, label and reuse values, reducing duplication and increasing the succinctness of your `bach` definitions.

To assign a variable, prefix a unique name with the `:` operator and provide a value (`<element>`).

In this case our variable's name is "Loop":

```
:Loop = [Note('C2'), Note('E2')]
```

Once assigned a name, variables may be dynamically referenced anywhere else in the track:

```
:LoopCopy = :Loop
```

Variables are not constants and can be reassigned, but variable hoisting is currently unsupported.

## Durations

The value of a `Beat`'s `<duration>` can be an integer, a fraction, or a mathematical expression composed of either.

```
1    = Whole note (or one entire measure when `@Meter = 4|4`)
1/2  = Half note
1/4  = Quarter note
1/8  = Eighth note
1/16 = Sixteenth note

1/512 = Minimum duration
1024  = Maximum duration

2 + (1/2) = Two and a half whole notes
2 * (6/8) = Two measures when `@Meter = 6|8`
```

To adhere with music theory, durations are strictly based on **common time** (`@Meter = 4|4`).

This means that, in your definitions, `1` always means 4 quarter notes, and only equates with a full measure when the number of beats in a measure is 4 (as in `4|4`, `3|4`, `5|4`, etc.).

The examples in the remainder of this section assume common time, since this is the default when a `@Meter` header is not provided.

### Examples

A `List` playing a `Note('C2')` for an entire measure, starting at the first `Beat`, would be specified like so:

```
[1 -> Note('C2')]
```

If you wanted to start playing the note on the second `Beat` of the measure, then simply rest (`~`) on the first `Beat`:

```
[1/4 -> ~, 1 -> Note('C2')]
```

When a `Beat` tuple is not provided in an assignment or a `Collection`, both the position and duration of the `Beat` will be implied at compile time to be the index of each respective `Element` as they are played.

The position and duration are both determined by the time signature a.k.a. meter (the default is common time, or `4|4`).

For instance:

```
[1/4 -> Note('C2'), 1/4 -> Note('F2')]
```

is the same as:

```
[Note('C2'), Note('F2')]
```

`Beat` durations can also use basic [mathematical operators](#operators). This makes the translation between sheet music and `bach` an easy task.

```
1 + 1/2 -> Chord('C2min6')
```

This is usefeul for specifying more complicated rhythms, like those seen in jazz.

```
:PartA = [
  1/2   -> Chord('D2min7')
  1+1/2 -> Chord('E2maj7')
  1+1/2 -> Chord('C2maj7')
]
```

### Headers

Optional header information, including the **tempo** and **time signature**, is specified with assignments at the top of the file and prefixed with the `@` operator.

The two most important (and [reserved](#reserved)) headers are `@Tempo` and `@Meter`, since these serve as the foundation for calculating rhythmic durations.

```
@Tempo = 65
@Meter = 6|8
```

Headers outside of those defined in the [documentation](#headers-1) are allowed and can be interpreted freely by the end user, just like `X-` headers in HTTP. The value of custom headers can be of any [primitive type](#primitives) or a `Collection` of primitives.

```
@Meter  = 4|4
@Tempo  = 90
@Title  = 'My bach track'
@Tags   = ['test', 'lullaby']

!Play [
  1/2 -> Chord('D2min7')
  1/2 -> Chord('G2min7')
  1 -> Chord('C2maj7')
]
```

### Play

Because `bach` supports references, it requires a mechanism for specifying which data should be used for playing the track. You can think of `Play` as your main method or default export.

In other words, you need to tell it which values should ultimately be made available to the `bach` interpreter.

Any `Elements` that aren't being referenced or used by the value exported with `!Play` will be **ignored** during compilation.

```
:Ignored  = [1 -> Chord('D2min6'), 1 -> Chord('A2min9')]
:Utilized = [1 -> Chord('C2Maj7'), 1 -> Chord('A2Maj7')]

!Play :Utilized
```

Only one `!Play` definition is allowed per track file.

## Constructs

### Elements

 - `Note` = Single note in scientific pitch notation
 - `Scale` = Scale in scientific pitch notation
 - `Chord` = Chord in scientific pitch notation
 - `Mode` = Mode in scientific notation
 - `Triad` = Triad of notes in scientific notation
 - `~` = Rest
 - `#` = Implicit (the interpreter determines if it's scale, chord or note based on the notation itself)

### Collections
 - `[]` = List (sequential / ordered)
 - `{}` = Set (parallel / unordered)

### Headers

#### Reserved

 - **`Tempo`** (number, beats per minute)
 - **`Meter`** (meter, time signature. ex: `6|8`, `4|4`)

#### Useful

 - `Key` (string, key signature)
 - `Audio` (url)
 - `Instrument` (string, arbitrary)
 - `Title` (string, arbitrary)
 - `Artist` (string, arbitrary)
 - `Desc` (string, arbitrary)
 - `Tags` (list or set of strings, arbitrary)
 - `Link` (string, url)

### Operators

 - `+` = Add
 - `-` = Subtract
 - `/` = Divide
 - `*` = Multiply
 - `|` = Meter (for time signatures, **not** arbitrary mathematical expressions)

### Primitives

 - `'foo'` or `"bar"` = string
 - `123` or `4.5` = number
 - `#000000` or `#fff` = color


## Conventions

## Limitations

 - Tempos may only be defined as a header, and this prevents tempo changes from occuring mid-track.
 - List destructuring is currently unsupported
