# Syntax

## Design

`bach`'s syntax is designed to make defining rhythmic data and timelines easy. It's primary use case is music, but its generic design enables rhythmic synchronization with nearly anything.

The second design goal of the syntax is to maximize human readability and flexibility. This is primarily achived by allowing semantic musical constructs to be defined.

Semantic musical constructs, such as scales and chords, always contain the same notes, and therfore it makes no sense to force the user to determine them every time, let alone write them as code. Pre-existing music notations such as ABC and GUIDO suffer from this problem.

Therefore it's important to be able to refer to these constructs in a semantic way, hoisting interpretation of the semantics to a high-level bach interpreter (such as [`bach-js`](https://github.com/slurmulon/bach-js)).

In other words, although `bach` is a "semantic" music notation, it actually doesn't concern itself with the details of the semantics, only establishing their presence. This maximizes flexibility and portability, while simultaneously keeping complexity low for the user.

Lastly, `bach` is a static notation. Although it supports basic variables and mathemetical expressions, it does not support conditionals, functions, classes or any other dynamic run-time features.

This is completely by design. Instead of re-inventing the wheel and resulting with an unnecessarily complicated syntax, it was decided to hoist all of this behavior to high-level intepreters. This allows users to dynamically generate or manipulate `bach` or `bach.json` data in their language of choice without sacrificing functionality.

## Grammar

An [Extended Backus-Naur Form (EBNF)](https://en.wikipedia.org/wiki/Extended_Backus%E2%80%93Naur_Form) formatted definition of the grammar can be found in [bach.ast](https://github.com/slurmulon/bach/blob/master/src/bach/ast.cljc).

## Documentation

## Elements

An `Element` is the atomic construct of `bach`, and is used for establishing data semantics.

`Elements` are much like classes in other languages, since `Elements` of the same kind are assumed to have the same meaning and similar behavior.

Individual `Elements` sharing the same kind can have their behavior and meaning customized by providing them with arguments.

`bach` defines and reserves several musical `Elements` to ensure consistent interpretation, however users are allowed and encouraged to define their own.

To find a list of every reserved construct supported by `bach` (such as `Note`, `Chord`, `Scale`, etc.), refer to the ["Constructs -> Elements"](#elements-1) section.

### Naming

`Element` semantics are established quite simply - by giving the `Element` a consistent human-friendly name, or `kind`.

`Elements` with the same `kind` values are semantically identical.

The naming convention for `Elements` is to capitalize `kind`, but lower-case is fine as well.

Names may only contain alpha-numeric characters.

It is the responsibility of the high-level `bach` interpreter to establish and enforce the meaning of an `Element`'s semantic `kind` (for example, which notes are in a Cmin chord).

### Syntax

```
<kind>(<arg>, ...)
```

### Examples

```
Scale('D')
```

```
Chord('A', shape: 'G')
```

```
Foo('bar')
```

## Beats

`Beats` represent an `Element` that will be played for a specific [duration](#durations).

The duration that a `Beat` is played for is specified using the tuple symbol, `->`, on the left-hand side.

### Syntax

```
<duration> -> <element>
```

### Example

```
1 -> Chord('F#')
```

## Collections

A `Collection` refers to a `List` or a `Set` of data, or some [combination of both](#nesting).

### Lists

A `List` is an ordered `Collection` of `Beats` and is fundamental in defining rhythms.

`Beats` defined in `Lists` will be played sequentially in the natural order (left to right) and will **not** overlap.

`Lists` may contain any number of `Beats` or `Elements`

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

`Beats` defined in `Sets` will be played in parallel, agnostic to their `duration` values.

`Sets` are primarily useful for grouping multiple `Elements` together at a certain point in time.

`Sets` may contain any number of elements.

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

 - Both `Lists` and `Sets` are considered `Collections`
 - `Lists` may contain `Sets`
 - `Lists` may **not** contain other `Lists`
 - `Sets` may **not** contain `Sets` or `Lists`

As a result, `Lists` **cannot** be nested in another `Collection` at _any_ level.

### Durations

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

#### Examples

A `List` playing a `Note('C2')` for an entire measure, starting at the first `Beat`, would be specified like so:

```
[1 -> Note('C2')]
```

If you wanted to start playing the note on the second `Beat` of the measure, then simply rest (`~`) on the first `Beat`:

```
[1/4 -> ~, 1 -> Note('C2')]
```

When a `Beat` tuple is not provided in an an assignment or a `Collection`, both the position and duration of the `Beat` will be implied at run-time to be the index of each respective element as they are played.

The position and duration are both determined by the time signature (the default is common time, or `4|4`).

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
1 + 1/2 -> Chord'(C2min6')
```

This is usefeul for specifying more complicated rhythms, like those seen in jazz.

```
:PartA = [
  1/2   -> Chord('D2min7')
  1+1/2 -> Chord('E2maj7')
  1+1/2 -> Chord('C2maj7')
]
```

You may also use the `-`, `*` and `/` operators.

### Instantiation

All `Elements`, unless already nested in a `List` or `Set`, must be instantiated in a `Beat` tuple (or implicitly converted into one, as shown in the previous section).

The first parameter of every `Element` is a string formatted in [`scientific pitch notation (SPN)`](https://en.wikipedia.org/wiki/Scientific_pitch_notation) (surrounded with `'` or `"`) such as `'C2'`, which is a second octave `C` note.

### Implicits

As a convenience, `Elements` may also be defined implicitly, specified using a `#`:

```
:Note  = #('C2')
:Chord = #('C2Maj7')
:Scale = #('C2 Minor')
```

Determining the semantic value of implicit `Elements` (i.e. whether it's a `Note`, `Chord`, etc.) is the responsibility of the `bach` interpreter.

It's suggested that you primarily use implicits as they will save you a _lot_ of typing over time.

## Variables

To assign a variable, prefix a unique name with the `:` operator and provide a value (`<element>`).

In this case our variable's name is "Loop":

```
:Loop = [1 -> Note('C2'), 1 -> Note('E2')]
```

Once assigned a name, variables may be dynamically referenced anywhere else in the track:

```
:LoopCopy = :Loop
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

Headers outside of those defined in the [documentation](#headers-1) are allowed and can be interpreted freely by the end user, just like `X-` headers in HTTP. The value of custom headers can be of any [primitive type](#primitives).

```
@Meter  = 4|4
@Tempo  = 90
@Title  = 'My bach track'
@Tags   = ['test', 'lullaby']
@Custom = 'so special'

:ABC = [
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
