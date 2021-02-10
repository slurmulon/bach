# Headers

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

## Meter

The meter header of a track defines its time signature, which specifies two important pieces of information: the number of beats in a measure and the beat unit.

The following meter tells `bach` that the track has 5 beats in a measure and that an 1/8th note is the beat unit, or the value of an individual beat.

```bach
@Meter = 5|8
```

When a meter header is not specified in your track, `bach` assumes you are using common time (`4|4`).

As of now the meter can only be defined once and cannot change at any point in the track.

## Tempo

The tempo header of a track determines how many beats there are in a minute (otherwise known as "bpm").

When a tempo is not specified, `bach` defaults to `120` bpm.

Tempos can be expressed as either an integer (e.g. `120`) or a decimal (e.g. `110.5`).

```bach
@Meter = 110
```

As with meter, the tempo can only be defined once, and, at least right now, cannot change at a later point.
