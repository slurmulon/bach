# Guide

The purpose of this guide is to help people learn `bach` well enough to put it to practical use.

It is primarily written for musicians and assumes as little as possible about their technical expertise, although techical terms and details are sometimes required to ensure a comprehensive understanding of the notation.

Naturally, it also assumes that the reader has at least a basic understanding of music theory concepts such as tempo, meter, beats and measures.

As you read this guide you will encounter various examples of `bach`, sometimes partial and focused snippets and other times complete examples.

Whenever you are faced with new concepts and terms that feel overlooked or unclear, try to ignore them in that moment.

This guide is organized so that it will progressively fill in those details for you, in a way that (we feel) best gives you a hollistic understanding of `bach`.

You can find a collection of useful example tracks in the [Examples](#examples) section, but the majority of this guide focuses on a single example track (in the [Tracks](#tracks) section) to increase clarity.

> If you are looking for a more technical and low-level resource on `bach`, head to the [Syntax](/syntax) page instead.

> If you are interested in the rationale for `bach` and the problems it solves, first check out the [Background](/background) page.

## Intro

`bach` is a music notation that people and computers can easily read or write. It is entirely text-based and can be written in any text editor.

`bach` is different from other music notations because it is **semantic**, meaning it allows you to work with higher-level music constructs such as chords and scales without needing to worry about the details.

Once the notation is compiled it can be used by apps to drive anything from real-time music players to sheet music generators.

## Basics

As with learning anything new, the ideal place to start is the fundamentals. There are several different components in `bach` that you need to be aware of before you can start using it.

This guide provides as many details and examples as possible to help ensure that common questions, concerns and caveats are thoroughly addressed.

It's advised (but not required) that you read each section in the order it appears, only moving on to the next section after you've obtained a decent grasp on the section's concepts.

With that said we can now dive into each of the components that make up `bach`, starting at the surface with "tracks".

### Tracks

Tracks can represent a loop, a song, or really any sort of rhythmic timeline.

Tracks are the highest-level concept in `bach`, so it's important to understand them on a macroscopic level before diving into other areas.

#### Example

We will begin by looking at a real-world example of a track, and this track will be referenced throughout this guide.

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

### Primitives

Let's begin with the simplest values found in a track, collectively referred to as "primitives" or "primitive values".

Because `bach` is minimal by design, it only supports two kinds of primitive values: numbers and strings.

You have already encountered numbers and strings in the [Tracks](#tracks) section, so after reading through the next sections see if you can identify the primitives in the example track.

#### Numbers

Numerical values can be expressed as either an integer (`1`) or a double (`1.5`).

Numbers cannot include commas, underscores or any other special symbol besides a period (for doubles).

Mathematical expressions like `1 + 1/2` are also supported, but we will cover them later in the [Durations](#durations) section since this is where they are mostly used.

##### Integers

An integer is a [whole number](https://en.wikipedia.org/wiki/Integer), or a number without a decimal.

```js
1
64
1024
```

##### Doubles

A double is a [rational number](https://en.wikipedia.org/wiki/Rational_number), a number that can be represented with a floating point/period.

```js
184.63
7.302756
1.0
```

#### Strings

The final primitive that `bach` supports is called a "string".

The term "string" may feel unfamiliar, but it's appropriately named since strings are found in nearly every programming language.

All you need to know is that a string is just a representation of literal text.

For instance, in apps strings are frequently used to represent a person's name, email, street address, etc.

In `bach` strings are used to express info like chord and scale values, such as `"Cmin7"` and `"C mixolydian"`.

Any sequence of characters surrounded by either matching single-quotes or double-quotes is considered a string.

```
"D major"
'Eb minor'
"Hello world"
```

### Headers

Headers are used to describe the track or give it some sort of context.

Headers can only be defined at the top of the track, since they are contextual and used to describe or influence the track as a whole.

Headers are defined using the `@` character, followed by a unique name, an equal (`=`) character, and then a value.

This example shows how to specify the meter and tempo of a track:

```bach
@Meter = 3|4
@Tempo = 110
```

`bach` allows you to define any headers you like, but `@Meter` and `@Tempo` are special and reserved since they influence how `bach` calculates beat durations.

You can find a list of useful headers in the [Syntax](/syntax#useful) document, but be aware that supporting these headers involves customizing your `bach` interpreter, a task that requires external code changes.

In other words, although you are free to make up your own headers, you need someone who knows how to code in order to add support for them.

#### Meter

The meter header of a track defines its time signature, which specifies two important pieces of information: the number of beats in a measure and the beat unit.

The following meter tells `bach` that the track has 5 beats in a measure and that an 1/8th note is the beat unit, or the value of an individual beat.

```bach
@Meter = 5|8
```

When a meter header is not specified in your track, `bach` assumes you are using common time (`4|4`).

As of now the meter can only be defined once and cannot change at any point in the track.

#### Tempo

The tempo header of a track determines how many beats there are in a minute (otherwise known as "bpm").

When a tempo is not specified, `bach` defaults to `120` bpm.

Tempos can be expressed as either an integer (e.g. `120`) or a decimal (e.g. `110.5`).

As with meter, the tempo can only be defined once, and, at least right now, cannot change at a later point.

### Elements

In general, elements let you say "hey `bach`, here is something for you to play".

Technically an element can be used to represent any sort of information.

But in our target domain of music, an element represents any of the following:

 - `Note`
 - `Scale`
 - `Chord`
 - `Rest` (`~`)

#### Arguments

Elements can be provided with custom data (within parenthesis) called arguments.

The first argument determines the **value** of the element and must always be provided.

In this example we are defining a chord with a value of Bm7:

```
Chord('Bm7')
```

_Note how the `Bm7` is surrounded by quotes, specifying this data as a string._

If you wanted to associate more information with this chord, such as which voicing the chord is in, you can simply add another argument, where each argument beyond the first is separated by a comma:

```
Chord('Bm7', voicing: 2, triad: 1)
```

In this example, the arguments being provided after the chord value are somewhat special because they have a label/name.

We call this type of named value an "attribute". An attribute allows you to not only associate some sort of information with an element, but to also give it a human-friendly name so that it can be easily reasoned with by a programmer developing with `bach`.

For now attributes can only be defined and used in the arguments of an element, but in the future `bach` may allow them to be used in alternative ways.

#### Duration

By themself elements are agnostic to rhythm and duration.

So in order to say "play this thing for this long", an element must be associated with a duration.

This is where "beats" come in.

### Beats

Beats represent an element paired with a duration in common time (more on this in the [Durations](#/durations) section).

So a beat not only says "here is something to play", but also "play it for this long".

Beats are defined using the `->` symbol, where the duration of the beat is written on the left, and the element to play is written on the right.

In this example we declare a beat that plays a "C minor" scale for a half note (or two beats) in common time.

```
1/2 -> Scale('C minor')
```

But a beat by itself isn't too useful. Rarely (if ever) does a song only contain a single note, chord or scale.

### Collections

Collections are what you use to group beats and/or elements together.

They let you say "play this, then this" and so on. In `bach` this pattern is called a "rhythmic timeline".

There are only two types of collections in `bach`: "lists" and "sets".

#### Lists

Lists are simply an ordered collection of beats to play.

This means that each beat in the list will be played in the order it is defined, either from left to right or top to bottom, depending on how you write.

Looked at another way, beats in a list will be played sequentially, for their entire duration, and will **not** overlap with each other (just like a queue).

So this (left to right):

```bach
[1/2 -> Chord('A'), 1/2 -> Chord('B'), 1 -> Chord('C')]
```

is the same as this (top to bottom):

```bach
[
  1/2 -> Chord'A')
  1/2 -> Chord('B')
  1 -> Chord('C')
]
```

Both of these examples will be interpreted like so:

1. Play chord `A` for a half note, or 2 beats, then
1. Play chord `B` for a half note, or 2 beats, then
1. Play chord `C` for a whole note, or an entire measure in common time

As you can see, how you format a list is mostly up to personal preference. You only need to follow these rules:

 - Lists are defined using a starting `[` character and an ending `]` character. `[` says "here is the beginning of the list" and `]` says "here is the end of the list".
 - Each beat in a list can be separated with a `,` character, but they are not required.
 - You can use as much "whitespace" (i.e. space and enter characters) around the list and its beats as you like, to make things easy to read. This is what we mean by "formatting".

Unlike other music notations, you don't have to explicitly concern yourself with measures or bars, but you can if you prefer.

Technically the only thing `bach` cares about are the beats to be played and in what order.

#### Sets

Sometimes you need to play multiple elements at once in a beat.

For instance, you may need to tell an app to show both a scale and a chord at the same time.

Sets allow you to do this because they are a collection that's agnostic to order.

With a set all you're saying is "group these elements together". Unlike lists, sets are not concerned with durations and have no influence on rhythm.

Sets are defined the same way as lists with one key difference: they use curly braces (`{` and `}`) instead of brackets.

```bach
{ Scale('B minor'), Chord('Bm') }
```

In the example from the [Tracks](#tracks) section, both a scale and chord will played on the first beat for 4 whole notes.

```bach
4 -> {
  Scale('B minor')
  Chord('Bm')
}
```

#### Nesting

Nesting, in the most general sense, is whenever you have one thing that contains another. You can think of it as a way to describe the general pattern of a hierarchy.

For instance, reflecting back to the example found in the [Tracks](#tracks) section, the first beat of the list contains a set, which contains both a scale and a chord. Both the scale and the chord are nested within the set.

You could also take things further in each direction.
The individual notes composing the scale and chord are nested within them.
You can also say that the set is nested within the list, or even that the list is nested within the track.

But when we talk about nesting in `bach` it can be assumed that we are referring to collection nesting, unless it's stated otherwise.

##### Usage

The most common use case for nesting is when you define a set within a beat in order to group multiple elements together, as in the first beat of the example we've been focusing on.

```bach
!Play [
  4 -> {
    Scale('B minor')
    Chord('Bm')
  }
]
```

With the help of some whitespace, we can naturally see that the beat is nested in the list, the set is nested in the beat, and the scale and the chord are nested in the set.

Whenever we can identify a point where nesting occurs in the hierarchy, we call this a "level". The list at the top of the track would be at the first level of nesting, the beats in that list on the second level and so on.

##### Rules

In the spirit of keeping `bach` simple and understandable, collection nesting is limited to the following rules:

 - Lists may contain sets
 - Lists may **not** contain other lists
 - Sets may **not** contain sets or lists

As a result, lists **cannot** be nested in another collection at _any_ level.

##### Formatting

For each level of nesting, it's suggested that you indent the text with two spaces.

This helps keep your tracks visually well organized, and therefore easier to read, understand and change later.

The general consensus is that, although it takes up more visual space, this:

```
!Play [
  4 -> {
    Scale('B minor')
    Chord('Bm')
  }
]
```

is easier to read and understand than this:

```
!Play [ 4 -> { Scale('B minor') Chord('Bm') } ]
```

But of course this ultimately comes down to your personal preferences and individual use cases.

### Durations

Durations are at the heart of rhythms, and therefore at the heart of `bach`.

When used in lists they become the mechanism that allows you to define rhythmic timelines.

As you might recall from the [Beats](#beats) section, beats are elements that are paired with a duration.

When beats are nested in a list, each beat is played for its entire duration before moving onto the next.

Therefore the duration not only specifies how long a beat is played for in a list, but it also determines when the next beat in that list is played, since the next beat will only be played _after_ the previous beat reaches the end of its duration.

#### Values

The value of a duration can be an integer, a fraction, or a mathematical expression composed of either.

```
1    = Whole note (or one entire measure when `@Meter = 4|4`
1/2  = Half note
1/4  = Quarter note
1/8  = Eighth note
1/16 = Sixteenth note

1/512 = Minimum duration
1024  = Maximum duration

2 + (1/2) = Two and a half whole notes
2 * (6/8) = Two measures when the meter is 6|8

1 + (1/2) = One and a half measures when the meter is 4|4
1 - (1/8) = Seven eigth notes
```

To adhere with music theory, durations are strictly based on **common time** (`@Meter = 4|4`).

This means that `1` always means four quarter notes, and only equates with a full measure when the number of beats in a measure is 4 (as in `4|4`, `3|4`, `5|4`, etc.).

The examples in the remainder of this section assume common time, since this is the default when a `@Meter` header is not provided.

#### Examples

We have already encountered several examples of durations throughout the guide, so let's now take a more focused look at durations in order to understand them hollistically.

A list playing a `Note('C2')` for an entire measure, starting at the first beat in the measure, would be specified like so:

```
[1 -> Note('C2')]
```

If you wanted to start playing the note on the second beat of the measure, then simply rest (`~`) on the first beat:

```
[1/4 -> ~, 1 -> Note('C2')]
```

When a beat is defined without a duration (in other words, just an element), the duration is implied to be one beat of the meter.

For instance, this, when the meter is common time:

```
[1/4 -> Note('C2'), 1/4 -> Note('F2')]
```

is the same as this:

```
[Note('C2'), Note('F2')]
```

Beat durations can also use basic mathematical operators (i.e. add, subtract, multiply, divide).

This makes the translation between sheet music and `bach` an easy task.

```
1/2 + 1/4 -> Chord('C2min6')
```

It's also usefeul for specifying more complicated rhythms:

```
[
  1/2   -> Chord('D2min7')
  1+1/2 -> Chord('E2maj7')
  1+1/2 -> Chord('C2maj7')
]
```

They also let you work (sanely) with less common meters such as `6|8`:

```
@Meter = 6|8

!Play [
  6/8 -> Chord('Dmin')
  6/8 -> Chord('Dmin/F')
  6/8 -> Chord('E7b9')
  6/8 -> Chord('Bb7')
  2 * 6/8 -> Chord('A7')
]
```

It's worth noting how the last chord, `A7`, is played for two measures (via `2 * 6/8`).

If we were using the `4|4` meter, we would just say `1` instead since all durations are based on common time.

### Variables

More often than not you will need to play the same element multiple times in a track.

Variables allow you to assign a name to a value and then reference that value later by the variable's name.

This helps to reduce duplication and human error and makes changing the track much easier later on.

Variables are declared using the `:` character, immediatelly followed by a unique name, a `=` character, and then a value (a primitive, element, collection, etc.).

```bach
:Bm = Chord('Bm')
```

Once a variable is declared it may be referenced and used in any area of the track proceeding it.

This means that a variable must be declared before it can be used elsewhere.

The recommended convention is to declare your variables immediately after your headers, as it helps people quickly determine all of the elements used in the track.

#### Example

Our primary example track already uses variables, so let's compare and see what it would look like without them.

Here is how the track looks as-is, with variables.

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

Here's how it looks without any variables:

```bach
@Meter = 4|4
@Tempo = 44

!Play [
  4 -> {
    Scale('B minor')
    Chord('Bm')
  }
  2 -> Chord('Em')
  2 -> Chord('Bm')
  2 -> Chord('F#m7')
  2 -> Chord('Bm')
]
```

We can see that the example using variables has more a couple more lines, but more importantly it has less repetition.

This is advantageous because if we wanted to say, change `Bm` to `Bm7`, we only have to make that change in one place instead of three.

If you use a value more than once it's recommended that you always assign it to a variable.

If you only use a value once, as is the case with `Scale('B minor')`, then it's up to you whether not to assign it to a variable.

#### Limitations

As of now there are a couple of limitations to variables regarding lists.

Assigning a list to a variable is perfectly valid, but there are currently some undesirable limitations that prevent lists from being combined and re-used.

As we discussed earlier, collection nesting, particularly around lists, is limited by design.

These limits are beneficial and justified, but right now they inhibit how much lists can benefit from variables.

Consider the following track:

```bach
@Tempo = 130

!Play [
  16 -> {
    Scale('E mixolydian')
    Chord('E')
  }

  1 -> Chord('A')
  1/2 -> Chord('C')
  1/2 -> Chord('B7')
  4 -> Chord('E')

  1 -> Chord('A')
  1/2 -> Chord('C')
  1/2 -> Chord('B7')
  4 -> Chord('E')
]
```

Note how one part in the list is repeated:

```bach
1 -> Chord('A')
1/2 -> Chord('C')
1/2 -> Chord('B7')
4 -> Chord('E')
```

The natural instinct is to assign this part to a variable as its own list.

This would reduce duplication in the track quite a bit:

```bach
@Tempo = 130

:Part = [
  1 -> Chord('A')
  1/2 -> Chord('C')
  1/2 -> Chord('B7')
  4 -> Chord('E')
]

!Play [
  16 -> {
    Scale('E mixolydian')
    Chord('E')
  }

  :Part
  :Part
]
```

The issue is that, at least right now, `bach` will interpret this as a list nested in a list, and this is not supported.

The ideal behavior would be for `bach` to sort of merge `:Part` into the main list, that way `bach` only deals with one list and avoids violating the nesting constraints.

This lacking feature is especially problematic for complex or long tracks that repeat not only the same musical elements but also the same phrases or parts several times over.

In the near future `bach` will elegantly handle this situation, but for now this must be noted as a limitation to work around.

> If you are a programmer who is interested in contributing to `bach` and solving this problem, please check out the [Contribute](/contribute) page.

### Play

The final but arguably most important element in `bach` is `!Play`.
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

## Authoring

Now that you are familiar with the fundamentals, we can begin putting `bach` to practical use by authoring some tracks.

Regardless of your level of familiarity or expertise, the ideal way to write `bach` tracks is to always start off with a similar example.

It's much better to start off with a block of marble and carve out a sculpture than to slowly build up a sculpture piece by piece.

That is why we provide an open-source collection of [examples tracks](#examples) for you to copy and modify to your liking.

But before we can even begin to make use of these examples (let alone change or build upon them), we must become familiar with the tools available to us.

### Tooling

### Audio

You have a `bach` track written, so how do we associate and synchronize it with audio?

The first thing to note is that, in order to keep `bach` simple and focused, `bach` doesn't explicitly concern itself with audio data.

By taking this approach it allows `bach` to rhythmically align with music produced by a human or to generate music on a computer.

On a practical level, this means it's up to your editor or application to associate audio data with your `bach` tracks.

## Examples

### Basic

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

### Advanced

#### Compound meters

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

#### Mixed meters

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


## Limitations

 - No native semantics around instruments
 - No local repeater symbols, all tracks are currently considered loopable by default

## Help
