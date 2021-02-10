# Durations

Durations are at the heart of rhythms, and therefore at the heart of `bach`.

When used in lists they become the mechanism that allows you to define rhythmic timelines.

As you might recall from the [Beats](#beats) section, beats are elements that are paired with a duration.

When beats are nested in a list, each beat is played for its entire duration before moving onto the next.

Therefore the duration not only specifies how long a beat is played for in a list, but it also determines when the next beat in that list is played, since the next beat will only be played _after_ the previous beat reaches the end of its duration.

## Values

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

## Examples

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

If we were using the `4|4` meter, we would just say `2` instead since all durations are based on common time.
