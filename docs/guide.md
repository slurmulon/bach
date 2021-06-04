# Guide

## Introduction

The purpose of this guide is to help people learn `bach` well enough to put it to practical use.

It is primarily written for musicians and assumes as little as possible about their technical expertise, although techical terms and details are sometimes required to ensure a comprehensive understanding of the notation.

Naturally, it also assumes that the reader has at least a basic understanding of music theory concepts such as tempo, meter, beats and measures.

As you read this guide you will encounter various examples of `bach`, sometimes partial and focused snippets and other times complete examples.

Whenever you are faced with new concepts and terms that feel overlooked or unclear, try to ignore them in that moment.

This guide is organized so that it will progressively fill in those details for you, in a way that (we feel) best gives you a hollistic understanding of `bach`.

You can find a collection of useful example tracks in the [Examples](#examples) section, but the majority of this guide focuses on a single example track (in the [Tracks](#tracks) section) to increase clarity.

> If you are looking for a more technical and low-level resource on `bach`, head to the [Syntax](/syntax) page instead.

## Editor

An open-source [`bach-editor`](https://github.com/slurmulon/bach-editor) is publically available over the web at **https://editor.codebach.tech**.

You can, and should, use this tool to run example `bach` tracks that you encounter in the guide.

The editor allows you to both hear and visualize the `bach` track, manage collections and access useful information about your track.

It also provides some basic usability settings to enhance and customize your experience.

### Usage

To run an example track in the editor, simply copy the code (or hover over it and click the "Copy to clipboard" button), then replace the contents of the code in your editor, directly under the "Code" tab.

Then simply click the blue play button in the bottom right. You should hear piano keys being played as your screen screen scrolls with the music (optional).

If you run into any problems with the editor, please be sure to open up an [issue on GitHub](https://github.com/slurmulon/bach-editor/issues) with a detailed description of your problem and reproduction steps.

### Limits

It's a new tool and currently only supports playback via piano (limited to the second octave for now, due to complexities acquiring a full open-source collection of sampled instruments keys).

The tool may eventually support multiple instruments, but long-term it will remain as minimal as possible to reduce overall complexity and maintenance overhead.

It also stores all data locally using browser storage due to hosting costs, so be sure to use the archive feature to create backups of your track collections.

> :warning: If you're using a private browser and close the window or end the session, you will lose your tracks unless you already created an archive!

## Components

As with learning anything new, the ideal place to start is the fundamentals. There are several different components of `bach` that you need to be knowledgeable in before you can start using it.

This guide provides as many details and examples as possible to help ensure that common questions, concerns and caveats are thoroughly addressed.

It's advised (but not required) that you read each component's section in the order it appears, only moving on to the next section after you've obtained a decent grasp on the current section's concepts.

With that said we can now dive into each of the components that make up `bach`, starting at the surface with "tracks".

### Tracks

Tracks can represent a loop, a song, or really any sort of rhythmic timeline.

Tracks are the highest-level concept in `bach`, so it's important to understand them on a macroscopic level before diving into other areas.

We will begin by looking at a real-world example of a track, and this track will be referenced throughout this guide.

#### Example

The following track represents the chord progression of a soul song.

```bach
@meter = 4|4
@tempo = 175

:B = Chord('Bm')
:E = Chord('Em')
:F = Chord('F#m7')

play! [
  2 -> {
    Scale('B minor')
    :B
  }
  1 -> :E
  1 -> :B
  1 -> :F
  1 -> :B
]
```

Putting special characters aside, we can see that several musical elements are clearly represented:

 - **Meter**: `4|4` or common time (default)
 - **Tempo**: `44` beats per minute
 - **Chord**: `Bm`, `Em` and `F#m7`
 - **Scale**: `B minor`
 - **Durations**: `2`, `1`

Once this track is processed and loaded into an app, it will be interpreted like so:

1. The scale `B minor` and the chord `:B`, or `Bm`, will be played for `2` whole notes, then
1. The chord `:E`, or `Em`, will be played for `1` whole note, then
1. The chord `:B`, or `Bm`, will be played for `1` whole note, then
1. The chord `:F`, or `F#m7`, will be played for `1` whole notes, then
1. The chord `:B`, or `Bm`, will be played for `1` whole notes, then
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

A double is a [rational number](https://en.wikipedia.org/wiki/Rational_number), a number that includes a decimal.

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
@meter = 3|4
@tempo = 110
```

`bach` allows you to define any headers you like, but `@meter` and `@tempo` are special and reserved since they influence how `bach` calculates beat durations.

You can find a list of useful headers in the [Syntax](/syntax#useful) document, but be aware that supporting these headers involves customizing your `bach` interpreter, a task that requires external code changes.

In other words, although you are free to make up your own headers, you need someone who knows how to code in order to add support for them.

#### Meter

The meter header of a track defines its time signature, which specifies two important pieces of information: the number of beats in a measure and the beat unit.

The following meter tells `bach` that the track has 5 beats in a measure and that an 1/8th note is the beat unit, or the value of an individual beat.

```bach
@meter = 5|8
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
 - `Rest` (`_`)

#### Arguments

Elements can be provided with custom data (within parenthesis) called arguments.

The first argument determines the **value** of the element and must always be provided.

In this example we are defining a chord with a value of Bm7:

```bach
Chord('Bm7')
```

_Note how the `Bm7` is surrounded by quotes, specifying this data as a string._

If you wanted to associate more information with this chord, such as which voicing the chord is in, you can simply add another argument, where each argument beyond the first is separated by a comma:

```bach
Chord('Bm7', voicing: 2, triad: true)
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

```bach
1/2 -> Scale('C minor')
```

But a beat by itself isn't too useful. Rarely (if ever) does a song only contain a single note, chord or scale.

### Lists

Collections are what you use to group beats and/or elements together.

They let you say "play this, then this" and so on. In `bach` this pattern is called a "rhythmic timeline".

Lists are simply an ordered collection of beats to play.

This means that each beat in the list will be played in the order it is defined, either from left to right or top to bottom, depending on how you write it.

Looked at another way, beats in a list will be played sequentially, for their entire duration, and will **not** overlap with each other (just like a queue).

So this (read left to right):

```bach
[1/2 -> Chord('A'), 1/2 -> Chord('B'), 1 -> Chord('C')]
```

is the same as this (read top to bottom):

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

### Sets

Sometimes you need to play multiple elements at once in a beat.

For instance, you may need to tell an app to show both a scale and a chord at the same time.

Sets allow you to do this because they are a collection that's agnostic to order.

With a set all you're saying is "group these elements together". Unlike lists, sets are not concerned with durations and have no influence on rhythm.

Sets are defined the same way as lists with one key difference: they use curly braces (`{` and `}`) instead of brackets.

```bach
{ Scale('B minor'), Chord('Bm') }
```

In the example from the [Tracks](#tracks) section, both a scale and chord will played on the first beat for 2 whole notes.

```bach
2 -> {
  Scale('B minor')
  Chord('Bm')
}
```

### Loops

Music naturally has sections that repeat. They can repeat identically or with variance based
on some condition (e.g. codas in sheet music).

`bach` supports this use case, and much more, with loops.

In the most basic sense, loops allow you to say "play this collection X times".

Loops are defined using `of`, with the number of iterations/repeats specified on the left-hand side and the collection to loop on the right:

```bach
4 of [
  2 + 1/2 -> Chord('A7')
  1 + 1/2 -> Chord('Em')
]
```

### Whens

Loops in `bach` are powerful and expressive because they give you control over a large number of conditions.

They do not limit you to simply saying "repeat this X times."

You can also say things like "repeat 4 times, but on the final repetition play this here instead."

This is achieved by using `when`, which must be defined inside of a loop.

`when` is followed by a condition (more on this next), `do`, and a set or list to play whenever the condition matches the loop's current repeat/iteration.

```bach
4 of [
  1 -> Chord('Em9')
  when { 1 3 } do { 1 -> Chord('C') }
  when { 2 4 } do { 1 -> Chord('C/D') }
]
```

The example above loops over a list four times. On every loop/iteration it will play `Chord('Em9')` on the first beat.

After this beat we have a `when` followed by another.
Just like anything else in a list, each `when` is processed sequentially (i.e. after the previous beat and before the next beat).

But unlike everything else that can be used in a list, a `when` will
only play if its condition matches the loop's current iteration.

Walking through the track sequentially helps to illustrate this logic:

- Loop 1
  * Beat 1: `Em9`
  * Beat 2: `C`
- Loop 2
  - Beat 1: `Em9`
  - Beat 2: `C/D`
- Loop 3
  - Beat 1: `Em9`
  - Beat 2: `C`
- Loop 4
  - Beat 1: `Em9`
  - Beat 2: `C/D`

Notice how each loop iteration alternates between the chords `C` and `C/D` on the second beat.

The condition of our first `when` is `{ 1 3 }`, which says "do this on iterations 1 or 3".
The condition of our second `when` has the same logic but instead applies to iterations 2 and 4.

By using some of `bach`'s additional `when` conditions, the previous example can be simplified further:

```bach
4 of [
  1 -> Chord('Em9')
  when odd? do { Chord('C') }
  when even? do { Chord('C/D') }
]
```

We achieve the exact same result, but with two benefits by using `odd?` and `even?`:
 - They are semantic and easier to read and understand
 - They do not need to be updated if we change the number of loop iterations (say, from 4 to 8)

Now that we understand `when` conditions in a basic sense, we will now cover all of the supported conditions.

#### Conditions

All `when` conditions are applied to the current repeat/iteration of the closest parent loop (during parsing, playback, etc.).

**Integer**

If a condition is an integer (e.g. 1, 5, 12) then it will only match on that iteration.

**Not**

If a condition is prefixed with `!` it will be negated.

Instead of saying "if the iteration matches this condition", you're saying "if the iteration does NOT match this condition".

This example loops over a list 4 times but only plays `Chord('B')` on iterations 1, 2, and 4 (in other words, "not 3").

```bach
4 of [
  1 -> Chord('A')
  when !3 do { 1 -> Chord('B') }
  1 -> Chord('C')
]
```

This results in the following playback sequence:

```
A B C
A B C
A B 
A B C
```

**Comparison**

Several keywords are supported for common comparisons.

 - `gt? 1`
   * Match iterations greater than 1
 - `gte? 1`
   * Match iterations greater than or equal to 1
 - `lt? 4`
   * Match iterations less than 4
 - `lte? 4`
   * Match iterations less than or equal to 4
 - `factor? 4`
   * Match iterations that are divisible by 4 (i.e. every 4th iteration)
 - `even?`
   * Match iterations that are even (i.e. divisible by 2, same as `factor? 2`)
 - `odd?`
   * Match iterations that are odd (i.e. not divisible by 2)
 - `first?`
   * Match the first iteration (semantic alias for `1`)
 - `last?`
   * Match the last iteration
 - `2..4`
   * Match any iteration between 2 and 4 (inclusive)

**Any**

If a condition is in curly braces (`{ }`), it will match if **any** conditions nested in the braces match the current iteration.

- `when { 1 3 } do`
  * Match if the iteration is 1 or 3
- `when !{ 1 4 } do`
  * Match iterations other than 1 and 4
- `when { odd? last? } do`
  * Match odd iterations and the last iteration

**All**

If a condition is in brackets (`[ ]`), it will match if **all** of the conditions nested in the brackets match the current iteration.

- `when [factor? 3 gte? 6] do`
  * Match iterations that are a factor of 3 and greater than or equal to 6
- `when [even? !{ 2 6 }] do`
  * Match iterations that are even but not 2 or 6

### Nesting

Nesting, in the most general sense, is whenever you have one thing that contains another. You can think of it as a way to describe the general pattern of a hierarchy.

For instance, reflecting back to the example found in the [Tracks](#tracks) section, the first beat of the list contains a set, which contains both a scale and a chord. Both the scale and the chord are nested within the set.

You could also take things further in each direction.
The individual notes composing the scale and chord are nested within them.
You can also say that the set is nested within the list, or even that the list is nested within the track.

But when we talk about nesting in `bach` it can be assumed that we are referring to collection nesting, unless it's stated otherwise.

#### Usage

The most common use case for nesting is when you define a set within a beat in order to group multiple elements together, as in the first beat of the example we've been focusing on.

```bach
play! [
  4 -> {
    Scale('B minor')
    Chord('Bm')
  }
]
```

With the help of some whitespace, we can naturally see that the beat is nested in the list, the set is nested in the beat, and the scale and the chord are nested in the set.

Whenever we can identify a point where nesting occurs in the hierarchy, we call this a "level". The list at the top of the track would be at the first level of nesting, the beats in that list on the second level and so on.

#### Rules

The following rules always apply:

 - Lists and Sets may contain any Collection
 - Beats may only contain Sets of Elements

While these rules apply to the first level of nesting:

 - Loops and Whens may only contain Lists or Sets

These "first level" rules do **not** influence or limit deeper nesting levels.

For example, although loops may only contain lists or sets, _those_ lists/sets
may contain loops:

```bach
:a = [1/2 -> chord('A') 1/2 -> chord('E')]
:b = [1/2 -> chord('B') 1/2 -> chord('G')]

4 of [
  2 of :a
  3 of :b
]
```

#### Variations

These minimal nesting rules empower you with a high degree of flexibility with how you
organize your tracks.

It also encourages people to discover, establish and refine conventions.

Lets explore some of the different ways you could use nesting to represent a track.

Say we have the following track, representing the harmony of a funk song:

```bach
:a = [
  7/8 -> {
    Scale('C# mixolydian')
    Chord('C#')
  }
  9/8 -> Chord('B')
]

:b = [
  1 -> {
    Scale('C# mixolydian')
    Chord('C#')
  }
  1 -> Chord('B')
]

:c = [
  1/2 -> Chord('F#')
  3/8 -> Chord('E')
  9/8 -> Chord('C#')
]

play! [:a :b :b :b :c :c :c :c]
```

There's a couple of new things going on here. We have broken out the individual repeated sections of the track and labeled them as `:a`, `:b`, and `:c` (using [Variables](#variables)).

We then `play!` a list that will play part `:a` 1 time, part `:b` 3 times and part `:c` four times.

Labeling these repeated sections is key here because it allows them to be defined only once but re-used endlessly.

This not only helps keep your tracks succinct and well-organized, but it also makes changes
easier to perform later since you only have to perform them in one place.

Of course, you are not forced to use labels whatsoever and can still produce well-organized and readable tracks using other strategies.

Here is the same track using loops instead of labels:

```bach
play! [
  7/8 -> {
    Scale('C# mixolydian')
    Chord('C#')
  }
  9/8 -> Chord('B')

  3 of [
    1 -> {
      Scale('C# mixolydian')
      Chord('C#')
    }
    1 -> Chord('B')
  ]

  4 of [
    1/2 -> Chord('F#')
    3/8 -> Chord('E')
    9/8 -> Chord('C#')
  ]
]
```

This track, in my opinion, is even easier to read and understand because you don't
have to refer back and forth between the labeled values and `play!` - it's just all in
one convenient place.

The key lesson here is that nesting allows the same track to be written and organized
any number of ways, so you should always be open to experimenting with different
approaches.

With that said, when you're writing tracks with a team of people, it's important
to favor consistency over experimentation.

In this case its best to establish conventions that ensure everybody's productivity
is high. Although they should be consistently applied, these conventions should be
kept open to iteration and change, adapting to whatever your team finds best over time.

Here are some other approaches and styles to consider:
 - Using lists to indicate measures, which is more natural for sheet music readers.

```bach
play! [
  [ 1/2 -> Chord('E') 1/2 -> Chord('C#m') ]
  [ 1/2 -> Chord('E') 1/2 -> Chord('B') ]
]
```

 - Grouping/sectioning your lists by element (e.g. scale, chord, etc.) and then bringing them together as a set

```bach
:scales = [ 1 -> Scale('E aeolian') ]
:chords = [ 3/8 -> Chord('Em9') 5/8 -> Chord('C') ]

play! { :scales :chords }
```

#### Formatting

For each level of nesting it's suggested that you indent the text with two spaces.

This helps keep your tracks visually well organized, and therefore easier to read, understand and change later.

The general consensus is that, although it takes up more visual space, this:

```bach
play! [
  4 -> {
    Scale('B minor')
    Chord('Bm')
  }
]
```

is easier to read and understand than this:

```bach
play! [ 4 -> { Scale('B minor') Chord('Bm') } ]
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
1    = Whole note (or one entire measure when `@meter = 4|4`
1/2  = Half note
1/4  = Quarter note
1/8  = Eighth note
1/16 = Sixteenth note

1/512 = Minimum duration
512   = Maximum duration

2 + (1/2) = Two and a half whole notes
2 * (6/8) = Two measures when the meter is 6|8

1 + (1/2) = One and a half measures when the meter is 4|4
1 - (1/8) = Seven eigth notes
```

`bach` also provides useful aliases for certain durations:

```
bar  = Whole measure based on meter
beat = Single beat based on meter
2n   = Half note
4n   = Quarter note
8n   = Eigth note
16n  = Sixteenth note
32n  = Thirty-second note
64n  = Sixty-fourth note
```

To adhere with music theory, numerical durations are strictly based on **common time** (`@meter = 4|4`).

This means that `1` always means four quarter notes, and only equates with a full measure when the number of beats in a measure is 4 (as in `4|4`, `3|4`, `5|4`, etc.).

The examples in the remainder of this section assume common time, since this is the default when a `@meter` header is not provided.

#### Examples

We have already encountered many examples of durations throughout the guide, so let's now take a more focused look at durations in order to understand them hollistically.

A list playing a `Note('C2')` for an entire measure in common time, starting at the first beat in the measure, would be specified like so:

```bach
[1 -> Note('C2')]
```

If you wanted to start playing the note on the second beat of the measure, then simply rest (`_`) on the first beat:

```bach
[1/4 -> _, 1 -> Note('C2')]
```

When a beat is defined without a duration (in other words, just an element), the duration is implied to be one beat of the meter.

For instance, this, when the meter is common time:

```bach
[1/4 -> Note('C2'), 1/4 -> Note('F2')]
```

is the same as this:

```
[Note('C2'), Note('F2')]
```

Numerical beat durations can also use basic mathematical operators (i.e. add, subtract, multiply, divide).

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
@meter = 6|8

play! [
  6/8 -> Chord('Dmin')
  6/8 -> Chord('Dmin/F')
  6/8 -> Chord('E7b9')
  6/8 -> Chord('Bb7')
  2 * 6/8 -> Chord('A7')
]
```

It's worth noting how the last chord, `A7`, is played for two measures (via `2 * 6/8`).

If we were using the `4|4` meter, we would just say `2` instead since all durations are based on common time.

Using duration aliases, we can simplify the track even further:

```
@meter = 6|8

play! [
  bar -> Chord('Dmin')
  bar -> Chord('Dmin/F')
  bar -> Chord('E7b9')
  bar -> Chord('Bb7')
  2 * bar -> Chord('A7')
]
```

This is also less affected by changes to the meter, since `bar` always means 1 measure.

### Variables

More often than not you will need to play the same element multiple times in a track.

If you've followed the guide linearly then you have already encountered this concept and have seen variables used.

Variables allow you to assign a name to a value and then reference that value later by the variable's name.

This helps to reduce duplication and human error and makes changing the track much easier later on.

Variables are declared using the `:` character, immediatelly followed by a unique name, a `=` character, and then a value (a primitive, element, collection, etc.).

```bach
:Bm = Chord('Bm')
```

Once a variable is declared it may be referenced and used in any area of the track following it.

This means that a variable **must** be declared before it can be used elsewhere.

The recommended convention is to declare your variables immediately after your headers, as it helps people quickly determine all of the elements used in the track.

#### Example

Our primary example track already uses variables, so let's compare and see what it would look like without them.

Here is how the track looks as-is, with variables.

```bach
@meter = 4|4
@tempo = 44

:B = Chord('Bm')
:E = Chord('Em')
:F = Chord('F#m7')

play! [
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
@meter = 4|4
@tempo = 44

play! [
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

### Play

The final but arguably most important element in `bach` is `play!`.
The reason it's so important is because it specifies the main entrypoint of a track.

In other words, `bach` looks for the `play!` element first and foremost, then it processes everything referenced by `play!` from there.

```bach
@tempo = 82

play! [
  8 -> {
    Scale('G mixolydian')
    Chord('G7')
  }
  4 -> Chord('C7')
  4 -> Chord('G7')
]
```

Only one `play!` element is allowed and expected per track, and anything that isn't referenced by the element will be **ignored** during processing.

## Authoring

Now that you are familiar with the fundamentals, we can begin putting `bach` to practical use by authoring some tracks.

Regardless of your level of familiarity or expertise, the ideal way to write `bach` tracks is to always start off with a similar example.

It's much better to start off with a block of marble and carve out a sculpture than to slowly build up a sculpture piece by piece.

That is why we provide an open-source collection of [examples tracks](#examples) for you to copy and modify to your liking.

### Tooling

You have a `bach` track, so how do we associate and synchronize it with real-time audio (in other words, how do we run it)?

If you've followed the guide linearly then you're already familiar with our open-source **[web editor](https://editor.codebach.tech)** and have used it to run example tracks.

If you haven't used the editor yet, we certainly advise that you check it out and run some example tracks. This allows you to see `bach` in action and how everything ultimately comes together.

## Examples

You can find a collection of open-source example tracks in the [Examples](examples) page.

## Limitations

 - New project that is still taking shape and finding its place in the world (so, there's not much tooling, yet :sob:)
 - No interpretation or application of music theory, such as the notes that make up a scale or code (this is the responsibility of [`bach-js`](/dev#bach-js))
 - No official support yet for clef
 - No official semantics around instruments (you can use an `@Instrument` header to indicate this)
 - Converting `bach.json` into `bach` again (i.e. inverse conversion) is not supported yet
