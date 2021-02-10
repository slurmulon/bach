# Collections

Collections are what you use to group beats and/or elements together.

They let you say "play this, then this" and so on. In `bach` this pattern is called a "rhythmic timeline".

There are only two types of collections in `bach`: "lists" and "sets".

## Lists

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

## Sets

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

## Nesting

Nesting, in the most general sense, is whenever you have one thing that contains another. You can think of it as a way to describe the general pattern of a hierarchy.

For instance, reflecting back to the example found in the [Tracks](#tracks) section, the first beat of the list contains a set, which contains both a scale and a chord. Both the scale and the chord are nested within the set.

You could also take things further in each direction.
The individual notes composing the scale and chord are nested within them.
You can also say that the set is nested within the list, or even that the list is nested within the track.

But when we talk about nesting in `bach` it can be assumed that we are referring to collection nesting, unless it's stated otherwise.

### Usage

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

### Rules

In the spirit of keeping `bach` simple and understandable, collection nesting is limited to the following rules:

 - Lists may contain sets
 - Lists may **not** contain other lists
 - Sets may **not** contain sets or lists

As a result, lists **cannot** be nested in another collection at _any_ level.

### Formatting

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
