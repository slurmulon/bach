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


